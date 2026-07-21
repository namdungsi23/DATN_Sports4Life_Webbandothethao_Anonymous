package poly.edu.ASSM.Services.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Category;
import poly.edu.ASSM.Entity.Orders;
import poly.edu.ASSM.Entity.Products;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.Repository.AccountRepository;
import poly.edu.ASSM.Repository.OrdersRepository;
import poly.edu.ASSM.security.AdminPermissionCodes;

@Service
public class AdminSearchService {

	@Autowired
	private AdminAccessService adminAccessService;

	@Autowired
	private ProductService productService;

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private OrdersRepository ordersRepository;

	@Transactional(readOnly = true)
	public Map<String, Object> search(String username, String q) {
		String kw = q == null ? "" : q.trim();
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("q", kw);
		body.put("products", List.of());
		body.put("categories", List.of());
		body.put("users", List.of());
		body.put("orders", List.of());

		if (kw.length() < 1) {
			return body;
		}

		AdminAccessService.AdminAccess access = adminAccessService.resolve(username);
		PageRequest limit = PageRequest.of(0, 5);

		if (adminAccessService.hasPermission(access, AdminPermissionCodes.PRODUCT)) {
			List<Map<String, Object>> products = new ArrayList<>();
			for (Products p : productService.findAll(0, 5, "name", "asc", kw).getContent()) {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("id", p.getId());
				row.put("name", p.getName());
				row.put("brand", p.getBrand());
				row.put("link", "/admin/product?edit=" + p.getId());
				products.add(row);
			}
			body.put("products", products);
		}

		if (adminAccessService.hasPermission(access, AdminPermissionCodes.CATEGORY)) {
			List<Map<String, Object>> categories = new ArrayList<>();
			for (Category c : categoryService.search(kw, limit).getContent()) {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("id", c.getId());
				row.put("name", c.getName());
				row.put("link", "/admin/category");
				categories.add(row);
			}
			body.put("categories", categories);
		}

		if (adminAccessService.hasPermission(access, AdminPermissionCodes.USER)) {
			List<Map<String, Object>> users = new ArrayList<>();
			for (Accounts a : accountRepository.search(kw, limit).getContent()) {
				Users profile = a.getUsers();
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("id", a.getId());
				row.put("username", a.getUsername());
				row.put("fullname", profile != null ? profile.getFullName() : null);
				row.put("email", a.getEmail());
				row.put("link", "/admin/user?keyword=" + a.getUsername());
				users.add(row);
			}
			body.put("users", users);
		}

		if (adminAccessService.hasPermission(access, AdminPermissionCodes.ORDER)) {
			Integer idExact = null;
			try {
				idExact = Integer.valueOf(kw);
			} catch (NumberFormatException ignored) {
				/* not numeric */
			}
			List<Map<String, Object>> orders = new ArrayList<>();
			for (Orders o : ordersRepository.searchByKeyword(kw, idExact, limit)) {
				Accounts a = o.getAccount();
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("id", o.getId());
				row.put("username", a != null ? a.getUsername() : "");
				row.put("orderStatus", o.getOrderStatus());
				row.put("totalAmount", o.getTotalAmount());
				row.put("link", "/admin/order/" + o.getId());
				orders.add(row);
			}
			body.put("orders", orders);
		}

		return body;
	}
}
