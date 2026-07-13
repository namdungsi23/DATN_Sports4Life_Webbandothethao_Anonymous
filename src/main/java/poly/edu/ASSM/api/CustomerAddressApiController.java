package poly.edu.ASSM.api;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import poly.edu.ASSM.Services.core.CustomerAddressService;
import poly.edu.ASSM.dto.request.CustomerAddressRequest;

@RestController
@RequestMapping("/api/addresses")
public class CustomerAddressApiController {

    @Autowired
    private CustomerAddressService customerAddressService;

    @GetMapping
    public Map<String, Object> list(Principal principal) {
        return customerAddressService.listAddresses(principal.getName());
    }

    @PostMapping
    public Map<String, Object> create(Principal principal, @Valid @RequestBody CustomerAddressRequest request) {
        return customerAddressService.createAddress(principal.getName(), request);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(
            Principal principal,
            @PathVariable Integer id,
            @Valid @RequestBody CustomerAddressRequest request) {
        return customerAddressService.updateAddress(principal.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(Principal principal, @PathVariable Integer id) {
        return customerAddressService.deleteAddress(principal.getName(), id);
    }

    @PostMapping("/{id}/default")
    public Map<String, Object> setDefault(Principal principal, @PathVariable Integer id) {
        return customerAddressService.setDefaultAddress(principal.getName(), id);
    }
}
