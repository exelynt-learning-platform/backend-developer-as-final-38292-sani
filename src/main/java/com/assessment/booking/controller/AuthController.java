package com.assessment.booking.controller;

import com.assessment.booking.dto.request.AuthRequest;
import com.assessment.booking.dto.request.RegisterRequest;
import com.assessment.booking.dto.response.ApiResponse;
import com.assessment.booking.dto.response.AuthResponse;
import com.assessment.booking.dto.response.UserResponse;
import com.assessment.booking.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "1. Authentication", description = "Endpoints for user registration, JWT login, and profile fetching")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and receive JWT token",
               description = "Authenticates user credentials and returns a signed JWT token along with user profile and expiration metadata.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", response));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user",
               description = "Registers a new user (defaults to ROLE_USER unless specified). Returns JWT token upon successful registration.")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(ApiResponse.success("User registered successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile",
               description = "Returns profile information of the user extracted from the JWT token in Authorization header.")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse response = authService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
