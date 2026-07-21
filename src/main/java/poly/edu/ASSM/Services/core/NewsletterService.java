package poly.edu.ASSM.Services.core;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.Entity.Voucher;
import poly.edu.ASSM.Repository.AccountRepository;
import poly.edu.ASSM.Repository.UsersRepository;
import poly.edu.ASSM.exception.InvalidInputException;

@Service
public class NewsletterService {

    private static final Logger log = LoggerFactory.getLogger(NewsletterService.class);
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final NumberFormat MONEY_FMT = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@sports4life.local}")
    private String mailFrom;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    public Map<String, Object> subscribe(String emailRaw) {
        String email = emailRaw == null ? "" : emailRaw.trim();
        if (email.isBlank()) {
            throw InvalidInputException.of("email", "Email không được để trống.");
        }
        if (!email.matches("(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$")) {
            throw InvalidInputException.of("email", "Email không hợp lệ.");
        }

        Accounts account = accountRepository.findFirstByEmailIgnoreCase(email);
        if (account == null || !Boolean.TRUE.equals(account.getIsActive())) {
            throw InvalidInputException.of(
                    "email",
                    "Gmail này chưa có tài khoản Sports4Life. Vui lòng đăng ký tài khoản trước.");
        }

        Users user = usersRepository.findByAccount_Id(account.getId())
                .orElseThrow(() -> InvalidInputException.of(
                        "email",
                        "Tài khoản chưa có hồ sơ người dùng. Vui lòng hoàn tất đăng ký."));

        boolean already = Boolean.TRUE.equals(user.getNewsletterOptIn());
        if (!already) {
            user.setNewsletterOptIn(true);
            usersRepository.save(user);
        }

        String to = account.getEmail();
        CompletableFuture.runAsync(() -> sendWelcomeMail(to, account.getUsername()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("alreadySubscribed", already);
        body.put(
                "message",
                already
                        ? "Gmail này đã đăng ký nhận mã voucher."
                        : "Đăng ký thành công! Kiểm tra Gmail để nhận mã voucher khi có khuyến mãi.");
        return body;
    }

    /** Gửi mail mã voucher mới tới user đã opt-in (không chặn tạo voucher). */
    public void notifyNewVoucher(Voucher voucher) {
        if (voucher == null) {
            return;
        }
        List<Users> subscribers = usersRepository.findNewsletterSubscribers();
        if (subscribers.isEmpty()) {
            log.info("Không có user opt-in để nhận mail voucher {}", voucher.getCode());
            return;
        }
        CompletableFuture.runAsync(() -> {
            int sent = 0;
            for (Users u : subscribers) {
                Accounts a = u.getAccount();
                if (a == null || a.getEmail() == null || a.getEmail().isBlank()) {
                    continue;
                }
                if (sendVoucherMail(a.getEmail(), voucher)) {
                    sent++;
                }
            }
            log.info("Đã gửi {} mail voucher mới: {}", sent, voucher.getCode());
        });
    }

    private void sendWelcomeMail(String to, String username) {
        String subject = "Sports4Life — Đăng ký nhận mã voucher";
        String text = """
                Xin chào %s,

                Bạn đã đăng ký nhận thông báo mã voucher / khuyến mãi từ Sports4Life qua Gmail.

                Khi cửa hàng phát hành mã mới, chúng tôi sẽ gửi mã và hướng dẫn sử dụng tới hộp thư này.

                Mua sắm ngay: %s/product

                Trân trọng,
                Sports4Life
                """.formatted(username != null ? username : "bạn", trimSlash(frontendUrl));
        sendMail(to, subject, text);
    }

    private boolean sendVoucherMail(String to, Voucher voucher) {
        String name = voucher.getName() != null && !voucher.getName().isBlank()
                ? voucher.getName().trim()
                : "Khuyến mãi Sports4Life";
        String subject = "Sports4Life — Mã voucher mới: " + voucher.getCode();
        StringBuilder text = new StringBuilder();
        text.append("Xin chào,\n\n");
        text.append("Sports4Life vừa phát hành mã khuyến mãi mới:\n\n");
        text.append("• Tên: ").append(name).append('\n');
        text.append("• Mã: ").append(voucher.getCode()).append('\n');
        text.append("• Ưu đãi: ").append(describeDiscount(voucher)).append('\n');
        if (voucher.getMinOrderValue() != null && voucher.getMinOrderValue().compareTo(BigDecimal.ZERO) > 0) {
            text.append("• Đơn tối thiểu: ").append(formatMoney(voucher.getMinOrderValue())).append('\n');
        }
        if (voucher.getExpiredAt() != null) {
            text.append("• Hết hạn: ").append(DATE_FMT.format(voucher.getExpiredAt())).append('\n');
        }
        text.append("\nÁp dụng tại checkout: ").append(trimSlash(frontendUrl)).append("/checkout\n\n");
        text.append("Trân trọng,\nSports4Life\n");
        return sendMail(to, subject, text.toString());
    }

    private String describeDiscount(Voucher voucher) {
        if (voucher.getDiscountPercent() != null && voucher.getDiscountPercent() > 0) {
            String s = voucher.getDiscountPercent() + "%";
            if (voucher.getMaxDiscount() != null && voucher.getMaxDiscount().compareTo(BigDecimal.ZERO) > 0) {
                s += " (tối đa " + formatMoney(voucher.getMaxDiscount()) + ")";
            }
            return s;
        }
        if (voucher.getDiscountAmount() != null && voucher.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            return "Giảm " + formatMoney(voucher.getDiscountAmount());
        }
        String code = voucher.getCode() != null ? voucher.getCode().toUpperCase(Locale.ROOT) : "";
        if (code.contains("SHIP") || code.contains("FREESHIP")) {
            return "Miễn phí vận chuyển";
        }
        return "Xem chi tiết trên website";
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0 đ";
        }
        return MONEY_FMT.format(amount) + " đ";
    }

    private boolean sendMail(String to, String subject, String text) {
        if (mailSender == null) {
            log.warn("JavaMailSender chưa cấu hình — bỏ qua mail tới {}", to);
            return false;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(mailFrom);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(text);
            mailSender.send(msg);
            return true;
        } catch (Exception e) {
            log.warn("Gửi mail voucher thất bại tới {}: {}", to, e.getMessage());
            return false;
        }
    }

    private static String trimSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:5173";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
