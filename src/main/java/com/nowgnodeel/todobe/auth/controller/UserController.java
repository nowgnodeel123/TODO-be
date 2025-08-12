package com.nowgnodeel.todobe.auth.controller;

import com.nowgnodeel.todobe.auth.dto.*;
import com.nowgnodeel.todobe.auth.service.UserService;
import com.nowgnodeel.todobe.auth.config.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @PostMapping("/join")
    public ResponseEntity<UserJoinResponseDto> join(@RequestBody @Valid UserJoinRequestDto requestDto) {
        UserJoinResponseDto created = userService.create(requestDto);
        return ResponseEntity.status(HttpStatus.OK).body(created);
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseDto> login(@RequestBody UserLoginRequestDto requestDto, HttpServletResponse response) {
        UserLoginResponseDto responseDto = userService.login(requestDto, response);
        return ResponseEntity.status(HttpStatus.OK).body(responseDto);
    }

    @PostMapping("/reissue")
    public ResponseEntity<UserReissueResponseDto> checkRefreshToken() {
        UserReissueResponseDto responseDto = userService.reissue();
        return ResponseEntity.status(HttpStatus.OK).body(responseDto);
    }

    @PostMapping("/exist/name")
    public ResponseEntity<ExistNameResponseDto> checkExistName(@RequestBody ExistNameRequestDto requestDTO) {
        ExistNameResponseDto responseDTO = userService.checkExistName(requestDTO);
        return ResponseEntity.ok(responseDTO);
    }

    @PostMapping("/user/verification")
    public ResponseEntity<UserVerificationResponseDto> checkVerification(@RequestBody UserVerificationRequestDto requestDTO, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        UserVerificationResponseDto responseDTO = userService.checkVerification(requestDTO, userDetails);
        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }

    @PostMapping("/user/update")
    public ResponseEntity<UserInformationResponseDto> updatePersonalInformation(@RequestBody UserInformationRequestDto requestDTO, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        UserInformationResponseDto responseDTO = userService.updatePersonalInformation(requestDTO, userDetails);
        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }
}
