package com.devcrafter.Patisserie.App.security.service;

import com.devcrafter.Patisserie.App.Exceptions.BadRequestException;
import com.devcrafter.Patisserie.App.Exceptions.ConflictException;
import com.devcrafter.Patisserie.App.Exceptions.ResourceNotFoundException;
import com.devcrafter.Patisserie.App.dto.request.SignUpRequest;
import com.devcrafter.Patisserie.App.dto.request.VerifyCodeRequest;
import com.devcrafter.Patisserie.App.dto.response.AuthResponse;
import com.devcrafter.Patisserie.App.enums.Role;
import com.devcrafter.Patisserie.App.Exceptions.AccessDeniedException;
import com.devcrafter.Patisserie.App.models.Client;
import com.devcrafter.Patisserie.App.models.SessionUser;
import com.devcrafter.Patisserie.App.models.User;
import com.devcrafter.Patisserie.App.models.VerificationCode;
import com.devcrafter.Patisserie.App.repository.UserRepository;
import com.devcrafter.Patisserie.App.repository.VerificationCodesRepository;
import com.devcrafter.Patisserie.App.security.component.GoogleTokenVerifier;
import com.devcrafter.Patisserie.App.services.EmailServiceWithResend;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final SessionService sessionService;
    private final UserRepository userRepository;
    private final VerificationCodesRepository codeRepository;
    private final EmailServiceWithResend emailService;


    public void signUp(SignUpRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException(
                    "Un compte avec cet email existe déjà. " +
                            "Connectez-vous directement."
            );
        }

        Client newClient = new Client();
        newClient.setEmail(request.getEmail());
        newClient.setLastname(request.getLastname());
        newClient.setFirstname(request.getFirstname());
        newClient.setTelephone(request.getPhone());
        newClient.setBirthDay(request.getBirthday());
        newClient.setRole(Role.ROLE_CLIENT);
        newClient.setIsVIP(false);
        newClient.setIsActif(false);

        userRepository.save(newClient);
        log.info("New client registered: {}", request.getEmail());
    }


    public void sendVerificationCode(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun compte trouvé avec cet email. " +
                                "Veuillez vous inscrire d'abord."
                ));

        if (user.getIsActif() != null && !user.getIsActif() && user.getGoogleSub() != null) {
            throw new AccessDeniedException(
                    "Votre compte a été désactivé. " +
                            "Contactez l'administrateur."
            );
        }

        codeRepository.deleteByEmail(email);

        String code = String.format("%06d",
                new java.util.Random().nextInt(999999)
        );

        VerificationCode vc = new VerificationCode();
        vc.setEmail(email);
        vc.setCode(code);
        vc.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        vc.setUsed(false);
        codeRepository.save(vc);

        emailService.sendVerificationCode(user, code);

        log.info("Verification code sent to {}", email);
    }


    public AuthResponse verifyCodeAndLogin(VerifyCodeRequest request) {

        VerificationCode vc = codeRepository
                .findTopByEmailOrderByCreatedAtDesc(request.getEmail())
                .orElseThrow(() -> new BadRequestException(
                        "Aucun code envoyé pour cet email. " +
                                "Veuillez en demander un nouveau."
                ));

        if (!vc.isValid()) {
            throw new BadRequestException(
                    vc.isExpired()
                            ? "Ce code a expiré. Demandez-en un nouveau."
                            : "Ce code a déjà été utilisé."
            );
        }

        if (!vc.getCode().equals(request.getCode())) {
            throw new BadRequestException(
                    "Code incorrect. Vérifiez votre email."
            );
        }

        vc.setUsed(true);
        codeRepository.save(vc);

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable"
                ));

        if (user.getIsActif() == null || !user.getIsActif()) {
            user.setIsActif(true);
            userRepository.save(user);
            log.info("Account activated on first login: {}",
                    user.getEmail());
        }

        SessionUser sessionUser = new SessionUser(
                user.getId(),
                user.getGoogleSub(),
                user.getFirstname(),
                user.getEmail(),
                user.getLastname(),
                user.getPhotoUrl(),
                user.getRole().name(),
                true,
                user.getTelephone()
        );
        String sessionId = sessionService.createSession(sessionUser);

        log.info("Email login successful for {}",
                user.getEmail());

        return new AuthResponse(sessionId, sessionUser);
    }


    /**
     * Called by POST /api/v1/auth/google
     * 1. Verifies the Google ID token
     * 2. Finds or creates the user in PostgreSQL
     * 3. Creates a Redis session
     * 4. Returns session ID + user info
     */
    public AuthResponse loginWithGoogle(String idTokenString) {

        log.info("idToken {}", idTokenString);

        //Verify with Google
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(idTokenString);
        if (payload == null) {
            throw new AccessDeniedException("Invalid or expired Google token");
        }

        String googleSub = payload.getSubject();
        String email = payload.getEmail();
        String lastname = (String) payload.get("family_name");
        String firstname = (String) payload.get("given_name");
        String photoUrl = (String) payload.get("picture");

        //Find or create user in PostgreSQL
        User user = userRepository
                .findByGoogleSub(googleSub)
                .orElseGet(() -> {
                    // First login — create the account automatically
                    return userRepository.findByEmail(email)
                            .map(existingUser -> {

                                if (existingUser.getGoogleSub() == null) {
                                    log.info("First Google login for pre-created user: {}", email);
                                    existingUser.setGoogleSub(googleSub);
                                    existingUser.setPhotoUrl(photoUrl);
                                    existingUser.setLastname(lastname);
                                    existingUser.setFirstname(firstname);
                                    return userRepository.save(existingUser);
                                }

                                throw new AccessDeniedException(
                                        "Ce compte email est déjà lié à un autre compte Google"
                                );
                            })
                            .orElseGet(() -> {

                                // Completely new user → create as CLIENT
                                log.info("New client registration: {}", email);
                                Client newClient = new Client();
                                newClient.setGoogleSub(googleSub);
                                newClient.setEmail(email);
                                newClient.setLastname(lastname);
                                newClient.setFirstname(firstname);
                                newClient.setPhotoUrl(photoUrl);
                                newClient.setRole(Role.ROLE_CLIENT);
                                newClient.setIsVIP(false);
                                newClient.setIsActif(true);
                                return userRepository.save(newClient);
                            });
                });

        // Update photo/lastname in case they changed on Google
        user.setLastname(lastname);
        user.setFirstname(firstname);
        user.setPhotoUrl(photoUrl);
        userRepository.save(user);

        if (user.getIsActif() == null || !user.getIsActif()) {
            throw new AccessDeniedException(
                    "Votre compte a été désactivé. Contactez l'administrateur."
            );
        }

        log.info("User: {}", lastname);
        log.info("User: {}", firstname);
        log.info("User: {}", photoUrl);

        // Create Redis session
        SessionUser sessionUser = new SessionUser(
                user.getId(),
                googleSub,
                firstname,
                email,
                lastname,
                photoUrl,
                user.getRole().name(),
                true,
                user.getTelephone()
        );
        String sessionId = sessionService.createSession(sessionUser);

        // Return session ID + user profile
        return new AuthResponse(sessionId, sessionUser);
    }

    public void logout(String sessionId) {
        sessionService.deleteSession(sessionId);
    }
}
