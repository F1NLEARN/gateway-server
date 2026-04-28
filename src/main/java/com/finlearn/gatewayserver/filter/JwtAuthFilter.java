package com.finlearn.gatewayserver.filter;

import com.finlearn.gatewayserver.jwt.GatewayJwtProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** JWT를 검증하고 다운스트림 서비스로 X-User-* 헤더를 주입하는 필터. */
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BLACKLIST_PREFIX = "BL:";

    private final GatewayJwtProvider jwtProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = jwtProvider.resolveToken(request.getHeader(GatewayJwtProvider.AUTHORIZATION_HEADER));

        if (token != null) {
            // JWT 서명·만료 검증 (실패 시 JwtException)
            Claims claims = jwtProvider.getClaims(token);

            // 로그아웃된 토큰 차단
//            if (Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token))) {
//                throw new io.jsonwebtoken.JwtException("INVALID_ACCESS_TOKEN");
//            }

            // 검증 통과 → 다운스트림 서비스에 유저 정보 헤더 주입
            MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(request);
            mutableRequest.addHeader("X-User-Id",   jwtProvider.getUserId(claims).toString());
            mutableRequest.addHeader("X-User-Role", jwtProvider.getRole(claims));

            filterChain.doFilter(mutableRequest, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    // 인증 없이 접근 가능한 경로는 필터를 건너뛴다.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/api/v1/users/auth/login")    ||
               path.equals("/api/v1/users/auth/reissue")  ||
               path.startsWith("/api/v1/users/signup")    ||
               path.startsWith("/actuator");
    }
}
