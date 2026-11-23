package com.example.tintuc.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlugUtilTest {

    @Test
    void chuyenTiengVietCoDauThanhSlug() {
        assertEquals("the-thao", SlugUtil.toSlug("Thể thao"));
        assertEquals("doi-song-va-suc-khoe", SlugUtil.toSlug("Đời sống và Sức khỏe"));
    }

    @Test
    void boKhoangTrangVaKyTuThua() {
        assertEquals("cong-nghe", SlugUtil.toSlug("  Công   nghệ!!  "));
    }

    @Test
    void chuoiRongTraVeRong() {
        assertEquals("", SlugUtil.toSlug(null));
        assertEquals("", SlugUtil.toSlug("???"));
    }
}
