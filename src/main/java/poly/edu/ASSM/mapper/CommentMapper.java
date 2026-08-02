package poly.edu.ASSM.mapper;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Comment;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.dto.response.CommentResponse;

@Component
public class CommentMapper {

	public CommentResponse toResponse(Comment c) {
		if (c == null) {
			return null;
		}
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

	public List<CommentResponse> toResponseList(Collection<Comment> entities) {
		if (entities == null) {
			return List.of();
		}
		return entities.stream().map(this::toResponse).collect(Collectors.toList());
	}
}
