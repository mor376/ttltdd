package com.example.tintuc.controller.admin;

import com.example.tintuc.entity.Category;
import com.example.tintuc.repository.ArticleRepository;
import com.example.tintuc.repository.CategoryRepository;
import com.example.tintuc.util.SlugUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class CategoryController {

    private static final int PAGE_SIZE = 10;

    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;

    // ===== Danh sách + tìm kiếm + phân trang =====
    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("id").descending());
        Page<Category> result = categoryRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);

        // Số bài viết của từng chuyên mục
        Map<Long, Long> counts = new HashMap<>();
        for (Category c : result.getContent()) {
            counts.put(c.getId(), articleRepository.countByCategoryId(c.getId()));
        }

        model.addAttribute("page", result);
        model.addAttribute("keyword", keyword);
        model.addAttribute("counts", counts);
        return "admin/categories/list";
    }

    // ===== Thêm =====
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("category", new Category());
        model.addAttribute("pageTitle", "Thêm chuyên mục");
        return "admin/categories/form";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("category") Category category,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        category.setId(null);
        String name = category.getName() == null ? "" : category.getName().trim();

        if (!result.hasFieldErrors("name") && categoryRepository.existsByName(name)) {
            result.rejectValue("name", "duplicate", "Tên chuyên mục đã tồn tại");
        }
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Thêm chuyên mục");
            return "admin/categories/form";
        }

        category.setName(name);
        category.setSlug(uniqueSlug(name, null));
        categoryRepository.save(category);

        ra.addFlashAttribute("success", "Đã thêm chuyên mục \"" + name + "\"");
        return "redirect:/admin/categories";
    }

    // ===== Sửa =====
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        return categoryRepository.findById(id).map(c -> {
            model.addAttribute("category", c);
            model.addAttribute("pageTitle", "Sửa chuyên mục");
            return "admin/categories/form";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "Không tìm thấy chuyên mục");
            return "redirect:/admin/categories";
        });
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("category") Category category,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        Category existing = categoryRepository.findById(id).orElse(null);
        if (existing == null) {
            ra.addFlashAttribute("error", "Không tìm thấy chuyên mục");
            return "redirect:/admin/categories";
        }

        String name = category.getName() == null ? "" : category.getName().trim();
        if (!result.hasFieldErrors("name") && categoryRepository.existsByNameAndIdNot(name, id)) {
            result.rejectValue("name", "duplicate", "Tên chuyên mục đã tồn tại");
        }
        if (result.hasErrors()) {
            category.setId(id); // để form biết đang ở chế độ sửa
            model.addAttribute("pageTitle", "Sửa chuyên mục");
            return "admin/categories/form";
        }

        if (!existing.getName().equals(name)) {
            existing.setSlug(uniqueSlug(name, id));
        }
        existing.setName(name);
        existing.setDescription(category.getDescription());
        categoryRepository.save(existing);

        ra.addFlashAttribute("success", "Đã cập nhật chuyên mục \"" + name + "\"");
        return "redirect:/admin/categories";
    }

    // ===== Xóa =====
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) {
            ra.addFlashAttribute("error", "Không tìm thấy chuyên mục");
        } else if (articleRepository.countByCategoryId(id) > 0) {
            ra.addFlashAttribute("error",
                    "Không thể xóa \"" + category.getName() + "\" vì còn bài viết thuộc chuyên mục này");
        } else {
            categoryRepository.delete(category);
            ra.addFlashAttribute("success", "Đã xóa chuyên mục \"" + category.getName() + "\"");
        }
        return "redirect:/admin/categories";
    }

    // ===== Tạo slug không trùng =====
    private String uniqueSlug(String name, Long excludeId) {
        String base = SlugUtil.toSlug(name);
        if (base.isEmpty()) base = "chuyen-muc";
        String slug = base;
        int i = 2;
        while (excludeId == null
                ? categoryRepository.existsBySlug(slug)
                : categoryRepository.existsBySlugAndIdNot(slug, excludeId)) {
            slug = base + "-" + i++;
        }
        return slug;
    }
}
