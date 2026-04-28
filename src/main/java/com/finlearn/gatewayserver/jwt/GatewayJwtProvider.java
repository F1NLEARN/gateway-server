package com.finlearn.gatewayserver.jwt;

import com.finlearn.common.exception.AuthErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Gateway 전용 JWT 검증·파싱 컴포넌트. */
@Component
public class GatewayJwtProvider {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret}")
    private String secretKeyPlain;

    private SecretKey secretKey;

    @PostConstruct
    private void init() {
        // user-service의 JwtTokenProvider와 동일하게 raw bytes로 키 생성
        this.secretKey = Keys.hmacShaKeyFor(secretKeyPlain.getBytes(StandardCharsets.UTF_8));
    }

    // 검증 실패 시 AuthErrorCode 이름을 메시지로 담은 JwtException을 던진다.
    public Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new JwtException(AuthErrorCode.EXPIRED_ACCESS_TOKEN.name());
        } catch (JwtException e) {
            throw new JwtException(AuthErrorCode.INVALID_ACCESS_TOKEN.name());
        }
    }

    // subject는 UUID 문자열 (user-service가 UUID로 서명함)
    public UUID getUserId(Claims claims) { return UUID.fromString(claims.getSubject()); }

    // JWT의 role claim 반환
    public String getRole(Claims claims) { return claims.get("role", String.class); }

    // Bearer 접두사를 제거하고 순수 토큰을 반환한다.
    public String resolveToken(String bearerToken) {
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
