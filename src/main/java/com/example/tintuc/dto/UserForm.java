package com.example.tintuc.dto;

import com.example.tintuc.entity.Role;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserForm {

    private Long id;

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập từ 3 đến 50 ký tự")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    // Bắt buộc khi thêm, để trống khi sửa nếu không muốn đổi (kiểm tra trong controller)
    private String password;

    @NotNull(message = "Vui lòng chọn vai trò")
    private Role role = Role.USER;
}
