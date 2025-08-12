package com.nowgnodeel.todobe.auth.config.Handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nowgnodeel.todobe.auth.dto.validation.SecurityExceptionDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            response.sendRedirect(referer);
            return;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        String json = new ObjectMapper().writeValueAsString(SecurityExceptionDto.builder()
                .statusCode(HttpServletResponse.SC_FORBIDDEN)
                .msg("Access Denied")
                .build());
        response.getWriter().write(json);
    }
}
