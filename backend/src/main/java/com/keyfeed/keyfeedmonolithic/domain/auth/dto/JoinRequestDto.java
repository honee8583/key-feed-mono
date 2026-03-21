package com.keyfeed.keyfeedmonolithic.domain.auth.dto;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class JoinRequestDto {
    private String email;
    private String password;
    private String name;

    public User toEntity(String encodedPassword) {
        return User.builder()
                .email(this.email)
                .password(encodedPassword)
                .username(this.name)
                .build();
    }
}
