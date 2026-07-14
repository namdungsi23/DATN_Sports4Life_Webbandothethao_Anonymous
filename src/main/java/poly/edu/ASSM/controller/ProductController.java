package poly.edu.ASSM.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import poly.edu.ASSM.entity.Products;
import poly.edu.ASSM.Services.core.CategoryServiceImpl;
import poly.edu.ASSM.Services.core.ProductServiceImpl;

@Controller
@RequestMapping("/product")
public class ProductController {
	@Autowired
	ProductServiceImpl productService;
	
	@Autowired
	CategoryServiceImpl catService;
	
	@GetMapping("/page")
    public String ajaxProducts(
            @RequestParam(required = false) String cat,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double min,
            @RequestParam(required = false) Double max,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "price") String sort,
            @RequestParam(defaultValue = "asc") String dir,
            Model model
    ) {

       Pageable pageable = PageRequest.of(page, 10, 
    		   						Sort.by(dir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sort));
       
       Page<Products> products = productService.filterProducts(cat, keyword, min, max, pageable);
       
       model.addAttribute("products", products);
       
       return "fragments/product-list :: productList";
    }

}
