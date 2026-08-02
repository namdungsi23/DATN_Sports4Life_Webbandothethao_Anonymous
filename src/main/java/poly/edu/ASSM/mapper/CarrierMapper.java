package poly.edu.ASSM.mapper;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Carriers;
import poly.edu.ASSM.dto.response.CarrierResponse;

@Component
public class CarrierMapper {

    public CarrierResponse toResponse(Carriers entity) {
        if (entity == null) {
            return null;
        }
        return CarrierResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .build();
    }

    public List<CarrierResponse> toResponseList(Collection<Carriers> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }
}
