package poly.edu.ASSM.Services.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.ASSM.entity.Orders;
import poly.edu.ASSM.entity.Ranks;
import poly.edu.ASSM.entity.Users;
import poly.edu.ASSM.repository.RankRepository;
import poly.edu.ASSM.repository.UsersRepository;
import poly.edu.ASSM.dto.request.RankRequest;
import poly.edu.ASSM.dto.response.RankResponse;
import poly.edu.ASSM.exception.InvalidInputException;
import poly.edu.ASSM.mapper.RankMapper;

@Service
public class RankService {

	/** 1 điểm / 1.000đ giá trị đơn đã giao */
	private static final BigDecimal VND_PER_POINT = BigDecimal.valueOf(1000);

	@Autowired
	private RankRepository rankRepository;

	@Autowired
	private UsersRepository usersRepository;

	@Autowired
	private RankMapper rankMapper;

	@Transactional(readOnly = true)
	public Map<String, Object> adminList() {
		List<RankResponse> ranks = rankRepository.findAll().stream()
				.sorted(Comparator.comparing(r -> r.getMinPoint() == null ? 0 : r.getMinPoint()))
				.map(this::toAdminResponse)
				.toList();
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("ranks", ranks);
		body.put("total", ranks.size());
		return body;
	}

	@Transactional
	public Map<String, Object> create(RankRequest request) {
		validateRequest(request, null);
		Ranks entity = rankMapper.toEntity(request);
		if (entity.getDiscountPercent() == null) {
			entity.setDiscountPercent(BigDecimal.ZERO);
		}
		if (entity.getIsActive() == null) {
			entity.setIsActive(true);
		}
		entity = rankRepository.save(entity);
		return Map.of("ok", true, "message", "Đã thêm hạng thành viên.", "rank", toAdminResponse(entity));
	}

	@Transactional
	public Map<String, Object> update(Integer id, RankRequest request) {
		Ranks entity = rankRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hạng."));
		validateRequest(request, id);
		rankMapper.applyRequest(entity, request);
		if (entity.getDiscountPercent() == null) {
			entity.setDiscountPercent(BigDecimal.ZERO);
		}
		entity = rankRepository.save(entity);
		recalculateAllMembers();
		return Map.of("ok", true, "message", "Đã cập nhật hạng.", "rank", toAdminResponse(entity));
	}

	@Transactional
	public Map<String, Object> delete(Integer id) {
		Ranks entity = rankRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hạng."));
		long members = usersRepository.countByRank_Id(id);
		if (members > 0) {
			throw new InvalidInputException(
					"Không xóa được: còn " + members + " thành viên thuộc hạng này. Hãy chuyển họ sang hạng khác hoặc tắt IsActive.");
		}
		if (id != null && id == 1) {
			throw new InvalidInputException("Không được xóa hạng mặc định (Id=1).");
		}
		rankRepository.delete(entity);
		return Map.of("ok", true, "message", "Đã xóa hạng.");
	}

	/** Cộng điểm + đồng bộ hạng (bình luận, đơn giao…). */
	@Transactional
	public void awardPoints(Users user, int points) {
		if (user == null || points <= 0) {
			return;
		}
		int current = user.getTotalPoint() != null ? user.getTotalPoint() : 0;
		user.setTotalPoint(current + points);
		syncRank(user);
		user.setUpdatedAt(Instant.now());
		usersRepository.save(user);
	}

	/** Khi đơn chuyển sang DELIVERED lần đầu — cộng điểm theo tổng tiền. */
	@Transactional
	public void awardForDeliveredOrder(Orders order) {
		if (order == null || order.getAccount() == null || order.getTotalAmount() == null) {
			return;
		}
		Users user = usersRepository.findByAccount_Id(order.getAccount().getId()).orElse(null);
		if (user == null) {
			return;
		}
		int points = order.getTotalAmount()
				.divide(VND_PER_POINT, 0, RoundingMode.DOWN)
				.intValue();
		if (points <= 0) {
			points = 1;
		}
		awardPoints(user, points);
	}

	@Transactional
	public void syncRank(Users user) {
		if (user == null) {
			return;
		}
		int points = user.getTotalPoint() != null ? user.getTotalPoint() : 0;
		Ranks best = resolveRankForPoints(points);
		if (best != null) {
			user.setRank(best);
		}
	}

	@Transactional(readOnly = true)
	public Ranks resolveRankForPoints(int points) {
		return rankRepository.findAll().stream()
				.filter(r -> !Boolean.FALSE.equals(r.getIsActive()))
				.filter(r -> r.getMinPoint() != null && points >= r.getMinPoint())
				.max(Comparator.comparing(Ranks::getMinPoint))
				.orElseGet(() -> rankRepository.findById(1).orElse(null));
	}

	@Transactional
	public void setMemberPoints(Long accountId, int totalPoint) {
		Users user = usersRepository.findByAccount_Id(accountId)
				.orElseThrow(() -> new InvalidInputException("Tài khoản chưa có hồ sơ Users."));
		if (totalPoint < 0) {
			throw new InvalidInputException("Điểm không được âm.");
		}
		user.setTotalPoint(totalPoint);
		syncRank(user);
		user.setUpdatedAt(Instant.now());
		usersRepository.save(user);
	}

	private void recalculateAllMembers() {
		for (Users user : usersRepository.findAll()) {
			syncRank(user);
			usersRepository.save(user);
		}
	}

	private void validateRequest(RankRequest request, Integer excludeId) {
		if (request.getMinPoint() != null && request.getMinPoint() < 0) {
			throw new InvalidInputException("MinPoint không được âm.");
		}
		boolean dupName = rankRepository.findAll().stream()
				.anyMatch(r -> r.getRankName() != null
						&& r.getRankName().equalsIgnoreCase(request.getRankName().trim())
						&& (excludeId == null || !excludeId.equals(r.getId())));
		if (dupName) {
			throw new InvalidInputException("Tên hạng đã tồn tại.");
		}
	}

	private RankResponse toAdminResponse(Ranks entity) {
		RankResponse res = rankMapper.toResponse(entity);
		if (res != null && entity.getId() != null) {
			res.setMemberCount(usersRepository.countByRank_Id(entity.getId()));
		}
		return res;
	}
}
