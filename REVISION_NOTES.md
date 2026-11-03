# Storefront refinements

1. Registration button hidden for authenticated users.
2. Customer profile names remain in the customer-service database. **They do not synchronize to Keycloak**. Keycloak identity attributes and business profile attributes are separate; use Keycloak Account Console for identity name changes or implement a secure server-side Keycloak Admin API integration.
3. Product cards accept a positive integer quantity and enforce available stock against the cart.
4. Checkout reserves inventory in the Product Service using pessimistic row locks and an internal secret. Aggregate quantities are checked, and the product stock is reduced before the order is persisted. On order-save failure, a best-effort stock restoration runs. **Not production-grade distributed transactions**: service crashes/network partitions may leave mismatched inventory; use a reservation ledger, outbox/saga and idempotency keys for production. Do not expose the internal secret to browsers.
5. Admin orders are grouped by customer ID with expandable order details and item subtotals.

Run `mvn clean package -DskipTests`, then `docker compose -f docker/docker-compose.yml up -d --build`. The frontend is bind-mounted and may update on browser refresh. Set `INVENTORY_INTERNAL_SECRET` to a long random shared secret in your local environment before running beyond a classroom demo.
