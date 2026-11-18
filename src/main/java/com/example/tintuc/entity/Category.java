package com.example.tintuc.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "categories")
@Getter
@Setter
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên chuyên mục không được để trống")
    @Size(max = 100, message = "Tên chuyên mục tối đa 100 ký tự")
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    @Column(length = 500)
    private String description;
}
