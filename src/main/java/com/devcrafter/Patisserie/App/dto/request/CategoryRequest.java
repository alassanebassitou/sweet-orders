package com.devcrafter.Patisserie.App.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CategoryRequest {
    private String name;
    private String description;
    private String photoUrl;
}
