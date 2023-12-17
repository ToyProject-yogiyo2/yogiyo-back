package toy.yogiyo.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import toy.yogiyo.common.exception.AuthenticationException;
import toy.yogiyo.common.exception.ErrorCode;
import toy.yogiyo.common.login.LoginOwner;
import toy.yogiyo.common.login.LoginUser;
import toy.yogiyo.common.login.UserType;
import toy.yogiyo.common.login.dto.LoginRequest;
import toy.yogiyo.common.login.dto.LoginResponse;
import toy.yogiyo.common.login.service.LoginService;
import toy.yogiyo.common.security.jwt.JwtProvider;
import toy.yogiyo.core.member.domain.Member;
import toy.yogiyo.core.owner.domain.Owner;

@RestController
@RequiredArgsConstructor
public class LoginController {

    private final String BEARER = "Bearer ";
    private final LoginService loginService;
    private final JwtProvider jwtProvider;

    @PostMapping("/memberLogin")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<LoginResponse> memberLogin(@RequestBody LoginRequest loginRequest){

        LoginResponse loginResponse = loginService.memberLogin(loginRequest);
        String accessToken = jwtProvider.createToken(loginResponse.getEmail(), loginRequest.getProviderType(), UserType.Member);

        //Authorization Header
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, createCookie(accessToken).toString());
        return new ResponseEntity<>(loginResponse, headers, HttpStatus.OK);
    }

    @PostMapping("/memberLogout/{memberId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> memberLogout(@LoginUser Member member, @PathVariable Long memberId){
        if(member.getId()!=memberId) throw new AuthenticationException(ErrorCode.MEMBER_UNAUTHORIZATION);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, deleteCookie().toString());
        return new ResponseEntity<>("멤버 로그아웃 완료", headers, HttpStatus.OK);
    }

    @PostMapping("/ownerLogin")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<LoginResponse> ownerLogin(@RequestBody LoginRequest loginRequest){

        LoginResponse loginResponse = loginService.ownerLogin(loginRequest);
        String accessToken = jwtProvider.createToken(loginResponse.getEmail(), loginRequest.getProviderType(), UserType.Owner);

        //Authorization Header
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, createCookie(accessToken).toString());
        return new ResponseEntity<>(loginResponse, headers, HttpStatus.OK);
    }

    @PostMapping("/ownerLogout/{ownerId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> ownerLogout(@LoginOwner Owner owner, @PathVariable Long ownerId){
        if(owner.getId()!=ownerId) throw new AuthenticationException(ErrorCode.MEMBER_UNAUTHORIZATION);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, deleteCookie().toString());
        return new ResponseEntity<>("점주 로그아웃 완료", headers, HttpStatus.OK);
    }

    private static ResponseCookie deleteCookie() {
        return ResponseCookie.from("accessToken", null)
                .maxAge(0)
                .path("/")
                .build();
    }

    private static ResponseCookie createCookie(String accessToken) {
        return ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .build();
    }
}
