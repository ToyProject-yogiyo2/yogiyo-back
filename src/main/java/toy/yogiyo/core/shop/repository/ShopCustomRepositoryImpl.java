package toy.yogiyo.core.shop.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.group.GroupBy;
import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import toy.yogiyo.core.deliveryplace.domain.QDeliveryPlace;
import toy.yogiyo.core.deliveryplace.domain.QDeliveryPriceInfo;
import toy.yogiyo.core.like.dto.LikeResponse;
import toy.yogiyo.core.like.dto.LikeScrollRequest;
import toy.yogiyo.core.order.domain.QOrder;
import toy.yogiyo.core.shop.domain.QBusinessHours;
import toy.yogiyo.core.shop.dto.ShopDetailsRequest;
import toy.yogiyo.core.shop.dto.ShopDetailsResponse;
import toy.yogiyo.core.shop.dto.ShopRecentRequest;
import toy.yogiyo.core.shop.dto.member.ShopInfoResponse;
import toy.yogiyo.core.shop.dto.scroll.ShopScrollListRequest;
import toy.yogiyo.core.shop.dto.scroll.ShopScrollResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.LocalTime.now;
import static toy.yogiyo.core.category.domain.QCategory.category;
import static toy.yogiyo.core.category.domain.QCategoryShop.categoryShop;
import static toy.yogiyo.core.deliveryplace.domain.QDeliveryPlace.deliveryPlace;
import static toy.yogiyo.core.deliveryplace.domain.QDeliveryPriceInfo.deliveryPriceInfo;
import static toy.yogiyo.core.like.domain.QLike.like;
import static toy.yogiyo.core.order.domain.QOrder.*;
import static toy.yogiyo.core.shop.domain.QBusinessHours.businessHours;
import static toy.yogiyo.core.shop.domain.QShop.shop;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ShopCustomRepositoryImpl implements ShopCustomRepository {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public ShopInfoResponse info(Long shopId, String code) {
        ShopInfoResponse result = jpaQueryFactory.from(shop)
                .leftJoin(deliveryPlace).on(deliveryPlace.shop.id.eq(shop.id).and(deliveryPlace.code.eq(code)))
                .leftJoin(deliveryPriceInfo).on(deliveryPriceInfo.deliveryPlace.id.eq(deliveryPlace.id))
                .where(shop.id.eq(shopId))
                .transform(GroupBy.groupBy(shop.id).list(
                        Projections.fields(ShopInfoResponse.class,
                                shop.id,
                                shop.name,
                                shop.noticeTitle,
                                shop.ownerNotice,
                                shop.noticeImages,
                                shop.callNumber,
                                shop.address,
                                GroupBy.list(Projections.fields(ShopInfoResponse.DeliveryPriceInfoDto.class,
                                        deliveryPriceInfo.id,
                                        deliveryPriceInfo.deliveryPrice,
                                        deliveryPriceInfo.orderPrice
                                )).as("deliveryPriceInfos")
                        )
                )).get(0);

        List<ShopInfoResponse.BusinessHoursDto> bh = jpaQueryFactory.select(Projections.fields(ShopInfoResponse.BusinessHoursDto.class,
                        businessHours.id,
                        businessHours.breakTimeStart,
                        businessHours.breakTimeEnd,
                        businessHours.openTime,
                        businessHours.closeTime,
                        businessHours.dayOfWeek,
                        businessHours.isOpen))
                .from(businessHours)
                .where(businessHours.shop.id.eq(shopId))
                .fetch();

        result.setBusinessHours(bh);
        result.setDeliveryPriceInfos(result.getDeliveryPriceInfos().stream()
                .filter(d -> d.getId() != null)
                .collect(Collectors.toList()));

        return result;
    }

    @Override
    public ShopDetailsResponse details(Long memberId, ShopDetailsRequest request) {
        ShopDetailsResponse shopDetails = jpaQueryFactory.select(Projections.fields(ShopDetailsResponse.class,
                        shop.id,
                        shop.name,
                        shop.reviewNum,
                        shop.likeNum,
                        shop.totalScore,
                        shop.banner,
                        shop.noticeTitle,
                        getShopDistance(request.getLatitude(), request.getLongitude()).as("distance"),
                        deliveryPlace.minOrderPrice,
                        deliveryPlace.minDeliveryPrice,
                        deliveryPlace.deliveryTime,
                        ExpressionUtils.as(JPAExpressions
                                .selectOne()
                                .from(like)
                                .where(like.shop.id.eq(request.getShopId())
                                        .and(like.member.id.eq(memberId)))
                                .exists(), "isLike")
                ))
                .from(shop)
                .leftJoin(deliveryPlace).on(shop.id.eq(deliveryPlace.shop.id).and(deliveryPlace.code.eq(request.getCode())))
                .where(shop.id.eq(request.getShopId()))
                .fetchOne();

        if (shopDetails != null) {
            shopDetails.setAvailableDelivery(shopDetails.getDeliveryTime() != null);
        }

        return shopDetails;
    }

    public List<ShopScrollResponse> recentOrder(Long memberId, ShopRecentRequest request){

        return jpaQueryFactory.selectDistinct(Projections.fields(ShopScrollResponse.class,
                        shop.id.as("shopId"),
                        shop.name.as("shopName"),
                        shop.totalScore,
                        shop.orderNum,
                        shop.reviewNum,
                        getShopDistance(request.getLatitude(), request.getLongitude()).as("distance"),
                        deliveryPlace.deliveryTime,
                        deliveryPlace.minDeliveryPrice,
                        deliveryPlace.maxDeliveryPrice,
                        shop.icon
                ))
                .from(shop)
                .join(order).on(order.shop.id.eq(shop.id))
                .leftJoin(deliveryPlace).on(shop.id.eq(deliveryPlace.shop.id).and(deliveryPlace.code.eq(request.getCode())))
                .where(order.member.id.eq(memberId))
                .orderBy(order.id.desc())
                .limit(20)
                .fetch();
    }

    @Override
    public List<LikeResponse> scrollLikes(Long memberId, LikeScrollRequest request) {
        return jpaQueryFactory
                .select(Projections.fields(LikeResponse.class,
                        like.id.as("likeId"),
                        shop.id.as("shopId"),
                        shop.name.as("shopName"),
                        shop.icon.as("shopImg"),
                        shop.totalScore.as("score")
                ))
                .from(shop)
                .join(like).on(shop.id.eq(like.shop.id))
                .where(like.member.id.eq(memberId),
                        lastIdLt(request.getOffset()))
                .orderBy(like.id.desc())
                .limit(request.getLimit() == null ? 6L : request.getLimit() + 1)
                .fetch();
    }

    @Override
    public List<ShopScrollResponse> scrollShopList(ShopScrollListRequest request) {
        OrderSpecifier<?> orderSpecifier = getOrderSpecifier(request);
        BooleanExpression cursorLt = getCursorLt(request);

        JPAQuery<ShopScrollResponse> query = jpaQueryFactory.select(Projections.fields(ShopScrollResponse.class,
                        shop.id.as("shopId"),
                        shop.name.as("shopName"),
                        shop.totalScore,
                        shop.orderNum,
                        shop.reviewNum,
                        getShopDistance(request.getLatitude(), request.getLongitude()).as("distance"),
                        deliveryPlace.deliveryTime,
                        deliveryPlace.minDeliveryPrice,
                        deliveryPlace.maxDeliveryPrice,
                        shop.icon
                ))
                .from(shop)
                .join(deliveryPlace).on(deliveryPlace.shop.id.eq(shop.id));

        if (!request.getCategory().getCategoryKoreanName().equals("전체")
                && !request.getCategory().getCategoryKoreanName().equals("신규맛집")) {
            query.join(categoryShop).on(categoryShop.shop.id.eq(shop.id))
                    .join(category).on(categoryShop.category.id.eq(category.id));
        }

        return query
                .where(
                        codeEq(request.getCode()),
                        categoryNameEq(request.getCategory().getCategoryKoreanName()),
                        deliveryPriceLt(request.getDeliveryPrice()),
                        leastOrderPriceLt(request.getLeastOrderPrice()),
                        isNewShop(request.getCategory().getCategoryKoreanName()),
                        cursorLt,
                        nameEq(request.getName())
                )
                .orderBy(orderSpecifier)
                .limit(request.getSize() == null ? 11L : request.getSize() + 1)
                .fetch();
    }

    private BooleanExpression nameEq(String name) {
        return name==null || name.isEmpty() ? null : shop.name.contains(name);
    }

    private BooleanExpression getCursorLt(ShopScrollListRequest request) {
        switch (request.getSortOption()) {
            case REVIEW:
                return request.getCursor() == null ?
                        shop.reviewNum.lt(Long.MAX_VALUE)
                        : shop.reviewNum.eq(request.getCursor().longValue()).and(shop.id.lt(request.getSubCursor())).or(shop.reviewNum.lt(request.getCursor().longValue()));
            case SCORE:
                return request.getCursor() == null ?
                        shop.totalScore.lt(5.1)
                        : shop.totalScore.eq(request.getCursor()).and(shop.id.lt(request.getSubCursor())).or(shop.totalScore.lt(request.getCursor()));
            case CLOSEST:
                return request.getCursor() == null ?
                        getShopDistance(request.getLatitude(), request.getLongitude()).gt(0)
                        : getShopDistance(request.getLatitude(), request.getLongitude()).gt(request.getCursor());
            default:
                return request.getCursor() == null ?
                        shop.orderNum.lt(Long.MAX_VALUE)
                        : shop.orderNum.eq(request.getCursor().longValue()).and(shop.id.lt(request.getSubCursor())).or(shop.orderNum.lt(request.getCursor().longValue()));
        }
//        if(request.getSortOption().name().equals("REVIEW")){
//            return request.getCursor()==null ?
//                    shop.reviewNum.lt(Long.MAX_VALUE)
//                    : shop.reviewNum.eq(request.getCursor().longValue()).and(shop.id.lt(request.getSubCursor())).or(shop.reviewNum.lt(request.getCursor().longValue()));
//        }
//        else if(request.getSortOption().name().equals("SCORE")){
//            return request.getCursor()==null ?
//                    shop.totalScore.lt(5.1)
//                    : shop.totalScore.eq(request.getCursor()).and(shop.id.lt(request.getSubCursor())).or(shop.totalScore.lt(request.getCursor()));
//        }else
//        //"CLOSEST" 추가해야함
//        return request.getCursor()==null ?
//                shop.orderNum.lt(Long.MAX_VALUE)
//                : shop.orderNum.eq(request.getCursor().longValue()).and(shop.id.lt(request.getSubCursor())).or(shop.orderNum.lt(request.getCursor().longValue()));
    }

    private OrderSpecifier<?> getOrderSpecifier(ShopScrollListRequest request) {
        switch (request.getSortOption()) {
            case REVIEW:
                return new OrderSpecifier<>(Order.DESC, shop.reviewNum);
            case SCORE:
                return new OrderSpecifier<>(Order.DESC, shop.totalScore);
            case CLOSEST:
                return new OrderSpecifier<>(Order.ASC, getShopDistance(request.getLatitude(), request.getLongitude()));
            default:
                return new OrderSpecifier<>(Order.DESC, shop.orderNum);
        }
//        if(sortOption.equals("REVIEW")){
//            return new OrderSpecifier<>(Order.DESC, shop.reviewNum);
//        }
//        else if(sortOption.equals("SCORE")){
//            return new OrderSpecifier<>(Order.DESC, shop.totalScore);
//        }
//        //"CLOSEST" 추가해야함
//        return new OrderSpecifier<>(Order.DESC, shop.orderNum);
    }

    private BooleanExpression isNewShop(String category) {
        if (category == null || category.isEmpty()) return null;
        return category.equals("신규맛집") ? getNewShop().loe(999) : null;
    }

    private NumberTemplate<Integer> getNewShop() {
        return Expressions.numberTemplate(
                Integer.class,
                "datediff({0}, {1})",
                Expressions.currentTimestamp(), shop.createdAt
        );
    }

    private BooleanExpression leastOrderPriceLt(Integer leastOrderPrice) {
        return leastOrderPrice == null ? null : deliveryPlace.minOrderPrice.loe(leastOrderPrice);
    }

    private BooleanExpression deliveryPriceLt(Integer deliveryPrice) {
        return deliveryPrice == null ? null : deliveryPlace.minDeliveryPrice.loe(deliveryPrice);
    }


    private OrderSpecifier<?> sortByClosest(Double latitude, Double longitude) {
        return new OrderSpecifier<>(Order.ASC, getShopDistance(latitude, longitude));
    }

    /*private BooleanExpression shopDistanceLt(Double latitude, Double longitude, Integer meter) {
        if (latitude == null || longitude == null || meter == null) {
            return null;
        }

        return getShopDistance(latitude, longitude).lt(meter);
    }*/

    private NumberTemplate<Double> getShopDistance(Double latitude, Double longitude) {
        return Expressions.numberTemplate(
                Double.class,
                "st_distance_sphere({0}, {1})",
                Expressions.stringTemplate("point({0}, {1})", shop.longitude, shop.latitude),
                Expressions.stringTemplate("point({0}, {1})", longitude, latitude)
        );
    }

    private BooleanExpression categoryNameEq(String categoryName) {
        if (categoryName == null) return null;
        return categoryName.equals("전체") | categoryName.equals("신규맛집") ? null : category.name.eq(categoryName);
    }

    private static BooleanExpression lastIdLt(Long lastId) {
        return lastId == null ? null : like.id.lt(lastId);
    }

    private BooleanExpression codeEq(String code) {
        if (code == null) return null;
        return deliveryPlace.code.eq(code);
    }
}
