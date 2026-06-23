package poly.edu.ASSM.api.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.ASSM.Services.core.AdminAccessService;

@RestController
@RequestMapping("/api/admin")
public class AdminSessionApiController {

    @Autowired
    private AdminAccessService adminAccessService;

    @GetMapping("/me")
    public Map<String, Object> me(java.security.Principal principal) {
        AdminAccessService.AdminAccess access = adminAccessService.resolve(principal.getName());
        Map<String, Object> body = new HashMap<>();
        body.put("username", principal.getName());
        body.put("roles", List.copyOf(access.roles()));
        body.put("permissions", List.copyOf(access.permissions()));
        body.put("panelAccess", access.panelUser());
        return body;
    }
}
