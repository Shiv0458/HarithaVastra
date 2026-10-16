import React, { useEffect } from "react";
import { createRoot } from "react-dom/client";
import {
  BrowserRouter,
  Routes,
  Route,
  useLocation,
  Link,
  useParams,
} from "react-router-dom";
import { Store, useStore } from "./store";
import { Header, Footer, About } from "./components";
import { Storefront, ProductPage, Wishlist } from "./Storefront";
import { CartPage, CheckoutPage, OrderPage } from "./Checkout";
import { Account } from "./Account";
import { Admin } from "./Admin";
import "./styles.css";
class ErrorBoundary extends React.Component {
  state = { error: false };
  static getDerivedStateFromError() {
    return { error: true };
  }
  render() {
    return this.state.error ? (
      <main className="page">
        <h1>Something went wrong.</h1>
        <p>Please reload the page to try again.</p>
        <button onClick={() => window.location.reload()}>Reload</button>
      </main>
    ) : (
      this.props.children
    );
  }
}
function Policy() {
  const { slug } = useParams();
  const names = {
    "shipping-policy": "Shipping Policy",
    "returns-and-refunds": "Returns and Refunds",
    "privacy-policy": "Privacy Policy",
    "terms-and-conditions": "Terms and Conditions",
  };
  return (
    <main className="page narrow" id="main">
      <p className="eyebrow">CUSTOMER CARE</p>
      <h1>{names[slug] || "Store information"}</h1>
      <div className="notice">
        <strong>Placeholder — pending store-owner approval</strong>
        <p>
          This is not a published policy. Complete and approve this page before
          enabling live sales.
        </p>
      </div>
      <p>
        {
          {
            "shipping-policy":
              "Add shipping regions, dispatch timelines, delivery estimates, charges and tracking procedures.",
            "returns-and-refunds":
              "Add return eligibility, timelines, exclusions, damaged-item handling and refund processing details.",
            "privacy-policy":
              "Add the business identity, data collection purposes, payment processors, retention periods and customer privacy contact.",
            "terms-and-conditions":
              "Add business contact details, order acceptance, pricing, cancellations, responsibilities and applicable terms.",
          }[slug]
        }
      </p>
      <Link to="/contact">Contact information</Link>
    </main>
  );
}
function Contact() {
  return (
    <main className="page narrow" id="main">
      <p className="eyebrow">WE’D LOVE TO HELP</p>
      <h1>Let’s find your saree.</h1>
      <div className="panel">
        <p>
          Email: <strong>Pending — add your business email</strong>
        </p>
        <p>
          WhatsApp / phone: <strong>Pending — add your business number</strong>
        </p>
        <p>
          Business address: <strong>Pending — add your public address</strong>
        </p>
        <a
          className="secondary"
          href="https://www.instagram.com/harithavastra_handlooms/"
          target="_blank"
          rel="noreferrer"
        >
          Visit Instagram ↗
        </a>
      </div>
    </main>
  );
}
function App() {
  const { notice, offline, initialize } = useStore();
  const location = useLocation();
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [location.pathname]);
  return (
    <>
      <a className="skip-link" href="#main">
        Skip to content
      </a>
      <Header />
      {offline && (
        <div className="connection-error" role="alert">
          Cannot connect to the store: {offline}{" "}
          <button onClick={initialize}>Retry connection</button>
        </div>
      )}
      <Routes>
        <Route path="/" element={<Storefront />} />
        <Route path="/products/:id" element={<ProductPage />} />
        <Route path="/wishlist" element={<Wishlist />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/checkout" element={<CheckoutPage />} />
        <Route path="/account" element={<Account />} />
        <Route path="/orders/:id" element={<OrderPage />} />
        <Route path="/admin" element={<Admin />} />
        <Route
          path="/about"
          element={
            <main id="main">
              <About />
            </main>
          }
        />
        <Route path="/contact" element={<Contact />} />
        <Route path="/policies/:slug" element={<Policy />} />
        <Route
          path="*"
          element={
            <main id="main" className="empty">
              <h1>Page not found.</h1>
              <Link to="/">Back to the collection</Link>
            </main>
          }
        />
      </Routes>
      <Footer />
      {notice && (
        <div className="toast" role="status">
          {notice}
        </div>
      )}
    </>
  );
}
createRoot(document.getElementById("root")).render(
  <ErrorBoundary>
    <BrowserRouter>
      <Store>
        <App />
      </Store>
    </BrowserRouter>
  </ErrorBoundary>,
);
