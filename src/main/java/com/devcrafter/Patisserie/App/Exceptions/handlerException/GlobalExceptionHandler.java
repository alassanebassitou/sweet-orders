package com.devcrafter.Patisserie.App.Exceptions.handlerException;

import com.devcrafter.Patisserie.App.Exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .status(404)
                        .error("Not Found")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .build()
                );
    }

    @ExceptionHandler({
            AccessDeniedException.class,
            org.springframework.security.access
                    .AccessDeniedException.class
    })
    public ResponseEntity<ErrorResponse> handleForbidden(
            Exception ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.builder()
                        .status(403)
                        .error("Forbidden")
                        .message("Vous n'avez pas les droits " +
                                "pour effectuer cette action")
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .build()
                );
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .status(400)
                        .error("Bad Request")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .build()
                );
    }

    @ExceptionHandler(SessionCreationFailedException.class)
    public ResponseEntity<ErrorResponse> handleSessionCreationFailed(
            SessionCreationFailedException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .status(400)
                        .error("Session Failed")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .build()
                );
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ConflictException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.builder()
                        .status(409)
                        .error("Conflict")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .build()
                );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.builder()
                        .status(422)
                        .error(ex.getCode())
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .build()
                );
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = error instanceof FieldError
                    ? ((FieldError) error).getField()
                    : error.getObjectName();
            String message = error.getDefaultMessage();
            fieldErrors.put(field, message);
        });

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .status(400)
                        .error("Validation Failed")
                        .message("Des champs sont invalides")
                        .fieldErrors(fieldErrors)
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .build()
                );
    }


    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getConstraintViolations().forEach(v -> {
            String field = v.getPropertyPath().toString();
            fieldErrors.put(field, v.getMessage());
        });

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .status(400)
                        .error("Validation Failed")
                        .message("Des contraintes ne sont pas respectées")
                        .fieldErrors(fieldErrors)
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .build()
                );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .status(400)
                        .error("Bad Request")
                        .message("Le corps de la requête est invalide " +
                                "ou mal formaté")
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .build()
                );
    }


    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .status(400)
                        .error("Missing Parameter")
                        .message("Paramètre requis manquant: "
                                + ex.getParameterName())
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .build()
                );
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .status(400)
                        .error("Type Mismatch")
                        .message("Valeur invalide pour le paramètre '"
                                + ex.getName() + "': "
                                + ex.getValue())
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .build()
                );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleFileSize(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ErrorResponse.builder()
                        .status(413)
                        .error("File Too Large")
                        .message("Le fichier dépasse la taille " +
                                "maximale autorisée (5MB)")
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .build()
                );
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(
            Exception ex,
            HttpServletRequest request) {

        log.error("500 on {} {}: {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage(),
                ex  // ← this prints full stack trace
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .status(500)
                        .error("Internal Server Error")
                        .message("Une erreur interne est survenue. " +
                                "Veuillez réessayer.")
                        .path(request.getRequestURI())
                        .timestamp(LocalDateTime.now())
                        .build()
                );
    }
}
