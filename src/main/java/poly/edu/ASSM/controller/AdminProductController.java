package poly.edu.ASSM.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import poly.edu.ASSM.entity.Products;
import poly.edu.ASSM.Services.core.CategoryService;
import poly.edu.ASSM.Services.core.ProductService;
import poly.edu.ASSM.Services.util.CloudinaryService;
import poly.edu.ASSM.exception.InvalidInputException;

@Controller
public class AdminProductController {

    @Autowired
    ProductService prt;

    @Autowired
    CategoryService ctr;

    @Autowired
    CloudinaryService cloudService;

    @GetMapping({ "/admin/product", "/admin/product/index" })
    public String index(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String cat,
            @RequestParam(required = false) Long editId) {
        Page<Products> pages = prt.filterProducts(
                cat,
                keyword,
                null,
                null,
                PageRequest.of(page, size, Sort.by("id").descending()));

        if (!model.containsAttribute("product")) {
            Products product = (editId != null) ? prt.findById(editId) : new Products();
            if (product == null) {
                product = new Products();
            }
            model.addAttribute("product", product);
        } else {
            model.addAttribute("product", model.getAttribute("product"));
        }

        handleMessage(model);

        model.addAttribute("products", pages.getContent());
        model.addAttribute("pages", pages);
        model.addAttribute("keyword", keyword);
        model.addAttribute("cat", cat);
        model.addAttribute("categories", ctr.findAll());

        return "admin/product";
    }

    @PostMapping("/admin/product/save")
    public String save(@ModelAttribute Products product,
            @RequestParam("inventory.quantity") Integer quantity,
            @RequestParam(required = false) MultipartFile uploadImage,
            RedirectAttributes redirectAttr,
            Model model) {

        try {
            prt.create(product, quantity != null ? quantity : 1);
            redirectAttr.addFlashAttribute("saveSuccessMsg", "Lưu thành công!");
            return "redirect:/admin/product";
        } catch (InvalidInputException e) {
            redirectAttr.addFlashAttribute("erMsg", e.getMessage());
            redirectAttr.addFlashAttribute("product", product);
            return "redirect:/admin/product";
        }
    }

    @GetMapping("/admin/product/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttr) {
        try {
            prt.delete(id);
            redirectAttr.addFlashAttribute("deleteSuccessMsg", "Xóa sản phẩm thành công!");
        } catch (Exception e) {
            redirectAttr.addFlashAttribute("deleteErrorMsg", "Xóa thất bại!");
        }
        return "redirect:/admin/product";
    }

    private void handleMessage(Model model) {
        Object saveSuccess = model.getAttribute("saveSuccessMsg");
        Object saveError = model.getAttribute("saveErrorMsg");
        Object deleteSuccess = model.getAttribute("deleteSuccessMsg");
        Object deleteError = model.getAttribute("deleteErrorMsg");

        if (saveSuccess != null) {
            model.addAttribute("saveSuccessMsg", saveSuccess);
        }
        if (saveError != null) {
            model.addAttribute("saveErrorMsg", saveError);
        }
        if (deleteSuccess != null) {
            model.addAttribute("deleteSuccessMsg", deleteSuccess);
        }
        if (deleteError != null) {
            model.addAttribute("deleteErrorMsg", deleteError);
        }
    }
}
