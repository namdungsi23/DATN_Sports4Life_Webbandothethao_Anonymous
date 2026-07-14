package poly.edu.ASSM.Services.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.ASSM.entity.Accounts;
import poly.edu.ASSM.entity.Addresses;
import poly.edu.ASSM.entity.Users;
import poly.edu.ASSM.repository.AccountRepository;
import poly.edu.ASSM.repository.AddressRepository;
import poly.edu.ASSM.repository.UsersRepository;
import poly.edu.ASSM.dto.request.CustomerAddressRequest;
import poly.edu.ASSM.dto.response.CustomerAddressResponse;
import poly.edu.ASSM.mapper.CustomerAddressMapper;

@Service
public class CustomerAddressServiceImpl implements CustomerAddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private CustomerAddressMapper customerAddressMapper;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> listAddresses(String username) {
        Accounts account = requireAccount(username);
        Users user = requireUser(account);
        List<CustomerAddressResponse> addresses = customerAddressMapper.toResponseList(
                addressRepository.findByAccount_IdOrderByIsDefaultDescCreatedAtDesc(account.getId()), user);

        Map<String, Object> body = new HashMap<>();
        body.put("addresses", addresses);
        body.put("count", addresses.size());
        body.put("accountFullName", resolveAccountFullName(user, account));
        body.put("accountPhone", user.getPhone());
        return body;
    }

    @Override
    @Transactional
    public Map<String, Object> createAddress(String username, CustomerAddressRequest request) {
        Accounts account = requireAccount(username);
        Users user = requireUser(account);
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultForAccount(account.getId());
        }

        Addresses entity = customerAddressMapper.toEntity(request, account);
        Addresses saved = addressRepository.save(entity);

        return Map.of(
                "message", "Đã thêm địa chỉ mới",
                "address", customerAddressMapper.toResponse(saved, user));
    }

    @Override
    @Transactional
    public Map<String, Object> updateAddress(String username, Integer addressId, CustomerAddressRequest request) {
        Accounts account = requireAccount(username);
        Users user = requireUser(account);
        Addresses entity = requireOwnedAddress(addressId, account.getId());

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultForAccount(account.getId());
        }

        customerAddressMapper.applyRequest(entity, request, account);
        Addresses saved = addressRepository.save(entity);

        return Map.of(
                "message", "Đã cập nhật địa chỉ",
                "address", customerAddressMapper.toResponse(saved, user));
    }

    @Override
    @Transactional
    public Map<String, Object> deleteAddress(String username, Integer addressId) {
        Accounts account = requireAccount(username);
        Addresses entity = requireOwnedAddress(addressId, account.getId());
        addressRepository.delete(entity);

        return Map.of(
                "message", "Đã xóa địa chỉ",
                "deletedId", addressId);
    }

    @Override
    @Transactional
    public Map<String, Object> setDefaultAddress(String username, Integer addressId) {
        Accounts account = requireAccount(username);
        Users user = requireUser(account);
        Addresses entity = requireOwnedAddress(addressId, account.getId());

        addressRepository.clearDefaultForAccount(account.getId());
        entity.setIsDefault(true);
        entity.setLabel(CustomerAddressMapper.DEFAULT_LABEL);
        entity.setUpdatedAt(java.time.Instant.now());
        Addresses saved = addressRepository.save(entity);

        return Map.of(
                "message", "Đã đặt làm địa chỉ mặc định",
                "address", customerAddressMapper.toResponse(saved, user));
    }

    @Transactional
    public CustomerAddressResponse saveFromCheckout(Accounts account, Users user,
            CustomerAddressRequest request, boolean setAsDefault) {
        request.setIsDefault(setAsDefault);
        if (setAsDefault) {
            addressRepository.clearDefaultForAccount(account.getId());
        }
        Addresses entity = customerAddressMapper.toEntity(request, account);
        return customerAddressMapper.toResponse(addressRepository.save(entity), user);
    }

    public Addresses requireOwnedAddressEntity(Integer addressId, Long accountId) {
        return requireOwnedAddress(addressId, accountId);
    }

    public Users requireUser(Accounts account) {
        return usersRepository.findByAccount_Id(account.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ người dùng"));
    }

    private Addresses requireOwnedAddress(Integer addressId, Long accountId) {
        return addressRepository.findByIdAndAccount_Id(addressId, accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy địa chỉ"));
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

    private static String resolveAccountFullName(Users user, Accounts account) {
        if (user != null && user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        return account != null ? account.getUsername() : null;
    }
}
