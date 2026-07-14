package poly.edu.ASSM.dto.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutConfirmRequest {

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod;

    @NotNull(message = "Tổng tiền không được để trống")
    @Positive(message = "Tổng tiền phải lớn hơn 0")
    private BigDecimal amount;

    @NotEmpty(message = "Giỏ hàng không được để trống")
    @Valid
    private List<CheckoutCartItemRequest> items;

    /** saved | new */
    @NotBlank(message = "Loại địa chỉ giao hàng không được để trống")
    private String addressMode;

    /** Bắt buộc khi addressMode = saved */
    private Integer savedAddressId;

    /** Bắt buộc khi addressMode = new — dùng để tạo OrderAddress snapshot */
    @Valid
    private OrderShippingRequest shippingAddress;

    /** Chỉ áp dụng khi addressMode = new */
    private Boolean saveToAddressBook;

    /** Chỉ áp dụng khi addressMode = new và saveToAddressBook = true */
    private Boolean setAsDefault;

    /** Nhãn khi lưu vào sổ địa chỉ (addressMode = new) */
    @Size(max = 50, message = "Nhãn địa chỉ tối đa 50 ký tự")
    private String addressBookLabel;
    /** Tuỳ chọn — nếu null thì không gán carrier ngay */
    private Integer carrierId;

    /** Mã khuyến mãi (tuỳ chọn) */
    @Size(max = 50, message = "Mã khuyến mãi tối đa 50 ký tự")
    private String voucherCode;
}
