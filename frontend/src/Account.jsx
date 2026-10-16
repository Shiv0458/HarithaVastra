import React, { useState } from "react";
import { Link, useSearchParams, useNavigate } from "react-router-dom";
import { useStore, useResource } from "./store";
import { api, money } from "./api";
import { Action, Feedback } from "./components";
import { DeliveryForm } from "./Checkout";
export function Account() {
  const { user, login, logout, setUser, notify } = useStore();
  const [mode, setMode] = useState("login"),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  const [params] = useSearchParams();
  const navigate = useNavigate();
  if (!user)
    return (
      <main className="page auth-page" id="main">
        <div className="auth-art">
          <span className="eyebrow">HARITHA VASTRA</span>
          <h1>
            A wardrobe of
            <br />
            beautiful memories.
          </h1>
          <p>Save your favourites. Find your next heirloom.</p>
        </div>
        <div className="auth-form">
          <p className="eyebrow">YOUR HARITHA VASTRA</p>
          <h1>
            {mode === "login" ? "Welcome back." : "Make yourself at home."}
          </h1>
          <p>
            {mode === "login"
              ? "Sign in to your collection of favourites."
              : "Create an account to save your details and orders."}
          </p>
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              setError("");
              setBusy(true);
              try {
                await login(
                  mode,
                  Object.fromEntries(new FormData(e.currentTarget)),
                );
                const next = params.get("next");
                if (next?.startsWith("/") && !next.startsWith("//"))
                  navigate(next);
              } catch (err) {
                setError(err.message);
              } finally {
                setBusy(false);
              }
            }}
          >
            {mode === "register" && (
              <label>
                Full name
                <input
                  name="name"
                  required
                  maxLength={100}
                  autoComplete="name"
                />
              </label>
            )}
            <label>
              Email address
              <input
                type="email"
                name="email"
                required
                maxLength={254}
                autoComplete="email"
              />
            </label>
            <label>
              Password
              <input
                type="password"
                name="password"
                required
                minLength={mode === "register" ? 10 : undefined}
                maxLength={72}
                autoComplete={
                  mode === "login" ? "current-password" : "new-password"
                }
              />
            </label>
            {mode === "register" && (
              <p className="muted">Use at least 10 characters.</p>
            )}
            {error && (
              <p role="alert" className="error">
                {error}
              </p>
            )}
            <button className="primary full" disabled={busy}>
              {busy
                ? "Please wait…"
                : mode === "login"
                  ? "Sign in"
                  : "Create account"}
            </button>
          </form>
          <button
            className="text-button"
            onClick={() => {
              setMode(mode === "login" ? "register" : "login");
              setError("");
            }}
          >
            {mode === "login"
              ? "New here? Create an account"
              : "Already have an account? Sign in"}
          </button>
          <Link to="/checkout">Continue checkout as guest</Link>
        </div>
      </main>
    );
  return (
    <main className="page" id="main">
      <div className="section-heading">
        <div>
          <p className="eyebrow">YOUR HARITHA VASTRA</p>
          <h1>Hello, {user.name}.</h1>
        </div>
        <Action className="secondary" onClick={logout}>
          Sign out
        </Action>
      </div>
      {user.role === "ADMIN" && (
        <Link className="primary" to="/admin">
          Open admin dashboard
        </Link>
      )}
      <div className="account-grid">
        <section className="panel">
          <h2>Your profile</h2>
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              try {
                setUser(
                  await api("/account", {
                    method: "PUT",
                    body: Object.fromEntries(new FormData(e.currentTarget)),
                  }),
                );
                notify("Profile saved");
              } catch (err) {
                notify(err.message);
              }
            }}
          >
            <label>
              Name
              <input
                name="name"
                defaultValue={user.name}
                required
                maxLength={100}
              />
            </label>
            <label>
              Phone
              <input
                name="phone"
                defaultValue={user.phone || ""}
                maxLength={20}
              />
            </label>
            <p className="muted">{user.email}</p>
            <button className="secondary">Save profile</button>
          </form>
        </section>
        <Addresses />
      </div>
      <OrderHistory />
    </main>
  );
}
function Addresses() {
  const data = useResource("/account/addresses");
  const { user } = useStore();
  const [adding, setAdding] = useState(false);
  return (
    <section className="panel">
      <h2>Saved addresses</h2>
      <Feedback loading={data.loading} error={data.error} retry={data.reload} />
      {data.data?.map((a) => (
        <div className="address-card" key={a.id}>
          <strong>{a.name}</strong>
          <p>
            {a.line1}, {a.line2}, {a.city}, {a.state} {a.pin}
          </p>
          <Action
            className="text-button"
            onClick={async () => {
              await api("/account/addresses/" + a.id, { method: "DELETE" });
              data.reload();
            }}
          >
            Remove
          </Action>
        </div>
      ))}
      {!data.data?.length && <p className="muted">No addresses saved yet.</p>}
      {adding ? (
        <DeliveryForm
          initial={{ name: user.name, email: user.email }}
          label="Save address"
          onSubmit={async (value) => {
            await api("/account/addresses", { method: "POST", body: value });
            setAdding(false);
            data.reload();
          }}
        />
      ) : (
        <button className="secondary" onClick={() => setAdding(true)}>
          Add address
        </button>
      )}
    </section>
  );
}
function OrderHistory() {
  const orders = useResource("/orders");
  return (
    <section className="order-history">
      <h2>Your orders</h2>
      <Feedback
        loading={orders.loading}
        error={orders.error}
        retry={orders.reload}
      />
      {orders.data?.length ? (
        orders.data.map((o) => (
          <Link className="order-row" key={o.id} to={"/orders/" + o.id}>
            <div>
              <strong>{o.id}</strong>
              <p>
                {new Date(o.createdAt).toLocaleDateString("en-IN")} ·{" "}
                {o.items.length} items
              </p>
            </div>
            <span>{o.status.replaceAll("_", " ")}</span>
            <strong>{money(o.total)} →</strong>
          </Link>
        ))
      ) : (
        <p className="muted">
          Your orders will appear here. Guest orders remain accessible in the
          browser where they were placed.
        </p>
      )}
    </section>
  );
}
