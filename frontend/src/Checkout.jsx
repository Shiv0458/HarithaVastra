import { createUuid } from "./uuid";
import React, { useState, useEffect } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  ShoppingBag,
  ArrowRight,
  Trash2,
  LockKeyhole,
  CheckCircle2,
} from "lucide-react";
import { useStore, useResource } from "./store";
import { api, money, imageUrl } from "./api";
import { Action, Feedback, Quantity, TotalsBox } from "./components";
export function CartPage() {
  const { cart, quantity } = useStore();
  const quote = useResource(
    "/cart/quote?revision=" +
      cart.map((i) => i.product.id + ":" + i.quantity).join(","),
  );
  return (
    <main className="page" id="main">
      <p className="eyebrow">YOUR BEAUTIFUL FINDS</p>
      <h1>Your shopping bag</h1>
      {cart.length ? (
        <div className="checkout-layout">
          <div className="cart-items">
            {cart.map(({ product: p, quantity: q }) => (
              <article className="cart-item" key={p.id}>
                <Link to={"/products/" + p.id}>
                  <img
                    src={imageUrl(p.images[0]?.url) || "/images/placeholder.svg"}
                    alt={p.name}
                  />
                </Link>
                <div>
                  <p className="eyebrow">{p.collection}</p>
                  <Link className="product-name" to={"/products/" + p.id}>
                    {p.name}
                  </Link>
                  <p>{money(p.price)}</p>
                  <Quantity
                    value={q}
                    max={p.stock}
                    onChange={async (v) => {
                      try {
                        await quantity(p.id, v);
                      } catch (e) {
                        alert(e.message);
                      }
                    }}
                  />
                  {(p.archived || q > p.stock) && (
                    <p className="inline-error">
                      Unavailable in this quantity. Remove or reduce this item.
                    </p>
                  )}
                </div>
                <div>
                  <strong>{money(p.price * q)}</strong>
                  <Action
                    className="text-button"
                    onClick={() => quantity(p.id, 0)}
                    aria-label={"Remove " + p.name}
                  >
                    <Trash2 size={16} /> Remove
                  </Action>
                </div>
              </article>
            ))}
            <Link className="text-link" to="/">
              ← Continue discovering
            </Link>
          </div>
          <aside className="summary">
            <h2>Order summary</h2>
            <Feedback
              loading={quote.loading}
              error={quote.error}
              retry={quote.reload}
            />
            {quote.data && !quote.error && (
              <>
                <TotalsBox totals={quote.data} />
                <Link className="primary full" to="/checkout">
                  Continue to checkout <ArrowRight size={17} />
                </Link>
              </>
            )}
            <p className="secure-note">
              <LockKeyhole size={14} /> Secure payment through Razorpay
            </p>
          </aside>
        </div>
      ) : (
        <div className="empty">
          <ShoppingBag size={40} />
          <h2>Your next favourite awaits.</h2>
          <p>Your bag is empty. Discover a saree you’ll love.</p>
          <Link className="primary" to="/">
            Explore sarees
          </Link>
        </div>
      )}
    </main>
  );
}
const fields = [
  ["name", "Full name", "text"],
  ["phone", "Mobile number", "tel"],
  ["email", "Email address", "email"],
  ["line1", "Address line 1", "text"],
  ["line2", "Address line 2 (optional)", "text"],
  ["city", "City", "text"],
  ["state", "State", "text"],
  ["pin", "PIN code", "text"],
];
export function DeliveryForm({
  initial = {},
  onSubmit,
  label = "Review order",
  extra,
}) {
  const [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  return (
    <form
      className="delivery-form"
      onSubmit={async (e) => {
        e.preventDefault();
        const value = Object.fromEntries(new FormData(e.currentTarget));
        setBusy(true);
        setError("");
        try {
          await onSubmit(value);
        } catch (err) {
          setError(err.message);
        } finally {
          setBusy(false);
        }
      }}
    >
      <div className="form-grid">
        {fields.map(([name, text, type]) => (
          <label key={name} className={name.startsWith("line") ? "wide" : ""}>
            {text}
            <input
              name={name}
              type={type}
              defaultValue={initial[name] || ""}
              required={name !== "line2"}
              maxLength={
                name === "pin"
                  ? 6
                  : name === "phone"
                    ? 10
                    : name.startsWith("line")
                      ? 255
                      : name === "email"
                        ? 254
                        : 100
              }
              pattern={
                name === "phone"
                  ? "[6-9][0-9]{9}"
                  : name === "pin"
                    ? "[1-9][0-9]{5}"
                    : undefined
              }
              inputMode={
                name === "pin" || name === "phone" ? "numeric" : undefined
              }
              autoComplete={
                {
                  name: "name",
                  phone: "tel-national",
                  email: "email",
                  line1: "address-line1",
                  line2: "address-line2",
                  city: "address-level2",
                  state: "address-level1",
                  pin: "postal-code",
                }[name]
              }
            />
          </label>
        ))}
      </div>
      {extra}
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      <button className="primary full" disabled={busy}>
        {busy ? "Please wait…" : label}
        <ArrowRight size={17} />
      </button>
    </form>
  );
}
let razorpayScript;
async function loadGateway() {
  if (window.Razorpay) return;
  if (!razorpayScript)
    razorpayScript = new Promise((resolve, reject) => {
      const script = document.createElement("script");
      script.src = "https://checkout.razorpay.com/v1/checkout.js";
      script.onload = resolve;
      script.onerror = () => {
        razorpayScript = null;
        script.remove();
        reject(
          new Error(
            "Could not load payment window. Check your connection and retry.",
          ),
        );
      };
      document.body.appendChild(script);
    });
  await razorpayScript;
}
export async function openPayment(order, config, navigate, notify) {
  await loadGateway();
  const checkout = new window.Razorpay({
    key: config.keyId,
    amount: Math.round(Number(order.total) * 100),
    currency: "INR",
    name: "Haritha Vastra",
    description: "Silk saree order " + order.id,
    order_id: order.gatewayOrderId,
    prefill: {
      name: order.delivery.name,
      email: order.delivery.email,
      contact: order.delivery.phone,
    },
    theme: { color: "#722e3b" },
    handler: async (result) => {
      try {
        const verified = await api("/orders/" + order.id + "/verify", {
          method: "POST",
          body: result,
        });
        notify(
          verified.status === "PAID"
            ? "Payment verified. Thank you!"
            : "Payment received; awaiting verification.",
        );
      } catch (e) {
        notify(e.message + " Check order status before paying again.");
      }
      navigate("/orders/" + order.id);
    },
    modal: {
      ondismiss: () => {
        notify(
          "Payment window closed. You can resume or cancel from your order.",
        );
        navigate("/orders/" + order.id);
      },
    },
  });
  checkout.on("payment.failed", () =>
    notify(
      "Payment failed. You can retry in the payment window or return to your order.",
    ),
  );
  checkout.open();
}
export function CheckoutPage() {
  const { cart, user, config, notify } = useStore();
  const quote = useResource("/cart/quote");
  const [delivery, setDelivery] = useState(null),
    [addresses, setAddresses] = useState([]),
    [selected, setSelected] = useState(null),
    [saveAddress, setSaveAddress] = useState(false);
  const navigate = useNavigate();
  useEffect(() => {
    if (user)
      api("/account/addresses")
        .then(setAddresses)
        .catch((e) => notify(e.message));
  }, [user]);
  async function pay() {
    if (!config?.paymentConfigured)
      throw new Error(
        "Add Razorpay test credentials on the backend to enable payments.",
      );
    let pending = JSON.parse(sessionStorage.getItem("hv-checkout") || "null");
    const fingerprint = JSON.stringify({
      delivery,
      items: cart.map((i) => [i.product.id, i.quantity]),
    });
    if (!pending || pending.fingerprint !== fingerprint) {
      pending = { key: createUuid(), fingerprint };
      sessionStorage.setItem("hv-checkout", JSON.stringify(pending));
    }
    const order = await api("/checkout", {
      method: "POST",
      headers: { "Idempotency-Key": pending.key },
      body: { delivery },
    });
    if (order.status === "PAID") {
      navigate("/orders/" + order.id);
      return;
    }
    if (order.status !== "PAYMENT_PENDING") {
      navigate("/orders/" + order.id);
      return;
    }
    if (Number(order.total) !== Number(quote.data.total)) {
      notify(
        "The total changed. Please review the saved order before payment.",
      );
      navigate("/orders/" + order.id);
      return;
    }
    await openPayment(order, config, navigate, notify);
  }
  return (
    <main className="page" id="main">
      <p className="eyebrow">A FEW DETAILS, THEN IT’S YOURS</p>
      <h1>Checkout</h1>
      <ol className="checkout-steps">
        <li>
          <Link to="/cart">Bag</Link>
        </li>
        <li className={!delivery ? "current" : ""}>Delivery details</li>
        <li className={delivery ? "current" : ""}>Review &amp; payment</li>
        <li>Confirmation</li>
      </ol>
      {!cart.length ? (
        <div className="empty">
          <p>Add a saree to your bag before checkout.</p>
          <Link to="/" className="primary">
            Explore sarees
          </Link>
        </div>
      ) : (
        <div className="checkout-layout">
          <div className="panel">
            {!delivery ? (
              <>
                <h2>Where should we deliver?</h2>
                {!user && (
                  <p className="muted">
                    Continue as a guest, or{" "}
                    <Link to="/account?next=/checkout">sign in</Link> to save
                    your details.
                  </p>
                )}
                {addresses.length > 0 && (
                  <label>
                    Use a saved address
                    <select
                      onChange={(e) =>
                        setSelected(
                          addresses.find(
                            (a) => a.id === Number(e.target.value),
                          ) || null,
                        )
                      }
                    >
                      <option value="">Enter a new address</option>
                      {addresses.map((a) => (
                        <option key={a.id} value={a.id}>
                          {a.line1}, {a.city}
                        </option>
                      ))}
                    </select>
                  </label>
                )}
                <DeliveryForm
                  key={selected?.id || "new"}
                  initial={{
                    name: user?.name,
                    email: user?.email,
                    ...selected,
                  }}
                  onSubmit={async (value) => {
                    if (saveAddress && user)
                      await api("/account/addresses", {
                        method: "POST",
                        body: value,
                      });
                    setDelivery(value);
                    quote.reload();
                  }}
                  extra={
                    user && (
                      <label className="checkbox">
                        <input
                          type="checkbox"
                          checked={saveAddress}
                          onChange={(e) => setSaveAddress(e.target.checked)}
                        />{" "}
                        Save this address to my account
                      </label>
                    )
                  }
                />
              </>
            ) : (
              <>
                <h2>Everything look right?</h2>
                <div className="address-review">
                  <strong>{delivery.name}</strong>
                  <p>
                    {delivery.line1}, {delivery.line2}
                    <br />
                    {delivery.city}, {delivery.state} – {delivery.pin}
                  </p>
                  <p>
                    {delivery.phone} · {delivery.email}
                  </p>
                  <button
                    className="text-button"
                    onClick={() => {
                      setSelected(delivery);
                      setDelivery(null);
                    }}
                  >
                    Edit delivery details
                  </button>
                </div>
                {cart.map(({ product: p, quantity: q }) => (
                  <div className="review-line" key={p.id}>
                    <span>
                      {p.name} × {q}
                    </span>
                    <strong>{money(p.price * q)}</strong>
                  </div>
                ))}
                <p className="muted">
                  Please review our{" "}
                  <Link to="/policies/shipping-policy">shipping</Link>,{" "}
                  <Link to="/policies/returns-and-refunds">returns</Link> and{" "}
                  <Link to="/policies/terms-and-conditions">terms</Link>. These
                  policies are currently placeholders and require completion
                  before live sales.
                </p>
                <Action
                  className="primary full"
                  disabled={
                    !quote.data ||
                    !!quote.error ||
                    quote.loading ||
                    !config?.paymentConfigured
                  }
                  onClick={pay}
                >
                  Pay {money(quote.data?.total)}{" "}
                  {config?.paymentMode === "test" ? "· test mode" : ""}
                  <LockKeyhole size={16} />
                </Action>
                <p className="secure-note">
                  UPI · Cards · Net banking (gateway availability applies)
                </p>
              </>
            )}
          </div>
          <aside className="summary">
            <h2>Your order</h2>
            <Feedback
              loading={quote.loading}
              error={quote.error}
              retry={quote.reload}
            />
            {quote.data && !quote.error && <TotalsBox totals={quote.data} />}
            <p className="notice">
              {config?.paymentConfigured
                ? `Razorpay ${config.paymentMode} mode. ${config.paymentMode === "test" ? "No real money is charged." : ""}`
                : "Payments are not configured yet. Add test credentials to enable this step."}
            </p>
          </aside>
        </div>
      )}
    </main>
  );
}
export function OrderPage() {
  const { id } = useParams();
  const order = useResource("/orders/" + id);
  const { config, notify, refresh } = useStore();
  const navigate = useNavigate();
  const o = order.data;
  useEffect(() => {
    if (o?.status === "PAID") {
      sessionStorage.removeItem("hv-checkout");
      api("/orders/" + id + "/sync-cart", { method: "POST" })
        .then(refresh)
        .catch((e) => notify(e.message));
    }
  }, [o?.status]);
  return (
    <main className="page narrow" id="main">
      <Feedback
        loading={order.loading}
        error={order.error}
        retry={order.reload}
      />
      {o && (
        <>
          <p className="eyebrow">
            {o.status === "PAID" ? "THANK YOU FOR YOUR ORDER" : "YOUR ORDER"}
          </p>
          <h1>
            {o.status === "PAID"
              ? "A beautiful choice."
              : o.status === "PAID_REVIEW"
                ? "Payment received. Review needed."
                : "Let’s keep you updated."}
          </h1>
          <p>
            Order <strong>{o.id}</strong>
          </p>
          <div className="status-row">
            <span className="status">{o.status.replaceAll("_", " ")}</span>
            <span>Fulfilment: {o.fulfillment}</span>
          </div>
          {o.status === "PAID_REVIEW" && (
            <p className="error">
              Your payment is captured but this order needs stock or payment
              review. Do not pay again. Contact the store for fulfilment or a
              refund.
            </p>
          )}
          {o.status === "GATEWAY_ERROR" && (
            <p className="notice">
              Gateway setup could not be completed. Do not reuse this checkout.
              Cancel this order to release stock, then try again.
            </p>
          )}
          <div className="panel">
            {o.items.map((i) => (
              <div className="review-line" key={i.id}>
                <span>
                  {i.name} × {i.quantity}
                </span>
                <strong>{money(i.unitPrice * i.quantity)}</strong>
              </div>
            ))}
            <TotalsBox totals={o} />
            <p>
              <strong>Delivering to {o.delivery.name}</strong>
              <br />
              {o.delivery.line1}, {o.delivery.line2}
              <br />
              {o.delivery.city}, {o.delivery.state}, {o.delivery.pin}
            </p>
          </div>
          <div className="button-row">
            <Action
              className="secondary"
              onClick={async () =>
                order.setData(
                  await api("/orders/" + id + "/refresh", { method: "POST" }),
                )
              }
            >
              Refresh payment status
            </Action>
            {o.status === "PAYMENT_PENDING" &&
              new Date(o.expiresAt) > new Date() && (
                <Action
                  className="primary"
                  onClick={() => openPayment(o, config, navigate, notify)}
                >
                  Resume payment
                </Action>
              )}
            {["CREATED", "PAYMENT_PENDING", "GATEWAY_ERROR"].includes(
              o.status,
            ) && (
              <Action
                className="text-button"
                onClick={async () => {
                  order.setData(
                    await api("/orders/" + id + "/cancel", { method: "POST" }),
                  );
                  sessionStorage.removeItem("hv-checkout");
                }}
              >
                Cancel unpaid order
              </Action>
            )}
            {o.status === "PAID" && (
              <Link className="primary" to="/">
                Continue shopping
              </Link>
            )}
          </div>
          <Link to="/account">View all orders</Link>
        </>
      )}
    </main>
  );
}
