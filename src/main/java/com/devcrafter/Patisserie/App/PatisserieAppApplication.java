package com.devcrafter.Patisserie.App;

import com.devcrafter.Patisserie.App.config.CorsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties(CorsProperties.class)
public class PatisserieAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(PatisserieAppApplication.class, args);
	}

}
