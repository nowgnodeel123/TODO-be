package com.nowgnodeel.todobe.auth.jwt;

import com.nowgnodeel.todobe.auth.config.security.UserDetailsServiceImpl;
import com.nowgnodeel.todobe.auth.dto.JwsDto;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final UserDetailsServiceImpl userDetailsService;

    public static final String ACCESS_TOKEN = "Authorization";
    public static final String REFRESH_TOKEN = "Refresh";
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret.key}")
    private String secretKeyBase64;

    @Value("${jwt.issuer:nowgnodeel}")
    private String issuer;

    @Value("${jwt.accessMillis:3600000}")
    private long accessValidityMs;

    @Value("${jwt.refreshMillis:604800000}")
    private long refreshValidityMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] bytes = Decoders.BASE64.decode(secretKeyBase64);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT secret too short (>=32 bytes after Base64).");
        }
        key = Keys.hmacShaKeyFor(bytes);
        log.info("JWT key initialized. issuer={}, accessValidityMs={}, refreshValidityMs={}", issuer, accessValidityMs, refreshValidityMs);
    }

    public String getToken(HttpServletRequest request, String jwsType) {
        String headerName = ACCESS_TOKEN.equals(jwsType) ? ACCESS_TOKEN : REFRESH_TOKEN;
        String headerVal = request.getHeader(headerName);
        if (!StringUtils.hasText(headerVal)) return null;
        return stripBearer(headerVal);
    }

    public String createAccessToken(String username) {
        return formatAsBearer(createToken(username, accessValidityMs));
    }

    public String createRefreshToken(String username) {
        return formatAsBearer(createToken(username, refreshValidityMs));
    }

    public JwsDto createAllTokens(String userId) {
        return JwsDto.builder()
                .accessToken(createAccessToken(userId))
                .refreshToken(createRefreshToken(userId))
                .build();
    }

    public boolean validateToken(String tokenOrHeaderValue) {
        String jws = stripBearer(tokenOrHeaderValue);
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(jws);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT signature/format: {}", e.getClass().getSimpleName());
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT token");
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT token");
        } catch (IllegalArgumentException e) {
            log.info("JWT claims is empty");
        }
        return false;
    }

    public String getUserInfo(String tokenOrHeaderValue) {
        String jws = stripBearer(tokenOrHeaderValue);
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(jws).getPayload().getSubject();
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT token, returning subject from claims.");
            return e.getClaims().getSubject();
        }
    }

    public Authentication getAuthentication(String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    public void setHeaderAccessToken(HttpServletResponse response, String tokenOrHeaderValue) {
        response.setHeader(ACCESS_TOKEN, formatAsBearer(stripBearer(tokenOrHeaderValue)));
    }

    private String createToken(String subject, long validityMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)
                .issuer(issuer)
                .issuedAt(new Date(now))
                .expiration(new Date(now + validityMs))
                .signWith(key)
                .compact();
    }

    private String stripBearer(String maybeBearer) {
        if (!StringUtils.hasText(maybeBearer)) return null;
        String trimmed = maybeBearer.trim();
        return trimmed.startsWith(BEARER_PREFIX) ? trimmed.substring(BEARER_PREFIX.length()).trim() : trimmed;
    }

    private String formatAsBearer(String rawToken) {
        if (!StringUtils.hasText(rawToken)) return rawToken;
        return rawToken.startsWith(BEARER_PREFIX) ? rawToken : BEARER_PREFIX + rawToken;
    }
}
