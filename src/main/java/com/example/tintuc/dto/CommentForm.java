package com.example.tintuc.dto;

import com.example.tintuc.entity.CommentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentForm {

    private Long id;

    @NotBlank(message = "Nội dung bình luận không được để trống")
    @Size(max = 2000, message = "Bình luận tối đa 2000 ký tự")
    private String content;

    @NotNull(message = "Vui lòng chọn bài viết")
    private Long articleId;

    @NotNull(message = "Vui lòng chọn người bình luận")
    private Long userId;

    @NotNull(message = "Vui lòng chọn trạng thái")
    private CommentStatus status = CommentStatus.PENDING;
}
