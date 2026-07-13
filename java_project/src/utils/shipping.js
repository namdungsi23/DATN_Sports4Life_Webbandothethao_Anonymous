export const FREE_SHIP_THRESHOLD = 499_000;
export const DEFAULT_SHIPPING_FEE = 30_000;

export function calcShippingFee(subTotal) {
  return Number(subTotal || 0) >= FREE_SHIP_THRESHOLD ? 0 : DEFAULT_SHIPPING_FEE;
}

export function calcOrderTotal(subTotal) {
  return Number(subTotal || 0) + calcShippingFee(subTotal);
}
