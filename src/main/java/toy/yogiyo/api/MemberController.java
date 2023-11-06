package toy.yogiyo.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import toy.yogiyo.common.login.LoginUser;
import toy.yogiyo.core.member.domain.Member;
import toy.yogiyo.core.member.dto.MemberJoinRequest;
import toy.yogiyo.core.member.dto.MemberJoinResponse;
import toy.yogiyo.core.member.dto.MemberMypageResponse;
import toy.yogiyo.core.member.dto.MemberUpdateRequest;
import toy.yogiyo.core.member.service.MemberService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
@Slf4j
public class MemberController {
    private final MemberService memberService;

    @PostMapping("/join")
    public MemberJoinResponse join(@RequestBody MemberJoinRequest memberJoinRequest){
        return memberService.join(memberJoinRequest);
    }

    @GetMapping("/mypage")
    public MemberMypageResponse showMypage(@LoginUser Member member){
        return memberService.findOne(member);
    }

    @PatchMapping("/update")
    public String update(@LoginUser Member member, @RequestBody MemberUpdateRequest memberUpdateRequest){
        memberService.update(member, memberUpdateRequest);
        return "update success";
    }

    @DeleteMapping("/delete")
    public String delete(@LoginUser Member member){
        memberService.delete(member);
        return "delete success";
    }
}
