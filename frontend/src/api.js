import { createUuid } from "./uuid";
export const API_BASE = import.meta.env.VITE_API_BASE || "";
export const guest = () => {
  let value = localStorage.getItem("hv-guest");
  if (!value) {
    value = createUuid();
    localStorage.setItem("hv-guest", value);
  }
  return value;
};
export async function api(
  path,
  { method = "GET", body, headers = {}, signal } = {},
) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 30000);
  const abort = () => controller.abort();
  signal?.addEventListener("abort", abort);
  const token = sessionStorage.getItem("hv-token");
  try {
    const response = await fetch(`${API_BASE}/api${path}`, {
      method,
      signal: controller.signal,
      headers: {
        "X-Guest-Token": guest(),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(body && !(body instanceof FormData)
          ? { "Content-Type": "application/json" }
          : {}),
        ...headers,
      },
      body:
        body instanceof FormData
          ? body
          : body
            ? JSON.stringify(body)
            : undefined,
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      const e = new Error(data.message || "Request failed. Please try again.");
      e.status = response.status;
      e.fields = data.fields;
      throw e;
    }
    return data;
  } catch (e) {
    if (e.name === "AbortError")
      throw new Error("The request took too long. Please retry.");
    throw e;
  } finally {
    clearTimeout(timeout);
    signal?.removeEventListener("abort", abort);
  }
}
export const money = (value) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(Number(value || 0));
export const imageUrl = (url) =>
  url?.startsWith("/uploads/") ? API_BASE + url : url;
