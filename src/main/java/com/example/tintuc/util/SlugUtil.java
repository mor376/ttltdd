package com.example.tintuc.util;

import java.text.Normalizer;
import java.util.Locale;

public class SlugUtil {

    private SlugUtil() {}

    /** "Thể thao & Đời sống" -> "the-thao-doi-song" */
    public static String toSlug(String input) {
        if (input == null) return "";
        String s = input.trim().replace('đ', 'd').replace('Đ', 'D');
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        return s.replaceAll("^-+|-+$", "");
    }
}
