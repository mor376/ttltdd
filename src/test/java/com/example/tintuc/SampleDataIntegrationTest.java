package com.example.tintuc;

import com.example.tintuc.config.DataSeeder;
import com.example.tintuc.entity.ArticleStatus;
import com.example.tintuc.repository.ArticleRepository;
import com.example.tintuc.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SampleDataIntegrationTest {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private DataSeeder dataSeeder;

    @Test
    void seedsFortyArticlesIncludingCurrentNewsAcrossCategoriesIdempotently() {
        assertThat(articleRepository.count()).isEqualTo(40);
        assertThat(categoryRepository.count()).isEqualTo(6);
        assertThat(articleRepository.countByStatus(ArticleStatus.PUBLISHED)).isEqualTo(36);
        assertThat(articleRepository.countByStatus(ArticleStatus.DRAFT)).isEqualTo(4);
        assertThat(articleRepository.findAll().stream()
                .filter(article -> article.getContent().contains("Nguồn tham khảo: VnExpress"))
                .count()).isEqualTo(20);
        List<Long> monthlyCounts = currentYearMonthlyCounts();
        assertThat(monthlyCounts).hasSize(YearMonth.now().getMonthValue())
                .allMatch(count -> count > 0);
        if (YearMonth.now().getMonthValue() > 1) {
            assertThat(monthlyCounts.stream().distinct().count()).isGreaterThan(1);
        }

        dataSeeder.run();

        assertThat(articleRepository.count()).isEqualTo(40);
        assertThat(currentYearMonthlyCounts()).isEqualTo(monthlyCounts);
    }

    private List<Long> currentYearMonthlyCounts() {
        YearMonth currentMonth = YearMonth.now();
        List<Long> monthlyCounts = new ArrayList<>();
        for (int month = 1; month <= currentMonth.getMonthValue(); month++) {
            YearMonth targetMonth = YearMonth.of(currentMonth.getYear(), month);
            LocalDateTime start = targetMonth.atDay(1).atStartOfDay();
            LocalDateTime end = targetMonth.plusMonths(1).atDay(1).atStartOfDay();
            monthlyCounts.add(articleRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                    start, end));
        }
        return monthlyCounts;
    }
}
