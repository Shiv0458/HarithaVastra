import React, { useState, useEffect } from "react";
import {
  Link,
  useSearchParams,
  useParams,
  useNavigate,
} from "react-router-dom";
import {
  SlidersHorizontal,
  X,
  ArrowRight,
  Heart,
  ZoomIn,
  Check,
  ChevronRight,
} from "lucide-react";
import { useResource, useStore } from "./store";
import { api, money, imageUrl } from "./api";
import {
  Action,
  Feedback,
  ProductCard,
  CategoryLinks,
  About,
  Price,
  Modal,
  Quantity,
} from "./components";
export function Storefront() {
  const [params, setParams] = useSearchParams();
  const [filters, setFilters] = useState(false);
  const { data, error, loading, reload } = useResource(
    "/products?" + params.toString(),
  );
  const categories = useResource("/products/facets");
  const update = (key, value) => {
    const next = new URLSearchParams(params);
    value ? next.set(key, value) : next.delete(key);
    next.delete("page");
    setParams(next);
  };
  const active = [...params.entries()].filter(
    ([k, v]) => !["sort", "page"].includes(k) && v,
  );
  const count = data?.total || 0;
  const FilterFields = () => (
    <>
      <div className="filter-heading">
        <h3>Refine your selection</h3>
        <button className="text-button" onClick={() => setParams({})}>
          Clear all
        </button>
      </div>
      <label>
        Collection
        <select
          value={params.get("collection") || ""}
          onChange={(e) => update("collection", e.target.value)}
        >
          <option value="">All collections</option>
          {(categories.data?.collections || []).map((c) => (
            <option key={c}>{c}</option>
          ))}
        </select>
      </label>
      <label>
        Colour
        <select
          value={params.get("colour") || ""}
          onChange={(e) => update("colour", e.target.value)}
        >
          <option value="">All colours</option>
          {(categories.data?.colours || []).map((c) => (
            <option key={c}>{c}</option>
          ))}
        </select>
      </label>
      <label>
        Occasion
        <select
          value={params.get("occasion") || ""}
          onChange={(e) => update("occasion", e.target.value)}
        >
          <option value="">Every occasion</option>
          {(categories.data?.occasions || []).map((o) => (
            <option key={o}>{o}</option>
          ))}
        </select>
      </label>
      <label>
        Price range
        <select
          value={params.get("max") || ""}
          onChange={(e) => update("max", e.target.value)}
        >
          <option value="">Any price</option>
          <option value="15000">Under ₹15,000</option>
          <option value="20000">Under ₹20,000</option>
          <option value="30000">Under ₹30,000</option>
        </select>
      </label>
      <label className="checkbox">
        <input
          type="checkbox"
          checked={params.get("available") === "true"}
          onChange={(e) => update("available", e.target.checked ? "true" : "")}
        />{" "}
        In stock only
      </label>
    </>
  );
  return (
    <>
      <div className="shop-layout">
        <aside className="sidebar">
          <p className="eyebrow">OUR COLLECTIONS</p>
          <CategoryLinks />
          <div className="filters">
            <FilterFields />
          </div>
          <div className="sidebar-note">
            <span>THE JOY OF A BEAUTIFUL DRAPE</span>
            <p>
              A little tradition.
              <br />A little you.
            </p>
          </div>
        </aside>
        <main id="main" className="catalog">
          <div className="catalog-title">
            <div>
              <p className="eyebrow">SAREES TO TREASURE</p>
              <h1>
                {params.get("q")
                  ? `Results for “${params.get("q")}”`
                  : params.get("collection")
                    ? params.get("collection") + " Sarees"
                    : params.get("occasion")
                      ? params.get("occasion") + " Collection"
                      : params.get("newest")
                        ? "New Arrivals"
                        : "Find your timeless favourite."}
              </h1>
              <p>
                Rich silks. Beautiful details. Made for your special moments.
              </p>
            </div>
            <span className="edition">
              THE SILK
              <br />
              COLLECTION
            </span>
          </div>
          <div className="catalog-toolbar">
            <p>
              {loading
                ? "Exploring the collection…"
                : `${count} sarees to discover`}
            </p>
            <div>
              <button
                className="filter-button"
                onClick={() => setFilters(true)}
              >
                <SlidersHorizontal size={16} /> Filters
              </button>
              <label className="sort">
                Sort by{" "}
                <select
                  value={params.get("sort") || "relevance"}
                  onChange={(e) => update("sort", e.target.value)}
                >
                  <option value="relevance">Relevance</option>
                  <option value="newest">Newest arrivals</option>
                  <option value="price-asc">Price: low to high</option>
                  <option value="price-desc">Price: high to low</option>
                  <option value="rating">Customer rating</option>
                </select>
              </label>
            </div>
          </div>
          {active.length > 0 && (
            <div className="active-filters">
              {active.map(([k, v]) => (
                <button key={k} onClick={() => update(k, "")}>
                  {k === "available"
                    ? "In stock"
                    : k === "newest"
                      ? "New arrivals"
                      : `${k}: ${v}`}
                  <X size={13} />
                </button>
              ))}
              <button onClick={() => setParams({})}>Clear all</button>
            </div>
          )}
          <Feedback loading={loading} error={error} retry={reload} />
          {!loading &&
            !error &&
            (count ? (
              <>
                <div className="product-grid">
                  {data.items.map((p, i) => (
                    <ProductCard key={p.id} p={p} index={i} />
                  ))}
                </div>
                <div className="pagination">
                  {Array.from({ length: data.pages }, (_, i) => (
                    <button
                      className={data.page === i ? "selected" : ""}
                      key={i}
                      aria-label={"Page " + (i + 1)}
                      aria-current={data.page === i ? "page" : undefined}
                      onClick={() => {
                        const n = new URLSearchParams(params);
                        n.set("page", i);
                        setParams(n);
                        window.scrollTo({ top: 0, behavior: "smooth" });
                      }}
                    >
                      {i + 1}
                    </button>
                  ))}
                </div>
              </>
            ) : (
              <div className="empty">
                <h2>No sarees match just yet.</h2>
                <p>Try another colour or remove a filter to explore more.</p>
                <button onClick={() => setParams({})}>
                  Explore all sarees
                </button>
              </div>
            ))}
        </main>
      </div>
      <About />
      {filters && (
        <Modal title="Refine your selection" onClose={() => setFilters(false)}>
          <div className="filters">
            <FilterFields />
            <button className="primary" onClick={() => setFilters(false)}>
              Show sarees
            </button>
          </div>
        </Modal>
      )}
    </>
  );
}
export function ProductPage() {
  const { id } = useParams();
  const product = useResource("/products/" + id),
    reviews = useResource("/products/" + id + "/reviews");
  const { data: p, error, loading, reload } = product;
  const { wishlist, wish, add, user, notify } = useStore();
  const [selected, setSelected] = useState(0),
    [zoom, setZoom] = useState(false),
    [qty, setQty] = useState(1);
  const navigate = useNavigate();
  const related = useResource(
    "/products?size=4&collection=" + encodeURIComponent(p?.collection || ""),
  );
  useEffect(() => {
    setSelected(0);
    setQty(1);
  }, [id]);
  if (loading || error)
    return (
      <main className="page" id="main">
        <Feedback loading={loading} error={error} retry={reload} />
      </main>
    );
  return (
    <main className="page" id="main">
      <nav className="breadcrumb">
        <Link to="/">All sarees</Link>
        <ChevronRight size={13} />
        <span>{p.name}</span>
      </nav>
      <div className="product-detail">
        <div>
          <button
            className="gallery-main"
            onClick={() => setZoom(true)}
            aria-label="Zoom product image"
          >
            <img
              src={imageUrl(p.images[selected]?.url) || "/images/placeholder.svg"}
              alt={
                p.name + " — " + (p.images[selected]?.label || "product view")
              }
            />
            <span>
              <ZoomIn size={18} /> View details
            </span>
          </button>
          <div className="thumbnails">
            {p.images.map((im, i) => (
              <button
                key={im.id}
                onClick={() => setSelected(i)}
                className={i === selected ? "selected" : ""}
                aria-label={"Show " + im.label}
                aria-pressed={i === selected}
              >
                <img src={imageUrl(im.url)} alt="" />
                <span>{im.label}</span>
              </button>
            ))}
          </div>
          {p.demo && (
            <p className="image-disclaimer">
              Demo product · AI-generated illustrative photography. Actual body,
              border, pallu and blouse photographs must be uploaded before sale.
            </p>
          )}
        </div>
        <div className="detail-copy">
          <p className="eyebrow">{p.collection} COLLECTION</p>
          <h1>{p.name}</h1>
          <p className="lead">{p.description}</p>
          <Price p={p} />
          <p className="review-note">
            {p.reviewCount
              ? `★ ${Number(p.rating).toFixed(1)} · ${p.reviewCount} reviews`
              : "No reviews yet"}
          </p>
          <p className={p.stock ? "stock" : "out"}>
            {p.stock ? `${p.stock} available` : "Currently sold out"}
          </p>
          <div className="purchase">
            <Quantity value={qty} onChange={setQty} max={p.stock} />
            <Action
              className="primary"
              disabled={!p.stock}
              onClick={() => add(p, qty)}
            >
              Add to bag
            </Action>
            <Action
              className="icon-button outlined"
              onClick={() => wish(p)}
              aria-label="Toggle wishlist"
              aria-pressed={wishlist.some((v) => v.id === p.id)}
            >
              <Heart
                fill={
                  wishlist.some((v) => v.id === p.id) ? "currentColor" : "none"
                }
              />
            </Action>
          </div>
          <Action
            className="secondary full"
            disabled={!p.stock}
            onClick={async () => {
              await add(p, qty);
              navigate("/checkout");
            }}
          >
            Buy now <ArrowRight size={17} />
          </Action>
          <div className="delivery-note">
            <Check size={18} />
            <p>
              Delivery within India. Shipping charges are calculated at
              checkout. Dispatch times are pending store configuration.{" "}
              <Link to="/policies/shipping-policy">Shipping</Link> ·{" "}
              <Link to="/policies/returns-and-refunds">Returns</Link>
            </p>
          </div>
          <details open>
            <summary>Details of your saree</summary>
            <dl className="specs">
              {Object.entries({
                Collection: p.collection,
                Colour: p.colour,
                ...p.specifications,
              })
                .filter(([, v]) => v !== null && v !== "")
                .map(([k, v]) => (
                  <div key={k}>
                    <dt>{k}</dt>
                    <dd>{String(v)}</dd>
                  </div>
                ))}
            </dl>
          </details>
          <details>
            <summary>Care &amp; keeping</summary>
            <p>
              {p.specifications?.["Care instructions"] ||
                "Follow the care label supplied with your saree. Contact us before cleaning if care instructions have not been supplied."}
            </p>
          </details>
        </div>
      </div>
      <section className="reviews-section">
        <h2>Customer reviews</h2>
        <Feedback loading={reviews.loading} error={reviews.error} />
        {reviews.data?.length ? (
          reviews.data.map((r, i) => (
            <article className="review" key={i}>
              <strong>{r.name}</strong>
              <span>{"★".repeat(r.rating)}</span>
              <p>{r.text}</p>
            </article>
          ))
        ) : (
          <p className="muted">
            No reviews yet. Reviews are accepted from customers after delivery.
          </p>
        )}
        {user && (
          <form
            className="review-form"
            onSubmit={async (e) => {
              e.preventDefault();
              const form = e.currentTarget;
              const f = new FormData(form);
              try {
                await api("/account/reviews/" + p.id, {
                  method: "POST",
                  body: {
                    rating: Number(f.get("rating")),
                    text: f.get("text"),
                  },
                });
                form.reset();
                reviews.reload();
                product.reload();
                notify("Thank you for your review");
              } catch (err) {
                notify(err.message);
              }
            }}
          >
            <label>
              Your rating
              <select name="rating">
                {[5, 4, 3, 2, 1].map((n) => (
                  <option key={n} value={n}>
                    {n} stars
                  </option>
                ))}
              </select>
            </label>
            <label>
              Your review
              <textarea name="text" maxLength={2000} required />
            </label>
            <button className="secondary">Submit review</button>
          </form>
        )}
      </section>
      <section className="related">
        <p className="eyebrow">MORE TO FALL IN LOVE WITH</p>
        <h2>A few more, just for you.</h2>
        <div className="product-grid">
          {related.data?.items
            .filter((v) => v.id !== p.id)
            .slice(0, 3)
            .map((v) => (
              <ProductCard p={v} key={v.id} />
            ))}
        </div>
      </section>
      {zoom && (
        <Modal title={p.name} onClose={() => setZoom(false)}>
          <div className="zoom-image">
            <img src={imageUrl(p.images[selected]?.url)} alt={p.name} />
          </div>
          <p>Scroll to explore the full-size image.</p>
        </Modal>
      )}
    </main>
  );
}
export function Wishlist() {
  const { wishlist, ready } = useStore();
  return (
    <main className="page" id="main">
      <p className="eyebrow">SAVED WITH LOVE</p>
      <h1>Your wishlist</h1>
      <Feedback loading={!ready} />
      {wishlist.length ? (
        <div className="product-grid">
          {wishlist.map((p) => (
            <ProductCard p={p} move key={p.id} />
          ))}
        </div>
      ) : (
        <div className="empty">
          <Heart size={38} />
          <h2>Keep your favourites close.</h2>
          <p>Tap the heart on a saree to save it here.</p>
          <Link className="primary" to="/">
            Explore the collection
          </Link>
        </div>
      )}
    </main>
  );
}
