package com.devcrafter.Patisserie.App.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // Fallback for browsers without WS
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix for topics the frontend subscribes to
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix for messages sent FROM frontend TO server
        registry.setApplicationDestinationPrefixes("/app");
    }
}
