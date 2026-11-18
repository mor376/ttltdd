# Ứng dụng Tin tức – Trang quản trị (Kiểm tra 1)

Môn: Thực tập lập trình di động
Đề tài: Ứng dụng đọc tin tức trên điện thoại và hệ thống quản trị nội dung.

Hệ thống gồm 3 phần: **trang quản trị (web)**, **backend API (JSON)**, **ứng dụng di động (React Native)**.
Phiên bản này hoàn thành phần **trang quản trị** (Buổi 1–3).

## Công nghệ

- Java 17+, Spring Boot 3.3 (Spring MVC, Spring Data JPA, Spring Security, Validation)
- Thymeleaf (giao diện admin)
- MySQL / MariaDB
- Maven

## Yêu cầu cài đặt

- JDK 17 trở lên
- Maven 3.8+ (hoặc dùng Maven của VS Code)
- MySQL (hoặc XAMPP)

## Cài đặt và chạy

1. Tạo database:

   ```sql
   CREATE DATABASE IF NOT EXISTS dbtintuc CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. Mở `src/main/resources/application.properties`, sửa `spring.datasource.username` và
   `spring.datasource.password` cho đúng với MySQL trên máy.

3. Chạy ứng dụng:

   ```bash
   mvn spring-boot:run
   ```

4. Mở trình duyệt: <http://localhost:8080/admin/login>

Lần chạy đầu tiên, ứng dụng tự tạo bảng và dữ liệu mẫu (tài khoản, 6 chuyên mục, 12 bài viết).

## Tài khoản mặc định

| Vai trò | Tên đăng nhập | Mật khẩu |
|---|---|---|
| ADMIN (vào được trang quản trị) | `admin` | `admin123` |
| USER | `nguoidung1`, `nguoidung2`, `nguoidung3` | `123456` |

Chỉ tài khoản ADMIN đăng nhập được vào trang quản trị.

## Các trang quản trị

| Chức năng | Đường dẫn |
|---|---|
| Đăng nhập | `/admin/login` |
| Dashboard | `/admin/dashboard` |
| Chuyên mục | `/admin/categories` (thêm: `/create`, sửa: `/edit/{id}`) |
| Người dùng | `/admin/users` (thêm: `/create`, sửa: `/edit/{id}`) |
| Bài viết | `/admin/articles` (thêm: `/create`, sửa: `/edit/{id}`) |

Xóa được thực hiện bằng phương thức POST tại `/delete/{id}` của từng mục.

## Chức năng chính

- Đăng nhập, đăng xuất, phân quyền (Spring Security, mật khẩu BCrypt)
- CRUD chuyên mục, người dùng, bài viết
- Tìm kiếm, lọc theo chuyên mục, phân trang
- Upload ảnh đại diện bài viết (thư mục `uploads/`)
- Kiểm tra dữ liệu nhập (validate) và thông báo lỗi/thành công

## Cấu trúc thư mục

```
src/main/java/com/example/tintuc/
├── config/        # SecurityConfig, WebConfig, DataSeeder
├── controller/admin/
├── dto/
├── entity/
├── repository/
├── security/
├── service/
└── util/
src/main/resources/
├── templates/admin/   # giao diện Thymeleaf
└── static/css/
```

## Ghi chú

- Ảnh upload lưu trong thư mục `uploads/` (không đưa lên Git).
- Các phần backend API, ứng dụng React Native và Docker sẽ bổ sung ở các giai đoạn sau.
