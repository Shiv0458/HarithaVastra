import { createUuid } from "./uuid";
import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
} from "react";
import { api } from "./api";
const Context = createContext(null);
export function Store({ children }) {
  const [user, setUser] = useState(null),
    [cart, setCart] = useState([]),
    [wishlist, setWishlist] = useState([]),
    [config, setConfig] = useState(null),
    [notice, setNotice] = useState(""),
    [ready, setReady] = useState(false),
    [offline, setOffline] = useState("");
  const notify = useCallback((message) => setNotice(message), []);
  const refresh = useCallback(async () => {
    const c = await api("/cart");
    const w = await api("/wishlist");
    setCart(c);
    setWishlist(w);
  }, []);
  const initialize = useCallback(async () => {
    setOffline("");
    try {
      if (sessionStorage.getItem("hv-token")) {
        try {
          setUser(await api("/account"));
        } catch (e) {
          if (e.status === 401) sessionStorage.removeItem("hv-token");
          else throw e;
        }
      }
      setConfig(await api("/config"));
      await refresh();
    } catch (e) {
      setOffline(e.message);
    } finally {
      setReady(true);
    }
  }, [refresh]);
  useEffect(() => {
    initialize();
  }, [initialize]);
  useEffect(() => {
    if (!notice) return;
    const id = setTimeout(() => setNotice(""), 4500);
    return () => clearTimeout(id);
  }, [notice]);
  async function login(mode, body) {
    const result = await api("/auth/" + mode, { method: "POST", body });
    sessionStorage.setItem("hv-token", result.token);
    setUser(result.user);
    await refresh();
    notify("Welcome, " + result.user.name);
  }
  async function logout() {
    await api("/auth/logout", { method: "POST" });
    sessionStorage.removeItem("hv-token");
    localStorage.setItem("hv-guest", createUuid());
    setUser(null);
    await refresh();
  }
  async function quantity(id, value) {
    const c = await api("/cart/items/" + id, {
      method: "PUT",
      body: { quantity: value },
    });
    setCart(c);
  }
  async function add(product, qty = 1) {
    const old = cart.find((i) => i.product.id === product.id)?.quantity || 0;
    await quantity(product.id, old + qty);
    notify("Added to your bag");
  }
  async function wish(product) {
    const exists = wishlist.some((p) => p.id === product.id);
    setWishlist(
      await api("/wishlist/" + product.id, {
        method: exists ? "DELETE" : "PUT",
      }),
    );
    notify(exists ? "Removed from wishlist" : "Saved to your wishlist");
  }
  async function move(id) {
    const r = await api("/wishlist/" + id + "/move", { method: "POST" });
    setCart(r.cart);
    setWishlist(r.wishlist);
    notify("Moved to your bag");
  }
  return (
    <Context.Provider
      value={{
        user,
        setUser,
        cart,
        wishlist,
        config,
        notice,
        notify,
        ready,
        offline,
        initialize,
        refresh,
        login,
        logout,
        quantity,
        add,
        wish,
        move,
      }}
    >
      {children}
    </Context.Provider>
  );
}
export const useStore = () => useContext(Context);
export function useResource(path) {
  const [data, setData] = useState(null),
    [error, setError] = useState(""),
    [loading, setLoading] = useState(true),
    [tick, setTick] = useState(0);
  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    api(path)
      .then((v) => {
        if (active) setData(v);
      })
      .catch((e) => {
        if (active) setError(e.message);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [path, tick]);
  return { data, error, loading, reload: () => setTick((v) => v + 1), setData };
}
