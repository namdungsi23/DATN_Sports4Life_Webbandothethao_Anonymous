package poly.edu.ASSM.mapper;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import poly.edu.ASSM.entity.Accounts;
import poly.edu.ASSM.entity.Addresses;
import poly.edu.ASSM.entity.Users;
import poly.edu.ASSM.dto.request.CustomerAddressRequest;
import poly.edu.ASSM.dto.response.CustomerAddressResponse;

@Component
public class CustomerAddressMapper {

    public static final String DEFAULT_LABEL = "Địa chỉ mặc định";

    public CustomerAddressResponse toResponse(Addresses entity, Users user) {
        if (entity == null) {
            return null;
        }
        Accounts account = entity.getAccount();
        return CustomerAddressResponse.builder()
                .id(entity.getId())
                .accountId(account != null ? account.getId() : null)
                .accountFullName(resolveFullName(user, account))
                .accountPhone(user != null ? user.getPhone() : null)
                .label(entity.getLabel())
                .province(entity.getProvince())
                .ward(entity.getWard())
                .addressDetail(entity.getAddressDetail())
                .isDefault(entity.getIsDefault())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public List<CustomerAddressResponse> toResponseList(Collection<Addresses> entities, Users user) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(entity -> toResponse(entity, user)).collect(Collectors.toList());
    }


    public Addresses toEntity(CustomerAddressRequest request, Accounts account) {
        Addresses entity = new Addresses();
        applyRequest(entity, request, account);
        return entity;
    }

    public void applyRequest(Addresses entity, CustomerAddressRequest request, Accounts account) {
        if (entity == null || request == null) {
            return;
        }
        boolean isDefault = Boolean.TRUE.equals(request.getIsDefault());
        entity.setAccount(account);
        entity.setProvince(request.getProvince().trim());
        entity.setWard(request.getWard().trim());
        entity.setAddressDetail(request.getAddressDetail().trim());
        entity.setIsDefault(isDefault);
        entity.setLabel(resolveLabel(request.getLabel(), isDefault));
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
    }

    public CustomerAddressRequest fromShippingLocation(String province, String ward, String addressDetail,
            String label, boolean isDefault) {
        CustomerAddressRequest request = new CustomerAddressRequest();
        request.setProvince(province);
        request.setWard(ward);
        request.setAddressDetail(addressDetail);
        request.setLabel(label);
        request.setIsDefault(isDefault);
        return request;
    }

    public String resolveLabel(String customLabel, boolean isDefault) {
        if (isDefault) {
            return DEFAULT_LABEL;
        }
        if (customLabel == null || customLabel.isBlank()) {
            return null;
        }
        return customLabel.trim();
    }

    private String resolveFullName(Users user, Accounts account) {
        if (user != null && user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (account != null && account.getUsername() != null) {
            return account.getUsername();
        }
        return null;
    }
}
