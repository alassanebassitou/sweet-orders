package com.devcrafter.Patisserie.App.dto.request;

import com.devcrafter.Patisserie.App.enums.FailureReason;
import lombok.Data;

@Data
public class FailureRequest {
    private FailureReason failureReason;
    private String notes;
}
