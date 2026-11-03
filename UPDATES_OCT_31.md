# Storefront updates — October 31, 2026

Changes in this revision:

1. The hero/motto appears only on the Shop tab.
2. My Orders uses expandable order details with line items and totals.
3. Admin Studio is renamed Manage Inventory and contains only the product form and inventory list.
4. The inventory category field is a dropdown with common categories; existing uncommon categories are retained when editing.
5. Manage Orders is a separate permission-controlled tab (`MANAGE_ORDERS`) with customer-ID filtering, order details, and status editing (`NEW`, `PROCESSING`, `SHIPPED`, `DELIVERED`). A new `PATCH /v1/order/{id}/status` endpoint updates only the status; the backend validates permitted statuses.
6. Shop category filter displays all products by default and filters using categories currently in the catalog.

## Build

From project root:

```powershell
mvn clean package -DskipTests
docker compose -f docker/docker-compose.yml up -d --build
```

Hard-refresh `http://localhost:3000` (Ctrl+Shift+R).

## Notes

- Existing PostgreSQL data is preserved by these source changes; do not run `docker compose down -v` unless you intend to delete database volumes.
- Order status edits require the `MANAGE_ORDERS` client role. The frontend hides the tab from other users and the backend independently enforces authorization.
- Product categories are free-text values already stored in the database; the dropdown provides suggested values for new inventory entries.
- Full Docker integration testing was not performed in this environment. Test checkout, role restrictions, order updates, and category filters locally.
