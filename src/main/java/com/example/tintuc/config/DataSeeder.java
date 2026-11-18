package com.example.tintuc.config;

import com.example.tintuc.entity.*;
import com.example.tintuc.repository.ArticleRepository;
import com.example.tintuc.repository.CategoryRepository;
import com.example.tintuc.repository.UserRepository;
import com.example.tintuc.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tao du lieu mau khi chay app (chi them khi chua co):
 *  - Tai khoan: admin / admin123 (ADMIN), nguoidung1..3 / 123456 (USER)
 *  - 6 chuyen muc
 *  - 12 bai viet (chi tao khi bang articles dang trong)
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        User admin = seedUsers();
        Map<String, Category> categories = seedCategories();
        seedArticles(admin, categories);
    }

    // ---------- Nguoi dung ----------
    private User seedUsers() {
        createUserIfMissing("admin", "admin@tintuc.local", "Quản trị viên", "admin123", Role.ADMIN);
        createUserIfMissing("nguoidung1", "nguoidung1@tintuc.local", "Nguyễn Văn An", "123456", Role.USER);
        createUserIfMissing("nguoidung2", "nguoidung2@tintuc.local", "Trần Thị Bình", "123456", Role.USER);
        createUserIfMissing("nguoidung3", "nguoidung3@tintuc.local", "Lê Minh Châu", "123456", Role.USER);
        return userRepository.findByUsername("admin").orElseThrow();
    }

    private void createUserIfMissing(String username, String email, String fullName, String rawPassword, Role role) {
        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) return;
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setFullName(fullName);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setRole(role);
        userRepository.save(u);
        System.out.println(">>> Da tao tai khoan " + username + " / " + rawPassword);
    }

    // ---------- Chuyen muc ----------
    private Map<String, Category> seedCategories() {
        List<String[]> data = List.of(
                new String[]{"Thời sự", "Tin tức trong nước và các vấn đề xã hội"},
                new String[]{"Thế giới", "Tin tức quốc tế nổi bật"},
                new String[]{"Thể thao", "Tin tức và hướng dẫn luyện tập thể thao"},
                new String[]{"Công nghệ", "Điện thoại, internet, trí tuệ nhân tạo"},
                new String[]{"Giải trí", "Phim ảnh, sách, âm nhạc"},
                new String[]{"Đời sống", "Mẹo vặt, sức khỏe, gia đình"}
        );
        Map<String, Category> map = new HashMap<>();
        for (String[] d : data) {
            Category c = categoryRepository.findAll().stream()
                    .filter(x -> x.getName().equalsIgnoreCase(d[0])).findFirst().orElse(null);
            if (c == null) {
                c = new Category();
                c.setName(d[0]);
                c.setDescription(d[1]);
                String slug = SlugUtil.toSlug(d[0]);
                c.setSlug(categoryRepository.existsBySlug(slug) ? slug + "-2" : slug);
                c = categoryRepository.save(c);
            }
            map.put(d[0], c);
        }
        return map;
    }

    // ---------- Bai viet ----------
    private void seedArticles(User author, Map<String, Category> categories) {
        if (articleRepository.count() > 0) return;

        String[][] data = {
                {"Công nghệ", "5 mẹo giúp tiết kiệm pin điện thoại hiệu quả",
                        "Một vài thay đổi nhỏ trong cài đặt có thể giúp điện thoại dùng được lâu hơn mỗi ngày."},
                {"Công nghệ", "Cách bảo vệ tài khoản trực tuyến an toàn hơn",
                        "Bật xác thực hai lớp, dùng mật khẩu riêng cho từng dịch vụ và cẩn trọng với đường link lạ."},
                {"Công nghệ", "Trí tuệ nhân tạo đang thay đổi cách chúng ta học tập",
                        "Các công cụ AI giúp người học tra cứu, luyện tập và nhận phản hồi nhanh hơn."},
                {"Thể thao", "Cách chọn giày chạy bộ phù hợp với bàn chân",
                        "Chọn đúng size và kiểu đệm giúp hạn chế chấn thương khi chạy."},
                {"Thể thao", "Lịch tập luyện 4 tuần dành cho người mới bắt đầu",
                        "Tăng dần cường độ và dành thời gian nghỉ ngơi hợp lý để cơ thể thích nghi."},
                {"Đời sống", "Thói quen buổi sáng giúp một ngày làm việc hiệu quả",
                        "Ngủ đủ giấc, uống nước và lên kế hoạch ngắn trước khi bắt đầu công việc."},
                {"Đời sống", "Mẹo giữ nhà cửa gọn gàng với chi phí thấp",
                        "Phân loại đồ đạc, dùng hộp đựng và dọn dẹp mỗi ngày vài phút."},
                {"Giải trí", "Gợi ý những bộ phim nhẹ nhàng cho cuối tuần",
                        "Danh sách thể loại phim phù hợp để thư giãn sau một tuần làm việc."},
                {"Giải trí", "Những cuốn sách nên đọc trong mùa mưa",
                        "Gợi ý các thể loại sách dễ đọc, phù hợp những ngày ở nhà."},
                {"Thời sự", "[Mẫu] Hướng dẫn thủ tục hành chính trực tuyến",
                        "Bài viết mẫu mô tả các bước thực hiện thủ tục qua cổng dịch vụ trực tuyến."},
                {"Thời sự", "[Mẫu] Cập nhật tình hình giao thông giờ cao điểm",
                        "Bài viết mẫu dùng để kiểm thử chức năng quản lý bài viết."},
                {"Thế giới", "[Mẫu] Điểm tin quốc tế nổi bật trong tuần",
                        "Bài viết mẫu tổng hợp các tin quốc tế, dùng để minh họa giao diện."}
        };

        for (int i = 0; i < data.length; i++) {
            Article a = new Article();
            a.setTitle(data[i][1]);
            a.setSummary(data[i][2]);
            a.setContent(data[i][2]
                    + "\n\nĐây là nội dung mẫu được tạo tự động để phục vụ việc kiểm thử và minh họa hệ thống quản trị tin tức."
                    + "\n\nBạn có thể chỉnh sửa hoặc xóa bài viết này trong trang quản trị, và bổ sung ảnh đại diện khi sửa bài.");
            a.setCategory(categories.get(data[i][0]));
            a.setAuthor(author);
            a.setStatus(i >= 10 ? ArticleStatus.DRAFT : ArticleStatus.PUBLISHED);
            a.setViewCount((long) (i + 1) * 17);
            articleRepository.save(a);
        }
        System.out.println(">>> Da tao " + data.length + " bai viet mau");
    }
}
