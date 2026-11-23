package com.example.tintuc.controller.admin;

import com.example.tintuc.dto.BarPoint;
import com.example.tintuc.dto.CategorySlice;
import com.example.tintuc.dto.ChartGridLine;
import com.example.tintuc.dto.ChartItem;
import com.example.tintuc.dto.DashboardStat;
import com.example.tintuc.entity.ArticleStatus;
import com.example.tintuc.entity.Category;
import com.example.tintuc.entity.User;
import com.example.tintuc.repository.ActivityLogRepository;
import com.example.tintuc.repository.ArticleRepository;
import com.example.tintuc.repository.CategoryRepository;
import com.example.tintuc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;
    private final ActivityLogRepository activityLogRepository;

    @GetMapping("/admin/dashboard")
    public String dashboard(@RequestParam(defaultValue = "6") String range,
                            Principal principal,
                            Model model) {
        String selectedRange = "12".equals(range) ? "12" : ("year".equals(range) ? "year" : "6");
        int months = monthCountFor(selectedRange);
        List<ChartItem> monthlyCounts = articlesByMonth(months, null);
        List<ChartItem> sixMonthTotals = articlesByMonth(6, null);
        List<ChartItem> sixMonthPublished = articlesByMonth(6, ArticleStatus.PUBLISHED);
        List<ChartItem> sixMonthDrafts = articlesByMonth(6, ArticleStatus.DRAFT);
        List<CategorySlice> categories = articlesByCategory();
        long articleCount = articleRepository.count();
        long publishedCount = articleRepository.countByStatus(ArticleStatus.PUBLISHED);
        long draftCount = articleRepository.countByStatus(ArticleStatus.DRAFT);
        long categoryCount = categoryRepository.count();
        long userCount = userRepository.count();
        String greetingName = userRepository.findByUsername(principal.getName())
                .map(User::getFullName)
                .filter(name -> !name.isBlank())
                .orElse("Admin");

        model.addAttribute("greetingName", greetingName);
        model.addAttribute("todayLabel", LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, 'ngày' dd 'tháng' M, yyyy", Locale.forLanguageTag("vi-VN"))));
        model.addAttribute("statCards", List.of(
                new DashboardStat("articles", "Tổng bài viết", Long.toString(articleCount),
                        "/admin/articles", "blue",
                        sparklineForItems(sixMonthTotals), "Bài mới trong 6 tháng",
                        monthComparison(sixMonthTotals)),
                new DashboardStat("published", "Đã xuất bản", Long.toString(publishedCount),
                        "/admin/articles?status=PUBLISHED", "green", sparklineForItems(sixMonthPublished),
                        "Bài xuất bản trong 6 tháng", monthComparison(sixMonthPublished)),
                new DashboardStat("drafts", "Bản nháp", Long.toString(draftCount),
                        "/admin/articles?status=DRAFT", "orange",
                        sparklineForItems(sixMonthDrafts), "Bản nháp trong 6 tháng", monthComparison(sixMonthDrafts)),
                new DashboardStat("categories", "Danh mục", Long.toString(categoryCount),
                        "/admin/categories", "violet",
                        sparkline(categories.stream().map(CategorySlice::getValue).toList()),
                        "Phân bổ bài theo danh mục", null),
                new DashboardStat("users", "Người dùng", Long.toString(userCount), "/admin/users", "teal",
                        null, "Tài khoản trong hệ thống", null),
                new DashboardStat("views", "Lượt xem", "—", "/admin/articles", "slate",
                        null, "Chưa có dữ liệu lượt xem thực tế", null)));
        model.addAttribute("selectedRange", selectedRange);
        model.addAttribute("rangeLabel", rangeLabel(selectedRange));
        model.addAttribute("monthPoints", barPoints(monthlyCounts));
        model.addAttribute("gridLines", gridLines(monthlyCounts));
        model.addAttribute("categorySlices", categories);
        model.addAttribute("categoryTotal", categories.stream().mapToLong(CategorySlice::getValue).sum());
        model.addAttribute("latestArticles", articleRepository.findTop5ByOrderByCreatedAtDesc());
        model.addAttribute("recentActivities", activityLogRepository.findTop8ByOrderByCreatedAtDesc());
        return "admin/dashboard";
    }

    private int monthCountFor(String range) {
        if ("12".equals(range)) return 12;
        if ("year".equals(range)) return YearMonth.now().getMonthValue();
        return 6;
    }

    private String rangeLabel(String range) {
        if ("12".equals(range)) return "12 tháng gần nhất";
        if ("year".equals(range)) return "Từ đầu năm đến nay";
        return "6 tháng gần nhất";
    }

    private List<ChartItem> articlesByMonth(int months, ArticleStatus status) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yyyy");
        YearMonth now = YearMonth.now();
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            labels.add(ym.format(fmt));
            LocalDateTime from = ym.atDay(1).atStartOfDay();
            LocalDateTime to = ym.plusMonths(1).atDay(1).atStartOfDay();
            values.add(status == null
                    ? articleRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to)
                    : articleRepository.countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                            status, from, to));
        }
        return toItems(labels, values);
    }

    private List<CategorySlice> articlesByCategory() {
        List<Category> categories = categoryRepository.findAll(Sort.by("name"));
        List<ChartItem> raw = new ArrayList<>();
        for (Category category : categories) {
            raw.add(new ChartItem(category.getName(), articleRepository.countByCategoryId(category.getId()), 0));
        }
        raw.sort(Comparator.comparingLong(ChartItem::getValue).reversed());

        long total = raw.stream().mapToLong(ChartItem::getValue).sum();
        double circumference = 2 * Math.PI * 78;
        double offset = 0;
        List<CategorySlice> slices = new ArrayList<>();
        List<String> colors = List.of("#2563eb", "#10b981", "#f59e0b", "#8b5cf6", "#ef6474", "#06b6d4");
        for (int i = 0; i < raw.size(); i++) {
            ChartItem item = raw.get(i);
            double length = total == 0 ? 0 : circumference * item.getValue() / total;
            int percent = total == 0 ? 0 : (int) Math.round(item.getValue() * 100.0 / total);
            slices.add(new CategorySlice(item.getLabel(), item.getValue(), percent, colors.get(i % colors.size()),
                    number(length) + " " + number(circumference - length), number(-offset)));
            offset += length;
        }
        return slices;
    }

    private List<ChartItem> toItems(List<String> labels, List<Long> values) {
        long max = values.stream().mapToLong(Long::longValue).max().orElse(0);
        List<ChartItem> items = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            int percent = max == 0 ? 0 : (int) Math.round(values.get(i) * 100.0 / max);
            items.add(new ChartItem(labels.get(i), values.get(i), percent));
        }
        return items;
    }

    private List<BarPoint> barPoints(List<ChartItem> stats) {
        long max = stats.stream().mapToLong(ChartItem::getValue).max().orElse(0);
        long step = Math.max(1, (long) Math.ceil(max / 4.0));
        long scaleMax = step * 4;
        int chartLeft = 58;
        int chartWidth = 680;
        int chartTop = 24;
        int chartHeight = 200;
        int slotWidth = chartWidth / Math.max(1, stats.size());
        int barWidth = Math.min(34, (int) (slotWidth * 0.58));
        List<BarPoint> points = new ArrayList<>();
        for (int i = 0; i < stats.size(); i++) {
            ChartItem item = stats.get(i);
            int height = (int) Math.round(chartHeight * item.getValue() / (double) scaleMax);
            int x = chartLeft + i * slotWidth + (slotWidth - barWidth) / 2;
            points.add(new BarPoint(item.getLabel(), item.getValue(), x, x + barWidth / 2,
                    chartTop + chartHeight - height, height, barWidth));
        }
        return points;
    }

    private List<ChartGridLine> gridLines(List<ChartItem> stats) {
        long max = stats.stream().mapToLong(ChartItem::getValue).max().orElse(0);
        long step = Math.max(1, (long) Math.ceil(max / 4.0));
        long scaleMax = step * 4;
        List<ChartGridLine> lines = new ArrayList<>();
        for (int i = 0; i <= 4; i++) {
            lines.add(new ChartGridLine(24 + i * 50, scaleMax - i * step));
        }
        return lines;
    }

    private String sparklineForItems(List<ChartItem> stats) {
        return sparkline(stats.stream().map(ChartItem::getValue).toList());
    }

    private String sparkline(List<Long> values) {
        if (values.isEmpty()) return "0,26 100,26";
        long min = values.stream().mapToLong(Long::longValue).min().orElse(0);
        long max = values.stream().mapToLong(Long::longValue).max().orElse(0);
        StringBuilder points = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            int x = values.size() == 1 ? 50 : (int) Math.round(i * 100.0 / (values.size() - 1));
            int y = max == min ? 15 : 27 - (int) Math.round((values.get(i) - min) * 24.0 / (max - min));
            if (i > 0) points.append(' ');
            points.append(x).append(',').append(y);
        }
        return points.toString();
    }

    private String monthComparison(List<ChartItem> stats) {
        if (stats.size() < 2) return null;
        long previous = stats.get(stats.size() - 2).getValue();
        long current = stats.get(stats.size() - 1).getValue();
        if (previous == 0) return null;
        long change = Math.round((current - previous) * 100.0 / previous);
        return (change > 0 ? "+" : "") + change + "% so với tháng trước";
    }

    private String number(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
