package com.example.tintuc.repository;

import com.example.tintuc.entity.Article;
import com.example.tintuc.entity.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {

    long countByCategoryId(Long categoryId);

    long countByAuthorId(Long authorId);

    long countByStatus(ArticleStatus status);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime to);

    long countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            ArticleStatus status, LocalDateTime from, LocalDateTime to);

    List<Article> findTop5ByOrderByCreatedAtDesc();

    Optional<Article> findByTitleIgnoreCase(String title);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE articles SET created_at = :createdAt WHERE id = :articleId", nativeQuery = true)
    int updateSeededCreatedAt(@Param("articleId") Long articleId,
                              @Param("createdAt") LocalDateTime createdAt);

    Page<Article> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Article> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Article> findByTitleContainingIgnoreCaseAndCategoryId(
            String title, Long categoryId, Pageable pageable);
}
