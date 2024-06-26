package toy.yogiyo.api.owner;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import toy.yogiyo.common.dto.scroll.Scroll;
import toy.yogiyo.core.review.dto.ReplyRequest;
import toy.yogiyo.core.review.dto.ReviewGetSummaryResponse;
import toy.yogiyo.core.review.dto.ReviewResponse;
import toy.yogiyo.core.review.dto.ReviewQueryCondition;
import toy.yogiyo.core.review.repository.ReviewQueryRepository;
import toy.yogiyo.core.review.service.ReviewManagementService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/owner/review")
public class ReviewManagementController {

    private final ReviewQueryRepository reviewQueryRepository;
    private final ReviewManagementService reviewManagementService;

    @GetMapping("/shop/{shopId}")
    public Scroll<ReviewResponse> getShopReviews(@PathVariable Long shopId,
                                                 @Validated @ModelAttribute ReviewQueryCondition condition) {

        return reviewQueryRepository.shopReviewScroll(shopId, condition);
    }

    @GetMapping("/shop/{shopId}/summary")
    public ReviewGetSummaryResponse getSummary(@PathVariable Long shopId) {
        return reviewQueryRepository.findReviewSummary(shopId);
    }

    @PatchMapping("/{reviewId}/reply")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@reviewManagementPermissionEvaluator.hasPermission(authentication, #reviewId)")
    public void reply(@PathVariable Long reviewId, @RequestBody ReplyRequest request) {
        reviewManagementService.reply(reviewId, request.getReply());
    }

    @DeleteMapping("/{reviewId}/reply")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@reviewManagementPermissionEvaluator.hasPermission(authentication, #reviewId)")
    public void deleteReply(@PathVariable Long reviewId) {
        reviewManagementService.deleteReply(reviewId);
    }

}
