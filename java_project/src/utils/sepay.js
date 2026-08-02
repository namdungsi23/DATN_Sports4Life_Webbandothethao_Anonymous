const DEFAULT_STORAGE_KEY = "sepay_checkout_pending";

export function saveSePayCheckout(sepay, { storageKey = DEFAULT_STORAGE_KEY, completionToken = null } = {}) {
  if (!sepay?.action || !sepay?.fields) {
    throw new Error("Thiếu dữ liệu thanh toán SePay");
  }
  sessionStorage.setItem(
    storageKey,
    JSON.stringify({
      ...sepay,
      completionToken: completionToken || null,
    })
  );
}

export function loadSePayCheckout(storageKey = DEFAULT_STORAGE_KEY) {
  try {
    const raw = sessionStorage.getItem(storageKey);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function loadSePayCompletionToken(storageKey = DEFAULT_STORAGE_KEY) {
  return loadSePayCheckout(storageKey)?.completionToken || null;
}

export function submitSePayForm(sepay, options = {}) {
  if (!sepay?.action || !sepay?.fields) {
    throw new Error("Thiếu dữ liệu thanh toán SePay");
  }

  const form = document.createElement("form");
  form.method = sepay.method || "POST";
  form.action = sepay.action;
  form.style.display = "none";
  if (options.target) {
    form.target = options.target;
  }

  Object.entries(sepay.fields).forEach(([name, value]) => {
    const input = document.createElement("input");
    input.type = "hidden";
    input.name = name;
    input.value = value == null ? "" : String(value);
    form.appendChild(input);
  });

  document.body.appendChild(form);
  form.submit();
  form.remove();
}

/** Mở trang QR SePay ở tab mới — tab gốc giữ poll trạng thái. */
export function openSePayCheckoutTab(sepay) {
  submitSePayForm(sepay, { target: "_blank" });
}
