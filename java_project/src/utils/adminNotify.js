import { useToast } from "../stores/appStore";

/** Thông báo toast dùng chung cho trang admin/staff */
export function useAdminNotify() {
  const toast = useToast();
  return {
    success: (message) => toast.success(message),
    error: (message) => toast.error(message),
    warning: (message) => toast.warning(message),
    info: (message) => toast.info(message),
  };
}
