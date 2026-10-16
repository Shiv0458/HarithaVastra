# API reference

Base: `/api`. JSON requests and responses. Successful reads return the documented resource; errors consistently return `{ "message": "...", "fields": [...] }` (fields only for validation errors). Prices are INR decimals, gateway amounts are integer paise. List pagination is zero-based.

Use `Authorization: Bearer <session-token>` after login. Guest shopping requests use `X-Guest-Token: <random UUID>`. Guest tokens are bearer secrets: the browser generates one with `crypto.randomUUID()` and retains it in localStorage. Login and registration also carry that header to merge shopping selections. Admin endpoints require an authenticated ADMIN role enforced by Spring Security.

| Method       | Endpoint                         | Purpose / access                                                        |
| ------------ | -------------------------------- | ----------------------------------------------------------------------- |
| GET          | `/config`                        | Demo flag, payment mode/public key, whether payments are configured     |
| GET          | `/categories`                    | Collections                                                             |
| GET          | `/products`                      | Public paginated catalogue                                              |
| GET          | `/products/{id}`                 | Product, images, supplied specifications and aggregate rating           |
| GET          | `/products/{id}/reviews`         | Stored reviews, no user identifiers                                     |
| POST         | `/auth/register`                 | `{name,email,password}`; returns token/user and merges guest selections |
| POST         | `/auth/login`                    | `{email,password}`; returns token/user and merges guest selections      |
| POST         | `/auth/logout`                   | Revokes the presented token                                             |
| GET / PUT    | `/account`                       | Own profile; PUT `{name,phone}`                                         |
| GET / POST   | `/account/addresses`             | List/add own delivery addresses                                         |
| DELETE       | `/account/addresses/{id}`        | Delete an owned address                                                 |
| POST         | `/account/reviews/{productId}`   | `{rating,text}`; requires delivered paid purchase                       |
| GET          | `/cart`                          | Guest/customer cart with current product information                    |
| PUT          | `/cart/items/{productId}`        | `{quantity: 0..10}`; zero removes                                       |
| GET          | `/cart/quote`                    | Revalidated server totals; inventory conflicts return 409               |
| GET          | `/wishlist`                      | Guest/customer wishlist                                                 |
| PUT / DELETE | `/wishlist/{productId}`          | Save/remove                                                             |
| POST         | `/wishlist/{productId}/move`     | Transactionally add to bag and remove from wishlist                     |
| POST         | `/checkout`                      | `{delivery:{...}}`; requires unique `Idempotency-Key`                   |
| GET          | `/orders`                        | Orders belonging to current user/guest owner                            |
| GET          | `/orders/{id}`                   | Owned order details                                                     |
| POST         | `/orders/{id}/verify`            | `{razorpay_order_id,razorpay_payment_id,razorpay_signature}`            |
| POST         | `/orders/{id}/refresh`           | Fetch current provider state and reconcile                              |
| POST         | `/orders/{id}/cancel`            | Cancel unpaid order after provider reconciliation                       |
| POST         | `/payments/webhook`              | Raw Razorpay JSON, `X-Razorpay-Signature`, optional event ID            |
| GET / POST   | `/admin/products`                | Paginated inventory / create                                            |
| PUT / DELETE | `/admin/products/{id}`           | Edit / archive                                                          |
| POST         | `/admin/images`                  | Multipart `file`, PNG/JPEG up to 8MB and 25 megapixels                  |
| POST         | `/admin/categories`              | `{name,slug}`                                                           |
| PUT          | `/admin/categories/{id}`         | Update category                                                         |
| GET          | `/admin/orders`                  | Paginated order list with delivery information                          |
| GET          | `/admin/orders/{id}/payments`    | Provider attempt records                                                |
| POST         | `/admin/orders/{id}/refresh`     | Reconcile provider state                                                |
| PUT          | `/admin/orders/{id}/fulfillment` | `{status}`: UNFULFILLED → PACKING → SHIPPED → DELIVERED; paid only      |

## Product query

`q`, `collection`, `colour`, `occasion`, `min`, `max`, `available=true`, `newest=true`, `sort`, `page`, `size` (1–48). Sort values: `relevance`, `newest`, `price-asc`, `price-desc`, `rating`. Relevance uses keyword match then recency; no made-up relevance scores. Newest filter is 30 days.

Example: `/products?collection=Kanchipattu&max=22000&available=true&sort=price-asc&page=0&size=12`

Response: `{items:[...], total:4, pages:1, page:0}`. Admin list responses use Spring page format (`content`, `totalPages`, `totalElements`, `number`, ...).

## Delivery shape

```json
{
  "name": "Test Customer",
  "phone": "9876543210",
  "email": "test@example.com",
  "line1": "12 Example Road",
  "line2": "",
  "city": "Bengaluru",
  "state": "Karnataka",
  "pin": "560001"
}
```

Address validation checks required values and Indian phone/PIN formatting; it does not promise carrier serviceability or verify the address exists.

## Product write shape

```json
{
  "name": "Your verified product name",
  "categoryId": 1,
  "collection": "Kanchipattu",
  "occasion": "Bridal",
  "colour": "Maroon",
  "description": "Product description supplied by the seller.",
  "price": 18500.0,
  "originalPrice": null,
  "stock": 3,
  "demo": false,
  "specifications": { "Material": "Value supplied by seller" },
  "images": [{ "url": "/uploads/uuid.png", "label": "Body" }]
}
```

`stock` is **available stock excluding existing reservations**. Set it accordingly when editing. Archived orders retain snapshots. Original price is optional and may not be below current price. A discount is displayed only for a strictly larger supplied original price; seed discounts are explicitly demonstration values.

`GET /products/facets` supplies live collection, colour and occasion filter values. `POST /orders/{id}/sync-cart` consumes the purchased cart quantities once after a verified paid order; subsequent calls preserve new cart additions.
