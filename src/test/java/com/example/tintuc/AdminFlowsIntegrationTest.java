package com.example.tintuc;

import com.example.tintuc.entity.Article;
import com.example.tintuc.entity.ArticleStatus;
import com.example.tintuc.entity.Category;
import com.example.tintuc.repository.ArticleRepository;
import com.example.tintuc.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminFlowsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void loginRequiresValidCredentialsAndProtectsAdminPages() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/admin/login"));

        mockMvc.perform(post("/admin/login")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?error"));

        mockMvc.perform(post("/admin/login")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    void articleCrudStatusFilterAndDashboardWorkTogether() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Category category = categoryRepository.findAll().get(0);
        String title = "Bài kiểm thử " + UUID.randomUUID();

        mockMvc.perform(post("/admin/articles/create")
                        .session(session)
                        .with(csrf())
                        .param("title", title)
                        .param("summary", "Tóm tắt bài kiểm thử")
                        .param("content", "Nội dung bài viết kiểm thử.")
                        .param("categoryId", category.getId().toString())
                        .param("status", "PUBLISHED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/articles"));

        Article article = articleRepository.findByTitleIgnoreCase(title).orElseThrow();
        mockMvc.perform(get("/admin/articles")
                        .session(session)
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(title)));
        mockMvc.perform(get("/admin/articles")
                        .session(session)
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(title))));

        mockMvc.perform(post("/admin/articles/edit/{id}", article.getId())
                        .session(session)
                        .with(csrf())
                        .param("title", title)
                        .param("summary", "Đã cập nhật")
                        .param("content", "Nội dung đã cập nhật.")
                        .param("categoryId", category.getId().toString())
                        .param("status", "DRAFT"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/admin/articles")
                        .session(session)
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(title)));
        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tổng quan")))
                .andExpect(content().string(containsString("Chào mừng trở lại")))
                .andExpect(content().string(containsString("Xem danh sách bài viết")))
                .andExpect(content().string(containsString("ngày")))
                .andExpect(content().string(containsString("Đã xuất bản")))
                .andExpect(content().string(containsString("Danh mục")))
                .andExpect(content().string(containsString("Người dùng")))
                .andExpect(content().string(containsString("Lượt xem")))
                .andExpect(content().string(containsString("Chưa có dữ liệu lượt xem thực tế")))
                .andExpect(content().string(containsString("Chưa ghi nhận lượt xem thực tế")))
                .andExpect(content().string(containsString("/admin/articles?status=DRAFT")))
                .andExpect(content().string(containsString("Bài viết theo thời gian")))
                .andExpect(content().string(containsString(
                        YearMonth.now().format(DateTimeFormatter.ofPattern("MM/yyyy", Locale.ROOT)))))
                .andExpect(content().string(containsString("Bài viết theo chuyên mục")))
                .andExpect(content().string(containsString("Bài viết mới nhất")))
                .andExpect(content().string(containsString("news-placeholder.svg")))
                .andExpect(content().string(containsString("Quản trị viên")))
                .andExpect(content().string(containsString("Hoạt động gần đây")))
                .andExpect(content().string(containsString(title)))
                .andExpect(content().string(containsString("Đã chỉnh sửa bài viết")))
                .andExpect(content().string(containsString("so với tháng trước")));

        mockMvc.perform(get("/admin/dashboard").session(session).param("range", "12"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("12 tháng gần nhất")));
        mockMvc.perform(get("/admin/dashboard").session(session).param("range", "year"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Từ đầu năm đến nay")));

        mockMvc.perform(post("/admin/articles/delete/{id}", article.getId())
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        org.junit.jupiter.api.Assertions.assertFalse(articleRepository.existsById(article.getId()));
        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đã xóa bài viết")));
    }

    private MockHttpSession loginAsAdmin() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin/login")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
