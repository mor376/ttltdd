package com.example.tintuc.controller.admin;

import com.example.tintuc.dto.CommentForm;
import com.example.tintuc.entity.ActivityType;
import com.example.tintuc.entity.Article;
import com.example.tintuc.entity.Comment;
import com.example.tintuc.entity.CommentStatus;
import com.example.tintuc.entity.User;
import com.example.tintuc.repository.ArticleRepository;
import com.example.tintuc.repository.CommentRepository;
import com.example.tintuc.repository.UserRepository;
import com.example.tintuc.service.ActivityLogService;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
public class CommentController {

    private static final int PAGE_SIZE = 10;

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(required = false) Long articleId,
                       @RequestParam(required = false) CommentStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        String term = keyword.trim().toLowerCase();
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("createdAt").descending());
        Specification<Comment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!term.isEmpty()) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("content")), "%" + term + "%"),
                        cb.like(cb.lower(root.get("user").get("fullName")), "%" + term + "%"),
                        cb.like(cb.lower(root.get("user").get("username")), "%" + term + "%")));
            }
            if (articleId != null) {
                predicates.add(cb.equal(root.get("article").get("id"), articleId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        model.addAttribute("page", commentRepository.findAll(spec, pageable));
        model.addAttribute("keyword", keyword);
        model.addAttribute("articleId", articleId);
        model.addAttribute("status", status);
        model.addAttribute("articles", articleRepository.findAll(Sort.by("title")));
        model.addAttribute("statuses", CommentStatus.values());
        return "admin/comments/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("commentForm", new CommentForm());
        prepareForm(model, "Thêm bình luận");
        return "admin/comments/form";
    }

    @Transactional
    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("commentForm") CommentForm form,
                         BindingResult result,
                         Principal principal,
                         Model model,
                         RedirectAttributes ra) {
        Article article = findArticle(form, result);
        User user = findUser(form, result);
        if (result.hasErrors()) {
            prepareForm(model, "Thêm bình luận");
            return "admin/comments/form";
        }

        Comment comment = new Comment();
        apply(form, comment, article, user);
        commentRepository.save(comment);
        activityLogService.record(principal.getName(), ActivityType.COMMENT_CREATED, article.getTitle());
        ra.addFlashAttribute("success", "Đã thêm bình luận");
        return "redirect:/admin/comments";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Comment comment = commentRepository.findById(id).orElse(null);
        if (comment == null) {
            ra.addFlashAttribute("error", "Không tìm thấy bình luận");
            return "redirect:/admin/comments";
        }

        CommentForm form = new CommentForm();
        form.setId(comment.getId());
        form.setContent(comment.getContent());
        form.setArticleId(comment.getArticle().getId());
        form.setUserId(comment.getUser().getId());
        form.setStatus(comment.getStatus());
        model.addAttribute("commentForm", form);
        prepareForm(model, "Sửa bình luận");
        return "admin/comments/form";
    }

    @Transactional
    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("commentForm") CommentForm form,
                         BindingResult result,
                         Principal principal,
                         Model model,
                         RedirectAttributes ra) {
        Comment comment = commentRepository.findById(id).orElse(null);
        if (comment == null) {
            ra.addFlashAttribute("error", "Không tìm thấy bình luận");
            return "redirect:/admin/comments";
        }

        Article article = findArticle(form, result);
        User user = findUser(form, result);
        if (result.hasErrors()) {
            form.setId(id);
            prepareForm(model, "Sửa bình luận");
            return "admin/comments/form";
        }

        apply(form, comment, article, user);
        commentRepository.save(comment);
        activityLogService.record(principal.getName(), ActivityType.COMMENT_UPDATED, article.getTitle());
        ra.addFlashAttribute("success", "Đã cập nhật bình luận");
        return "redirect:/admin/comments";
    }

    @Transactional
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        if (!commentRepository.existsById(id)) {
            ra.addFlashAttribute("error", "Không tìm thấy bình luận");
        } else {
            Comment comment = commentRepository.findById(id).orElseThrow();
            commentRepository.deleteById(id);
            activityLogService.record(principal.getName(), ActivityType.COMMENT_DELETED,
                    comment.getArticle().getTitle());
            ra.addFlashAttribute("success", "Đã xóa bình luận");
        }
        return "redirect:/admin/comments";
    }

    private Article findArticle(CommentForm form, BindingResult result) {
        if (form.getArticleId() == null) return null;
        Article article = articleRepository.findById(form.getArticleId()).orElse(null);
        if (article == null) result.rejectValue("articleId", "notfound", "Bài viết không tồn tại");
        return article;
    }

    private User findUser(CommentForm form, BindingResult result) {
        if (form.getUserId() == null) return null;
        User user = userRepository.findById(form.getUserId()).orElse(null);
        if (user == null) result.rejectValue("userId", "notfound", "Người dùng không tồn tại");
        return user;
    }

    private void apply(CommentForm form, Comment comment, Article article, User user) {
        comment.setContent(form.getContent().trim());
        comment.setStatus(form.getStatus());
        comment.setArticle(article);
        comment.setUser(user);
    }

    private void prepareForm(Model model, String title) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("articles", articleRepository.findAll(Sort.by("title")));
        model.addAttribute("users", userRepository.findAll(Sort.by("fullName")));
        model.addAttribute("statuses", CommentStatus.values());
    }
}
