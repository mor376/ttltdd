package com.example.tintuc.controller.admin;

import com.example.tintuc.entity.ArticleStatus;
import com.example.tintuc.repository.ArticleRepository;
import com.example.tintuc.repository.CategoryRepository;
import com.example.tintuc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("userCount", userRepository.count());
        model.addAttribute("categoryCount", categoryRepository.count());
        model.addAttribute("articleCount", articleRepository.count());
        model.addAttribute("draftCount", articleRepository.countByStatus(ArticleStatus.DRAFT));
        model.addAttribute("latestArticles", articleRepository.findTop5ByOrderByCreatedAtDesc());
        return "admin/dashboard";
    }
}
