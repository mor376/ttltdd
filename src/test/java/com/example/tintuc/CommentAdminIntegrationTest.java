package com.example.tintuc;

import com.example.tintuc.entity.Article;
import com.example.tintuc.entity.Comment;
import com.example.tintuc.entity.CommentStatus;
import com.example.tintuc.entity.User;
import com.example.tintuc.repository.ArticleRepository;
import com.example.tintuc.repository.CommentRepository;
import com.example.tintuc.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommentAdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void adminCanCreateFilterUpdateAndDeleteComments() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Article article = articleRepository.findAll().get(0);
        User user = userRepository.findByUsername("nguoidung1").orElseThrow();
        String content = "Bình luận kiểm thử " + UUID.randomUUID();

        mockMvc.perform(post("/admin/comments/create")
                        .session(session)
                        .with(csrf())
                        .param("content", content)
                        .param("articleId", article.getId().toString())
                        .param("userId", user.getId().toString())
                        .param("status", "PENDING"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/comments"));

        Comment comment = commentRepository.findAll().stream()
                .filter(item -> item.getContent().equals(content))
                .findFirst()
                .orElseThrow();
        mockMvc.perform(get("/admin/comments")
                        .session(session)
                        .param("status", "PENDING")
                        .param("keyword", content))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(content)));

        mockMvc.perform(post("/admin/comments/edit/{id}", comment.getId())
                        .session(session)
                        .with(csrf())
                        .param("content", content + " đã duyệt")
                        .param("articleId", article.getId().toString())
                        .param("userId", user.getId().toString())
                        .param("status", "APPROVED"))
                .andExpect(status().is3xxRedirection());

        Comment updated = commentRepository.findById(comment.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(CommentStatus.APPROVED, updated.getStatus());

        mockMvc.perform(post("/admin/comments/delete/{id}", comment.getId())
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertFalse(commentRepository.existsById(comment.getId()));
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
