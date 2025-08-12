package com.nowgnodeel.todobe.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nowgnodeel.todobe.auth.dto.validation.SecurityExceptionDto;
import com.nowgnodeel.todobe.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import static com.nowgnodeel.todobe.auth.jwt.JwtTokenProvider.ACCESS_TOKEN;
import static com.nowgnodeel.todobe.auth.jwt.JwtTokenProvider.REFRESH_TOKEN;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String accessToken = jwtTokenProvider.getToken(request, ACCESS_TOKEN);
        String refreshToken = jwtTokenProvider.getToken(request, REFRESH_TOKEN);

        if ("/api/v1/reissue".equals(uri)) {
            if (!StringUtils.hasText(refreshToken) || !jwtTokenProvider.validateToken(refreshToken)) {
                writeError(response, "Invalid or missing refresh token.", HttpStatus.UNAUTHORIZED.value());
                return;
            }
            String usernameFromRefresh = jwtTokenProvider.getUserInfo(refreshToken);
            if (!StringUtils.hasText(usernameFromRefresh)) {
                writeError(response, "Cannot extract subject from refresh token.", HttpStatus.UNAUTHORIZED.value());
                return;
            }
            if (StringUtils.hasText(accessToken)) {
                String usernameFromAccess = jwtTokenProvider.getUserInfo(accessToken);
                if (StringUtils.hasText(usernameFromAccess) && !usernameFromRefresh.equals(usernameFromAccess)) {
                    writeError(response, "Tokens belong to different users.", HttpStatus.UNAUTHORIZED.value());
                    return;
                }
            }
            userRepository.findByUsername(usernameFromRefresh).orElseThrow(() -> new IllegalArgumentException("User not found"));
            String newAccess = jwtTokenProvider.createAccessToken(usernameFromRefresh);
            response.setHeader(ACCESS_TOKEN, newAccess);
            response.setStatus(HttpStatus.OK.value());
            return;
        }

        if (StringUtils.hasText(accessToken)) {
            if (jwtTokenProvider.validateToken(accessToken)) {
                setAuthentication(jwtTokenProvider.getUserInfo(accessToken));
            } else {
                writeError(response, "Invalid Access Token.", HttpStatus.UNAUTHORIZED.value());
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private void setAuthentication(String username) {
        Authentication authentication = jwtTokenProvider.getAuthentication(username);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void writeError(HttpServletResponse response, String msg, int statusCode) {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        try {
            String json = new ObjectMapper().writeValueAsString(SecurityExceptionDto.builder().statusCode(statusCode).msg(msg).build());
            response.getWriter().write(json);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
