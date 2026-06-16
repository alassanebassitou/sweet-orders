package com.devcrafter.Patisserie.App.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class DeleteCommandeRequest {
    private List<Long> ids;
}
