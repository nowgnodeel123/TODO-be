package com.nowgnodeel.todobe.auth.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final long ACCESS_TOKEN_TTL = 30 * 60 * 1000L;
    private static final long REFRESH_TOKEN_TTL = 7L * 24 * 60 * 60 * 1000;
    private static final long CLOCK_SKEW_SECONDS = 30L;
    private static final String ISSUER = "off";

    private static final String CLAIM_AUTH = "auth";
    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final Key key;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        if (keyBytes.length < 32)
            throw new IllegalArgumentException("jwt.secret must be Base64-encoded 256-bit key or larger");
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public JwtToken generateToken(Authentication authentication) {
        long now = System.currentTimeMillis();
        Date iat = new Date(now);

        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        String subject = authentication.getName();

        String accessToken = Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setIssuer(ISSUER)
                .setSubject(subject)
                .claim(CLAIM_AUTH, authorities)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(iat)
                .setExpiration(new Date(now + ACCESS_TOKEN_TTL))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        String refreshToken = Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setIssuer(ISSUER)
                .setSubject(subject)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(iat)
                .setExpiration(new Date(now + REFRESH_TOKEN_TTL))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return JwtToken.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public Optional<Authentication> validateAccessTokenAndGetAuthentication(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .requireIssuer(ISSUER)
                    .setAllowedClockSkewSeconds(CLOCK_SKEW_SECONDS)
                    .build()
                    .parseClaimsJws(token);

            String alg = jws.getHeader().getAlgorithm();
            if (!SignatureAlgorithm.HS256.getValue().equals(alg)) return Optional.empty();

            Claims claims = jws.getBody();
            if (!TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) return Optional.empty();

            String authClaim = claims.get(CLAIM_AUTH, String.class);
            if (authClaim == null || authClaim.isBlank()) return Optional.empty();

            var authorities = Arrays.stream(authClaim.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            String username = claims.getSubject();
            UserDetails principal = User.withUsername(username).password("").authorities(authorities).build();

            return Optional.of(new UsernamePasswordAuthenticationToken(principal, null, authorities));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseClaimsStrict(token);
            return TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
        } catch (BadCredentialsException e) {
            return false;
        }
    }

    public Claims parseClaimsAllowExpired(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .requireIssuer(ISSUER)
                    .setAllowedClockSkewSeconds(CLOCK_SKEW_SECONDS)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    private Claims parseClaimsStrict(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .requireIssuer(ISSUER)
                    .setAllowedClockSkewSeconds(CLOCK_SKEW_SECONDS)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new BadCredentialsException("만료된 토큰입니다.", e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("유효하지 않은 JWT입니다.", e);
        }
    }
}
