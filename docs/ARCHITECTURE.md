# Scent Zone architecture and testing guide

## Layers

```text
JavaFX FXML / CSS
       |
LoginController / RegisterController / AdminController / CustomerController
       |
AuthService / OrderService
       |
UserDAO / PerfumeDAO / OrderDAO
       |
JDBC -> Local MySQL (scent_zone_db)
```

The controllers interact with services or DAO classes, not with handwritten SQL. Models are Java records except the mutable `CartLine` model. The GUI is defined in FXML so layout can evolve independently from Java logic.

## ER relationships

```text
users (1) -------- (*) orders
brands (1) ------- (*) perfumes
orders (1) ------ (*) order_items
perfumes (1) ---- (*) order_items
```

Order items store an immutable purchase-time unit price; editing a perfume's catalog price does not change old orders.

## Manual testing checklist

1. Open GUI and register a new customer; repeat the same email and confirm rejection.
2. Try a password shorter than 8 characters; verify validation.
3. Log in with a wrong password; verify rejection.
4. Log in as admin; create a brand and a perfume.
5. Edit that perfume's stock and price; refresh and verify.
6. Hide a perfume; verify it disappears from the customer catalog, but remains in admin inventory.
7. Log in as customer; search by name and filter by brand.
8. Add an item to cart and try to exceed available stock; verify the check.
9. Place an order; verify rows in `orders` and `order_items` and stock decrement in `perfumes`.
10. Check that orders for one customer do not appear in another customer's My Orders list.
11. Log in as admin; view order items and advance an order through statuses.
12. Create another pending order and cancel it; verify stock restoration exactly once.
13. Attempt to cancel a delivered order; verify the operation is rejected.
14. Stop MySQL, attempt a login or refresh, and verify an understandable error rather than a crash.

## Security notes

- Registration does not accept a client-provided role; public registration only creates `CUSTOMER`.
- Admin creation is a separate local CLI utility.
- Passwords are salted and hashed via PBKDF2-HMAC-SHA256.
- Queries use prepared statements; prices are recalculated on checkout from locked database rows.
- This is a local desktop demonstration. Admin/customer role checks are primarily at the GUI layer. A production-grade app should expose permissions and data access through a **server-side backend**, because desktop-distributed database credentials are not a secure authorization boundary.
- No real payments, email notifications, or parcel deliveries are performed.

## Ideas for future versions

- Product images and bottle detail screens
- Discounts, shipping fees, and promo codes
- Dedicated branded printable PDF invoices
- Reports and visual sales charts
- Reset-password workflow
- A server-side REST API (Spring Boot) with authorization
- JUnit and Testcontainers integration tests with disposable MySQL
