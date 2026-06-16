package com.devcrafter.Patisserie.App.dto.response;

import com.devcrafter.Patisserie.App.models.SessionUser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String sessionId;
    private SessionUser sessionUser;
}
