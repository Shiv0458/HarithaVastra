import React, { useState, useRef, useEffect } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import {
  Search,
  Heart,
  ShoppingBag,
  UserRound,
  Menu,
  X,
  ArrowUpRight,
  ArrowRight,
  Plus,
  Minus,
  Check,
  Leaf,
  ChevronDown,
} from "lucide-react";
import { useStore } from "./store";
import { money, imageUrl } from "./api";
export function Action({ children, onClick, className = "", ...props }) {
  const [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  return (
    <>
      <button
        {...props}
        className={className}
        disabled={props.disabled || busy}
        onClick={async (e) => {
          setError("");
          setBusy(true);
          try {
            await onClick?.(e);
          } catch (err) {
            setError(err.message);
          } finally {
            setBusy(false);
          }
        }}
      >
        {busy ? "Please wait…" : children}
      </button>
      {error && (
        <span className="inline-error" role="alert">
          {error}
        </span>
      )}
    </>
  );
}
export function Feedback({ loading, error, retry }) {
  return loading ? (
    <div className="loading" role="status">
      <span className="spinner" /> Gathering something beautiful…
    </div>
  ) : error ? (
    <div className="error" role="alert">
      <p>{error}</p>
      {retry && <button onClick={retry}>Try again</button>}
    </div>
  ) : null;
}
export function Modal({ title, children, onClose }) {
  const ref = useRef(null);
  useEffect(() => {
    const old = document.activeElement;
    const dialog = ref.current;
    dialog.showModal();
    return () => {
      dialog.close();
      old?.focus();
    };
  }, []);
  return (
    <dialog
      ref={ref}
      onCancel={onClose}
      onClick={(e) => {
        if (e.target === ref.current) onClose();
      }}
      aria-label={title}
    >
      <div className="modal-title">
        <h2>{title}</h2>
        <button className="icon-button" onClick={onClose} aria-label="Close">
          <X />
        </button>
      </div>
      {children}
    </dialog>
  );
}
export function Price({ p }) {
  return (
    <div className="price">
      <strong>{money(p.price)}</strong>
      {p.discountPercent > 0 && (
        <>
          <del>{money(p.originalPrice)}</del>
          <span>{p.discountPercent}% off</span>
        </>
      )}
    </div>
  );
}
export function ProductCard({ p, move = false, index = 10 }) {
  const { wishlist, wish, add, move: moveWish } = useStore();
  const liked = wishlist.some((v) => v.id === p.id);
  return (
    <article className="product-card">
      <div className="product-visual">
        <Link to={"/products/" + p.id} tabIndex={-1} aria-label={p.name}>
          <img
            src={imageUrl(p.images?.[0]?.url) || "/images/placeholder.svg"}
            alt={
              p.images?.[0]?.label ? `${p.name} — ${p.images[0].label}` : p.name
            }
            loading={index < 3 ? "eager" : "lazy"}
            width="600"
            height="800"
          />
        </Link>
        <div className="image-badges">
          {p.demo && <span>DEMO</span>}
          {!p.stock && <span>Sold out</span>}
        </div>
        <Action
          className={"heart " + (liked ? "liked" : "")}
          onClick={() => wish(p)}
          aria-label={`${liked ? "Remove" : "Save"} ${p.name} ${liked ? "from" : "to"} wishlist`}
          aria-pressed={liked}
        >
          <Heart size={19} fill={liked ? "currentColor" : "none"} />
        </Action>
      </div>
      <div className="product-copy">
        <p className="eyebrow">{p.collection}</p>
        <Link className="product-name" to={"/products/" + p.id}>
          {p.name}
        </Link>
        <p className="product-description">{p.description}</p>
        <Price p={p} />
        <p className="review-note">
          {p.reviewCount
            ? `★ ${Number(p.rating).toFixed(1)} · ${p.reviewCount} reviews`
            : "No reviews yet"}
          <span className={p.stock ? "stock" : "out"}>
            {p.stock ? "Available" : "Sold out"}
          </span>
        </p>
        <Action
          className="add-button"
          disabled={!p.stock}
          onClick={() => (move ? moveWish(p.id) : add(p))}
        >
          {move ? "Move to bag" : "Add to bag"}
          <Plus size={16} />
        </Action>
      </div>
    </article>
  );
}
export const collections = [
  ["All Sarees", ""],
  ["Kanchipattu Sarees", "Kanchipattu"],
  ["Dharmavaram Sarees", "Dharmavaram"],
  ["Bridal Collection", "Bridal"],
  ["Festive Collection", "Festive"],
  ["New Arrivals", "New"],
];
export function CategoryLinks({ close }) {
  return (
    <nav aria-label="Collections" className="category-links">
      {collections.map(([label, key]) => (
        <Link
          key={label}
          to={
            "/" +
            (key
              ? `?${key === "Bridal" || key === "Festive" ? "occasion" : key === "New" ? "newest" : "collection"}=${key === "New" ? "true" : key}`
              : "")
          }
          onClick={close}
        >
          {label}
          <ArrowUpRight size={14} />
        </Link>
      ))}
    </nav>
  );
}
export function Header() {
  const { cart, wishlist, user, config } = useStore();
  const [drawer, setDrawer] = useState(false),
    [q, setQ] = useState("");
  const navigate = useNavigate();
  return (
    <>
      <div className="announcement">
        A celebration of colour, craft &amp; tradition{" "}
        <span>
          Explore our silk collections <ArrowRight size={13} />
        </span>
      </div>
      <header className="header">
        <div className="header-top">
          <button
            className="icon-button mobile-only"
            onClick={() => setDrawer(true)}
            aria-label="Open collections"
          >
            <Menu />
          </button>
          <Link className="brand" to="/">
            Haritha Vastra<span>PURE HANDLOOM SILK SAREES</span>
          </Link>
          <nav className="header-actions" aria-label="Your shopping">
            <Link to="/account" aria-label="Your account">
              <UserRound />
              <span className="desktop-label">
                {user ? user.name.split(" ")[0] : "Account"}
              </span>
            </Link>
            <Link
              to="/wishlist"
              aria-label={`Wishlist, ${wishlist.length} items`}
            >
              <Heart />
              <b>{wishlist.length}</b>
            </Link>
            <Link
              to="/cart"
              aria-label={`Shopping bag, ${cart.reduce((n, i) => n + i.quantity, 0)} items`}
            >
              <ShoppingBag />
              <b>{cart.reduce((n, i) => n + i.quantity, 0)}</b>
            </Link>
          </nav>
        </div>
        <form
          className="search"
          onSubmit={(e) => {
            e.preventDefault();
            navigate("/?q=" + encodeURIComponent(q));
          }}
          role="search"
        >
          <Search size={20} />
          <input
            aria-label="Search sarees"
            placeholder="Search a colour, a collection, a saree you love…"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            list="search-suggestions"
          />
          <datalist id="search-suggestions">
            <option value="Kanchipattu" />
            <option value="Dharmavaram" />
            <option value="Maroon" />
            <option value="Emerald" />
            <option value="Bridal" />
          </datalist>
          {q && (
            <button
              type="button"
              className="icon-button"
              aria-label="Clear search"
              onClick={() => setQ("")}
            >
              <X size={16} />
            </button>
          )}
          <button type="submit" className="search-submit">
            Search <ArrowRight size={17} />
          </button>
        </form>
      </header>
      {config?.demo && (
        <div className="demo-notice">
          DEMONSTRATION STORE · Illustrative products and AI-generated
          photography ·{" "}
          {config.paymentMode === "test"
            ? "Test payments only"
            : "Live payment configuration enabled — do not purchase demo products"}
        </div>
      )}
      {drawer && (
        <Modal
          title="Discover our collections"
          onClose={() => setDrawer(false)}
        >
          <CategoryLinks close={() => setDrawer(false)} />
        </Modal>
      )}
    </>
  );
}
export function Footer() {
  return (
    <footer>
      <div className="footer-brand">
        <Link className="brand" to="/">
          Haritha Vastra<span>PURE HANDLOOM SILK SAREES</span>
        </Link>
        <p>For the moments you’ll always remember.</p>
      </div>
      <div>
        <p className="eyebrow">EXPLORE</p>
        <Link to="/about">About us</Link>
        <Link to="/contact">Contact us</Link>
        <Link to="/account">Your account</Link>
      </div>
      <div>
        <p className="eyebrow">CUSTOMER CARE</p>
        {[
          "Shipping Policy",
          "Returns and Refunds",
          "Privacy Policy",
          "Terms and Conditions",
        ].map((t) => (
          <Link
            key={t}
            to={"/policies/" + t.toLowerCase().replaceAll(" ", "-")}
          >
            {t}
          </Link>
        ))}
      </div>
      <div>
        <p className="eyebrow">STAY CONNECTED</p>
        <a
          href="https://www.instagram.com/harithavastra_handlooms/"
          target="_blank"
          rel="noreferrer"
        >
          Instagram ↗
        </a>
        <Link to="/contact">WhatsApp — number to be added</Link>
        <p className="muted">Contact details pending setup.</p>
      </div>
      <div className="footer-bottom">
        © {new Date().getFullYear()} Haritha Vastra
        <span>Woven with tradition. Chosen with love.</span>
      </div>
    </footer>
  );
}
export function About() {
  return (
    <section className="about" id="about">
      <div className="about-title">
        <p className="eyebrow">THE HARITHA VASTRA EDIT</p>
        <h2>Tradition, in every thread.</h2>
        <p>
          Discover Pure Handloom Kanchipattu and Dharmavaram Silk Sarees — rich
          colours, intricate motifs and thoughtful choices for weddings,
          festivals and the moments that matter.
        </p>
        <Link to="/about">
          Get to know Haritha Vastra <ArrowUpRight size={17} />
        </Link>
      </div>
      <div className="about-grid">
        {[
          [
            "01",
            "Kanchipattu Collection",
            "Explore richly coloured silk sarees, striking borders and motifs that bring a sense of occasion to every drape.",
          ],
          [
            "02",
            "Dharmavaram Collection",
            "Discover expressive colour combinations and detailed pallus, with sarees to make your celebrations feel personal.",
          ],
          [
            "03",
            "The Beauty of Handloom",
            "The character of a weave lives in its small details. Refer to each product’s supplied specifications for its material and weave.",
          ],
          [
            "04",
            "Saree Care",
            "Follow the care label supplied with your saree. Store away from direct sunlight and ask us about care before cleaning.",
          ],
        ].map(([n, title, text]) => (
          <div key={n}>
            <span>{n}</span>
            <h3>{title}</h3>
            <p>{text}</p>
          </div>
        ))}
      </div>
    </section>
  );
}
export function TotalsBox({ totals }) {
  return (
    <div className="totals">
      {[
        ["Subtotal", totals.subtotal],
        ["Discount", -totals.discount],
        ["Shipping", totals.shipping],
        ["Tax", totals.tax],
      ].map(([label, value]) => (
        <div key={label}>
          <span>{label}</span>
          <span>
            {label === "Shipping" && Number(value) === 0
              ? "₹0.00"
              : money(value)}
          </span>
        </div>
      ))}
      <div className="total">
        <strong>Total</strong>
        <strong>{money(totals.total)}</strong>
      </div>
      <small>
        Prices and availability are checked by the server. Shipping and tax
        reflect the current store configuration.
      </small>
    </div>
  );
}
export function Quantity({ value, onChange, max = 10 }) {
  return (
    <div className="quantity">
      <button
        type="button"
        disabled={value <= 1}
        onClick={() => onChange(value - 1)}
        aria-label="Reduce quantity"
      >
        <Minus size={15} />
      </button>
      <output aria-label="Quantity">{value}</output>
      <button
        type="button"
        disabled={value >= Math.min(10, max)}
        onClick={() => onChange(value + 1)}
        aria-label="Increase quantity"
      >
        <Plus size={15} />
      </button>
    </div>
  );
}
