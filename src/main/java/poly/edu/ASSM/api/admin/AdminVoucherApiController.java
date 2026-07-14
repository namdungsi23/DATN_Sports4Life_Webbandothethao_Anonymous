package poly.edu.ASSM.api.admin;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import poly.edu.ASSM.Services.core.VoucherService;
import poly.edu.ASSM.dto.request.VoucherRequest;
import poly.edu.ASSM.dto.response.VoucherResponse;

@RestController
@RequestMapping("/api/admin/vouchers")
@PreAuthorize("@adminAuth.has('VOUCHER_VIEW')")
public class AdminVoucherApiController {

    @Autowired
    private VoucherService voucherService;

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return voucherService.list(keyword, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
    }

    @GetMapping("/{id}")
    public VoucherResponse one(@PathVariable int id) {
        return voucherService.findById(id);
    }

    @PostMapping
    @PreAuthorize("@adminAuth.has('VOUCHER_CREATE')")
    public VoucherResponse create(@Valid @RequestBody VoucherRequest request) {
        return voucherService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@adminAuth.has('VOUCHER_UPDATE')")
    public VoucherResponse update(@PathVariable int id, @Valid @RequestBody VoucherRequest request) {
        return voucherService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@adminAuth.has('VOUCHER_DELETE')")
    public Map<String, Object> delete(@PathVariable int id) {
        voucherService.delete(id);
        return Map.of("ok", true, "message", "Đã xóa mã khuyến mãi");
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("@adminAuth.has('VOUCHER_UPDATE')")
    public VoucherResponse toggle(@PathVariable int id, @RequestParam boolean active) {
        return voucherService.toggleActive(id, active);
    }
}
