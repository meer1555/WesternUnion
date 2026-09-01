# Deploying to EC2 (Ubuntu + Docker, port 8080)

This walks through getting the app running on a fresh Ubuntu EC2 instance,
reachable at `http://<your-ec2-public-ip>:8080`.

## 1. Open the port in your Security Group

In the AWS Console: **EC2 → your instance → Security → Security Groups →
Edit inbound rules → Add rule**

| Type       | Protocol | Port range | Source                  |
|------------|----------|------------|--------------------------|
| Custom TCP | TCP      | 8080       | 0.0.0.0/0 (or your IP)   |
| SSH        | TCP      | 22         | Your IP (already set)    |

## 2. SSH into the instance

```bash
ssh -i your-key.pem ubuntu@<your-ec2-public-ip>
```

## 3. Install Docker + Docker Compose

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg

sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Run docker without sudo (log out/in once after this)
sudo usermod -aG docker $USER
newgrp docker

docker --version
docker compose version
```

## 4. Get the project onto the server

From your own machine, copy the project up (run this locally, not on the
EC2 box):

```bash
scp -i your-key.pem -r westernunion-bank ubuntu@<your-ec2-public-ip>:~/westernunion-bank
```

(Or, if you push this project to a git repo, just `git clone` it on the
EC2 box instead.)

## 5. Configure secrets

Back on the EC2 box:

```bash
cd ~/westernunion-bank
cp .env.example .env
nano .env       # set MYSQL_ROOT_PASSWORD and APP_JWT_SECRET to strong random values
```

Generate strong values quickly with:

```bash
openssl rand -base64 32
```

## 6. Build and start

```bash
docker compose up -d --build
```

This builds the app image (JDK 21 + Maven build inside the container, so
you don't need Java/Maven installed on the host at all), starts MySQL,
waits for it to be healthy, then starts the app.

Check it's up:

```bash
docker compose ps
docker compose logs -f app
```

## 7. Visit the app

```
http://<your-ec2-public-ip>:8080
```

## 8. Common operations

```bash
# View logs
docker compose logs -f app
docker compose logs -f mysql

# Restart after pulling new code
git pull            # or re-scp your updated files
docker compose up -d --build

# Stop everything
docker compose down

# Stop and WIPE the database too (careful — deletes all accounts/data)
docker compose down -v
```

## 9. Notes / hardening for a real deployment

- **This app is a demo/educational project.** Before treating it as
  production banking software, add things like rate limiting, audit
  logging, 2FA, and a real secrets manager (AWS Secrets Manager / SSM
  Parameter Store instead of a plaintext `.env` file).
- MySQL's port is **not** published to the host in `docker-compose.yml` by
  default — only the `app` container can reach it. Leave it that way
  unless you specifically need external DB access.
- `APP_JWT_SECRET` and `MYSQL_ROOT_PASSWORD` in `.env` are never committed
  to git — keep `.env` out of version control (already covered by
  `.dockerignore`; add `.env` to `.gitignore` too if you set up a repo).
- If you later want HTTPS with a domain instead of the bare IP:8080, add
  an Nginx reverse proxy + Certbot in front of the app container — happy
  to set that up if/when you're ready for it.
- Consider putting the EC2 instance behind an Elastic IP so the address
  doesn't change on reboot.
