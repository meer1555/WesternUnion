# Deploying to EC2 manually (no Docker) — Ubuntu, port 8080

Installs JDK 21, Maven, and MySQL directly on the EC2 host, builds the jar,
and runs it as a systemd service so it survives reboots and crashes.

## 1. Open the port in your Security Group

AWS Console → **EC2 → your instance → Security → Security Groups →
Edit inbound rules → Add rule**

| Type       | Protocol | Port range | Source                |
|------------|----------|------------|------------------------|
| Custom TCP | TCP      | 8080       | 0.0.0.0/0 (or your IP) |
| SSH        | TCP      | 22         | Your IP (already set)  |

## 2. SSH into the instance

```bash
ssh -i your-key.pem ubuntu@<your-ec2-public-ip>
```

## 3. Install JDK 21 and Maven

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk maven

java -version    # should show 21.x
mvn -version
```

## 4. Install and secure MySQL

```bash
sudo apt install -y mysql-server
sudo systemctl enable --now mysql

# Set a root password and lock down defaults
sudo mysql_secure_installation
```

Answer its prompts (set a strong root password, remove anonymous users,
disallow remote root login, remove test database, reload privileges — yes
to all is fine for a single-app server).

Then create the database and a dedicated app user (don't run the app as
MySQL root):

```bash
sudo mysql -u root -p
```

```sql
CREATE DATABASE westernunion_bank CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'wuapp'@'localhost' IDENTIFIED BY 'ChangeThisToAStrongPassword!';
GRANT ALL PRIVILEGES ON westernunion_bank.* TO 'wuapp'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

## 5. Get the project onto the server

From your **own machine** (not the EC2 box):

```bash
scp -i your-key.pem -r westernunion-bank ubuntu@<your-ec2-public-ip>:~/westernunion-bank
```

(Or `git clone` it on the EC2 box if you've pushed it to a repo.)

## 6. Configure the app for production

```bash
cd ~/westernunion-bank
nano src/main/resources/application.properties
```

Update these lines to match step 4:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/westernunion_bank?useSSL=false&serverTimezone=UTC
spring.datasource.username=wuapp
spring.datasource.password=ChangeThisToAStrongPassword!

app.jwt.secret=<a long random string — generate one below>
```

Generate a strong JWT secret:

```bash
openssl rand -base64 48
```

Paste that as `app.jwt.secret`. Save and exit (`Ctrl+O`, `Enter`, `Ctrl+X`
in nano).

## 7. Build the jar

```bash
mvn clean package -DskipTests
ls target/westernunion-bank.jar   # confirm it was built
```

## 8. Quick manual test (optional but recommended)

```bash
java -jar target/westernunion-bank.jar
```

Visit `http://<your-ec2-public-ip>:8080` in a browser. If the landing page
loads, `Ctrl+C` to stop it and move on to running it as a proper service.

## 9. Install it as a systemd service (auto-starts, auto-restarts)

Move the jar somewhere permanent and create a dedicated service user:

```bash
sudo mkdir -p /opt/westernunion-bank
sudo cp target/westernunion-bank.jar /opt/westernunion-bank/
sudo useradd -r -s /bin/false wubank
sudo chown -R wubank:wubank /opt/westernunion-bank
```

Create the unit file:

```bash
sudo nano /etc/systemd/system/westernunion-bank.service
```

Paste in:

```ini
[Unit]
Description=Western Union Bank Spring Boot App
After=network.target mysql.service
Requires=mysql.service

[Service]
User=wubank
Group=wubank
WorkingDirectory=/opt/westernunion-bank
ExecStart=/usr/bin/java -jar /opt/westernunion-bank/westernunion-bank.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10
# Optional: cap memory so it can't take down a small instance
# ExecStart=/usr/bin/java -Xmx512m -jar /opt/westernunion-bank/westernunion-bank.jar

[Install]
WantedBy=multi-user.target
```

Save and exit, then enable and start it:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now westernunion-bank
sudo systemctl status westernunion-bank
```

## 10. Visit the app

```
http://<your-ec2-public-ip>:8080
```

## 11. Common operations

```bash
# Tail logs
sudo journalctl -u westernunion-bank -f

# Restart after deploying new code
cd ~/westernunion-bank
git pull                       # or re-scp updated files
mvn clean package -DskipTests
sudo cp target/westernunion-bank.jar /opt/westernunion-bank/
sudo systemctl restart westernunion-bank

# Stop / start
sudo systemctl stop westernunion-bank
sudo systemctl start westernunion-bank

# Check it's enabled on boot
sudo systemctl is-enabled westernunion-bank
```

## 12. Notes

- Since MySQL is bound to `localhost` by default, it isn't reachable from
  the internet — no extra security group rule needed for it. Leave it
  that way unless you specifically need remote DB access.
- `mvn clean package -DskipTests` needs to reach Maven Central to download
  dependencies the first time — make sure outbound internet access is
  allowed from the instance (default for most EC2 setups).
- This is a demo/educational app; see the "hardening" notes in
  `DEPLOYMENT.md` (the Docker guide) for things worth adding before
  treating it as production banking software.
