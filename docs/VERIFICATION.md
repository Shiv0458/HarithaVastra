# Verification report

Verified in the build environment on 10 October 2026.

| Check | Result |
|---|---|
| Vite production build | Passed; compiled frontend and optimized assets |
| Frontend Vitest suite | 2 passed |
| Maven package / Java 17 / Spring Boot | Passed; runnable JAR built |
| Backend unit and Spring integration tests | 19 passed, 0 failures |
| Real MySQL Testcontainers test | 1 skipped: Docker is not available in this environment |
| Browser/device visual and accessibility testing | Not run: supported browser automation is unavailable |
| Actual Razorpay test-account transaction/webhook | Not run: credentials and reachable webhook host have not been supplied |
| Docker Compose startup | Not run here: Docker unavailable; files and local instructions supplied |
| Public/live deployment | Not performed |

## Backend coverage

- Decimal totals, discounts, shipping, tax rounding and paise conversion.
- Invalid/empty quantities and a known HMAC-SHA256 vector.
- Tampered signatures and payment amount mismatches.
- Duplicate checkout keys and preserved purchased-price snapshots.
- Two concurrent customers attempting to reserve the final available unit.
- Duplicate captured callbacks and duplicate authenticated webhook events.
- Idempotent cancellation and late capture with unavailable stock.
- Guest cart/wishlist merging without quantity doubling.
- Customer/guest denial of admin endpoints and cross-owner order isolation.
- Archived cart item removal.
- Server search, rating sort, pagination and dynamic filter facets.
- Idempotent paid-order cart cleanup that preserves later additions.
- Core + demo Flyway migrations executed in H2 MySQL compatibility mode.
- Real admin bootstrap/login, protected write access, logout and token revocation.
- Missing payment credentials reported as unavailable rather than simulated success.

The Spring integration tests run actual controllers, validation, security, repositories and transactions. Payment-gateway responses are mocked where indicated. H2 success does not substitute for the included real-MySQL migration test; run `mvn test` with Docker available before release.

## Manual acceptance before launch

1. Start Compose, then browse at desktop/tablet/320px and narrow viewport sizes. Verify sidebar/drawer, focus visibility, keyboard operation, modals, zoom and no overflow.
2. Search/filter/sort all collections; create a new colour through admin and confirm the facets update after a reload.
3. Test guest wishlist/cart, reload persistence, account merge, saved addresses and logout on shared devices.
4. Test out-of-stock, archived and changed-price products between cart and checkout. Create real products with missing optional attributes and verify no invented claims appear.
5. Upload JPEG/PNG body, border, pallu and blouse photos. Validate gallery labels and zoom. Test invalid/oversized uploads and denied customer admin access.
6. Follow PAYMENTS.md for success, failure, cancelled, pending, repeated, lost-callback, webhook retry and late-capture cases in a real Razorpay test account.
7. Verify order history, paid-only fulfilment updates and review submission only after delivery.
8. Complete the production setup list in OPERATIONS.md before enabling real sales.
