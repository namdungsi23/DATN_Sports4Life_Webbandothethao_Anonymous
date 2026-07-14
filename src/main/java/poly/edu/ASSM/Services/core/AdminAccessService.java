package poly.edu.ASSM.Services.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.ASSM.entity.Accounts;
import poly.edu.ASSM.entity.Permissions;
import poly.edu.ASSM.entity.Roles;
import poly.edu.ASSM.repository.AccountRepository;
import poly.edu.ASSM.repository.PermissionRepository;
import poly.edu.ASSM.security.AdminPermissionCodes;
import poly.edu.ASSM.security.SpringRoleNames;

@Service
public class AdminAccessService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    public record AdminAccess(Set<String> roles, Set<String> permissions, boolean panelUser) {
    }

    @Transactional(readOnly = true)
    public AdminAccess resolve(String username) {
        Accounts account = accountRepository.findByUsername(username);
        if (account == null) {
            return new AdminAccess(Set.of(), Set.of(), false);
        }
        return resolveLoaded(account);
    }

    public AdminAccess resolve(Accounts account) {
        if (account == null) {
            return new AdminAccess(Set.of(), Set.of(), false);
        }
        return resolve(account.getUsername());
    }

    private AdminAccess resolveLoaded(Accounts account) {
        Set<String> roles = new LinkedHashSet<>();
        Set<String> permissions = new LinkedHashSet<>();

        Roles role = account.getRole();
        if (role != null && role.getName() != null && !role.getName().isBlank()) {
            String normalized = SpringRoleNames.normalize(role.getName());
            roles.add(normalized);
            loadPermissionsForRole(role, permissions);

            if (normalized.contains("ADMIN")) {
                grantAllPermissions(permissions);
            } else if (normalized.contains("STAFF")) {
                grantStaffPermissions(permissions);
            }
        }

        // Chỉ ADMIN/STAFF vào panel — không dựa vào permissions (ROLE_USER từng bị gán nhầm quyền xem)
        boolean panelUser = roles.stream().anyMatch(this::isStaffOrAdminRole);
        return new AdminAccess(roles, permissions, panelUser);
    }

    public List<GrantedAuthority> toAuthorities(AdminAccess access) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : access.roles()) {
            authorities.add(new SimpleGrantedAuthority(SpringRoleNames.normalize(role)));
        }
        for (String permission : access.permissions()) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        return authorities;
    }

    public boolean hasPermission(AdminAccess access, String required) {
        if (access == null || required == null) {
            return false;
        }
        if (isAdminRole(access)) {
            return true;
        }
        return access.permissions().stream().anyMatch(p -> AdminPermissionCodes.matches(required, p));
    }

    public boolean isAdminRole(AdminAccess access) {
        if (access == null) {
            return false;
        }
        return access.roles().stream().anyMatch(r -> {
            String normalized = SpringRoleNames.normalize(r).toUpperCase();
            return normalized.contains("ADMIN") || normalized.contains("SUPER_ADMIN");
        });
    }

    public boolean isStaffOnly(AdminAccess access) {
        if (access == null || isAdminRole(access)) {
            return false;
        }
        return access.roles().stream().anyMatch(r ->
                SpringRoleNames.normalize(r).toUpperCase().contains("STAFF"));
    }

    public boolean canWriteCatalog(AdminAccess access) {
        return isAdminRole(access);
    }

    public boolean canAccessPanel(AdminAccess access) {
        return access != null && access.panelUser();
    }

    private void loadPermissionsForRole(Roles role, Set<String> permissions) {
        if (role.getId() == null) {
            return;
        }
        for (Permissions permission : permissionRepository.findByRoleId(role.getId())) {
            if (permission.getPermissionName() != null) {
                permissions.add(permission.getPermissionName());
            }
        }
    }

    private void grantAllPermissions(Set<String> permissions) {
        for (Permissions permission : permissionRepository.findAll()) {
            if (permission.getPermissionName() != null) {
                permissions.add(permission.getPermissionName());
            }
        }
        permissions.add(AdminPermissionCodes.DASHBOARD);
        permissions.add(AdminPermissionCodes.PRODUCT);
        permissions.add(AdminPermissionCodes.CATEGORY);
        permissions.add(AdminPermissionCodes.ORDER);
        permissions.add(AdminPermissionCodes.ORDER_UPDATE);
        permissions.add(AdminPermissionCodes.USER);
        permissions.add("PRODUCT_CREATE");
        permissions.add("PRODUCT_UPDATE");
        permissions.add("PRODUCT_DELETE");
    }

    private void grantStaffPermissions(Set<String> permissions) {
        permissions.add(AdminPermissionCodes.DASHBOARD);
        permissions.add(AdminPermissionCodes.PRODUCT);
        permissions.add(AdminPermissionCodes.CATEGORY);
        permissions.add(AdminPermissionCodes.ORDER);
        permissions.add(AdminPermissionCodes.ORDER_UPDATE);
        permissions.add(AdminPermissionCodes.USER);
    }

    private boolean isStaffOrAdminRole(String role) {
        String normalized = SpringRoleNames.normalize(role).toUpperCase();
        return normalized.contains("ADMIN") || normalized.contains("STAFF");
    }

    public List<String> roleNamesForAccount(Accounts account) {
        if (account == null || account.getUsername() == null) {
            return List.of();
        }
        return resolve(account.getUsername()).roles().stream().toList();
    }
}
