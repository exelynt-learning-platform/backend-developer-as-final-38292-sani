package com.assessment.booking.service;

import com.assessment.booking.dto.request.AuthRequest;
import com.assessment.booking.dto.request.RegisterRequest;
import com.assessment.booking.dto.response.AuthResponse;
import com.assessment.booking.dto.response.UserResponse;
import com.assessment.booking.entity.User;

public interface AuthService {

    AuthResponse login(AuthRequest request);

    AuthResponse register(RegisterRequest request);

    User getCurrentAuthenticatedUser();

    UserResponse getCurrentUserProfile();
}
