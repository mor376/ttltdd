package com.example.tintuc.controller.admin;

import com.example.tintuc.dto.UserForm;
import com.example.tintuc.entity.ActivityType;
import com.example.tintuc.entity.Role;
import com.example.tintuc.entity.User;
import com.example.tintuc.repository.ArticleRepository;
import com.example.tintuc.repository.UserRepository;
import com.example.tintuc.service.ActivityLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserController {

    private static final int PAGE_SIZE = 10;

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;

    // ===== Danh sách + tìm kiếm + phân trang =====
    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       Principal principal,
                       Model model) {
        String k = keyword.trim();
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("id").descending());
        Page<User> result = userRepository
                .findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                        k, k, k, pageable);

        model.addAttribute("page", result);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentUsername", principal.getName());
        return "admin/users/list";
    }

    // ===== Thêm =====
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("userForm", new UserForm());
        prepare(model, "Thêm người dùng");
        return "admin/users/form";
    }

    @Transactional
    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("userForm") UserForm form,
                         BindingResult result,
                         Principal principal,
                         Model model,
                         RedirectAttributes ra) {
        form.setId(null);
        String username = form.getUsername() == null ? "" : form.getUsername().trim();
        String email = form.getEmail() == null ? "" : form.getEmail().trim();

        if (!result.hasFieldErrors("username") && userRepository.existsByUsername(username)) {
            result.rejectValue("username", "duplicate", "Tên đăng nhập đã tồn tại");
        }
        if (!result.hasFieldErrors("email") && userRepository.existsByEmail(email)) {
            result.rejectValue("email", "duplicate", "Email đã được sử dụng");
        }
        checkPassword(form.getPassword(), true, result);

        if (result.hasErrors()) {
            prepare(model, "Thêm người dùng");
            return "admin/users/form";
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(form.getFullName().trim());
        user.setRole(form.getRole());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        userRepository.save(user);
        activityLogService.record(principal.getName(), ActivityType.USER_CREATED, user.getUsername());

        ra.addFlashAttribute("success", "Đã thêm người dùng \"" + username + "\"");
        return "redirect:/admin/users";
    }

    // ===== Sửa =====
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            ra.addFlashAttribute("error", "Không tìm thấy người dùng");
            return "redirect:/admin/users";
        }
        UserForm form = new UserForm();
        form.setId(user.getId());
        form.setUsername(user.getUsername());
        form.setEmail(user.getEmail());
        form.setFullName(user.getFullName());
        form.setRole(user.getRole());
        model.addAttribute("userForm", form);
        prepare(model, "Sửa người dùng");
        return "admin/users/form";
    }

    @Transactional
    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("userForm") UserForm form,
                         BindingResult result,
                         Principal principal,
                         Model model,
                         RedirectAttributes ra) {
        User existing = userRepository.findById(id).orElse(null);
        if (existing == null) {
            ra.addFlashAttribute("error", "Không tìm thấy người dùng");
            return "redirect:/admin/users";
        }

        // Tên đăng nhập không cho đổi khi sửa
        form.setUsername(existing.getUsername());

        String email = form.getEmail() == null ? "" : form.getEmail().trim();
        if (!result.hasFieldErrors("email") && userRepository.existsByEmailAndIdNot(email, id)) {
            result.rejectValue("email", "duplicate", "Email đã được sử dụng");
        }
        checkPassword(form.getPassword(), false, result);

        // Không cho tự hạ quyền chính mình
        if (!result.hasFieldErrors("role")
                && existing.getUsername().equals(principal.getName())
                && form.getRole() != Role.ADMIN) {
            result.rejectValue("role", "self", "Không thể tự hạ quyền của chính mình");
        }

        if (result.hasErrors()) {
            form.setId(id);
            prepare(model, "Sửa người dùng");
            return "admin/users/form";
        }

        existing.setEmail(email);
        existing.setFullName(form.getFullName().trim());
        existing.setRole(form.getRole());
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(form.getPassword()));
        }
        userRepository.save(existing);
        ActivityType action = existing.getUsername().equals(principal.getName())
                ? ActivityType.ADMIN_PROFILE_UPDATED : ActivityType.USER_UPDATED;
        activityLogService.record(principal.getName(), action, existing.getFullName());

        ra.addFlashAttribute("success", "Đã cập nhật người dùng \"" + existing.getUsername() + "\"");
        return "redirect:/admin/users";
    }

    // ===== Xóa =====
    @Transactional
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            ra.addFlashAttribute("error", "Không tìm thấy người dùng");
        } else if (user.getUsername().equals(principal.getName())) {
            ra.addFlashAttribute("error", "Bạn không thể tự xóa tài khoản của mình");
        } else if (articleRepository.countByAuthorId(id) > 0) {
            ra.addFlashAttribute("error",
                    "Không thể xóa \"" + user.getUsername() + "\" vì người này còn bài viết");
        } else {
            userRepository.delete(user);
            activityLogService.record(principal.getName(), ActivityType.USER_DELETED, user.getUsername());
            ra.addFlashAttribute("success", "Đã xóa người dùng \"" + user.getUsername() + "\"");
        }
        return "redirect:/admin/users";
    }

    // ===== Hỗ trợ =====
    private void prepare(Model model, String title) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("roles", Role.values());
    }

    private void checkPassword(String password, boolean required, BindingResult result) {
        if (password == null || password.isBlank()) {
            if (required) {
                result.rejectValue("password", "required", "Mật khẩu không được để trống");
            }
        } else if (password.length() < 6) {
            result.rejectValue("password", "short", "Mật khẩu tối thiểu 6 ký tự");
        }
    }
}
