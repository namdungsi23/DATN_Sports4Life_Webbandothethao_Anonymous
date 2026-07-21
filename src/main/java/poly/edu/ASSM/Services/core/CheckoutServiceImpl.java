package poly.edu.ASSM.Services.core;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Carriers;
import poly.edu.ASSM.Entity.Addresses;
import poly.edu.ASSM.Entity.OrderAddresses;
import poly.edu.ASSM.Entity.OrderDetails;
import poly.edu.ASSM.Entity.Orders;
import poly.edu.ASSM.Entity.ProductVariants;
import poly.edu.ASSM.Entity.Shipments;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.Repository.AccountRepository;
import poly.edu.ASSM.Repository.CarrierRepository;
import poly.edu.ASSM.Repository.OrderAddressRepository;
import poly.edu.ASSM.Repository.OrderDetailsRepository;
import poly.edu.ASSM.Repository.OrdersRepository;
import poly.edu.ASSM.Repository.ProductVariantRepository;
import poly.edu.ASSM.Repository.ShipmentRepository;
import poly.edu.ASSM.domain.ShippingFeePolicy;
import poly.edu.ASSM.domain.ShippingStatus;
import poly.edu.ASSM.domain.VoucherDiscountResult;
import poly.edu.ASSM.dto.request.CheckoutCartItemRequest;
import poly.edu.ASSM.dto.request.CheckoutConfirmRequest;
import poly.edu.ASSM.dto.request.VoucherApplyRequest;
import poly.edu.ASSM.dto.request.CustomerAddressRequest;
import poly.edu.ASSM.dto.request.OrderShippingRequest;
import poly.edu.ASSM.dto.response.CustomerAddressResponse;
import poly.edu.ASSM.mapper.CustomerAddressMapper;
import poly.edu.ASSM.mapper.OrderAddressMapper;

