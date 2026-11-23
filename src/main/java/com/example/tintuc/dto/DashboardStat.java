package com.example.tintuc.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardStat {

    private final String key;
    private final String label;
    private final String value;
    private final String href;
    private final String tone;
    private final String sparkline;
    private final String hint;
    private final String comparison;
}
