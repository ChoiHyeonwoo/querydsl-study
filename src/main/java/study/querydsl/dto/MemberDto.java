package study.querydsl.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {

    private String username;
    private Integer age;

    public MemberDto(String username, Integer age) {
        this.username = username;
        this.age = age;
    }
}
