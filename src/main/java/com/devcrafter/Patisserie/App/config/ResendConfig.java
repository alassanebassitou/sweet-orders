package com.devcrafter.Patisserie.App.config;

import com.resend.Resend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResendConfig {

    @Bean
    public Resend resend(@Value("${RESEND_API_KEY}") String apiKey) {
        return new Resend(apiKey);
    }
}
