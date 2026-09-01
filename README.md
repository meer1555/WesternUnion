# Western Union Bank — Online Banking App

A full-stack demo banking application: **Java 21 + Spring Boot 3** backend
with **MySQL**, and a premium, animated **HTML/CSS/JS** frontend. Sign up,
get an instant bank account number, then deposit, withdraw, and transfer
funds between accounts.

> **Note:** This is an independent educational/demo project named "Western
> Union Bank" for illustration purposes only. It is not affiliated with,
> endorsed by, or connected to The Western Union Company. Do not deploy it
> publicly under this name or present it as a real financial service.

---

## 1. Requirements

- **JDK 21+**
- **Maven 3.9+**
- **MySQL 8+** running locally (or update `application.properties` to point
  elsewhere)

## 2. Database setup

The app will auto-create the `westernunion_bank` database and tables on
first run (`spring.jpa.hibernate.ddl-auto=update` +
`createDatabaseIfNotExist=true`). You only need a MySQL user with rights to
create databases, e.g.:

```sql
CREATE USER 'root'@'localhost' IDENTIFIED BY 'root';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'localhost';
```

Then edit `src/main/resources/application.properties` with your real
credentials:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/westernunion_bank?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=root
```

A reference `schema.sql` (manual DDL) is also included under
`src/main/resources/schema.sql` if you'd rather provision the schema
yourself and switch `ddl-auto` to `validate` or `none`.

## 3. Run the app

```bash
mvn spring-boot:run
```

Or build a jar and run it directly:

```bash
mvn clean package
java -jar target/westernunion-bank.jar
```

The app starts on **http://localhost:8080**. Open that URL in your browser
— the static frontend is served straight from the Spring Boot app (no
separate frontend server needed).

## 4. Using the app

1. Go to `/signup.html` and create an account (name, email, phone,
   password). You're immediately issued a unique account number
   (e.g. `WU384920175610`) and logged in.
2. On the dashboard you can:
   - **Deposit** money into your account
   - **Withdraw** money from your account
   - **Transfer** money to any other account number (type the recipient's
     account number and the UI will confirm their name before you send)
   - View your **transaction history**, updated live
3. Log out and log back in any time from `/login.html`.

## 5. API overview

All endpoints are under `/api`. Authenticated endpoints expect
`Authorization: Bearer <token>` (the token returned by signup/login).

| Method | Endpoint                        | Description                          |
|--------|----------------------------------|---------------------------------------|
| POST   | `/api/auth/signup`               | Create user + auto-create account     |
| POST   | `/api/auth/login`                | Log in, returns JWT                   |
| GET    | `/api/account/me`                | Current user's account (auth)         |
| POST   | `/api/account/deposit`           | `{ amount, description? }` (auth)     |
| POST   | `/api/account/withdraw`          | `{ amount, description? }` (auth)     |
| POST   | `/api/account/transfer`          | `{ toAccountNumber, amount, description? }` (auth) |
| GET    | `/api/account/transactions`      | Transaction history (auth)            |
| GET    | `/api/account/lookup/{acctNum}`  | Public: masked recipient name lookup  |

## 6. Security notes

- Passwords are hashed with BCrypt.
- Auth uses stateless JWT (HS256), configured in `application.properties`
  (`app.jwt.secret`, `app.jwt.expiration-ms`). **Change the secret before
  any real deployment.**
- Deposit/withdraw/transfer all run inside a `@Transactional` boundary with
  pessimistic row locks on the account(s) involved, to keep balances
  consistent under concurrent requests. Transfers lock both accounts in a
  deterministic order to avoid deadlocks.

## 7. Frontend / premium UI

The frontend lives entirely under `src/main/resources/static/`:

- `index.html` — animated landing page (particle canvas background,
  gradient "aurora" blobs, hero video/photo panel, floating balance card)
- `login.html` / `signup.html` — split-screen auth pages with a
  full-bleed video/photo panel and glassmorphism form card
- `dashboard.html` + `js/dashboard.js` — balance card with animated
  counters, deposit/withdraw/transfer modals, live transaction feed
- `css/style.css` — all animations, glassmorphism, gradients, and
  responsive layout
- `js/particles.js` — dependency-free canvas particle/constellation effect

**Bring your own 4K media:** drop video files into
`src/main/resources/static/videos/` (`hero-banking.mp4`,
`auth-banking.mp4`) and/or swap the Unsplash placeholder image URLs in the
HTML for your own — see the `README.txt` files in `static/videos/` and
`static/images/` for free, no-attribution stock sources. Until you do, the
pages fall back to a high-quality static photo so the UI still looks
polished out of the box.

## 8. Project structure

```
westernunion-bank/
├── pom.xml
├── schema.sql (reference copy also under src/main/resources)
└── src/main/java/com/westernunion/bank/
    ├── BankApplication.java
    ├── config/SecurityConfig.java
    ├── controller/ (AuthController, AccountController)
    ├── service/ (AuthService, AccountService, AccountNumberGenerator)
    ├── security/ (JwtService, JwtAuthFilter, CustomUserDetailsService)
    ├── model/ (User, Account, Transaction)
    ├── repository/ (JPA repositories)
    ├── dto/ (request/response objects)
    └── exception/ (BankException, GlobalExceptionHandler)
└── src/main/resources/
    ├── application.properties
    ├── schema.sql
    └── static/ (index.html, login.html, signup.html, dashboard.html, css/, js/, images/, videos/)
```

## 9. Extending it

Ideas if you want to keep building:
- Email/OTP verification on signup
- Scheduled interest accrual for savings accounts
- Admin panel to view all accounts/transactions
- Rate limiting on login attempts
- Dockerfile + docker-compose (app + MySQL) for one-command startup
