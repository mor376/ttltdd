package com.example.tintuc.dto;

import com.example.tintuc.entity.ArticleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticleForm {

    private Long id;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    @Size(max = 500, message = "Tóm tắt tối đa 500 ký tự")
    private String summary;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    @NotNull(message = "Vui lòng chọn chuyên mục")
    private Long categoryId;

    @NotNull(message = "Vui lòng chọn trạng thái")
    private ArticleStatus status = ArticleStatus.DRAFT;

    // Tên file ảnh hiện tại (dùng để hiển thị khi sửa)
    private String thumbnail;
}
