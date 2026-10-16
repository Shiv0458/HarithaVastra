import React, { useState } from "react";
import { Link } from "react-router-dom";
import { useStore, useResource } from "./store";
import { api, money, imageUrl } from "./api";
import { Action, Feedback, Modal } from "./components";
const empty = {
  name: "",
  collection: "Kanchipattu",
  categoryId: "",
  occasion: "Bridal",
  colour: "",
  description: "",
  price: "",
  originalPrice: "",
  stock: 0,
  demo: false,
  specifications: {},
  images: [],
};
export function Admin() {
  const { user, ready } = useStore();
  const [tab, setTab] = useState("products"),
    [page, setPage] = useState(0),
    [editing, setEditing] = useState(null);
  const products = useResource("/admin/products?page=" + page),
    orders = useResource("/admin/orders?page=" + page),
    categories = useResource("/categories");
  if (!ready) return <Feedback loading />;
  if (user?.role !== "ADMIN")
    return (
      <main className="page" id="main">
        <h1>Administrator access required.</h1>
        <p>
          Sign in with the account configured using ADMIN_EMAIL and
          ADMIN_PASSWORD.
        </p>
        <Link to="/account" className="primary">
          Sign in
        </Link>
      </main>
    );
  const data = tab === "products" ? products : orders;
  return (
    <main className="page" id="main">
      <div className="section-heading">
        <div>
          <p className="eyebrow">HARITHA VASTRA · ADMINISTRATION</p>
          <h1>Your store, thoughtfully managed.</h1>
        </div>
        {tab === "products" && (
          <button className="primary" onClick={() => setEditing({ ...empty })}>
            Add product +
          </button>
        )}
      </div>
      <nav className="tabs" aria-label="Admin sections">
        {["products", "orders", "categories"].map((t) => (
          <button
            className={tab === t ? "active" : ""}
            key={t}
            onClick={() => {
              setTab(t);
              setPage(0);
            }}
          >
            {t}
          </button>
        ))}
      </nav>
      {tab === "categories" ? (
        <Categories categories={categories} />
      ) : (
        <>
          <Feedback
            loading={data.loading}
            error={data.error}
            retry={data.reload}
          />
          {tab === "products" ? (
            <div className="admin-products">
              {products.data?.content.map((p) => (
                <div className="admin-product" key={p.id}>
                  <img
                    src={imageUrl(p.images[0]?.url) || "/images/placeholder.svg"}
                    alt=""
                  />
                  <div>
                    <strong>{p.name}</strong>
                    <p>
                      {p.collection} · {money(p.price)} · {p.stock} available
                    </p>
                    <span className="muted">
                      {p.archived
                        ? "Archived"
                        : p.demo
                          ? "Demonstration product"
                          : "Active product"}
                    </span>
                  </div>
                  <div className="button-row">
                    <button className="secondary" onClick={() => setEditing(p)}>
                      Edit
                    </button>
                    <Action
                      disabled={p.archived}
                      className="text-button"
                      onClick={async () => {
                        await api("/admin/products/" + p.id, {
                          method: "DELETE",
                        });
                        products.reload();
                      }}
                    >
                      Archive
                    </Action>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div>
              {orders.data?.content.map((o) => (
                <AdminOrder key={o.id} o={o} reload={orders.reload} />
              ))}
              {!orders.data?.content.length && (
                <p className="empty">Orders will appear here.</p>
              )}
            </div>
          )}
          <div className="pagination">
            <button disabled={!page} onClick={() => setPage((p) => p - 1)}>
              Previous
            </button>
            <span>Page {page + 1}</span>
            <button
              disabled={page + 1 >= (data.data?.totalPages || 1)}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </button>
          </div>
        </>
      )}
      {editing && (
        <Modal
          title={editing.id ? "Edit saree" : "Add a saree"}
          onClose={() => setEditing(null)}
        >
          <ProductEditor
            initial={editing}
            categories={categories.data || []}
            onSave={() => {
              setEditing(null);
              products.reload();
            }}
          />
        </Modal>
      )}
    </main>
  );
}
function ProductEditor({ initial, categories, onSave }) {
  const [images, setImages] = useState(initial.images || []),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false),
    [uploading, setUploading] = useState(false);
  const [specs, setSpecs] = useState(
    Object.entries(initial.specifications || {}),
  );
  return (
    <form
      onSubmit={async (e) => {
        e.preventDefault();
        setError("");
        setBusy(true);
        try {
          const f = Object.fromEntries(new FormData(e.currentTarget));
          const body = {
            ...f,
            price: Number(f.price),
            originalPrice: f.originalPrice ? Number(f.originalPrice) : null,
            stock: Number(f.stock),
            categoryId: f.categoryId ? Number(f.categoryId) : null,
            demo: f.demo === "on",
            images: images.map(({ url, label }) => ({ url, label })),
            specifications: Object.fromEntries(
              specs.filter(([k, v]) => k.trim() && v.trim()),
            ),
          };
          await api("/admin/products" + (initial.id ? "/" + initial.id : ""), {
            method: initial.id ? "PUT" : "POST",
            body,
          });
          onSave();
        } catch (err) {
          setError(err.message);
        } finally {
          setBusy(false);
        }
      }}
    >
      <div className="form-grid">
        <label className="wide">
          Product name
          <input
            name="name"
            defaultValue={initial.name}
            maxLength={200}
            required
          />
        </label>
        <label>
          Collection
          <input
            name="collection"
            defaultValue={initial.collection}
            required
            maxLength={100}
          />
        </label>
        <label>
          Category
          <select name="categoryId" defaultValue={initial.categoryId || ""}>
            <option value="">Select category</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Colour
          <input name="colour" defaultValue={initial.colour} maxLength={60} />
        </label>
        <label>
          Occasion
          <input
            name="occasion"
            defaultValue={initial.occasion}
            maxLength={60}
          />
        </label>
        <label>
          Selling price (₹)
          <input
            type="number"
            min="0.01"
            step="0.01"
            name="price"
            defaultValue={initial.price}
            required
          />
        </label>
        <label>
          Genuine original price (optional)
          <input
            type="number"
            min="0.01"
            step="0.01"
            name="originalPrice"
            defaultValue={initial.originalPrice || ""}
          />
        </label>
        <label>
          Available stock
          <input
            type="number"
            min="0"
            step="1"
            name="stock"
            defaultValue={initial.stock}
            required
          />
        </label>
        <label className="checkbox">
          <input type="checkbox" name="demo" defaultChecked={initial.demo} />{" "}
          Demonstration product
        </label>
        <label className="wide">
          Description
          <textarea
            name="description"
            defaultValue={initial.description}
            maxLength={4000}
            required
          />
        </label>
      </div>
      <h3>Supplied specifications</h3>
      <p className="muted">
        Add material, weave, border colour, pallu details, zari details, length,
        blouse, weight and care only when verified.
      </p>
      {specs.map(([k, v], i) => (
        <div className="spec-editor" key={i}>
          <input
            aria-label={"Specification name " + (i + 1)}
            placeholder="e.g. Material"
            value={k}
            maxLength={100}
            onChange={(e) =>
              setSpecs(specs.map((s, n) => (n === i ? [e.target.value, v] : s)))
            }
          />
          <input
            aria-label={"Specification value " + (i + 1)}
            placeholder="Supplied value"
            value={v}
            maxLength={2000}
            onChange={(e) =>
              setSpecs(specs.map((s, n) => (n === i ? [k, e.target.value] : s)))
            }
          />
          <button
            type="button"
            className="text-button"
            onClick={() => setSpecs(specs.filter((s, n) => n !== i))}
            aria-label="Remove specification"
          >
            ×
          </button>
        </div>
      ))}
      <button
        type="button"
        className="secondary"
        disabled={specs.length >= 25}
        onClick={() => setSpecs([...specs, ["", ""]])}
      >
        Add specification
      </button>
      <h3>Product photography</h3>
      <p className="muted">
        First image is the cover. Add real body, border, pallu and blouse views.
        PNG/JPEG, up to 8 MB.
      </p>
      {images.map((im, i) => (
        <div className="image-editor" key={i}>
          <img src={imageUrl(im.url)} alt={im.label} />
          <input
            aria-label={"Image description " + (i + 1)}
            value={im.label}
            required
            maxLength={100}
            onChange={(e) =>
              setImages(
                images.map((v, n) =>
                  n === i ? { ...v, label: e.target.value } : v,
                ),
              )
            }
          />
          <button
            type="button"
            className="text-button"
            disabled={i === 0}
            onClick={() => setImages([im, ...images.filter((v, n) => n !== i)])}
          >
            Make cover
          </button>
          <button
            type="button"
            className="text-button"
            onClick={() => setImages(images.filter((v, n) => n !== i))}
          >
            Remove
          </button>
        </div>
      ))}
      <label>
        Upload photo
        <input
          type="file"
          accept="image/png,image/jpeg"
          disabled={uploading || images.length >= 12}
          onChange={async (e) => {
            const f = e.target.files?.[0];
            if (!f) return;
            setError("");
            setUploading(true);
            try {
              const body = new FormData();
              body.append("file", f);
              const result = await api("/admin/images", {
                method: "POST",
                body,
              });
              setImages((v) => [
                ...v,
                { url: result.url, label: "Product view" },
              ]);
            } catch (err) {
              setError(err.message);
            } finally {
              setUploading(false);
              e.target.value = "";
            }
          }}
        />
      </label>
      {uploading && <p role="status">Uploading image…</p>}
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      <button className="primary full" disabled={busy || uploading}>
        {busy ? "Saving…" : "Save product"}
      </button>
    </form>
  );
}
function AdminOrder({ o, reload }) {
  const [payments, setPayments] = useState(null);
  return (
    <article className="panel admin-order">
      <div className="section-heading">
        <strong>{o.id}</strong>
        <span className="status">{o.status}</span>
      </div>
      <p>
        {o.delivery.name} · {o.delivery.email} · {o.delivery.phone}
      </p>
      <p>
        {o.delivery.line1}, {o.delivery.line2}, {o.delivery.city},{" "}
        {o.delivery.state}, {o.delivery.pin}
      </p>
      {o.items.map((i) => (
        <p key={i.id}>
          {i.name} × {i.quantity} — {money(i.unitPrice * i.quantity)}
        </p>
      ))}
      <strong>Total {money(o.total)}</strong>
      <div className="button-row">
        <label>
          Fulfilment
          <select
            value={o.fulfillment}
            disabled={o.status !== "PAID"}
            onChange={async (e) => {
              try {
                await api("/admin/orders/" + o.id + "/fulfillment", {
                  method: "PUT",
                  body: { status: e.target.value },
                });
                reload();
              } catch (err) {
                alert(err.message);
              }
            }}
          >
            {["UNFULFILLED", "PACKING", "SHIPPED", "DELIVERED"].map((s) => (
              <option key={s}>{s}</option>
            ))}
          </select>
        </label>
        <Action
          className="secondary"
          onClick={async () =>
            setPayments(await api("/admin/orders/" + o.id + "/payments"))
          }
        >
          Payment records
        </Action>
        <Action
          className="secondary"
          onClick={async () => {
            await api("/admin/orders/" + o.id + "/refresh", { method: "POST" });
            reload();
          }}
        >
          Reconcile payment
        </Action>
      </div>
      {payments && (
        <div>
          {payments.length ? (
            payments.map((p) => (
              <p key={p.id}>
                {p.id} · {p.status} · {money(p.amount / 100)}
              </p>
            ))
          ) : (
            <p>No payment attempts recorded.</p>
          )}
        </div>
      )}
      {o.status === "PAID_REVIEW" && (
        <p className="error">
          Manual review required. Check Razorpay and arrange fulfilment or a
          refund; do not ship without reconciliation.
        </p>
      )}
    </article>
  );
}
function Categories({ categories }) {
  const [editing, setEditing] = useState(null),
    [error, setError] = useState("");
  return (
    <div className="panel">
      <h2>Collections &amp; categories</h2>
      {categories.data?.map((c) => (
        <div className="review-line" key={c.id}>
          <span>
            {c.name} · {c.slug}
          </span>
          <button className="text-button" onClick={() => setEditing(c)}>
            Edit
          </button>
        </div>
      ))}
      <form
        key={editing?.id || "new"}
        onSubmit={async (e) => {
          e.preventDefault();
          try {
            await api("/admin/categories" + (editing ? "/" + editing.id : ""), {
              method: editing ? "PUT" : "POST",
              body: Object.fromEntries(new FormData(e.currentTarget)),
            });
            setEditing(null);
            categories.reload();
            e.target.reset();
          } catch (err) {
            setError(err.message);
          }
        }}
      >
        <h3>{editing ? "Edit category" : "Add category"}</h3>
        <label>
          Name
          <input
            name="name"
            defaultValue={editing?.name || ""}
            required
            maxLength={100}
          />
        </label>
        <label>
          Slug
          <input
            name="slug"
            defaultValue={editing?.slug || ""}
            pattern="[a-z0-9-]+"
            required
            maxLength={100}
          />
        </label>
        {error && <p className="error">{error}</p>}
        <button className="primary">Save category</button>
      </form>
    </div>
  );
}
