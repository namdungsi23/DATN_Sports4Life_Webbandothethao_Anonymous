package poly.edu.ASSM.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImageReorderRequest {
    private List<Long> imageIds;
}
