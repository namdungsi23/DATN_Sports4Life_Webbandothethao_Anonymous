package poly.edu.ASSM.domain;



import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor @AllArgsConstructor @Getter @Setter
public class CartItem {
	private Integer productId;
    private String name;
    private Double price;
    private int quantity;
}
