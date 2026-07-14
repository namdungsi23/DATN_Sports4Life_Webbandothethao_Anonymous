package poly.edu.ASSM.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sepay")
public class SePayProperties {

    public static final String SANDBOX_CHECKOUT_URL = "https://pay-sandbox.sepay.vn/v1/checkout/init";
    public static final String PRODUCTION_CHECKOUT_URL = "https://pay.sepay.vn/v1/checkout/init";

    private boolean enabled = false;
    private String env = "sandbox";
    private String merchantId = "";
    private String secretKey = "";
    private String checkoutUrl = SANDBOX_CHECKOUT_URL;
    private String frontendBaseUrl = "http://localhost:5173";
    private String ipnPath = "/api/public/sepay/ipn";

    public boolean isConfigured() {
        return enabled
                && merchantId != null && !merchantId.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }

    /** True when credentials or explicit env point to production (live payments). */
    public boolean isProductionMode() {
        if ("production".equalsIgnoreCase(env) || "live".equalsIgnoreCase(env)) {
            return true;
        }
        if ("sandbox".equalsIgnoreCase(env) || "test".equalsIgnoreCase(env)) {
            return looksLikeProductionCredentials();
        }
        return looksLikeProductionCredentials();
    }

    private boolean looksLikeProductionCredentials() {
        String mid = merchantId != null ? merchantId.trim().toUpperCase() : "";
        String sk = secretKey != null ? secretKey.trim() : "";
        return mid.contains("LIVE") || sk.startsWith("spsk_live_");
    }

    /** Checkout URL that matches merchant credentials — avoids LIVE key + sandbox URL mismatch. */
    public String getResolvedCheckoutUrl() {
        String expected = isProductionMode() ? PRODUCTION_CHECKOUT_URL : SANDBOX_CHECKOUT_URL;
        if (checkoutUrl == null || checkoutUrl.isBlank()) {
            return expected;
        }
        String configured = checkoutUrl.trim();
        boolean configuredSandbox = configured.contains("pay-sandbox");
        boolean configuredProduction = configured.contains("pay.sepay.vn") && !configuredSandbox;
        if (isProductionMode() && configuredSandbox) {
            return expected;
        }
        if (!isProductionMode() && configuredProduction) {
            return expected;
        }
        return configured;
    }

    public String getEffectiveEnv() {
        return isProductionMode() ? "production" : "sandbox";
    }

    public boolean hasEnvMismatch() {
        if (checkoutUrl == null || checkoutUrl.isBlank()) {
            return false;
        }
        String configured = checkoutUrl.trim();
        boolean configuredSandbox = configured.contains("pay-sandbox");
        boolean configuredProduction = configured.contains("pay.sepay.vn") && !configuredSandbox;
        return (isProductionMode() && configuredSandbox) || (!isProductionMode() && configuredProduction);
    }

    public String ipnUrl(String backendPublicUrl) {
        String base = backendPublicUrl != null ? backendPublicUrl.replaceAll("/$", "") : "";
        return base + ipnPath;
    }
}
