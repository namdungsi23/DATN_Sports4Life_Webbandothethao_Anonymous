package poly.edu.ASSM.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Category;
import poly.edu.ASSM.Entity.Orders;
import poly.edu.ASSM.Entity.Products;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.dto.response.AdminSearchCategoryHit;
import poly.edu.ASSM.dto.response.AdminSearchOrderHit;
import poly.edu.ASSM.dto.response.AdminSearchProductHit;
import poly.edu.ASSM.dto.response.AdminSearchResponse;
import poly.edu.ASSM.dto.response.AdminSearchUserHit;

@Component
public class AdminSearchMapper {

    public AdminSearchProductHit toProductHit(Products product) {
        if (product == null) {
            return null;
        }
        return AdminSearchProductHit.builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand())
                .link("/admin/product?edit=" + product.getId())
                .build();
    }

    public AdminSearchCategoryHit toCategoryHit(Category category) {
        if (category == null) {
            return null;
        }
        return AdminSearchCategoryHit.builder()
                .id(category.getId())
                .name(category.getName())
                .link("/admin/category")
                .build();
    }

    public AdminSearchUserHit toUserHit(Accounts account, Users profile) {
        if (account == null) {
            return null;
        }
        return AdminSearchUserHit.builder()
                .id(account.getId())
                .username(account.getUsername())
                .fullname(profile != null ? profile.getFullName() : null)
                .email(account.getEmail())
                .link("/admin/user?keyword=" + account.getUsername())
                .build();
    }

    public AdminSearchUserHit toUserHit(Accounts account) {
        return toUserHit(account, account != null ? account.getUsers() : null);
    }

    public AdminSearchOrderHit toOrderHit(Orders order) {
        if (order == null) {
            return null;
        }
        Accounts account = order.getAccount();
        return AdminSearchOrderHit.builder()
                .id(order.getId())
                .username(account != null ? account.getUsername() : "")
                .orderStatus(order.getOrderStatus())
                .totalAmount(order.getTotalAmount())
                .link("/admin/order/" + order.getId())
                .build();
    }

    public List<AdminSearchProductHit> toProductHitList(Collection<Products> products) {
        if (products == null) {
            return List.of();
        }
        return products.stream().map(this::toProductHit).collect(Collectors.toList());
    }

    public List<AdminSearchCategoryHit> toCategoryHitList(Collection<Category> categories) {
        if (categories == null) {
            return List.of();
        }
        return categories.stream().map(this::toCategoryHit).collect(Collectors.toList());
    }

    public List<AdminSearchUserHit> toUserHitList(Collection<Accounts> accounts) {
        if (accounts == null) {
            return List.of();
        }
        return accounts.stream().map(this::toUserHit).collect(Collectors.toList());
    }

    public List<AdminSearchOrderHit> toOrderHitList(Collection<Orders> orders) {
        if (orders == null) {
            return List.of();
        }
        return orders.stream().map(this::toOrderHit).collect(Collectors.toList());
    }

    public AdminSearchResponse toSearchResponse(
            String q,
            List<AdminSearchProductHit> products,
            List<AdminSearchCategoryHit> categories,
            List<AdminSearchUserHit> users,
            List<AdminSearchOrderHit> orders) {
        return AdminSearchResponse.builder()
                .q(q == null ? "" : q.trim())
                .products(products != null ? products : List.of())
                .categories(categories != null ? categories : List.of())
                .users(users != null ? users : List.of())
                .orders(orders != null ? orders : List.of())
                .build();
    }

    @SuppressWarnings("unchecked")
    public AdminSearchResponse fromMap(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        return AdminSearchResponse.builder()
                .q(stringValue(body.get("q")))
                .products(mapList(body.get("products"), AdminSearchProductHit.class))
                .categories(mapList(body.get("categories"), AdminSearchCategoryHit.class))
                .users(mapList(body.get("users"), AdminSearchUserHit.class))
                .orders(mapList(body.get("orders"), AdminSearchOrderHit.class))
                .build();
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> mapList(Object value, Class<T> type) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toList());
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
