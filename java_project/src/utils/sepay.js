export function submitSePayForm(sepay) {
  if (!sepay?.action || !sepay?.fields) {
    throw new Error("Thiếu dữ liệu thanh toán SePay");
  }

  const form = document.createElement("form");
  form.method = sepay.method || "POST";
  form.action = sepay.action;
  form.style.display = "none";

  Object.entries(sepay.fields).forEach(([name, value]) => {
    const input = document.createElement("input");
    input.type = "hidden";
    input.name = name;
    input.value = value == null ? "" : String(value);
    form.appendChild(input);
  });

  document.body.appendChild(form);
  form.submit();
}
