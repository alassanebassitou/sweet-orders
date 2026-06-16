package com.devcrafter.Patisserie.App.Exceptions.handlerException;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int         status;
    private String      error;
    private String      message;
    private String      path;
    private LocalDateTime timestamp;

    // For validation errors — field → error message
    private Map<String, String> fieldErrors;

    // For multiple errors
    private List<String> errors;
}
