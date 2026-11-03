# E-Commerce Semester Project — Phase 2 (development scaffold)

This is an extension of the uploaded Phase 1 source, not a replacement. **Not verified by a successful Maven build or Docker runtime test in this environment.** Review the known limitations below before submitting.

## Requirements
- JDK 21, Maven 3.9+, Docker Desktop / Docker Compose
- Build at project root: `mvn clean package -DskipTests`
- Start: `cd docker && docker compose up --build -d`
- Stop: `docker compose down`
- Scale product instances: `docker compose up -d --scale productservice=2`

## URLs
- Gateway: http://localhost:8072
- Eureka dashboard: http://localhost:8070
- Keycloak administration: http://localhost:8080 (admin/admin — local development only)
- Keycloak realm through gateway: http://localhost:8072/auth/realms/ecommerce
- Product API: GET http://localhost:8072/v1/product
- Order API: http://localhost:8072/v1/order
- Customer API: http://localhost:8072/v1/customer

## Authentication demonstration
1. Use the Keycloak registration UI at `http://localhost:8080/realms/ecommerce/protocol/openid-connect/registrations?client_id=ecommerce-postman&response_type=code&redirect_uri=https%3A%2F%2Foauth.pstmn.io%2Fv1%2Fcallback` (requires a browser/redirect flow). For the gateway version, substitute `http://localhost:8072/auth/realms/ecommerce` in the URL.
2. Obtain a development token by POST to `http://localhost:8072/auth/realms/ecommerce/protocol/openid-connect/token` with form-urlencoded `grant_type=password`, `client_id=ecommerce-postman`, `username=demo-admin`, `password=ChangeMe123!`. Password grant is for the local demonstration only; use Authorization Code + PKCE for production.
3. Send `Authorization: Bearer <token>` to protected endpoints through the gateway.
4. To reset passwords, configure SMTP in Keycloak's realm Email settings; Keycloak's Forgot Password flow cannot deliver mail until SMTP is configured.

## Important limitations / follow-up work
- Keycloak self-registration and reset are configured, but **outbound SMTP is not configured**. Password recovery needs a working SMTP provider and an end-to-end test.
- The existing customer data model is not yet tied to Keycloak subject (`sub`). Customer and order ownership checks are **not** implemented; do not use this scaffold with real customer data. Admin-only guards exist on customer list/delete and product mutations, but ownership enforcement is still required.
- The Order Service forwards the incoming bearer token to Product Service and validates current price/stock. Inventory is **not reserved/decremented atomically**; concurrent orders can oversell. The fallback rejects order creation rather than silently confirming it.
- The gateway forwards `/auth/**` to Keycloak. Browser-based Keycloak redirects may still point to port 8080 directly because of the development hostname setting; configure a reverse-proxy-aware Keycloak hostname for a strict gateway-only deployment.
- The stress-test implementation and measured results are **not included**. Add Gatling scenarios and capture baseline, scaled, and failure test metrics before submitting Phase 2.
- A full build, security audit, and live Docker smoke test are still needed.
- Demo admin password and database password are insecure development defaults. Replace before exposing outside localhost.

## Phase 2 demo checklist
Registration, login, password reset email, authorized admin write, forbidden customer write, 401 unauthenticated, JWT propagation during order creation, Eureka registration, 2 product replicas, Gatling stress test, circuit-breaker response under service failure.
