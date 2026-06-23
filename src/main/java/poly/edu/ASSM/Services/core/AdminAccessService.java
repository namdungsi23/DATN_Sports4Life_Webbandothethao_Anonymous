package poly.edu.ASSM.Services.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Permissions;
import poly.edu.ASSM.Entity.Roles;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.Repository.AccountRepository;
import poly.edu.ASSM.Repository.PermissionRepository;
import poly.edu.ASSM.Repository.UsersRepository;
import poly.edu.ASSM.security.AdminPermissionCodes;
import poly.edu.ASSM.security.SpringRoleNames;

@Service
public class AdminAccessService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    public record AdminAccess(Set<String> roles, Set<String> permissions, boolean panelUser) {
    }

    public AdminAccess resolve(String username) {
        Accounts account = accountRepository.findByUsername(username);
        if (account == null) {
            return new AdminAccess(Set.of(), Set.of(), false);
        }
        return resolve(account);
    }

    public AdminAccess resolve(Accounts account) {
        Set<String> roles = new LinkedHashSet<>();
        Set<String> permissions = new LinkedHashSet<>();

        usersRepository.findByAccount_Id(account.getId()).ifPresent(user -> {
            for (Roles role : user.getRoles()) {
                if (role.getName() != null && !role.getName().isBlank()) {
                    roles.add(SpringRoleNames.normalize(role.getName()));
                }
                loadPermissionsForRole(role, permissions);
            }
        });

        if (Boolean.TRUE.equals(account.getAdmin())) {
            roles.add(SpringRoleNames.normalize("ADMIN"));
            grantAllPermissions(permissions);
        }

        boolean panelUser = Boolean.TRUE.equals(account.getAdmin())
                || roles.stream().anyMatch(this::isStaffOrAdminRole)
                || !permissions.isEmpty();

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
        if (access.roles().contains(SpringRoleNames.normalize("ADMIN"))
                || access.roles().contains(SpringRoleNames.normalize("SUPER_ADMIN"))) {
            return true;
        }
        return access.permissions().stream().anyMatch(p -> AdminPermissionCodes.matches(required, p));
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
        permissions.add(AdminPermissionCodes.USER);
        permissions.add("PRODUCT_CREATE");
        permissions.add("PRODUCT_UPDATE");
        permissions.add("PRODUCT_DELETE");
    }

    private boolean isStaffOrAdminRole(String role) {
        String normalized = SpringRoleNames.normalize(role).toUpperCase();
        return normalized.contains("ADMIN") || normalized.contains("STAFF");
    }

    public List<String> roleNamesForAccount(Accounts account) {
        if (account == null) {
            return List.of();
        }
        Users user = usersRepository.findByAccount_Id(account.getId()).orElse(null);
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            if (Boolean.TRUE.equals(account.getAdmin())) {
                return List.of(SpringRoleNames.normalize("ADMIN"));
            }
            return List.of();
        }
        return user.getRoles().stream()
                .map(Roles::getName)
                .filter(n -> n != null && !n.isBlank())
                .map(SpringRoleNames::normalize)
                .distinct()
                .toList();
    }
}
