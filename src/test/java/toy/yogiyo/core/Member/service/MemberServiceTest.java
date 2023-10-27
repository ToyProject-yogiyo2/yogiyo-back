package toy.yogiyo.core.Member.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import toy.yogiyo.common.exception.EntityExistsException;
import toy.yogiyo.core.Member.domain.Member;
import toy.yogiyo.core.Member.dto.MemberJoinRequest;
import toy.yogiyo.core.Member.dto.MemberJoinResponse;
import toy.yogiyo.core.Member.dto.MemberMypageResponse;
import toy.yogiyo.core.Member.dto.MemberUpdateRequest;
import toy.yogiyo.core.Member.repository.MemberRepository;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks
    MemberService memberService;

    @Mock
    MemberRepository memberRepository;

    Member member;
    @BeforeEach
    void beforeEach(){
        member = Member.builder()
                .id(1L)
                .nickname("test")
                .email("test@gmail.com")
                .password("1234")
                .build();
    }

    @DisplayName("회원가입")
    @Test
    void join(){
        MemberJoinRequest memberJoinRequest = MemberJoinRequest.builder()
                .nickname("test")
                .email("test@gmail.com")
                .password("1234")
                .build();

        given(memberRepository.findByEmailAndProvider(any(), any())).willReturn(Optional.empty());
        given(memberRepository.save(any())).willReturn(member);

        MemberJoinResponse memberJoinResponse = memberService.join(memberJoinRequest);

        assertAll(
                () -> verify(memberRepository).findByEmailAndProvider(any(), any()),
                () -> verify(memberRepository).save(any()),
                () -> assertThat(memberJoinResponse.getId()).isEqualTo(member.getId())
        );
    }

    @DisplayName("이미 존재하는 회원이어서 회원가입 실패")
    @Test
    void join_fail_Entity_duple(){
        MemberJoinRequest memberJoinRequest = MemberJoinRequest.builder()
                .nickname("test")
                .email("test@gmail.com")
                .password("1234")
                .build();

        given(memberRepository.findByEmailAndProvider(any(), any())).willReturn(Optional.ofNullable(member));

        assertThatThrownBy(() -> memberService.join(memberJoinRequest)).isInstanceOf(EntityExistsException.class);
    }

    @DisplayName("마이페이지 조회")
    @Test
    void findOne(){
        MemberMypageResponse response = memberService.findOne(member);

        assertAll(
            () -> assertThat(response.getNickname()).isEqualTo(member.getNickname()),
            () -> assertThat(response.getEmail()).isEqualTo(member.getEmail())
        );
    }

    @DisplayName("마이페이지 업데이트")
    @Test
    void update() {
        MemberUpdateRequest memberUpdateRequest = MemberUpdateRequest.builder()
                .nickname("test")
                .build();

        memberService.update(member, memberUpdateRequest);

        assertAll(
                () -> assertThat(member.getNickname()).isEqualTo(memberUpdateRequest.getNickname())
        );
    }

    @DisplayName("멤버 삭제")
    @Test
    void delete() {
        memberService.delete(member);

        verify(memberRepository).delete(member);
    }
}