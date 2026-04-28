package com.finlearn.gatewayserver.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.common.exception.AuthErrorCode;
import com.finlearn.common.response.ErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** JwtAuthFilter에서 발생한 JwtException을 JSON 에러 응답으로 변환하는 필터. */
@RequiredArgsConstructor
public class JwtExceptionFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (JwtException e) {
            writeErrorResponse(response, resolveErrorCode(e.getMessage()));
        }
    }

    private AuthErrorCode resolveErrorCode(String message) {
        try {
            return AuthErrorCode.valueOf(message);
        } catch (IllegalArgumentException e) {
            return AuthErrorCode.INVALID_ACCESS_TOKEN;
        }
    }

    private void writeErrorResponse(HttpServletResponse response, AuthErrorCode errorCode) throws IOException {
        HttpStatus status = errorCode.getStatus();
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                objectMapper.writeValueAsString(ErrorResponse.of(status, errorCode.getMessage()))
        );
    }
}
