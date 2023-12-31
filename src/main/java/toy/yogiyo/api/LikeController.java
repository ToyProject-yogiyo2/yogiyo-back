package toy.yogiyo.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import toy.yogiyo.common.dto.scroll.Scroll;
import toy.yogiyo.common.login.LoginUser;
import toy.yogiyo.core.like.dto.LikeResponse;
import toy.yogiyo.core.like.dto.LikeScrollRequest;
import toy.yogiyo.core.like.service.LikeService;
import toy.yogiyo.core.member.domain.Member;
import toy.yogiyo.core.shop.domain.Shop;

@RestController
@RequiredArgsConstructor
@RequestMapping("/like")
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/{shopId}")
    @ResponseStatus(HttpStatus.OK)
    public void toggleLike(@LoginUser Member member, @PathVariable Long shopId) {
        likeService.toggleLike(member, shopId);
    }

    @GetMapping("/scroll")
    @ResponseStatus(HttpStatus.OK)
    public Scroll<LikeResponse> getLikes(@LoginUser Member member, @ModelAttribute LikeScrollRequest request){
        return likeService.getLikes(member, request);
    }
}
