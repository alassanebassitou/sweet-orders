package com.devcrafter.Patisserie.App.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewsRequest {
    @NotNull
    private Long productId;

    @NotNull
    private Long commandeId;

    @NotNull
    @Min(1) @Max(5)
    private Integer note;

    private String comment;
}
