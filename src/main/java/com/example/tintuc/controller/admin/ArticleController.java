package com.example.tintuc.controller.admin;

import com.example.tintuc.dto.ArticleForm;
import com.example.tintuc.entity.ActivityType;
import com.example.tintuc.entity.Article;
import com.example.tintuc.entity.ArticleStatus;
import com.example.tintuc.entity.Category;
import com.example.tintuc.repository.ArticleRepository;
import com.example.tintuc.repository.CategoryRepository;
import com.example.tintuc.repository.UserRepository;
import com.example.tintuc.service.ActivityLogService;
import com.example.tintuc.service.FileStorageService;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/articles")
@RequiredArgsConstructor
public class ArticleController {

    private static final int PAGE_SIZE = 10;

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ActivityLogService activityLogService;

    // ===== Danh sách + tìm kiếm + lọc chuyên mục/trạng thái + phân trang =====
    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) ArticleStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        String k = keyword.trim().toLowerCase();
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("id").descending());

        Specification<Article> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!k.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + k + "%"));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Article> result = articleRepository.findAll(spec, pageable);

        model.addAttribute("page", result);
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", status);
        model.addAttribute("categories", categoryRepository.findAll(Sort.by("name")));
        return "admin/articles/list";
    }

    // ===== Thêm =====
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("articleForm", new ArticleForm());
        prepare(model, "Thêm bài viết");
        return "admin/articles/form";
    }

    @Transactional
    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("articleForm") ArticleForm form,
                         BindingResult result,
                         @RequestParam(value = "thumbnailFile", required = false) MultipartFile file,
                         Principal principal,
                         Model model,
                         RedirectAttributes ra) {
        form.setId(null);
        Category category = findCategory(form, result);
        validateFile(file, result);

        if (result.hasErrors()) {
            prepare(model, "Thêm bài viết");
            return "admin/articles/form";
        }

        Article article = new Article();
        apply(form, article, category);
        article.setAuthor(userRepository.findByUsername(principal.getName()).orElseThrow());

        if (file != null && !file.isEmpty()) {
            try {
                article.setThumbnail(fileStorageService.save(file));
            } catch (IOException e) {
                result.rejectValue("thumbnail", "io", "Không lưu được ảnh, vui lòng thử lại");
                prepare(model, "Thêm bài viết");
                return "admin/articles/form";
            }
        }
        articleRepository.save(article);
        activityLogService.record(principal.getName(), ActivityType.ARTICLE_CREATED, article.getTitle());

        ra.addFlashAttribute("success", "Đã thêm bài viết \"" + article.getTitle() + "\"");
        return "redirect:/admin/articles";
    }

    // ===== Sửa =====
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Article article = articleRepository.findById(id).orElse(null);
        if (article == null) {
            ra.addFlashAttribute("error", "Không tìm thấy bài viết");
            return "redirect:/admin/articles";
        }
        ArticleForm form = new ArticleForm();
        form.setId(article.getId());
        form.setTitle(article.getTitle());
        form.setSummary(article.getSummary());
        form.setContent(article.getContent());
        form.setCategoryId(article.getCategory().getId());
        form.setStatus(article.getStatus());
        form.setThumbnail(article.getThumbnail());
        model.addAttribute("articleForm", form);
        prepare(model, "Sửa bài viết");
        return "admin/articles/form";
    }

    @Transactional
    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("articleForm") ArticleForm form,
                         BindingResult result,
                         @RequestParam(value = "thumbnailFile", required = false) MultipartFile file,
                         Principal principal,
                         Model model,
                         RedirectAttributes ra) {
        Article article = articleRepository.findById(id).orElse(null);
        if (article == null) {
            ra.addFlashAttribute("error", "Không tìm thấy bài viết");
            return "redirect:/admin/articles";
        }

        Category category = findCategory(form, result);
        validateFile(file, result);

        if (result.hasErrors()) {
            form.setId(id);
            form.setThumbnail(article.getThumbnail());
            prepare(model, "Sửa bài viết");
            return "admin/articles/form";
        }

        apply(form, article, category);

        // Có chọn ảnh mới thì thay ảnh cũ, không thì giữ ảnh cũ
        if (file != null && !file.isEmpty()) {
            try {
                String oldThumbnail = article.getThumbnail();
                article.setThumbnail(fileStorageService.save(file));
                fileStorageService.delete(oldThumbnail);
            } catch (IOException e) {
                result.rejectValue("thumbnail", "io", "Không lưu được ảnh, vui lòng thử lại");
                form.setId(id);
                form.setThumbnail(article.getThumbnail());
                prepare(model, "Sửa bài viết");
                return "admin/articles/form";
            }
        }
        articleRepository.save(article);
        activityLogService.record(principal.getName(), ActivityType.ARTICLE_UPDATED, article.getTitle());

        ra.addFlashAttribute("success", "Đã cập nhật bài viết \"" + article.getTitle() + "\"");
        return "redirect:/admin/articles";
    }

    // ===== Xóa =====
    @Transactional
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        Article article = articleRepository.findById(id).orElse(null);
        if (article == null) {
            ra.addFlashAttribute("error", "Không tìm thấy bài viết");
        } else {
            fileStorageService.delete(article.getThumbnail());
            articleRepository.delete(article);
            activityLogService.record(principal.getName(), ActivityType.ARTICLE_DELETED, article.getTitle());
            ra.addFlashAttribute("success", "Đã xóa bài viết \"" + article.getTitle() + "\"");
        }
        return "redirect:/admin/articles";
    }

    // ===== Hỗ trợ =====
    private void prepare(Model model, String title) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("categories", categoryRepository.findAll(Sort.by("name")));
        model.addAttribute("statuses", ArticleStatus.values());
    }

    private Category findCategory(ArticleForm form, BindingResult result) {
        if (form.getCategoryId() == null) return null;
        Category category = categoryRepository.findById(form.getCategoryId()).orElse(null);
        if (category == null) {
            result.rejectValue("categoryId", "notfound", "Chuyên mục không tồn tại");
        }
        return category;
    }

    private void validateFile(MultipartFile file, BindingResult result) {
        String error = fileStorageService.validate(file);
        if (error != null) {
            result.rejectValue("thumbnail", "invalid", error);
        }
    }

    private void apply(ArticleForm form, Article article, Category category) {
        article.setTitle(form.getTitle().trim());
        article.setSummary(form.getSummary());
        article.setContent(form.getContent());
        article.setStatus(form.getStatus());
        article.setCategory(category);
    }
}
