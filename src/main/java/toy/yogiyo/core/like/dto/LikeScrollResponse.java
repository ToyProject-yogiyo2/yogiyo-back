package toy.yogiyo.core.like.dto;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LikeScrollResponse {

    private List<LikeResponse> likeResponses;
    private Long lastId;
    private boolean hasNext;
}
