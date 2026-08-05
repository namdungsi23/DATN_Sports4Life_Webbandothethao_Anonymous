package poly.edu.ASSM.Services.core;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Category;
import poly.edu.ASSM.Entity.Orders;
import poly.edu.ASSM.Entity.Products;
import poly.edu.ASSM.Repository.AccountRepository;
import poly.edu.ASSM.Repository.OrdersRepository;
import poly.edu.ASSM.dto.response.AdminSearchCategoryHit;
import poly.edu.ASSM.dto.response.AdminSearchOrderHit;
import poly.edu.ASSM.dto.response.AdminSearchProductHit;
import poly.edu.ASSM.dto.response.AdminSearchResponse;
import poly.edu.ASSM.dto.response.AdminSearchUserHit;
import poly.edu.ASSM.mapper.AdminSearchMapper;
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

	@Autowired
	private AdminSearchMapper adminSearchMapper;

	@Transactional(readOnly = true)
	public AdminSearchResponse search(String username, String q) {
		String kw = q == null ? "" : q.trim();
		List<AdminSearchProductHit> products = List.of();
		List<AdminSearchCategoryHit> categories = List.of();
		List<AdminSearchUserHit> users = List.of();
		List<AdminSearchOrderHit> orders = List.of();

		if (kw.length() < 1) {
			return adminSearchMapper.toSearchResponse(kw, products, categories, users, orders);
		}

		AdminAccessService.AdminAccess access = adminAccessService.resolve(username);
		PageRequest limit = PageRequest.of(0, 5);

		if (adminAccessService.hasPermission(access, AdminPermissionCodes.PRODUCT)) {
			List<Products> productEntities = productService.findAll(0, 5, "name", "asc", kw).getContent();
			products = adminSearchMapper.toProductHitList(productEntities);
		}

		if (adminAccessService.hasPermission(access, AdminPermissionCodes.CATEGORY)) {
			List<Category> categoryEntities = categoryService.search(kw, limit).getContent();
			categories = adminSearchMapper.toCategoryHitList(categoryEntities);
		}

		if (adminAccessService.hasPermission(access, AdminPermissionCodes.USER)) {
			List<Accounts> accountEntities = accountRepository.search(kw, limit).getContent();
			users = adminSearchMapper.toUserHitList(accountEntities);
		}

		if (adminAccessService.hasPermission(access, AdminPermissionCodes.ORDER)) {
			Integer idExact = null;
			try {
				idExact = Integer.valueOf(kw);
			} catch (NumberFormatException ignored) {
				/* not numeric */
			}
			List<Orders> orderEntities = ordersRepository.searchByKeyword(kw, idExact, limit);
			orders = adminSearchMapper.toOrderHitList(orderEntities);
		}

		return adminSearchMapper.toSearchResponse(kw, products, categories, users, orders);
	}
}
