package toy.yogiyo.api;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import toy.yogiyo.common.dto.scroll.Scroll;
import toy.yogiyo.core.review.domain.Review;
import toy.yogiyo.core.review.dto.ReplyRequest;
import toy.yogiyo.core.review.dto.ReviewManagementResponse;
import toy.yogiyo.core.review.dto.ReviewQueryCondition;
import toy.yogiyo.core.review.repository.ReviewQueryRepository;
import toy.yogiyo.core.review.service.ReviewManagementService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/management/review")
public class ReviewManagementController {

    private final ReviewQueryRepository reviewQueryRepository;
    private final ReviewManagementService reviewManagementService;

    @GetMapping("/shop/{shopId}")
    public Scroll<ReviewManagementResponse> getShopReviews(@PathVariable Long shopId,
                                                           @ModelAttribute ReviewQueryCondition condition) {

        Scroll<Review> reviews = reviewQueryRepository.shopReviewScroll(shopId, condition);
        return reviews.map(ReviewManagementResponse::from);
    }

    @PatchMapping("/{reviewId}/reply")
    @PreAuthorize("@reviewManagementPermissionEvaluator.hasPermission(authentication, #reviewId)")
    public String reply(@PathVariable Long reviewId, @RequestBody ReplyRequest request) {
        reviewManagementService.reply(reviewId, request.getReply());
        return "success";
    }

    @DeleteMapping("/{reviewId}/reply")
    @PreAuthorize("@reviewManagementPermissionEvaluator.hasPermission(authentication, #reviewId)")
    public String deleteReply(@PathVariable Long reviewId) {
        reviewManagementService.deleteReply(reviewId);
        return "success";
    }

}
