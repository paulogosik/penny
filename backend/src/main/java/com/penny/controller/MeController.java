package com.penny.controller;

import com.penny.dto.MeRequest;
import com.penny.dto.PhoneNumberRequest;
import com.penny.dto.UserResponse;
import com.penny.model.User;
import com.penny.service.UserService;
import com.penny.web.CurrentUser;
import com.penny.web.CurrentUserArgumentResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse get(@CurrentUser User user) {
        return UserResponse.from(user);
    }

    @PutMapping
    public ResponseEntity<UserResponse> register(
        @RequestHeader(value = CurrentUserArgumentResolver.HEADER, required = false) String telegramUserId,
        @Valid @RequestBody(required = false) MeRequest request
    ) {
        UserService.Registration registration = userService.register(
            CurrentUserArgumentResolver.parseTelegramUserId(telegramUserId),
            request == null ? new MeRequest(null, null) : request
        );
        HttpStatus status = registration.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(registration.user());
    }

    @PutMapping("/phone")
    public UserResponse updatePhoneNumber(@CurrentUser User user, @Valid @RequestBody PhoneNumberRequest request) {
        return userService.updatePhoneNumber(user, request.phoneNumber());
    }
}
