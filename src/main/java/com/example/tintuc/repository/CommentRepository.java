package com.example.tintuc.repository;

import com.example.tintuc.entity.Comment;
import com.example.tintuc.entity.CommentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CommentRepository extends JpaRepository<Comment, Long>, JpaSpecificationExecutor<Comment> {

    long countByStatus(CommentStatus status);
}
