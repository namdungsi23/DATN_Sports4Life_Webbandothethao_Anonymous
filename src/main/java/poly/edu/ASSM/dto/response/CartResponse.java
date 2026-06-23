package poly.edu.ASSM.dto.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {
    private Integer id;
    private Integer userId;
    private Instant createdAt;

    @Builder.Default
    private List<CartItemResponse> items = new ArrayList<>();
}
