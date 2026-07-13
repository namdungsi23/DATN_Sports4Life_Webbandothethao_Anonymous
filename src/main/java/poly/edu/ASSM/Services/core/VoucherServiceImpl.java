package poly.edu.ASSM.Services.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.ASSM.Entity.ProductVariants;
import poly.edu.ASSM.Entity.Voucher;
import poly.edu.ASSM.Repository.ProductVariantRepository;
import poly.edu.ASSM.Repository.VoucherRepository;
import poly.edu.ASSM.domain.VoucherDiscountCalculator;
import poly.edu.ASSM.domain.VoucherDiscountResult;
import poly.edu.ASSM.dto.request.CheckoutCartItemRequest;
import poly.edu.ASSM.dto.request.VoucherRequest;
import poly.edu.ASSM.dto.response.VoucherResponse;
import poly.edu.ASSM.mapper.VoucherMapper;

@Service
public class VoucherServiceImpl implements VoucherService {

    private static final BigDecimal MAX_MONEY = new BigDecimal("99999999.99");

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private VoucherMapper voucherMapper;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> list(String keyword, Pageable pageable) {
        Page<Voucher> page;
        if (keyword == null || keyword.isBlank()) {
            page = voucherRepository.findAll(pageable);
        } else {
            String q = keyword.trim();
            page = voucherRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(q, q, pageable);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("items", voucherMapper.toResponseList(page.getContent()));
        body.put("totalElements", page.getTotalElements());
        body.put("totalPages", page.getTotalPages());
        body.put("page", page.getNumber());
        body.put("size", page.getSize());
        return body;
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse findById(int id) {
        return voucherMapper.toResponse(requireVoucher(id));
    }

    @Override
    @Transactional
    public VoucherResponse create(VoucherRequest request) {
        sanitizeRequest(request);
        validateRequest(request, null);
        Voucher entity = voucherMapper.toEntity(request);
        normalizeEntity(entity, resolveDiscountType(request));
        if (entity.getUsedCount() == null) {
            entity.setUsedCount(0);
        }
        if (entity.getIsActive() == null) {
            entity.setIsActive((short) 1);
        }
        entity.setCode(normalizeCode(request.getCode()));
        return voucherMapper.toResponse(voucherRepository.save(entity));
    }

    @Override
    @Transactional
    public VoucherResponse update(int id, VoucherRequest request) {
        Voucher entity = requireVoucher(id);
        sanitizeRequest(request);
        validateRequest(request, id);
        request.setCode(normalizeCode(request.getCode()));
        voucherMapper.applyRequest(entity, request);
        normalizeEntity(entity, resolveDiscountType(request));
        return voucherMapper.toResponse(voucherRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(int id) {
        if (!voucherRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy mã khuyến mãi");
        }
        voucherRepository.deleteById(id);
    }

    @Override
    @Transactional
    public VoucherResponse toggleActive(int id, boolean active) {
        Voucher entity = requireVoucher(id);
        entity.setIsActive(active ? (short) 1 : (short) 0);
        return voucherMapper.toResponse(voucherRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherDiscountResult previewApply(String code, BigDecimal subTotal, BigDecimal shippingFee) {
        Voucher voucher = requireByCode(code);
        return VoucherDiscountCalculator.calculate(voucher, subTotal, shippingFee);
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherDiscountResult applyForCheckout(String code, BigDecimal subTotal, BigDecimal shippingFee) {
        if (code == null || code.isBlank()) {
            return VoucherDiscountResult.none();
        }
        Voucher voucher = requireByCode(code);
        return VoucherDiscountCalculator.calculate(voucher, subTotal, shippingFee);
    }

    @Override
    @Transactional
    public void markUsed(VoucherDiscountResult result) {
        if (result == null || result.voucher() == null) {
            return;
        }
        Voucher voucher = voucherRepository.findById(result.voucher().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã khuyến mãi không tồn tại"));
        int used = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;
        voucher.setUsedCount(used + 1);
        voucherRepository.save(voucher);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateSubTotal(List<CheckoutCartItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giỏ hàng trống");
        }
        BigDecimal subTotal = BigDecimal.ZERO;
        for (CheckoutCartItemRequest item : items) {
            ProductVariants variant = resolveVariant(item);
            subTotal = subTotal.add(variant.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return subTotal;
    }

    private Voucher requireVoucher(int id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy mã khuyến mãi"));
    }

    private Voucher requireByCode(String code) {
        return voucherRepository.findByCodeIgnoreCase(normalizeCode(code))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã khuyến mãi không tồn tại"));
    }

    private void validateRequest(VoucherRequest request, Integer excludeId) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã khuyến mãi không được để trống");
        }
        String code = normalizeCode(request.getCode());
        boolean exists = excludeId == null
                ? voucherRepository.existsByCodeIgnoreCase(code)
                : voucherRepository.existsByCodeIgnoreCaseAndIdNot(code, excludeId);
        if (exists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã khuyến mãi đã tồn tại");
        }

        String type = resolveDiscountType(request);
        if ("PERCENT".equals(type)) {
            if (request.getDiscountPercent() == null || request.getDiscountPercent() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phần trăm giảm giá phải từ 1 đến 100");
            }
            if (request.getDiscountPercent() > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phần trăm giảm giá không được vượt quá 100");
            }
        } else if ("FIXED".equals(type) || "FREESHIP".equals(type)) {
            if (request.getDiscountAmount() == null || request.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá trị giảm phải lớn hơn 0");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn loại giảm giá hợp lệ");
        }

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượt sử dụng phải lớn hơn 0");
        }
        if (request.getStartDate() == null || request.getExpiredAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập ngày bắt đầu và kết thúc");
        }
        if (request.getExpiredAt().isBefore(request.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày kết thúc phải sau ngày bắt đầu");
        }
    }

    private String resolveDiscountType(VoucherRequest request) {
        if (request.getDiscountType() != null && !request.getDiscountType().isBlank()) {
            return request.getDiscountType().trim().toUpperCase(Locale.ROOT);
        }
        if (request.getDiscountPercent() != null && request.getDiscountPercent() > 0) {
            return "PERCENT";
        }
        if ("FREESHIP".equalsIgnoreCase(request.getCode())) {
            return "FREESHIP";
        }
        return "FIXED";
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    private void sanitizeRequest(VoucherRequest request) {
        String type = resolveDiscountType(request);
        request.setMinOrderValue(normalizeMoney(request.getMinOrderValue(), "Đơn tối thiểu", false));
        request.setDiscountAmount(normalizeMoney(request.getDiscountAmount(), "Giá trị giảm", false));
        request.setMaxDiscount(normalizeMoney(request.getMaxDiscount(), "Giảm tối đa", false));

        if ("PERCENT".equals(type)) {
            request.setDiscountAmount(null);
            Integer percent = request.getDiscountPercent();
            if (percent != null) {
                request.setDiscountPercent(Math.min(100, Math.max(1, percent)));
            }
            if (request.getMaxDiscount() != null && request.getMaxDiscount().compareTo(BigDecimal.ZERO) == 0) {
                request.setMaxDiscount(null);
            }
        } else {
            request.setDiscountPercent(null);
            BigDecimal amount = request.getDiscountAmount();
            if (amount != null) {
                request.setMaxDiscount(amount);
            }
        }

        if (request.getIsActive() == null) {
            request.setIsActive((short) 1);
        } else {
            request.setIsActive((short) (request.getIsActive() == 1 ? 1 : 0));
        }
    }

    private void normalizeEntity(Voucher entity, String type) {
        entity.setMinOrderValue(normalizeMoney(entity.getMinOrderValue(), "Đơn tối thiểu", false));
        entity.setDiscountAmount(normalizeMoney(entity.getDiscountAmount(), "Giá trị giảm", false));
        entity.setMaxDiscount(normalizeMoney(entity.getMaxDiscount(), "Giảm tối đa", false));

        if ("PERCENT".equals(type)) {
            entity.setDiscountAmount(null);
            if (entity.getDiscountPercent() != null) {
                entity.setDiscountPercent(Math.min(100, Math.max(1, entity.getDiscountPercent())));
            }
            if (entity.getMaxDiscount() != null && entity.getMaxDiscount().compareTo(BigDecimal.ZERO) == 0) {
                entity.setMaxDiscount(null);
            }
        } else {
            entity.setDiscountPercent(null);
            if (entity.getDiscountAmount() != null) {
                entity.setMaxDiscount(entity.getDiscountAmount());
            }
        }

        entity.setIsActive(entity.getIsActive() != null && entity.getIsActive() == 1 ? (short) 1 : (short) 0);
    }

    private BigDecimal normalizeMoney(BigDecimal value, String label, boolean required) {
        if (value == null) {
            if (required) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " không được để trống");
            }
            return null;
        }
        BigDecimal scaled = value.setScale(2, RoundingMode.HALF_UP);
        if (scaled.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " không được âm");
        }
        if (scaled.compareTo(MAX_MONEY) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    label + " không được vượt quá 99.999.999,99đ");
        }
        return scaled;
    }

    private ProductVariants resolveVariant(CheckoutCartItemRequest item) {
        if (item.getVariantId() != null) {
            ProductVariants variant = productVariantRepository.findDetailedById(item.getVariantId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Biến thể sản phẩm không hợp lệ"));
            if (variant.getProduct() == null || !variant.getProduct().getId().equals(item.getProductId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Biến thể sản phẩm không hợp lệ");
            }
            return variant;
        }
        return productVariantRepository.findByProduct_IdAndIsDefaultTrue(item.getProductId())
                .or(() -> {
                    List<ProductVariants> variants = productVariantRepository
                            .findByProduct_IdOrderByDisplayOrderAscIdAsc(item.getProductId());
                    return variants.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(variants.get(0));
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm không có biến thể"));
    }
}
