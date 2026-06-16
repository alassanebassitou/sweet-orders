package com.devcrafter.Patisserie.App.dto.response;

import com.devcrafter.Patisserie.App.models.Reviews;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewsResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productPhotoUrl;
    private Long clientId;
    private String clientName;
    private String clientPhotoUrl;
    private Integer note;
    private String comment;
    private Boolean isVisible;
    private LocalDateTime createdAt;

    public static ReviewsResponse from(Reviews r) {
        ReviewsResponse rs = new ReviewsResponse();
        rs.setId(r.getId());
        rs.setProductId(r.getProduct().getId());
        rs.setProductName(r.getProduct().getName());
        rs.setProductPhotoUrl(r.getProduct().getPhotoUrl());
        rs.setClientId(r.getClient().getId());
        rs.setClientName(
                r.getClient().getFirstname()
                        + " "
                        + r.getClient().getLastname()
        );
        rs.setClientPhotoUrl(r.getClient().getPhotoUrl());
        rs.setNote(r.getNote());
        rs.setComment(r.getComment());
        rs.setIsVisible(r.getIsVisible());
        rs.setCreatedAt(r.getCreatedAt());
        return rs;
    }
}
