package com.devcrafter.Patisserie.App.services;

import com.devcrafter.Patisserie.App.Exceptions.ResourceNotFoundException;
import com.devcrafter.Patisserie.App.Exceptions.BadRequestException;
import com.devcrafter.Patisserie.App.dto.request.ClientManuallyCreationRequest;
import com.devcrafter.Patisserie.App.dto.request.UserProfileRequest;
import com.devcrafter.Patisserie.App.dto.request.UserRoleRequest;
import com.devcrafter.Patisserie.App.dto.response.UserResponse;
import com.devcrafter.Patisserie.App.enums.Role;
import com.devcrafter.Patisserie.App.Exceptions.MultipleUsersFoundWithEmailException;
import com.devcrafter.Patisserie.App.Exceptions.AccessDeniedException;
import com.devcrafter.Patisserie.App.models.Client;
import com.devcrafter.Patisserie.App.models.SessionUser;
import com.devcrafter.Patisserie.App.models.User;
import com.devcrafter.Patisserie.App.repository.ClientRepository;
import com.devcrafter.Patisserie.App.repository.CommandeRepository;
import com.devcrafter.Patisserie.App.repository.PaymentsRepository;
import com.devcrafter.Patisserie.App.repository.UserRepository;
import com.devcrafter.Patisserie.App.security.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.devcrafter.Patisserie.App.utils.AppConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final CommandeRepository commandeRepository;
    private final PaymentsRepository paymentsRepository;
    private final ClientRepository clientRepository;


    /**
     * Created a manual client by admin only
     * */
    public UserResponse createdClientManually(ClientManuallyCreationRequest request) {

        Pattern pattern = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}");
        Matcher mat = pattern.matcher(request.getEmail());

        // If the mail is in the right format, we create the doctor and return it
        if (mat.matches()) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new MultipleUsersFoundWithEmailException(request.getEmail());
            } else {
                Client client = getClient(request);
                return UserResponse.from(clientRepository.save(client));
            }
        } else {throw new BadRequestException(EMAIL_INVALID); }
    }

    @NotNull
    private static Client getClient(ClientManuallyCreationRequest request) {
        Client client = new Client();

        if (request.getLastname() != null && !request.getLastname().isBlank()) {
            client.setLastname(request.getLastname());
        }

        if (request.getFirstname() != null && !request.getFirstname().isBlank()) {
            client.setFirstname(request.getFirstname());
        }
        client.setEmail(request.getEmail());
        client.setAddress(request.getAddress());
        client.setTelephone(request.getPhone());
        client.setIsVIP(false);
        client.setIsActif(false);
        client.setRole(Role.ROLE_CLIENT);
        return client;
    }

    /**
     * Update the current user's own profile.
     * A user can only update their own profile — never someone else's.
     */
    public UserResponse updateMyProfile(SessionUser currentUser,
                                        UserProfileRequest request) {

        User user = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND, currentUser.getUserId()));

        // Update common fields
        if (request.getLastname() != null)       user.setLastname(request.getLastname());
        if (request.getFirstname() != null)    user.setFirstname(request.getFirstname());
        if (request.getTelephone() != null) user.setTelephone(request.getTelephone());

        // Update client-specific fields if the user is a Client
        if (user instanceof Client client) {
            if (request.getAddress() != null) client.setAddress(request.getAddress());
            if (request.getCity() != null)    client.setCity(request.getCity());
        }

        User saved = userRepository.save(user);
        log.info("Profile updated for user: {}", currentUser.getEmail());

        return UserResponse.from(saved);
    }

    /**
     * List all users — admin only.
     */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::buildUserResponse)
                .toList();
    }

    /**
     * Change a user's role — admin only.
     * An admin cannot demote themselves to avoid locking out the system.
     */
    public UserResponse changeUserRole(Long targetUserId,
                                       UserRoleRequest request,
                                       SessionUser currentUser) {

        // Prevent admin from changing their own role
        if (targetUserId.equals(currentUser.getUserId())) {
            throw new AccessDeniedException(
                    "Vous ne pouvez pas modifier votre propre rôle"
            );
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        USER_NOT_FOUND, targetUserId
                ));

        // Validate the role value
        try {
            Role newRole = Role.valueOf(request.getRole());
            user.setRole(newRole);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    INVALID_ROLE + request.getRole() +
                            ". Valeurs acceptées: ROLE_ADMIN, ROLE_CLIENT"
            );
        }

        User saved = userRepository.save(user);
        log.info("Role changed to {} for user: {}", request.getRole(), user.getEmail());

        return UserResponse.from(saved);
    }

    public UserResponse updatePhone(Long userId, String telephone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId)
                );
        user.setTelephone(telephone);
        return UserResponse.from(userRepository.save(user));
    }

    /**
     * Deactivate a user — admin only.
     * The user will be blocked on their very next request.
     */
    public UserResponse deactivateUser(Long userId, SessionUser currentUser) {

        // Admin cannot deactivate themselves
        if (userId.equals(currentUser.getUserId())) {
            throw new AccessDeniedException(
                    "Vous ne pouvez pas désactiver votre propre compte"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND, userId));

        user.setIsActif(false);
        userRepository.save(user);

        // Immediately invalidate their Redis session
        // They won't even finish their current request
        sessionService.deleteAllSessionsForUser(user.getGoogleSub());

        log.info("User deactivated: {}", user.getEmail());
        return UserResponse.from(user);
    }

    /**
     * Reactivate a user — admin only.
     */
    public UserResponse activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND, userId));

        user.setIsActif(true);
        userRepository.save(user);

        log.info("User activated: {}", user.getEmail());
        return UserResponse.from(user);
    }

    private UserResponse buildUserResponse(User user) {
        UserResponse response = UserResponse.from(user);

        if ("ROLE_CLIENT".equals(user.getRole().name())) {
            Integer total = commandeRepository
                    .countByClientId(user.getId());
            BigDecimal expense = paymentsRepository
                    .totalExpenseByClient(user.getId());

            response.setTotalCommande(
                    total != null ? total : 0
            );
            response.setTotalExpenses(
                    expense != null ? expense : BigDecimal.ZERO
            );
        } else {
            response.setTotalCommande(0);
            response.setTotalExpenses(BigDecimal.ZERO);
        }

        return response;
    }
}
