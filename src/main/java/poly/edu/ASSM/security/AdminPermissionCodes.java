package poly.edu.ASSM.security;

/** Permission keys aligned with Sport4L.Permissions.PermissionName */
public final class AdminPermissionCodes {

    private AdminPermissionCodes() {
    }

    public static final String DASHBOARD = "DASHBOARD_VIEW";
    public static final String PRODUCT = "PRODUCT_VIEW";
    public static final String CATEGORY = "CATEGORY_VIEW";
    public static final String ORDER = "ORDER_VIEW";
    public static final String ORDER_UPDATE = "ORDER_UPDATE";
    public static final String USER = "USER_VIEW";

    public static boolean matches(String required, String granted) {
        if (required == null || granted == null) {
            return false;
        }
        return required.trim().equalsIgnoreCase(granted.trim());
    }
}
