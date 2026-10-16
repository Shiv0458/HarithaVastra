import { describe, it, expect } from "vitest";
import { money, imageUrl } from "./api";
describe("presentation helpers", () => {
  it("formats Indian currency without floating point display artifacts", () => {
    expect(money(18500)).toBe("₹18,500.00");
    expect(money(0.1 + 0.2)).toBe("₹0.30");
  });
  it("preserves catalogue images", () => {
    expect(imageUrl("/images/maroon.webp")).toBe("/images/maroon.webp");
  });
});
