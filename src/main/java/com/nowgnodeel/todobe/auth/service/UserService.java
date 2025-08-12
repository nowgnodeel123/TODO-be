package com.nowgnodeel.todobe.user.service;

import com.nowgnodeel.todobe.global.security.jwt.JwtToken;
import com.nowgnodeel.todobe.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public JwtToken signIn(String username, String password) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(username, password);

        try {
            Authentication authentication = authenticationManager.authenticate(authToken);
            return jwtTokenProvider.generateToken(authentication);
        } catch (BadCredentialsException e) {
            log.debug("Bad credentials for user: {}", username);
            throw e;
        } catch (DisabledException | LockedException e) {
            log.debug("Disabled/Locked account: {}", username);
            throw e;
        }
    }
}