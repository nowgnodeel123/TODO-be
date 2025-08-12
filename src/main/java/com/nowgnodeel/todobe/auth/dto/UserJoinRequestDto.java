package com.nowgnodeel.todobe.auth.dto;

import com.nowgnodeel.todobe.auth.common.Gender;
import com.nowgnodeel.todobe.auth.common.Role;
import lombok.Getter;

@Getter
public class UserJoinRequestDto {
    private String username;
    private String password;
    private String nickname;
    private String birth;
    private Gender gender;
    private String phone;
    private Role role;
}
