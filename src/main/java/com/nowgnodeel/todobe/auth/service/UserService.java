package com.nowgnodeel.todobe.auth.service;

import com.nowgnodeel.todobe.auth.config.security.UserDetailsImpl;
import com.nowgnodeel.todobe.auth.dto.*;
import com.nowgnodeel.todobe.auth.entity.User;
import com.nowgnodeel.todobe.auth.exception.InvalidPasswordException;
import com.nowgnodeel.todobe.auth.jwt.JwtTokenProvider;
import com.nowgnodeel.todobe.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public UserJoinResponseDto create(UserJoinRequestDto requestDto) {
        if (userRepository.existsByUsername(requestDto.getUsername())) {
            throw new IllegalArgumentException("이미 존재하는 사용자명입니다.");
        }
        User user = User.builder()
                .username(requestDto.getUsername())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .nickname(requestDto.getNickname())
                .birth(requestDto.getBirth())
                .gender(requestDto.getGender())
                .phone(requestDto.getPhone())
                .role(requestDto.getRole())
                .build();
        userRepository.save(user);
        return UserJoinResponseDto.builder().msg("회원가입 완료").build();
    }

    @Transactional
    public UserLoginResponseDto login(UserLoginRequestDto requestDto, HttpServletResponse response) {
        User findUser = userRepository.findByUsername(requestDto.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("잘못된 요청입니다."));
        if (!passwordEncoder.matches(requestDto.getPassword(), findUser.getPassword())) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }
        JwsDto jwsDto = jwtTokenProvider.createAllTokens(findUser.getUsername());
        response.addHeader(JwtTokenProvider.ACCESS_TOKEN, jwsDto.getAccessToken());
        response.addHeader(JwtTokenProvider.REFRESH_TOKEN, jwsDto.getRefreshToken());
        return UserLoginResponseDto.builder().msg("로그인 완료").build();
    }

    public User getAuth(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("인증되지 않은 사용자입니다."));
    }

    @Transactional
    public UserReissueResponseDto reissue() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) throw new IllegalStateException("요청 컨텍스트가 없습니다.");
        HttpServletRequest request = attrs.getRequest();
        HttpServletResponse response = attrs.getResponse();
        if (response == null) throw new IllegalStateException("응답 컨텍스트가 없습니다.");

        String refresh = request.getHeader(JwtTokenProvider.REFRESH_TOKEN);
        if (!StringUtils.hasText(refresh) || !jwtTokenProvider.validateToken(refresh)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }
        String username = jwtTokenProvider.getUserInfo(refresh);
        userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        String newAccess = jwtTokenProvider.createAccessToken(username);
        response.addHeader(JwtTokenProvider.ACCESS_TOKEN, newAccess);
        return UserReissueResponseDto.builder().msg("refresh token reissue complete").build();
    }

    public ExistNameResponseDto checkExistName(ExistNameRequestDto requestDto) {
        boolean exists = userRepository.existsByUsername(requestDto.getUsername());
        return ExistNameResponseDto.builder().message(exists ? "false" : "true").build();
    }

    public UserVerificationResponseDto checkVerification(UserVerificationRequestDto requestDto, UserDetailsImpl userDetails) {
        User findUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("잘못된 요청입니다."));
        if (!passwordEncoder.matches(requestDto.getPassword(), findUser.getPassword())) {
            throw new InvalidPasswordException("false");
        }
        return UserVerificationResponseDto.builder().msg("true").build();
    }

    @Transactional
    public UserInformationResponseDto updatePersonalInformation(UserInformationRequestDto requestDto, UserDetailsImpl userDetails) {
        User findUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("잘못된 요청입니다."));
        findUser.patch(requestDto, passwordEncoder.encode(requestDto.getPassword()));
        userRepository.save(findUser);
        return UserInformationResponseDto.builder().message("complete").build();
    }
}
