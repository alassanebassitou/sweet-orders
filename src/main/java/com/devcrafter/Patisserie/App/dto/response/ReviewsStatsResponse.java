package com.devcrafter.Patisserie.App.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class ReviewsStatsResponse {
    private Double moyenneNote;
    private Long   totalAvis;
    private Map<Integer, Long> distribution;
    private List<ReviewsResponse> reviews;
}
