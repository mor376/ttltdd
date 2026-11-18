package com.example.tintuc.repository;

import com.example.tintuc.entity.Article;
import com.example.tintuc.entity.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    long countByCategoryId(Long categoryId);

    long countByAuthorId(Long authorId);

    long countByStatus(ArticleStatus status);

    List<Article> findTop5ByOrderByCreatedAtDesc();

    Page<Article> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Article> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Article> findByTitleContainingIgnoreCaseAndCategoryId(
            String title, Long categoryId, Pageable pageable);
}