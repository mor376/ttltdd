package com.example.tintuc.controller.admin;

import com.example.tintuc.entity.CommentStatus;
import com.example.tintuc.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class AdminLayoutAdvice {

    private final CommentRepository commentRepository;

    @ModelAttribute("pendingCommentCount")
    public long pendingCommentCount() {
        return commentRepository.countByStatus(CommentStatus.PENDING);
    }
}
