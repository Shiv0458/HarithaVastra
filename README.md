# Haritha Vastra

A responsive React storefront with a Spring Boot / Java backend and MySQL persistence. MySQL Workbench is the database administration client, not the database server.

## Start here

The simplest local setup uses **Docker Desktop with Docker Compose**. You can also run Java and Vite directly.

1. Extract this archive and open a terminal in `haritha-vastra`.
2. Copy `.env.example` to `.env` (`cp .env.example .env` on macOS/Linux; `Copy-Item .env.example .env` in PowerShell).
3. Replace `DB_PASSWORD` and `MYSQL_ROOT_PASSWORD` with different strong local passwords.
4. Set `ADMIN_EMAIL` and `ADMIN_PASSWORD` (12–72 characters) if you want an administrator. There is no shared/default admin password.
5. Leave Razorpay fields empty to browse, register, edit inventory, use carts and wishlists without payments. Add **test-mode** credentials to enable test checkout.
6. Run `docker compose up --build -d`.
7. Open **http://localhost:3000**. Sign in at `/account`; an admin account sees the dashboard link. Backend API: **http://localhost:8080/api/products**.
8. View logs with `docker compose logs -f backend`. Stop containers with `docker compose down` (data is retained). Do not use `down -v` unless deliberately deleting your local database and uploads.

On the first demo-profile launch, Flyway creates the schema and loads four fictional products. Images are AI-generated demonstration photographs. **No customer reviews, silk certifications, business history or zari purity claims are seeded.** The demo page explicitly identifies this data. Product attributes omitted from inventory are omitted from the product page.

## Architecture

`Browser → React/Vite → Spring REST + Security → services/transactions → JPA → MySQL`

`Spring PaymentGateway → Razorpay HTTPS API`; authenticated Razorpay webhooks come back to Spring. The frontend never holds a gateway secret. Spring issues opaque expiring bearer sessions; only SHA-256 hashes of tokens are stored. Passwords use BCrypt. Session tokens are held in sessionStorage; guest cart tokens in localStorage. No cookie authentication is used, so CSRF cookies are not required for this API design. Protect against XSS and apply a deployment CSP.

CSS uses a small shared token-based design system instead of Tailwind: ivory, maroon, muted gold, consistent spacing, reusable controls and responsive grids. Heading font is Cormorant Garamond, body font DM Sans, with readable local fallbacks if Google Fonts is unavailable.

```text
haritha-vastra/
  compose.yaml                  MySQL + backend + Nginx frontend
  .env.example                  Server/database/payment examples
  backend/
    pom.xml                     Java 17 / Spring Boot 3.5.7 / Maven
    src/main/java/com/harithavastra/
      config/                   Security, CORS, errors, uploads, admin bootstrap
      controller/               REST API and authorization boundaries
      dto/                      Validated request records
      model/                    Persistent entities
      repository/               Spring Data repositories and locking queries
      service/                  Catalog, carts, accounts, totals, inventory, payments
    src/main/resources/
      application.yml
      db/migration/V1__schema.sql
      db/demo/V2__demonstration_catalog.sql
    src/test/                   Unit, integration and optional real-MySQL tests
  frontend/
    src/                        React routes, shared UI, API and state
    public/images/              Four optimized demo photographs
    nginx.conf                  SPA routing and reverse proxy
  docs/
    API.md                      Endpoint and request reference
    PAYMENTS.md                 Test mode, callbacks, webhook setup, live prerequisites
    OPERATIONS.md               Security, inventory and deployment considerations
    VERIFICATION.md             Tests performed and verification limits
    ASSETS.md                   Demo image provenance and prompts
```

## Run directly in an IDE

Prerequisites: Java **17+**, Maven **3.9+**, Node.js **22 LTS**, MySQL **8.4**. Import `backend/pom.xml` into IntelliJ. Open `frontend` in your preferred editor.

### Database / MySQL Workbench

Start MySQL locally, or run only the Compose database:

```sh
docker compose up -d db
```