@Service
public class CheckoutServiceImpl implements CheckoutService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private OrderDetailsRepository orderDetailsRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;
    @Autowired
    private CarrierRepository carrierRepository;
    @Autowired
    private OrderAddressRepository orderAddressRepository;
    @Autowired
    private ShipmentRepository shipmentRepository;
    @Autowired
    private CustomerAddressServiceImpl customerAddressService;

    @Autowired
    private OrderAddressMapper orderAddressMapper;

    @Autowired
    private CustomerAddressMapper customerAddressMapper;

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private SePayService sePayService;
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listActiveCarriers() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Carriers carrier : carrierRepository.findByActiveTrueOrderByNameAsc()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", carrier.getId());
            m.put("name", carrier.getName());
            m.put("code", carrier.getCode());
            list.add(m);
        }
        return list;
    }
    @Override
    @Transactional
    public Map<String, Object> confirmCheckout(String username, CheckoutConfirmRequest request) {
        Accounts account = requireAccount(username);
        Users user = customerAddressService.requireUser(account);
        ShippingSnapshot snapshot = resolveShippingSnapshot(account, user, request);
        BigDecimal subTotal = calculateSubTotal(request.getItems());
        BigDecimal shippingFee = ShippingFeePolicy.calculate(subTotal);
        VoucherDiscountResult voucherResult = voucherService.applyForCheckout(
                request.getVoucherCode(), subTotal, shippingFee);
        BigDecimal discountAmount = voucherResult.totalDiscount();
        BigDecimal finalShippingFee = shippingFee.subtract(voucherResult.shippingDiscount()).max(BigDecimal.ZERO);
        BigDecimal totalAmount = subTotal.subtract(voucherResult.subtotalDiscount())
                .add(finalShippingFee)
                .max(BigDecimal.ONE);
        Orders order = new Orders();

        order.setAccount(account);
        order.setOrderStatus("PENDING");
        order.setPaymentMethod(normalizePaymentMethod(request.getPaymentMethod()));
        order.setPaymentStatus(resolvePaymentStatus(request.getPaymentMethod()));
        order.setSubTotal(subTotal);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(totalAmount);
        order.setCreateDate(Instant.now());
        if (voucherResult.voucher() != null) {
            order.setVoucher(voucherResult.voucher());
        }
        Orders savedOrder = ordersRepository.save(order);

        createOrderDetails(savedOrder, request.getItems());

        OrderAddresses orderAddress = orderAddressMapper.createSnapshot(
                savedOrder,
                snapshot.receiverName(),
                snapshot.receiverPhone(),
                snapshot.province(),
                snapshot.ward(),
                snapshot.addressDetail());
        OrderAddresses savedSnapshot = orderAddressRepository.save(orderAddress);

        Shipments shipment = new Shipments();
        shipment.setOrder(savedOrder);
        shipment.setShippingStatus(ShippingStatus.PENDING.name());
        shipment.setShippingFee(finalShippingFee);
        shipment.setCreatedAt(Instant.now());
        if (request.getCarrierId() != null) {
            Carriers carrier = carrierRepository.findById(request.getCarrierId())
                    .filter(c -> Boolean.TRUE.equals(c.getActive()))
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Đơn vị vận chuyển không hợp lệ"));
            shipment.setCarrier(carrier);
        }
        shipmentRepository.save(shipment);

        voucherService.markUsed(voucherResult);

        CustomerAddressResponse savedAddressBook = null;
        if ("new".equalsIgnoreCase(request.getAddressMode())
                && Boolean.TRUE.equals(request.getSaveToAddressBook())
                && request.getShippingAddress() != null) {
            boolean setDefault = Boolean.TRUE.equals(request.getSetAsDefault());
            OrderShippingRequest shipping = request.getShippingAddress();
            CustomerAddressRequest bookRequest = customerAddressMapper.fromShippingLocation(
                    shipping.getProvince(),
                    shipping.getWard(),
                    shipping.getAddressDetail(),
                    request.getAddressBookLabel(),
                    setDefault);
            savedAddressBook = customerAddressService.saveFromCheckout(account, user, bookRequest, setDefault);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("message", "Đặt hàng thành công");
        body.put("orderId", savedOrder.getId());
        body.put("subTotal", subTotal);
        body.put("discountAmount", discountAmount);
        body.put("shippingFee", finalShippingFee);
        body.put("totalAmount", totalAmount);
        if (voucherResult.voucher() != null) {
            body.put("voucherCode", voucherResult.voucher().getCode());
            body.put("discountType", voucherResult.discountType());
        }
        body.put("orderAddress", orderAddressMapper.toResponse(savedSnapshot));
        if (savedAddressBook != null) {
            body.put("savedAddress", savedAddressBook);
        }

        if (isSePayMethod(request.getPaymentMethod())) {
            body.put("paymentMethod", "SEPAY");
            body.put("paymentPending", true);
            body.put("sepay", sePayService.buildCheckoutForm(savedOrder, account.getUsername()));
        }

        return body;
    }

    private boolean isSePayMethod(String paymentMethod) {
        return paymentMethod != null && "SEPAY".equalsIgnoreCase(paymentMethod.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> previewVoucher(String username, VoucherApplyRequest request) {
        requireAccount(username);
        BigDecimal subTotal = voucherService.calculateSubTotal(request.getItems());
        BigDecimal shippingFee = ShippingFeePolicy.calculate(subTotal);
        VoucherDiscountResult result = voucherService.previewApply(request.getVoucherCode(), subTotal, shippingFee);
        BigDecimal finalShippingFee = shippingFee.subtract(result.shippingDiscount()).max(BigDecimal.ZERO);
        BigDecimal totalAmount = subTotal.subtract(result.subtotalDiscount()).add(finalShippingFee).max(BigDecimal.ONE);

        Map<String, Object> body = new HashMap<>();
        body.put("valid", true);
        body.put("voucherCode", result.voucher().getCode());
        body.put("voucherName", result.voucher().getName());
        body.put("discountType", result.discountType());
        body.put("subTotal", subTotal);
        body.put("discountAmount", result.totalDiscount());
        body.put("subtotalDiscount", result.subtotalDiscount());
        body.put("shippingDiscount", result.shippingDiscount());
        body.put("shippingFee", finalShippingFee);
        body.put("totalAmount", totalAmount);
        return body;
    }

    private BigDecimal calculateSubTotal(List<CheckoutCartItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giỏ hàng trống");
        }
        BigDecimal subTotal = BigDecimal.ZERO;
        for (CheckoutCartItemRequest item : items) {
            ProductVariants variant = resolveVariant(item);
            subTotal = subTotal.add(
                    variant.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return subTotal;
    }

    private void createOrderDetails(Orders order, List<CheckoutCartItemRequest> items) {
        List<OrderDetails> details = new ArrayList<>();
        for (CheckoutCartItemRequest item : items) {
            ProductVariants variant = resolveVariant(item);
            ensureStock(variant, item.getQuantity());

            OrderDetails detail = new OrderDetails();
            detail.setOrders(order);
            detail.setVariant(variant);
            detail.setPrice(variant.getPrice().doubleValue());
            detail.setQuantity(item.getQuantity());
            details.add(detail);

            updateVariantStock(variant, item.getQuantity());
        }
        orderDetailsRepository.saveAll(details);
    }

    private ProductVariants resolveVariant(CheckoutCartItemRequest item) {
        if (item.getVariantId() != null) {
            ProductVariants variant = productVariantRepository.findDetailedById(item.getVariantId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Biến thể sản phẩm không hợp lệ"));
            if (variant.getProduct() == null
                    || !variant.getProduct().getId().equals(item.getProductId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Biến thể sản phẩm không hợp lệ");
            }
            return variant;
        }

        return productVariantRepository.findByProduct_IdAndIsDefaultTrue(item.getProductId())
                .or(() -> {
                    List<ProductVariants> variants = productVariantRepository
                            .findByProduct_IdOrderByDisplayOrderAscIdAsc(item.getProductId());
                    return variants.isEmpty() ? java.util.Optional.empty()
                            : java.util.Optional.of(variants.get(0));
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Sản phẩm không có biến thể để đặt hàng"));
    }

    private void ensureStock(ProductVariants variant, int quantity) {
        Short stock = variant.getQuantity();
        if (stock != null && stock < quantity) {
            String label = variant.getSku() != null ? variant.getSku() : "Sản phẩm";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "\"" + label + "\" không đủ tồn kho");
        }
    }

    private void updateVariantStock(ProductVariants variant, int quantity) {
        if (variant.getQuantity() != null) {
            variant.setQuantity((short) (variant.getQuantity() - quantity));
        }
        int sold = variant.getSoldCount() != null ? variant.getSoldCount() : 0;
        variant.setSoldCount(sold + quantity);
        productVariantRepository.save(variant);
    }

    private ShippingSnapshot resolveShippingSnapshot(Accounts account, Users user, CheckoutConfirmRequest request) {
        String mode = request.getAddressMode();
        if (mode == null || mode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn phương thức nhập địa chỉ");
        }

        if ("saved".equalsIgnoreCase(mode)) {
            if (request.getSavedAddressId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn địa chỉ đã lưu");
            }
            Addresses saved = customerAddressService.requireOwnedAddressEntity(
                    request.getSavedAddressId(), account.getId());
            ReceiverInfo receiver = resolveReceiverFromAccount(user, account);
            return new ShippingSnapshot(
                    receiver.name(),
                    receiver.phone(),
                    saved.getProvince(),
                    saved.getWard(),
                    saved.getAddressDetail());
        }

        if ("new".equalsIgnoreCase(mode)) {
            OrderShippingRequest input = request.getShippingAddress();
            if (input == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập địa chỉ giao hàng");
            }
            validateShipping(input);
            return new ShippingSnapshot(
                    input.getReceiverName().trim(),
                    input.getReceiverPhone().trim(),
                    input.getProvince().trim(),
                    input.getWard().trim(),
                    input.getAddressDetail().trim());
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phương thức địa chỉ không hợp lệ");
    }

    private ReceiverInfo resolveReceiverFromAccount(Users user, Accounts account) {
        String name = user.getFullName();
        if (name == null || name.isBlank()) {
            name = account.getUsername();
        }
        String phone = user.getPhone();
        if (phone == null || phone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vui lòng cập nhật số điện thoại trong hồ sơ trước khi đặt hàng");
        }
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vui lòng cập nhật họ tên trong hồ sơ trước khi đặt hàng");
        }
        return new ReceiverInfo(name.trim(), phone.trim());
    }

    private void validateShipping(OrderShippingRequest input) {
        if (isBlank(input.getReceiverName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Họ và tên người nhận không được để trống");
        }
        if (isBlank(input.getReceiverPhone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại người nhận không được để trống");
        }
        if (isBlank(input.getProvince())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thành phố không được để trống");
        }
        if (isBlank(input.getWard())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phường không được để trống");
        }
        if (isBlank(input.getAddressDetail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Địa chỉ không được để trống");
        }
    }

    private static String normalizePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return "CASH";
        }
        return paymentMethod.trim().toUpperCase();
    }

    private String resolvePaymentStatus(String paymentMethod) {
        if (paymentMethod == null) {
            return "UNPAID";
        }
        return switch (paymentMethod.toUpperCase()) {
            case "MOMO", "TECHCOMBANK" -> "PAID";
            case "SEPAY" -> "UNPAID";
            default -> "UNPAID";
        };
    }

    private Accounts requireAccount(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
        }
        Accounts account = accountRepository.findByUsername(username);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản");
        }
        return account;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ReceiverInfo(String name, String phone) {
    }

    private record ShippingSnapshot(
            String receiverName,
            String receiverPhone,
            String province,
            String ward,
            String addressDetail) {
    }
}
