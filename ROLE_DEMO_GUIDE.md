# Phase 2 — Composite Roles + Storefront

## Architecture

- Keycloak realm `ecommerce`, public OIDC client `ecommerce-project`, Authorization Code + PKCE.
- Realm composite roles: `CUSTOMER` (default for signups), `ADMIN` (manual assignment).
- Client permissions: `VIEW_PRODUCTS`, `PLACE_ORDERS`, `VIEW_OWN_ORDERS`, `MANAGE_OWN_PROFILE`, `MANAGE_PRODUCTS`, `MANAGE_ORDERS`, `MANAGE_CUSTOMERS`.
- Product, Order, and Customer microservices validate JWT and convert `resource_access.ecommerce-project.roles` to Spring authorities. Realm roles become `ROLE_CUSTOMER` and `ROLE_ADMIN`.
- Frontend: static HTML/CSS/JS served by Nginx on http://localhost:3000. `/api/*` is reverse-proxied to Gateway, avoiding browser CORS issues.
- Keycloak owns credentials. Customer-service owns profile data keyed by Keycloak JWT `sub` (customer ID).

## First-time startup (Windows PowerShell)

1. From repository root: `mvn clean package -DskipTests` (requires Java 21, Maven).
2. `cd docker`
3. `docker compose build`
4. `docker compose up -d`
5. Browse http://localhost:3000 and http://localhost:8080/admin/ (admin/admin).

If your prior Keycloak realm was imported with old roles, you must recreate **only the Keycloak container** to reimport its JSON when using the included ephemeral development Keycloak database:
`docker compose stop keycloak; docker compose rm -sf keycloak; docker compose up -d keycloak`.
This deletes any Keycloak accounts created interactively. Do not use `docker compose down -v` casually because it can erase database volumes.

## Test users

- `demo-customer` / `ChangeMe123!` — customer composite role, can shop, place orders, view own orders and profile.
- `demo-admin` / `ChangeMe123!` — both ADMIN and CUSTOMER, can manage products, view/manage all orders, view customers.
- Use **Create an account** → **Sign in** → Keycloak **Register** to test new users (registration enabled). Newly registered users inherit CUSTOMER.
- The admin console credentials (`admin/admin`) are separate from the demo-admin e-commerce account.

## Role demo checklist

1. Sign in as `demo-customer` in the storefront. Add product and place order; inspect My Orders and My Profile.
2. Confirm the Admin Studio tab is hidden.
3. In Postman, request a token with `grant_type=password`, `client_id=ecommerce-project`, `username=demo-customer`, `password=ChangeMe123!` at `http://localhost:8080/realms/ecommerce/protocol/openid-connect/token` (form URL encoded). A `POST /v1/product` through Gateway with that Bearer token must return **403**.
4. Sign out and sign in as `demo-admin`. Admin Studio appears; create, edit, and delete a product.
5. Verify that `GET /v1/customer` and all orders are visible to admin but not to customers.
6. Decode tokens: `realm_access.roles` shows CUSTOMER/ADMIN, and `resource_access.ecommerce-project.roles` shows composite client permissions.

## Security and implementation notes

- Frontend uses browser Authorization Code + PKCE, with tokens held in sessionStorage for this course demo; production deployments should assess XSS/token storage risks and consider a backend-for-frontend with secure cookies.
- The password grant is enabled **only for assignment/Postman testing**, not recommended for production.
- `GET /v1/product` is intentionally public. Customer profile `GET/PUT /v1/customer/me` is bound to token `sub`; profile creation is lazy (first visit).
- Customer order POST ignores any supplied customer ID/name/status and derives these from JWT. Customer GET orders is filtered by JWT subject; admin can list all orders. Update/delete order are admin-only.
- Price and product name are calculated server-side from product-service; **stock is checked but not reserved/decremented**. Payment processing, transactional inventory, production SMTP, persistent Keycloak DB, and automated E2E tests are out of scope of this demo.
- For password reset emails, configure SMTP in Keycloak Realm Settings → Email.
- Existing sample database rows are not associated with demo users, so the seeded sample order appears only to admins.
- Docker Compose uses local demonstration credentials; change before deployment.