Workbench connection: host `127.0.0.1`, port `3306`, user `root`, password from `8555949422@Ss`, schema `haritha_vastra`. If port 3306 is already occupied, use your existing server or change the published port and `DB_URL` consistently. Compose provisions the database/user; Flyway creates tables when Spring starts.

If using an existing MySQL server, create the schema and a dedicated database user through Workbench. Give that user schema-level permissions needed by Flyway, not global administrator privileges. Use a separate migration account with DDL rights in production. Do not run Hibernate schema auto-update; the application validates Flyway's schema.

### Backend

Set environment variables from `.env` in the IntelliJ run configuration. Spring does not automatically load a root `.env` file when launched outside Compose. On macOS/Linux, with the provided simple dotenv format and safely quoted values:

```sh
set -a
. ./.env
set +a
cd backend
mvn spring-boot:run
```

On Windows, set `DB_PASSWORD`, `DB_USER`, `DB_URL`, `SPRING_PROFILES_ACTIVE=demo`, `ADMIN_EMAIL` and `ADMIN_PASSWORD` in IntelliJ's run configuration or your PowerShell session. Payment fields remain optional until checkout.

### Frontend

```sh
cd frontend
npm ci
npm run dev
```

Open **http://localhost:5173**. Vite proxies `/api` and `/uploads` to Spring on port 8080. For a separately hosted backend, set `VITE_API_BASE=https://your-api-host` before building and allow your exact frontend origin with `ALLOWED_ORIGINS`.

### Tests and builds

```sh
cd backend
mvn test
mvn package
```

```sh
cd frontend
npm ci
npm test
npm run build
```

`MySqlMigrationTest` uses Testcontainers and automatically skips without Docker. The other integration tests use H2 in MySQL compatibility mode, actual Spring Security, Flyway and JPA, plus a mocked payment gateway. Real Razorpay credentials are never required by tests. See `docs/VERIFICATION.md` for what was actually run in the build environment.

## Shopping and inventory behaviour

- Search covers name, collection, colour and description keywords. Collection/occasion/colour/price/availability filters are executed on the backend with pagination. “Relevance” uses matching results ordered newest-first, not an invented machine-learning rank. New arrivals means the last 30 days.
- Guest carts/wishlists persist with a random browser bearer token. Login merges wishlist sets and uses the larger quantity per product (capped by stock and 10), so retried login does not double quantities. Logging out rotates the guest token on that browser.
- Reviews require a delivered, paid purchase. No ratings are shown as customer feedback without stored reviews.
- Checkout locks the cart and product rows in product-id order, recomputes all totals, snapshots the order and reserves stock for 30 minutes. A scheduled job checks gateway status before releasing expired inventory. A gateway outage retains reservations until reconciliation succeeds.
- Duplicate checkout keys return the saved order. Duplicate callbacks do not reserve/deduct stock again. An order only becomes `PAID` after a valid signature plus a server fetch that reports the expected order, amount, INR currency and captured state.
- Orders, gateway attempts and fulfilment are separate records/states. Late captures after release reacquire available stock atomically; insufficient stock produces `PAID_REVIEW`, which blocks fulfilment and requires operator review/refund.
- Archived products are removed from the storefront; purchased product names and prices remain in order snapshots.
- Shipping and tax defaults are zero **configuration examples**. Have your business/tax advisor supply correct pricing, tax treatment and shipping rules before selling. Current tax logic applies one configured rate to merchandise after discounts, excluding shipping.

## Production setup still required

See `docs/OPERATIONS.md` for the full list. At minimum: replace all demo products/images, add verified specifications and stock, approve contact/policy pages, configure shipping/tax, deploy Java/MySQL on appropriate infrastructure with HTTPS, enable account protection/rate limits and backups, configure Razorpay account/live keys/capture/webhooks, test real provider flows and run MySQL + browser acceptance tests. Do not present the demo catalogue as genuine stock.

This project is a runnable implementation, not a claim of an audited production deployment. No external site was deployed: the requested Java/MySQL server needs a JVM host, whereas the available Sites hosting runs JavaScript Workers.

After a verified paid order is opened, the frontend calls the idempotent cart-sync endpoint and refreshes the bag. Reopening confirmation does not remove later additions.
