package com.nowgnodeel.todobe.auth.dto;

import com.nowgnodeel.todobe.auth.common.Gender;
import lombok.Getter;

@Getter
public class UserInformationRequestDto {
    private String password;
    private String nickname;
    private String birth;
    private Gender gender;
    private String phone;
}
