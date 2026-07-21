package poly.edu.ASSM.Services.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Comment;
import poly.edu.ASSM.Entity.Products;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.Repository.AccountRepository;
import poly.edu.ASSM.Repository.CommentRepository;
import poly.edu.ASSM.Repository.ProductRepository;
import poly.edu.ASSM.Repository.UsersRepository;
import poly.edu.ASSM.dto.request.CommentRequest;
import poly.edu.ASSM.dto.response.CommentResponse;
import poly.edu.ASSM.exception.InvalidInputException;

@Service
public class CommentService {

	private static final int POINTS_PER_COMMENT = 20;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private UsersRepository usersRepository;

	@Autowired
	private RankService rankService;

	@Transactional(readOnly = true)
	public Map<String, Object> listByProduct(Long productId) {
		requireProduct(productId);
		List<CommentResponse> comments = commentRepository.findVisibleByProductId(productId).stream()
				.map(this::toResponse)
				.toList();
		Map<String, Object> stats = ratingStats(productId);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("comments", comments);
		body.put("avgRating", stats.get("avgRating"));
		body.put("ratingCount", stats.get("ratingCount"));
		return body;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> ratingStats(Long productId) {
		Double avg = commentRepository.avgRating(productId);
		long count = commentRepository.countVisible(productId);
		Map<String, Object> stats = new HashMap<>();
		stats.put("avgRating", BigDecimal.valueOf(avg == null ? 0 : avg).setScale(1, RoundingMode.HALF_UP));
		stats.put("ratingCount", count);
		return stats;
	}

	@Transactional
	public Map<String, Object> createOrUpdate(String username, CommentRequest request) {
		Accounts account = accountRepository.findByUsername(username);
		if (account == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập.");
		}
		Users user = usersRepository.findByAccount_Id(account.getId())
				.orElseThrow(() -> new InvalidInputException("Tài khoản chưa có hồ sơ Users."));

		Products product = requireProduct(request.getProductId());

		// Chỉ cần đăng nhập — không bắt buộc đơn DELIVERED
		String content = request.getContent().trim();
		Comment comment = commentRepository.findByProduct_IdAndUsers_Id(product.getId(), user.getId())
				.orElse(null);
		boolean isNew = comment == null;
		Instant now = Instant.now();
		if (isNew) {
			comment = new Comment();
			comment.setProduct(product);
			comment.setUsers(user);
			comment.setCreatedAt(now);
			comment.setStatus(true);
		}
		comment.setContent(content);
		comment.setRating(request.getRating());
		comment.setUpdatedAt(now);
		comment = commentRepository.save(comment);

		if (isNew) {
			rankService.awardPoints(user, POINTS_PER_COMMENT);
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("ok", true);
		body.put("message", isNew ? "Đã gửi bình luận." : "Đã cập nhật bình luận.");
		body.put("comment", toResponse(comment));
		body.putAll(ratingStats(product.getId()));
		return body;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> adminList(String keyword, Boolean visible) {
		String kw = keyword == null ? "" : keyword.trim();
		List<CommentResponse> rows = commentRepository.adminSearch(kw.isEmpty() ? null : kw, visible).stream()
				.map(this::toResponse)
				.toList();
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("comments", rows);
		body.put("total", rows.size());
		return body;
	}

	@Transactional
	public Map<String, Object> adminSetVisible(int id, boolean visible) {
		Comment comment = commentRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bình luận."));
		comment.setStatus(visible);
		comment.setUpdatedAt(Instant.now());
		commentRepository.save(comment);
		return Map.of(
				"ok", true,
				"message", visible ? "Đã hiện bình luận." : "Đã ẩn bình luận.",
				"comment", toResponse(comment));
	}

	@Transactional
	public Map<String, Object> adminDelete(int id) {
		Comment comment = commentRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bình luận."));
		commentRepository.delete(comment);
		return Map.of("ok", true, "message", "Đã xóa bình luận.");
	}

	private Products requireProduct(Long productId) {
		return productRepository.findById(productId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm."));
	}

	private CommentResponse toResponse(Comment c) {
		Users u = c.getUsers();
		Accounts acc = u != null ? u.getAccount() : null;
		String rankName = u != null && u.getRank() != null ? u.getRank().getRankName() : null;
		return CommentResponse.builder()
				.id(c.getId())
				.productId(c.getProduct() != null ? c.getProduct().getId() : null)
				.productName(c.getProduct() != null ? c.getProduct().getName() : null)
				.userId(u != null ? u.getId() : null)
				.username(acc != null ? acc.getUsername() : null)
				.fullName(u != null ? u.getFullName() : null)
				.avatar(u != null ? u.getAvatar() : null)
				.rankName(rankName)
				.rating(c.getRating())
				.content(c.getContent())
				.status(c.getStatus() == null || Boolean.TRUE.equals(c.getStatus()))
				.createdAt(c.getCreatedAt())
				.updatedAt(c.getUpdatedAt())
				.build();
	}
}
