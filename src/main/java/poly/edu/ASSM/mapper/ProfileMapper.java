package poly.edu.ASSM.mapper;

import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.dto.response.ProfileResponse;

@Component
public class ProfileMapper {

    public ProfileResponse toResponse(Accounts account, Users users) {
        if (account == null) {
            return null;
        }
        ProfileResponse.ProfileResponseBuilder b = ProfileResponse.builder()
                .username(account.getUsername())
                .email(account.getEmail())
                .createdAt(account.getCreatedAt());

        if (users != null) {
            b.fullname(users.getFullName())
                    .photo(users.getAvatar())
                    .avatar(users.getAvatar())
                    .phone(users.getPhone())
                    .createDate(users.getCreatedAt())
                    .totalPoint(users.getTotalPoint() != null ? users.getTotalPoint() : 0);
            if (users.getRank() != null) {
                b.rankId(users.getRank().getId())
                        .rankName(users.getRank().getRankName())
                        .rankDiscountPercent(users.getRank().getDiscountPercent())
                        .rankMinPoint(users.getRank().getMinPoint());
            }
        } else {
            b.totalPoint(0);
        }
        return b.build();
    }
}
