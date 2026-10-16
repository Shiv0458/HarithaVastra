# Razorpay setup and test guide

## Enable test checkout

1. In your Razorpay account, switch to **Test mode** and create test API keys. Put the public key ID in `RAZORPAY_KEY_ID`, secret in `RAZORPAY_KEY_SECRET`, and `PAYMENT_MODE=test` on the backend. Never put the secret in a Vite variable.
2. Create a webhook with a separately chosen secret in `RAZORPAY_WEBHOOK_SECRET`. The gateway is considered configured only when all three values are non-empty. Subscribe to `payment.authorized`, `payment.captured` and `payment.failed`.
3. Set the webhook URL to `https://<your reachable backend>/api/payments/webhook`. Razorpay cannot call localhost; use an HTTPS tunnel or a staging deployment. Keep the webhook path outside customer authentication; HMAC authenticates its raw request body.
4. Configure automatic capture in the Razorpay Dashboard. This application does not treat authorization alone as a completed sale and does not implement a capture button.
5. Restart the backend after changing environment variables. It rejects test/live key-prefix mismatches.
6. Browse a demo product, add it to the bag, enter valid-format Indian delivery data, review the server totals, and open Razorpay Checkout.
7. Use the **current test instruments shown in Razorpay documentation**. UPI/card/net-banking availability can differ by account and provider test environment; do not use real customer credentials or assume every method is available.
8. A successful browser callback calls `/orders/{id}/verify`. The server verifies HMAC over `stored_order_id|payment_id`, then fetches the payment from Razorpay. Only `captured` with the expected order/amount/currency sets `PAID`.
9. If the callback is lost, the signed webhook or “Refresh payment status” reconciles the order. Confirmation shows order number, delivery address, totals, payment status and items.

## Failure and replay checks

- Close the payment window: the order remains unpaid/pending, with resume/cancel controls.
- Fail a test payment: the gateway reports failure; another attempt may succeed on the same order. Failed attempts never decrement stock again.
- Modify a signature, payment ID, amount or currency in a test: verification must fail. The checkout request accepts delivery details, not a client total.
- Send the same valid webhook repeatedly: one order and one reservation survive. Status cannot regress from captured to pending due to a delayed event because the server fetches current provider state.
- Retry `POST /checkout` using the same `Idempotency-Key`: the saved order is reused. Reuse with a changed delivery payload is rejected.
- Attempt two simultaneous purchases of the last unit: only one stock reservation succeeds.
- Cancel an unpaid order twice: stock is returned once. If a later captured payment arrives after cancellation, stock is reacquired or the order enters `PAID_REVIEW` for manual resolution.
- Gateway order creation timeouts are ambiguous. The app records `GATEWAY_ERROR` and does not automatically create another provider order. An operator should inspect the receipt in Razorpay; cancel/restart the checkout when safe. No payment is represented as successful merely because a gateway request was attempted.

## Important operational limits

- Refund execution is handled by an operator in Razorpay Dashboard; no refund API or refund-state synchronization is implemented here. `PAID_REVIEW` orders must be resolved before fulfilment.
- Shipping is a configured flat charge; tax is a configured merchandise rate. No carrier, GST invoice or tax determination service is integrated.
- Webhook idempotency and order row locks protect duplicate processing. A transient conflict returns a non-2xx response so Razorpay can retry.
- Webhook API calls are performed synchronously with timeouts. At larger scale, replace this with a durable verified-event queue, replay tooling and monitored reconciliation workers.
- Browser success is not proof of capture. An authorized/pending payment displays pending until capture is confirmed.
- Guest order access uses that browser’s random guest token. Do not share guest tokens. Email-based order recovery and guest-order linking to later accounts are future work.

## Live activation

Replace the demo database with a clean production database/profile, add actual products and photography, approve policies/contact details and validate shipping/taxes. Complete Razorpay onboarding, use live keys with `PAYMENT_MODE=live`, configure an HTTPS live webhook with its own secret, enable capture, add monitoring/reconciliation alerts, and test small controlled live purchases/refunds after test acceptance. Live checkout rejects demo products.

Official references used when implementing (recheck before deployment):

- https://razorpay.com/docs/payments/payment-gateway/web-integration/standard/integration-steps/
- https://razorpay.com/docs/api/orders/create/
- https://razorpay.com/docs/api/payments/fetch-with-id/
- https://razorpay.com/docs/webhooks/validate-test/
- https://github.com/razorpay/markdown-docs/blob/master/webhooks/payments.md
