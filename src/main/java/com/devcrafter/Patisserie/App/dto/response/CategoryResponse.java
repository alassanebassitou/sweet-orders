package com.devcrafter.Patisserie.App.dto.response;

import com.devcrafter.Patisserie.App.models.Category;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CategoryResponse {
    private Long id;
    private String name;
    private String description;
    private String photoUrl;
    private LocalDateTime createdAt;

    public static CategoryResponse from(Category category) {
        CategoryResponse response = new CategoryResponse();

        response.setId(category.getId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setCreatedAt(category.getCreatedAt());
        response.setPhotoUrl(category.getPhotoUrl());

        return response;
    }
}
