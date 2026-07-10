package poly.edu.ASSM.Services.core;

import java.util.Map;

import poly.edu.ASSM.dto.request.CustomerAddressRequest;

public interface CustomerAddressService {

    Map<String, Object> listAddresses(String username);

    Map<String, Object> createAddress(String username, CustomerAddressRequest request);

    Map<String, Object> updateAddress(String username, Integer addressId, CustomerAddressRequest request);

    Map<String, Object> deleteAddress(String username, Integer addressId);

    Map<String, Object> setDefaultAddress(String username, Integer addressId);
}
