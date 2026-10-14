package com.penny.web;

import com.penny.exception.ApiException;
import com.penny.exception.NotFoundException;
import com.penny.model.User;
import com.penny.repository.UserRepository;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String HEADER = "X-Telegram-User-Id";

    private final UserRepository userRepository;

    public CurrentUserArgumentResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) && parameter.getParameterType().equals(User.class);
    }

    @Override
    public User resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory
    ) {
        long telegramUserId = parseTelegramUserId(webRequest.getHeader(HEADER));
        return userRepository.findByTelegramUserId(telegramUserId)
            .orElseThrow(() -> new NotFoundException(
                "USER_NOT_REGISTERED",
                "Usuário não cadastrado. Envie /start para o bot primeiro."
            ));
    }

    public static long parseTelegramUserId(String header) {
        if (header == null || header.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_HEADER", "Cabeçalho obrigatório ausente: " + HEADER + ".");
        }
        try {
            return Long.parseLong(header.strip());
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_HEADER", "Cabeçalho " + HEADER + " inválido.");
        }
    }
}
