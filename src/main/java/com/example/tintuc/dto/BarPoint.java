package com.example.tintuc.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BarPoint {

    private final String label;
    private final long value;
    private final int x;
    private final int centerX;
    private final int y;
    private final int height;
    private final int width;
}
