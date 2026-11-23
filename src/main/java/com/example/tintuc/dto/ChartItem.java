package com.example.tintuc.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Một cột/thanh của biểu đồ trên Dashboard. */
@Getter
@AllArgsConstructor
public class ChartItem {
    private final String label;
    private final long value;
    private final int percent; // 0..100, so với giá trị lớn nhất
}
