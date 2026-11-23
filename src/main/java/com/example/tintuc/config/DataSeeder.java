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

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tao du lieu mau khi chay app:
 *  - Tai khoan: admin / admin123 (ADMIN), nguoidung1..3 / 123456 (USER)
 *  - 6 chuyen muc
 *  - 20 bai viet mau duoc phan bo theo thang trong nam hien tai
 *  - 20 ban tin tham khao cap nhat ngay 24/11/2026
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final List<Integer> SAMPLE_MONTHS = List.of(
            0, 1, 1, 2, 3, 3, 4, 5, 5, 6, 7, 7, 7, 8, 9, 9, 10, 10, 11, 11);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        User admin = seedUsers();
        Map<String, Category> categories = seedCategories();
        seedArticles(admin, categories);
        seedCurrentNews(admin, categories);
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
        System.out.println(">>> Da tao tai khoan " + username);
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
        int created = 0;
        int updated = 0;
        int seedIndex = 0;
        for (ArticleSeed seed : articleSeeds()) {
            Article article = articleRepository.findByTitleIgnoreCase(seed.title()).orElse(null);
            if (article == null && seed.legacyTitle() != null) {
                article = articleRepository.findByTitleIgnoreCase(seed.legacyTitle()).orElse(null);
            }

            if (article == null) {
                article = new Article();
                applySeed(article, seed, author, categories);
                article = articleRepository.saveAndFlush(article);
                created++;
            } else if (seed.legacyTitle() != null
                    && article.getTitle().equalsIgnoreCase(seed.legacyTitle())) {
                applySeed(article, seed, author, categories);
                articleRepository.save(article);
                updated++;
            }
            articleRepository.updateSeededCreatedAt(article.getId(), sampleCreatedAt(seedIndex));
            seedIndex++;
        }
        System.out.println(">>> Du lieu bai viet mau: them " + created + ", cap nhat " + updated);
    }

    private LocalDateTime sampleCreatedAt(int seedIndex) {
        YearMonth currentMonth = YearMonth.now();
        int monthIndex = SAMPLE_MONTHS.get(seedIndex) * currentMonth.getMonthValue() / 12;
        YearMonth sampleMonth = YearMonth.of(currentMonth.getYear(), monthIndex + 1);
        int day = Math.min(5 + seedIndex % 20, sampleMonth.lengthOfMonth());
        return sampleMonth.atDay(day).atTime(9, seedIndex * 7 % 60);
    }

    private void seedCurrentNews(User author, Map<String, Category> categories) {
        int created = 0;
        DateTimeFormatter sourceDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (CurrentNewsSeed seed : currentNewsSeeds()) {
            Article article = articleRepository.findByTitleIgnoreCase(seed.title()).orElse(null);
            if (article == null) {
                article = new Article();
                article.setTitle(seed.title());
                article.setSummary(seed.summary());
                article.setContent(seed.content() + "\n\nNguồn tham khảo: VnExpress\n"
                        + seed.sourceUrl() + "\nNgày đăng nguồn: "
                        + seed.publishedAt().format(sourceDateFormat));
                article.setCategory(categories.get(seed.category()));
                article.setAuthor(author);
                article.setStatus(ArticleStatus.PUBLISHED);
                article.setViewCount(0L);
                article = articleRepository.saveAndFlush(article);
                created++;
            }
            articleRepository.updateSeededCreatedAt(article.getId(), seed.publishedAt());
        }
        System.out.println(">>> Tin tuc hien tai: them " + created);
    }

    private void applySeed(Article article, ArticleSeed seed, User author, Map<String, Category> categories) {
        article.setTitle(seed.title());
        article.setSummary(seed.summary());
        article.setContent(seed.content());
        article.setCategory(categories.get(seed.category()));
        article.setAuthor(author);
        article.setStatus(seed.status());
        article.setViewCount(seed.viewCount());
    }

    private List<CurrentNewsSeed> currentNewsSeeds() {
        return List.of(
                new CurrentNewsSeed("Thời sự", "Miền Trung được cảnh báo hai đợt mưa lớn cuối tháng 11",
                        "Dự báo mưa diện rộng từ ngày 24 đến 30/11, một số nơi có thể mưa trên 750 mm trong bảy ngày.",
                        "Cơ quan khí tượng dự báo không khí lạnh kết hợp các hình thế thời tiết khác sẽ gây hai đợt mưa tại miền Trung từ ngày 24 đến 30/11. Lượng mưa có thể rất lớn ở một số khu vực, làm tăng nguy cơ lũ trên sông, ngập vùng trũng, lũ quét và sạt lở đất.\n\n"
                                + "Người dân cần theo dõi bản tin dự báo tại địa phương, hạn chế đi qua khu vực ngập sâu và chuẩn bị phương án an toàn. Đây là dự báo theo thời điểm bài đăng; tình hình có thể được cập nhật.",
                        LocalDateTime.of(2026, 11, 23, 16, 51, 11),
                        "https://vnexpress.net/mien-trung-co-the-mua-tren-750-mm-trong-7-ngay-toi-5148111.html"),
                new CurrentNewsSeed("Thời sự", "TP HCM dành 370 tỷ đồng chăm lo Tết cho người lao động",
                        "Chương trình dự kiến hỗ trợ 370.000 công nhân, ưu tiên người khó khăn, mất việc hoặc giảm thu nhập.",
                        "Công đoàn TP HCM triển khai khoản hỗ trợ tiền mặt tổng cộng 370 tỷ đồng cho khoảng 370.000 người lao động, tương ứng một triệu đồng mỗi người. Đợt chuyển khoản đầu tiên được thực hiện tại lễ khởi động chăm lo Tết Đinh Mùi 2027 ngày 23/11.\n\n"
                                + "Kế hoạch còn có vé xe, tàu và máy bay cho công nhân xa quê, các phiên chợ Tết và hoạt động hỗ trợ người ở lại thành phố. Danh sách và tiến độ thực hiện do tổ chức công đoàn thông báo.",
                        LocalDateTime.of(2026, 11, 23, 19, 14, 4),
                        "https://vnexpress.net/tp-hcm-chuyen-khoan-370-ty-dong-qua-tet-den-cong-nhan-5148203.html"),
                new CurrentNewsSeed("Thời sự", "Đoạn trên cao Vành đai 3 TP HCM đặt mục tiêu hoàn thành cuối tháng 11",
                        "Hơn 17 km tuyến chính nối Tân Vạn với nút Long Trường đang hoàn thiện các hạng mục cuối.",
                        "Đoạn Vành đai 3 trên cao dài hơn 17 km, có bốn làn xe hai chiều và vận tốc thiết kế 100 km/h, dự kiến hoàn thành thi công tuyến chính trước ngày 30/11. Tuyến kết nối khu vực Tân Vạn với nút Long Trường và cao tốc TP HCM - Long Thành - Dầu Giây.\n\n"
                                + "Phần cầu cạn cơ bản hoàn thành; các đội thi công đang tập trung vào mặt đường, hệ thống an toàn và những nút giao. Mốc nêu trong bài là kế hoạch thi công, không đồng nghĩa tuyến đã mở cho xe lưu thông.",
                        LocalDateTime.of(2026, 11, 24, 0, 0),
                        "https://vnexpress.net/17-km-duong-tren-cao-dau-tien-o-tp-hcm-sap-hoan-thanh-5148208.html"),
                new CurrentNewsSeed("Thời sự", "TP HCM đặt mục tiêu vào nhóm 100 thành phố đáng sống năm 2030",
                        "Quy hoạch mới định hướng phát triển đô thị dựa trên khoa học công nghệ, kinh tế xanh và kết nối vùng.",
                        "Tại hội nghị công bố quy hoạch tổng thể thời kỳ 2025-2050, TP HCM đặt mục tiêu đến năm 2030 trở thành đô thị hiện đại, thuộc nhóm 100 thành phố đáng sống và có hệ sinh thái đổi mới sáng tạo nổi bật.\n\n"
                                + "Định hướng quy hoạch đề cập các động lực như khoa học công nghệ, kinh tế số, kinh tế xanh, kinh tế tuần hoàn và kinh tế biển. Đây là mục tiêu quy hoạch dài hạn, cần được cụ thể hóa qua các kế hoạch và dự án trong những năm tới.",
                        LocalDateTime.of(2026, 11, 23, 12, 32, 50),
                        "https://vnexpress.net/tp-hcm-dat-muc-tieu-vao-nhom-100-thanh-pho-dang-song-tren-the-gioi-5147930.html"),
                new CurrentNewsSeed("Công nghệ", "Hải Phòng khởi công nhà máy chế biến sâu vật liệu đất hiếm",
                        "Dự án tại Cát Hải có vốn 1.580 tỷ đồng, công suất thiết kế 10.000 tấn sản phẩm mỗi năm.",
                        "Nhà máy vật liệu tinh khiết ứng dụng trong công nghiệp công nghệ cao được khởi công tại khu công nghiệp Xuân Cầu, đặc khu Cát Hải. Dự án dự kiến có năm dây chuyền, tổng công suất thiết kế 10.000 tấn mỗi năm và hướng tới tạo ra các oxit đất hiếm có độ tinh khiết cao.\n\n"
                                + "Sản phẩm được định hướng phục vụ nam châm hiệu suất cao, điện tử, năng lượng và một số ngành công nghiệp khác. Khả năng vận hành thực tế còn phụ thuộc tiến độ xây dựng, công nghệ và yêu cầu môi trường.",
                        LocalDateTime.of(2026, 11, 23, 15, 10, 26),
                        "https://vnexpress.net/hai-phong-khoi-cong-nha-may-vat-lieu-tinh-khiet-tu-dat-hiem-5148076.html"),
                new CurrentNewsSeed("Đời sống", "Kinh tế bạc hướng tới nhu cầu của gần 16,5 triệu người cao tuổi",
                        "Việt Nam thảo luận phát triển dịch vụ, sản phẩm và cơ hội việc làm phù hợp với nhóm từ 60 tuổi trở lên.",
                        "Với gần 16,5 triệu người từ 60 tuổi trở lên, nhóm dân số cao tuổi đang tạo ra nhu cầu ngày càng đa dạng về chăm sóc sức khỏe, nhà ở, tài chính và dịch vụ đời sống. Khái niệm kinh tế bạc xem đây vừa là thị trường cần phục vụ, vừa là nguồn kinh nghiệm và năng lực lao động.\n\n"
                                + "Các chuyên gia lưu ý cần thiết kế dịch vụ dễ tiếp cận, bảo vệ người tiêu dùng và tạo điều kiện để người cao tuổi tham gia phù hợp. Quy mô dân số trong bài được dẫn theo số liệu được công bố tại thời điểm đưa tin.",
                        LocalDateTime.of(2026, 11, 23, 9, 0, 8),
                        "https://vnexpress.net/kinh-te-bac-co-hoi-tu-16-5-trieu-nguoi-cao-tuoi-5147144.html"),
                new CurrentNewsSeed("Đời sống", "Bệnh viện Tâm Anh tham gia thử nghiệm thuốc miễn dịch RBS2418",
                        "Nghiên cứu quốc tế pha 2a đánh giá thuốc uống thử nghiệm cho bệnh nhân ung thư gan; thuốc chưa phải điều trị đã được xác nhận.",
                        "Viện Nghiên cứu, Bệnh viện Đa khoa Tâm Anh tham gia thử nghiệm quốc tế VISTA-2 đối với thuốc miễn dịch đường uống RBS2418. Nghiên cứu pha 2a đánh giá thuốc ở bệnh nhân ung thư gan và được triển khai tại nhiều trung tâm ở một số quốc gia.\n\n"
                                + "Đây là thuốc đang được nghiên cứu, chưa thể xem là phương pháp điều trị đã được phê duyệt hay bảo đảm hiệu quả cho từng người bệnh. Người bệnh cần trao đổi với bác sĩ về chẩn đoán và lựa chọn điều trị.",
                        LocalDateTime.of(2026, 11, 23, 9, 0),
                        "https://vnexpress.net/viet-nam-cung-my-thu-nghiem-thuoc-moi-dieu-tri-ung-thu-gan-5147769.html"),
                new CurrentNewsSeed("Công nghệ", "Bộ trưởng khuyến nghị Việt Nam tập trung phát triển chip chuyên dụng",
                        "Định hướng được nêu tại Vietnam Semiconductor Summit 2026, gắn thiết kế chip với nhu cầu điện tử và IoT.",
                        "Tại Vietnam Semiconductor Summit 2026 ở TP HCM, Bộ trưởng Khoa học và Công nghệ Vũ Hải Quân cho rằng Việt Nam có thể ưu tiên chip chuyên dụng thay vì cạnh tranh trực tiếp ở chip phổ thông.\n\n"
                                + "Định hướng được nêu gồm thiết kế, đóng gói và kiểm thử tiên tiến, tận dụng nhu cầu từ ngành điện tử trong nước. Bài viết cũng đặt ra các vấn đề về nhân lực thiết kế, hạ tầng dùng chung và mức độ tham gia của doanh nghiệp Việt trong chuỗi cung ứng.",
                        LocalDateTime.of(2026, 11, 23, 13, 20, 13),
                        "https://vnexpress.net/viet-nam-nen-tap-trung-vao-chip-chuyen-dung-5147936.html"),
                new CurrentNewsSeed("Công nghệ", "TP HCM nghiên cứu mô hình đô thị khoa học và đổi mới sáng tạo",
                        "Tọa đàm ngày 23/11 thảo luận cách liên kết trường, viện nghiên cứu, doanh nghiệp và khu công nghệ.",
                        "TP HCM đang xem xét mô hình khu đô thị khoa học, công nghệ và đổi mới sáng tạo nhằm kết nối nghiên cứu, đào tạo, sản xuất thử nghiệm và thương mại hóa sản phẩm. Tọa đàm về đề tài này ghi nhận 50 bài tham luận.\n\n"
                                + "Các nội dung được đặt ra gồm cơ chế quản trị, nguồn lực, nhân lực chất lượng cao và cách đo lường kết quả đến năm 2030. Đây là quá trình xây dựng mô hình và chính sách, chưa phải thông báo khu đô thị đã hoàn thành.",
                        LocalDateTime.of(2026, 11, 23, 20, 44, 7),
                        "https://vnexpress.net/tp-hcm-phat-trien-mo-hinh-do-thi-gan-voi-khoa-hoc-cong-nghe-5148194.html"),
                new CurrentNewsSeed("Công nghệ", "30 nhà khoa học trẻ Việt Nam được chọn dự đại hội tại Nga",
                        "Danh sách do NAFOSTED công bố gồm sáu nhóm lĩnh vực và có thêm năm ứng viên dự bị.",
                        "Quỹ Phát triển khoa học và công nghệ quốc gia công bố 30 nhà khoa học trẻ được chọn tham dự Đại hội các nhà khoa học trẻ lần thứ VI tại Saint Petersburg từ ngày 2 đến 4/12/2026. Các lĩnh vực gồm vật liệu, công nghệ thông tin, khí hậu, y học, công nghệ vũ trụ và năng lượng.\n\n"
                                + "Ứng viên được đánh giá dựa trên chuyên môn, kết quả nghiên cứu, nội dung trình bày và khả năng trao đổi quốc tế. Danh sách còn có năm thành viên dự bị.",
                        LocalDateTime.of(2026, 11, 23, 21, 24, 49),
                        "https://vnexpress.net/30-nha-khoa-hoc-tre-viet-nam-duoc-chon-du-dai-hoi-tai-nga-5148222.html"),
                new CurrentNewsSeed("Thế giới", "Tổng thống Iran kêu gọi tiết kiệm điện trong bối cảnh thiếu nguồn cung",
                        "Ông Masoud Pezeshkian dùng hình ảnh văn phòng tắt đèn để truyền đi thông điệp sử dụng năng lượng tiết kiệm.",
                        "Tổng thống Iran Masoud Pezeshkian mời phóng viên ghi hình tại văn phòng chỉ sử dụng đèn bàn và không bật hệ thống sưởi. Ông nói hành động này nhằm khuyến khích người dân và cơ quan công quyền hạn chế lãng phí điện.\n\n"
                                + "Bài báo dẫn thông tin Iran áp dụng cắt điện luân phiên trong bối cảnh nguồn cung năng lượng chịu sức ép. Một quan chức Iran mô tả buổi ghi hình là thông điệp mang tính biểu tượng, không phản ánh điều kiện làm việc thường ngày của Tổng thống.",
                        LocalDateTime.of(2026, 11, 23, 9, 45, 6),
                        "https://vnexpress.net/tong-thong-iran-tat-den-van-phong-de-keu-goi-dan-tiet-kiem-dien-5147814.html"),
                new CurrentNewsSeed("Công nghệ", "Tàu hàng chạy pin BRF Froan chuẩn bị khai thác tuyến ven biển Na Uy",
                        "Con tàu dùng bộ pin 23,5 MWh và dự kiến vận chuyển hàng trên hành trình khoảng 426 km.",
                        "Tàu BRF Froan được thiết kế chạy hoàn toàn bằng điện, sử dụng bộ pin 23,5 MWh và hai cánh buồm rotor để hỗ trợ lực đẩy. Tàu dự kiến chở đá cẩm thạch dọc bờ biển Na Uy, giữa Brønnøy và Elnesvågen.\n\n"
                                + "Đơn vị khai thác kỳ vọng hệ thống điện, thiết bị bốc dỡ và hỗ trợ từ gió sẽ giảm phát thải trong vận chuyển. Các con số về mức giảm khí thải là ước tính của dự án, chưa phải kết quả vận hành dài hạn.",
                        LocalDateTime.of(2026, 11, 23, 19, 0),
                        "https://vnexpress.net/tau-cho-hang-chay-dien-lon-nhat-the-gioi-5148119.html"),
                new CurrentNewsSeed("Thế giới", "Sự cố hệ thống tự động làm chậm hơn 1.000 chuyến bay Delta",
                        "Delta cho biết hệ thống tính trọng lượng và cân bằng máy bay gặp trục trặc ngày 22/11 rồi được khôi phục.",
                        "Một lỗi kỹ thuật khiến hệ thống tính toán trọng lượng và cân bằng máy bay của Delta Air Lines ngừng hoạt động ngày 22/11. Hãng phải xử lý thủ công để bảo đảm an toàn, trong khi nhiều chuyến bay bị chậm; các nguồn tin đưa ra ước tính hơn 1.000 chuyến.\n\n"
                                + "Delta thông báo hệ thống đã hoạt động lại vào ngày hôm sau và hãng sắp xếp lại hành trình cho hành khách bị ảnh hưởng. Theo thông tin được bài báo dẫn lại, sự cố không được xác định là một cuộc tấn công mạng.",
                        LocalDateTime.of(2026, 11, 23, 6, 34, 29),
                        "https://vnexpress.net/su-co-tu-dong-hoa-khien-hon-1-000-chuyen-bay-cua-delta-bi-cham-5147770.html"),
                new CurrentNewsSeed("Công nghệ", "NASA xử lý đồng thời sự cố liên lạc của Perseverance và Mars Odyssey",
                        "Robot Perseverance gặp vấn đề với ăngten, còn tàu quỹ đạo Odyssey phải tạm dừng hoạt động khoa học để kiểm tra bánh đà.",
                        "NASA đang khắc phục hai trục trặc trên Sao Hỏa: robot Perseverance gặp vấn đề ở hệ thống liên lạc, còn tàu Mars Odyssey phát hiện lỗi ở một bánh đà phản lực dùng để giữ hướng.\n\n"
                                + "Perseverance chuyển sang phương án liên lạc dự phòng nhưng tốc độ truyền dữ liệu bị hạn chế. Odyssey tạm ngừng công việc khoa học trong khi đội vận hành đánh giá khả năng phục hồi thiết bị. NASA khi đó đặt mục tiêu đưa các phương tiện trở lại hoạt động vào cuối tháng 11.",
                        LocalDateTime.of(2026, 11, 23, 13, 8, 28),
                        "https://vnexpress.net/robot-va-tau-vu-tru-nasa-cung-gap-su-co-tren-sao-hoa-5147991.html"),
                new CurrentNewsSeed("Thế giới", "Tòa Hà Lan ban hành lệnh tạm thời với mẫu xe BYD Ti7 tại EU",
                        "Phán quyết sơ bộ liên quan tranh chấp quyền thiết kế giữa BYD và Jaguar Land Rover; BYD cho biết sẽ kháng cáo.",
                        "Tòa án tại The Hague ban hành biện pháp khẩn cấp tạm thời ngăn BYD bán mẫu SUV Ti7 tại Liên minh châu Âu trong vụ tranh chấp thiết kế với Jaguar Land Rover. Lệnh cũng ảnh hưởng đến kế hoạch sản xuất mẫu xe này tại một nhà máy ở Hungary.\n\n"
                                + "Vụ việc chưa được giải quyết cuối cùng. BYD phản đối phán quyết và nói sẽ kháng cáo; vì vậy diễn biến tiếp theo phụ thuộc quá trình tố tụng.",
                        LocalDateTime.of(2026, 11, 23, 9, 5, 35),
                        "https://vnexpress.net/byd-bi-cam-ban-xe-o-chau-au-vi-giong-land-rover-defender-5147784.html"),
                new CurrentNewsSeed("Thể thao", "U17 Việt Nam hòa New Zealand 3-3 tại World Cup",
                        "Đội tuyển ba lần gỡ hòa, giành điểm đầu tiên ở lượt trận thứ hai bảng G tại Qatar.",
                        "U17 Việt Nam hòa New Zealand 3-3 trong trận đấu ngày 23/11 tại bảng G U17 World Cup 2026. Đội bóng Việt Nam ba lần bị dẫn trước rồi gỡ hòa; bàn ấn định tỷ số của Duy Khang đến ở phút bù giờ.\n\n"
                                + "Kết quả mang về điểm số đầu tiên cho Việt Nam tại giải. Đây là diễn biến của riêng trận đấu này; thứ hạng và cơ hội đi tiếp còn phụ thuộc các lượt trận còn lại.",
                        LocalDateTime.of(2026, 11, 23, 21, 45, 41),
                        "https://vnexpress.net/truc-tiep-viet-nam-vs-new-zealand-5148242.html"),
                new CurrentNewsSeed("Thể thao", "Kình ngư 17 tuổi Nguyễn Thúy Hiền giành năm HCV và phá kỷ lục",
                        "Vận động viên Quân đội lập thành tích ở năm nội dung bơi trong hai ngày tại Đại hội Thể thao toàn quốc 2026.",
                        "Nguyễn Thúy Hiền giành bốn huy chương vàng cá nhân và một huy chương vàng tiếp sức sau hai ngày thi đấu môn bơi. Các thành tích ở 50 m tự do, 100 m ếch, tiếp sức 4x200 m tự do, 50 m ếch và 100 m tự do đều đi kèm kỷ lục mới theo bài tường thuật.\n\n"
                                + "Vận động viên sinh năm 2009 thi đấu cho đoàn Quân đội tại Đại hội Thể thao toàn quốc 2026 ở TP HCM.",
                        LocalDateTime.of(2026, 11, 23, 22, 46, 54),
                        "https://vnexpress.net/kinh-ngu-17-tuoi-pha-5-ky-luc-dai-hoi-the-thao-toan-quoc-5148238.html"),
                new CurrentNewsSeed("Thể thao", "Marc Marquez đăng quang MotoGP 2026 và cân bằng kỷ lục tám danh hiệu",
                        "Tay đua Ducati về thứ tám tại chặng Bồ Đào Nha, đủ điểm để vô địch trước một chặng.",
                        "Marc Marquez cán đích thứ tám tại Grand Prix Bồ Đào Nha ngày 22/11, kết quả giúp anh giành chức vô địch MotoGP 2026 sớm một vòng. Tay đua người Tây Ban Nha đạt danh hiệu thứ tám ở hạng đua cao nhất, ngang kỷ lục của Giacomo Agostini.\n\n"
                                + "Marquez chọn giữ vị trí an toàn trong những vòng cuối để bảo đảm điểm số. Danh hiệu này khép lại mùa giải nhiều biến động sau giai đoạn anh phải nghỉ thi đấu vì chấn thương.",
                        LocalDateTime.of(2026, 11, 23, 11, 35, 25),
                        "https://vnexpress.net/marc-marquez-can-bang-ky-luc-vo-dich-motogp-5147964.html"),
                new CurrentNewsSeed("Thể thao", "Diego Simeone lập kỷ lục 345 trận thắng tại La Liga",
                        "Atletico Madrid thắng Getafe 3-0, giúp HLV người Argentina vượt thành tích của Luis Aragonés.",
                        "Chiến thắng 3-0 trước Getafe ngày 22/11 đưa số trận thắng La Liga của Diego Simeone lên 345 sau 567 trận dẫn dắt. Thành tích này vượt một chiến thắng so với kỷ lục cũ của Luis Aragonés.\n\n"
                                + "Toàn bộ số trận thắng của Simeone tại giải đấu đến cùng Atletico Madrid. Ông dẫn dắt câu lạc bộ từ năm 2011 và từng cùng đội hai lần vô địch La Liga.",
                        LocalDateTime.of(2026, 11, 23, 9, 42, 42),
                        "https://vnexpress.net/simeone-pha-ky-luc-thang-ton-tai-22-nam-o-la-liga-5147855.html"),
                new CurrentNewsSeed("Giải trí", "Thư Kỳ tri ân đạo diễn Hầu Hiếu Hiền tại lễ trao giải Kim Mã",
                        "Nữ diễn viên xúc động khi nhận giải Đạo diễn mới xuất sắc và nhắc đến người đã dìu dắt sự nghiệp của cô.",
                        "Tại lễ trao giải Kim Mã tối 22/11, Thư Kỳ nghẹn ngào khi nhận danh hiệu Đạo diễn mới xuất sắc cho phim đầu tay Cô gái. Cô dành lời tri ân cho đạo diễn Hầu Hiếu Hiền, người đã khuyến khích cô thử sức với những vai diễn vượt khỏi giới hạn quen thuộc.\n\n"
                                + "Bộ phim lấy cảm hứng từ trải nghiệm thời thơ ấu của nữ nghệ sĩ. Bài viết cũng nhìn lại quá trình cô chuyển từ diễn viên sang đạo diễn sau hơn ba thập kỷ hoạt động.",
                        LocalDateTime.of(2026, 11, 23, 9, 26, 16),
                        "https://vnexpress.net/thu-ky-khoc-khi-noi-ve-nguoi-thay-doi-cuoc-doi-co-5147806.html")
        );
    }

    private List<ArticleSeed> articleSeeds() {
        return List.of(
                new ArticleSeed("Công nghệ", "5 mẹo giúp tiết kiệm pin điện thoại hiệu quả",
                        "Điều chỉnh màn hình, kết nối và ứng dụng nền để kéo dài thời lượng pin trong ngày.",
                        "Màn hình thường là một trong những thành phần tiêu thụ nhiều năng lượng nhất. Hãy giảm độ sáng, bật tự động điều chỉnh và đặt thời gian tắt màn hình ngắn hơn.\n\n"
                                + "Tắt Wi-Fi, Bluetooth hoặc định vị khi không dùng; kiểm tra ứng dụng chạy nền và bật chế độ tiết kiệm pin khi cần. Hiệu quả thực tế tùy thuộc thiết bị và cách sử dụng.",
                        ArticleStatus.PUBLISHED, 420, null),
                new ArticleSeed("Công nghệ", "Cách bảo vệ tài khoản trực tuyến an toàn hơn",
                        "Dùng mật khẩu riêng, bật xác thực nhiều lớp và kiểm tra kỹ đường dẫn trước khi đăng nhập.",
                        "Mỗi dịch vụ nên có một mật khẩu dài và riêng biệt. Trình quản lý mật khẩu giúp tạo và lưu mật khẩu khó đoán mà không phải ghi nhớ tất cả.\n\n"
                                + "Bật xác thực hai lớp bằng ứng dụng hoặc khóa bảo mật nếu có thể. Khi nhận yêu cầu đăng nhập bất ngờ, hãy tự mở trang chính thức thay vì bấm vào liên kết trong tin nhắn.",
                        ArticleStatus.PUBLISHED, 385, null),
                new ArticleSeed("Công nghệ", "Trí tuệ nhân tạo đang thay đổi cách chúng ta học tập",
                        "Công cụ AI có thể hỗ trợ tra cứu và luyện tập, nhưng cần được kiểm chứng và sử dụng có trách nhiệm.",
                        "Người học có thể dùng công cụ AI để gợi ý câu hỏi ôn tập, giải thích khái niệm hoặc xây dựng kế hoạch học. Cách dùng hiệu quả là yêu cầu công cụ trình bày từng bước rồi tự đối chiếu với tài liệu đáng tin cậy.\n\n"
                                + "Nội dung do AI tạo có thể thiếu chính xác hoặc thiên lệch. Không nên nhập dữ liệu cá nhân, thông tin trường lớp hay tài liệu riêng tư vào dịch vụ chưa được cho phép.",
                        ArticleStatus.PUBLISHED, 310, null),
                new ArticleSeed("Thể thao", "Cách chọn giày chạy bộ phù hợp với bàn chân",
                        "Thử giày vào cuối ngày, kiểm tra độ vừa ở mũi và chọn theo địa hình thường chạy.",
                        "Khi thử giày, nên mang loại tất thường dùng và chừa một khoảng nhỏ ở mũi để các ngón chân cử động. Gót cần được giữ chắc nhưng không gây cọ xát.\n\n"
                                + "Địa hình, quãng đường và cảm giác cá nhân đều ảnh hưởng đến lựa chọn. Hãy đi thử vài phút; nếu có đau kéo dài, nên hỏi chuyên gia y tế hoặc chuyên viên phù hợp.",
                        ArticleStatus.PUBLISHED, 278, null),
                new ArticleSeed("Thể thao", "Lịch tập luyện 4 tuần dành cho người mới bắt đầu",
                        "Bắt đầu bằng các buổi ngắn, tăng tải từ từ và dành ngày nghỉ để cơ thể phục hồi.",
                        "Tuần đầu có thể xen kẽ đi bộ nhanh và chạy nhẹ trong 20 phút. Mỗi tuần chỉ nên tăng thời lượng hoặc cường độ một cách vừa phải, đồng thời giữ ít nhất một ngày nghỉ.\n\n"
                                + "Khởi động trước buổi tập, thả lỏng sau khi kết thúc và dừng lại nếu xuất hiện đau bất thường. Lịch tập cần được điều chỉnh theo thể trạng, không phải mục tiêu bắt buộc cho mọi người.",
                        ArticleStatus.PUBLISHED, 246, null),
                new ArticleSeed("Đời sống", "Thói quen buổi sáng giúp một ngày làm việc hiệu quả",
                        "Chuẩn bị việc cần làm, uống đủ nước và dành vài phút khởi động ngày mới.",
                        "Một danh sách ngắn gồm ba việc ưu tiên giúp ngày làm việc có trọng tâm hơn. Có thể chuẩn bị quần áo, bữa sáng hoặc vật dụng cần thiết từ tối hôm trước để giảm việc phải quyết định vào buổi sáng.\n\n"
                                + "Không có một khung giờ phù hợp cho tất cả mọi người. Hãy chọn nhịp sinh hoạt có thể duy trì lâu dài và ưu tiên ngủ đủ giấc.",
                        ArticleStatus.PUBLISHED, 231, null),
                new ArticleSeed("Đời sống", "Mẹo giữ nhà cửa gọn gàng với chi phí thấp",
                        "Chia việc dọn dẹp thành từng khu vực nhỏ và tận dụng đồ dùng sẵn có.",
                        "Bắt đầu từ một ngăn kéo hoặc một mặt bàn thay vì dọn cả căn nhà cùng lúc. Phân loại đồ thường dùng, đồ cần cất và đồ không còn cần thiết sẽ giúp việc sắp xếp đơn giản hơn.\n\n"
                                + "Hộp giấy, giỏ có sẵn hoặc nhãn ghi chú có thể hỗ trợ phân loại mà không cần mua nhiều vật dụng mới. Duy trì vài phút dọn dẹp mỗi ngày thường dễ thực hiện hơn một buổi tổng vệ sinh kéo dài.",
                        ArticleStatus.PUBLISHED, 194, null),
                new ArticleSeed("Giải trí", "Gợi ý những bộ phim nhẹ nhàng cho cuối tuần",
                        "Chọn phim theo thời lượng, tâm trạng và sở thích để có một buổi xem thoải mái.",
                        "Một buổi xem phim thư giãn không nhất thiết cần danh sách dài. Phim hài gia đình, hoạt hình hoặc những câu chuyện đời thường thường phù hợp khi muốn giải trí nhẹ nhàng.\n\n"
                                + "Bạn có thể xem trước thời lượng và phân loại độ tuổi trên nền tảng phát hành, rồi chọn nội dung phù hợp với mọi người cùng xem.",
                        ArticleStatus.PUBLISHED, 183, null),
                new ArticleSeed("Giải trí", "Những cuốn sách nên đọc trong mùa mưa",
                        "Từ truyện ngắn đến sách kỹ năng, hãy chọn thể loại phù hợp với thời gian đọc mỗi ngày.",
                        "Nếu chỉ có ít thời gian, tuyển tập truyện ngắn hoặc tản văn giúp người đọc dễ bắt đầu và tạm dừng. Sách dài có thể phù hợp cho những khoảng thời gian yên tĩnh hơn.\n\n"
                                + "Thư viện địa phương và sách điện tử là những lựa chọn tiết kiệm không gian. Hãy chọn chủ đề khiến bạn tò mò thay vì cố đọc theo một danh sách bắt buộc.",
                        ArticleStatus.PUBLISHED, 165, null),
                new ArticleSeed("Thời sự", "Hướng dẫn thủ tục hành chính trực tuyến từ A đến Z",
                        "Chuẩn bị giấy tờ, kiểm tra cổng dịch vụ công chính thức và lưu lại mã hồ sơ.",
                        "Trước khi nộp hồ sơ, hãy đọc kỹ thành phần giấy tờ, điều kiện và thời hạn trên cổng dịch vụ công của cơ quan có thẩm quyền. Chuẩn bị bản điện tử rõ nét và kiểm tra thông tin cá nhân trước khi gửi.\n\n"
                                + "Sau khi nộp, lưu mã tiếp nhận để tra cứu tình trạng xử lý. Nếu cần bổ sung hồ sơ hoặc gặp lỗi, liên hệ kênh hỗ trợ chính thức được công bố trên cổng dịch vụ.",
                        ArticleStatus.PUBLISHED, 152, "[Mẫu] Hướng dẫn thủ tục hành chính trực tuyến"),
                new ArticleSeed("Thời sự", "Bản đồ số hỗ trợ cập nhật tình hình giao thông đô thị",
                        "Ứng dụng bản đồ có thể gợi ý tuyến đường, nhưng người tham gia giao thông vẫn cần tuân thủ biển báo.",
                        "Dữ liệu bản đồ và báo cáo cộng đồng giúp người đi đường tham khảo tình hình trên các tuyến phố. Thông tin có thể thay đổi nhanh, vì vậy hãy kiểm tra lại trước khi khởi hành.\n\n"
                                + "Không thao tác điện thoại khi đang điều khiển phương tiện. Ưu tiên hướng dẫn của lực lượng chức năng và biển báo tại hiện trường.",
                        ArticleStatus.PUBLISHED, 139, "[Mẫu] Cập nhật tình hình giao thông giờ cao điểm"),
                new ArticleSeed("Thế giới", "Những góc nhìn cần theo dõi khi đọc tin quốc tế",
                        "Đối chiếu nhiều nguồn, phân biệt tin tức với bình luận và lưu ý thời điểm cập nhật.",
                        "Một sự kiện quốc tế thường được đưa tin từ nhiều góc độ. Người đọc nên kiểm tra thời điểm đăng, nguồn trích dẫn và phân biệt dữ kiện đã xác nhận với nhận định của tác giả.\n\n"
                                + "Khi thông tin còn đang phát triển, các chi tiết ban đầu có thể được cập nhật. Hãy tìm thông báo từ cơ quan liên quan và so sánh với các nguồn báo chí độc lập.",
                        ArticleStatus.PUBLISHED, 127, "[Mẫu] Điểm tin quốc tế nổi bật trong tuần"),
                new ArticleSeed("Công nghệ", "Cách nhận biết đường dẫn giả mạo trước khi đăng nhập",
                        "Kiểm tra tên miền, tránh liên kết lạ và không cung cấp mã xác thực qua tin nhắn.",
                        "Kẻ gian có thể tạo trang đăng nhập gần giống dịch vụ thật. Hãy kiểm tra kỹ tên miền, dấu câu và chứng thư bảo mật; an toàn hơn là tự nhập địa chỉ trang chính thức.\n\n"
                                + "Ngân hàng và nhà cung cấp uy tín không yêu cầu gửi mật khẩu hoặc mã xác thực dùng một lần qua cuộc gọi hay tin nhắn. Nếu đã nhập thông tin vào trang đáng ngờ, hãy đổi mật khẩu và liên hệ nhà cung cấp.",
                        ArticleStatus.PUBLISHED, 118, null),
                new ArticleSeed("Thể thao", "Khởi động đúng cách trước buổi chạy bộ",
                        "Tăng dần nhịp vận động và chọn bài khởi động phù hợp với buổi tập.",
                        "Bắt đầu bằng vài phút đi bộ nhanh, sau đó thực hiện các động tác vận động linh hoạt cho khớp hông, cổ chân và vai. Khởi động nên tăng dần nhịp tim thay vì kéo giãn mạnh khi cơ thể còn lạnh.\n\n"
                                + "Điều chỉnh bài tập theo quãng đường và thể trạng. Nếu có bệnh nền hoặc chấn thương, nên hỏi chuyên gia y tế trước khi thay đổi chương trình vận động.",
                        ArticleStatus.PUBLISHED, 104, null),
                new ArticleSeed("Đời sống", "Đọc nhãn dinh dưỡng để chọn thực phẩm phù hợp",
                        "Quan sát khẩu phần, thành phần và lượng đường, muối trước khi so sánh sản phẩm.",
                        "Thông tin dinh dưỡng thường được tính theo một khẩu phần xác định, có thể nhỏ hơn lượng người dùng thực tế. Hãy xem cả kích thước khẩu phần và số khẩu phần trong một gói.\n\n"
                                + "Danh sách thành phần được sắp theo tỷ lệ từ cao xuống thấp. Người có dị ứng hoặc chế độ ăn đặc biệt cần kiểm tra cảnh báo và tham khảo tư vấn chuyên môn khi cần.",
                        ArticleStatus.PUBLISHED, 96, null),
                new ArticleSeed("Giải trí", "Podcast và sách nói mở rộng thói quen tiếp nhận nội dung",
                        "Nội dung âm thanh phù hợp khi di chuyển hoặc làm việc nhẹ, nhưng cần chọn thời điểm an toàn.",
                        "Podcast và sách nói giúp người nghe tiếp cận câu chuyện, kiến thức hoặc phỏng vấn mà không cần nhìn màn hình. Nhiều ứng dụng cho phép tải trước nội dung để nghe khi không có kết nối.\n\n"
                                + "Hãy điều chỉnh âm lượng vừa phải và không đeo tai nghe khi cần nhận biết môi trường xung quanh, đặc biệt khi tham gia giao thông.",
                        ArticleStatus.PUBLISHED, 88, null),
                new ArticleSeed("Thời sự", "Các bước chuẩn bị hồ sơ trước khi làm thủ tục trực tuyến",
                        "Bản nháp tổng hợp cách kiểm tra yêu cầu hồ sơ và chuẩn bị tài liệu số trước khi nộp.",
                        "Trước khi bắt đầu, xác định đúng thủ tục và cơ quan tiếp nhận trên cổng thông tin chính thức. Đọc kỹ định dạng, dung lượng tệp và yêu cầu về bản sao điện tử.\n\n"
                                + "Đây là bản nháp cần biên tập, bổ sung liên kết nguồn và xác minh quy định hiện hành trước khi xuất bản.",
                        ArticleStatus.DRAFT, 72, null),
                new ArticleSeed("Thời sự", "Kỹ năng kiểm chứng thông tin từ các nguồn tin chính thống",
                        "Bản nháp về cách đối chiếu thời gian, nguồn dẫn và thông báo của cơ quan liên quan.",
                        "Khi gặp thông tin lan truyền nhanh, hãy tìm bài đăng gốc, kiểm tra ngày tháng và đối chiếu với thông báo từ đơn vị có trách nhiệm. Ảnh hoặc video cũ đôi khi bị chia sẻ lại trong ngữ cảnh khác.\n\n"
                                + "Cần bổ sung ví dụ và nguồn tham khảo đáng tin cậy trước khi xuất bản bài viết này.",
                        ArticleStatus.DRAFT, 51, null),
                new ArticleSeed("Thế giới", "Vai trò của các tổ chức quốc tế trong hoạt động cứu trợ",
                        "Bản nháp giới thiệu cách phối hợp cứu trợ và các thách thức về hậu cần.",
                        "Hoạt động cứu trợ quốc tế thường cần phối hợp giữa cơ quan địa phương, tổ chức nhân đạo và cộng đồng. Việc đánh giá nhu cầu tại chỗ giúp ưu tiên nguồn lực và hạn chế trùng lặp.\n\n"
                                + "Bài viết cần dẫn nguồn từ các tổ chức chính thức và cập nhật thông tin trước khi xuất bản.",
                        ArticleStatus.DRAFT, 39, null),
                new ArticleSeed("Thế giới", "Những yếu tố cần theo dõi khi đọc tin kinh tế quốc tế",
                        "Bản nháp gợi ý cách đọc số liệu kinh tế trong bối cảnh và tránh kết luận từ một chỉ số đơn lẻ.",
                        "Các chỉ số kinh tế cần được đặt trong bối cảnh thời gian, phương pháp thống kê và nguồn công bố. Một con số riêng lẻ không thể phản ánh đầy đủ tình hình của một quốc gia hay khu vực.\n\n"
                                + "Cần bổ sung số liệu có nguồn dẫn và giải thích thuật ngữ trước khi hoàn thiện bài.",
                        ArticleStatus.DRAFT, 24, null)
        );
    }

    private record ArticleSeed(String category, String title, String summary, String content,
                               ArticleStatus status, long viewCount, String legacyTitle) {
    }

    private record CurrentNewsSeed(String category, String title, String summary, String content,
                                   LocalDateTime publishedAt, String sourceUrl) {
    }
}
