package com.example.tintuc.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CategorySlice {

    private final String label;
    private final long value;
    private final int percent;
    private final String color;
    private final String dashArray;
    private final String dashOffset;
}
