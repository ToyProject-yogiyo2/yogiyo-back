package toy.yogiyo.common.login.dto;

import lombok.Builder;
import lombok.Getter;
import toy.yogiyo.core.member.domain.Member;
import toy.yogiyo.core.owner.domain.Owner;

@Getter
@Builder
public class LoginResponse {

    private Long userId;
    private String email;

    public static LoginResponse of(Member member){
        return LoginResponse.builder()
                .userId(member.getId())
                .email(member.getEmail())
                .build();
    }

    public static LoginResponse of(Owner owner){
        return LoginResponse.builder()
                .userId(owner.getId())
                .email(owner.getEmail())
                .build();
    }
}
