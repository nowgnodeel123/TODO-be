package com.nowgnodeel.todobe.auth.entity;

import com.nowgnodeel.todobe.auth.common.Gender;
import com.nowgnodeel.todobe.auth.common.Role;
import com.nowgnodeel.todobe.auth.dto.UserInformationRequestDto;
import com.nowgnodeel.todobe.global.entity.Timestamped;
import com.nowgnodeel.todobe.todo.entity.Todo;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String birth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Todo> todos = new ArrayList<>();

    public void patch(UserInformationRequestDto dto, String encodedPassword) {
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            this.password = encodedPassword;
        }
        if (dto.getNickname() != null && !dto.getNickname().isBlank()) {
            this.nickname = dto.getNickname();
        }
        if (dto.getBirth() != null && !dto.getBirth().isBlank()) {
            this.birth = dto.getBirth();
        }
        if (dto.getGender() != null) {
            this.gender = dto.getGender();
        }
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            this.phone = dto.getPhone();
        }
    }
}
