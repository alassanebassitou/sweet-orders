package com.devcrafter.Patisserie.App.dto.request;

import lombok.Data;

@Data
public class SettingRequest {
    private String namePatisserie;
    private String whatsappPhoneNumber;
    private String email;
    private String address;
    private Integer minimumDelayHour;
    private Integer depositPercentage;
    private Integer productionOverloadThreshold;
}
