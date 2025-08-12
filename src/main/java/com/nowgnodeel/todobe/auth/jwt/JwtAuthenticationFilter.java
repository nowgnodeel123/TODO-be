package com.nowgnodeel.todobe.auth.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final boolean failFast;

    public JwtAuthenticationFilter(JwtTokenProvider provider) {
        this.jwtTokenProvider = provider;
        this.failFast = false;
    }

    public JwtAuthenticationFilter(JwtTokenProvider provider, boolean failFast) {
        this.jwtTokenProvider = provider;
        this.failFast = failFast;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        try {
            if (StringUtils.hasText(token)) {
                Optional.ofNullable(jwtTokenProvider.validateAccessTokenAndGetAuthentication(token))
                        .flatMap(a -> a)
                        .ifPresentOrElse(auth -> {
                            SecurityContext context = SecurityContextHolder.createEmptyContext();
                            context.setAuthentication(auth);
                            SecurityContextHolder.setContext(context);
                        }, () -> {
                            SecurityContextHolder.clearContext();
                            if (failFast) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            }
                        });
                if (failFast && response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) return;
            } else {
                SecurityContextHolder.clearContext();
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            log.debug("JWT error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getServletPath();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true; // CORS preflight
        return p.equals("/api/v1/users/sign-in") || p.equals("/api/v1/users/refresh");
    }
}
