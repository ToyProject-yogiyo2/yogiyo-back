package toy.yogiyo.core.shop.repository;

import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import toy.yogiyo.core.like.dto.LikeResponse;
import toy.yogiyo.core.like.dto.LikeScrollRequest;
import toy.yogiyo.core.shop.dto.scroll.ShopScrollListRequest;
import toy.yogiyo.core.shop.dto.scroll.ShopScrollResponse;

import java.util.List;

import static java.time.LocalTime.now;
import static toy.yogiyo.core.category.domain.QCategory.category;
import static toy.yogiyo.core.category.domain.QCategoryShop.categoryShop;
import static toy.yogiyo.core.like.domain.QLike.like;
import static toy.yogiyo.core.shop.domain.QShop.shop;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ShopCustomRepositoryImpl implements ShopCustomRepository{

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<LikeResponse> scrollLikes(Long memberId, LikeScrollRequest request) {
        return jpaQueryFactory
                .select(Projections.fields(LikeResponse.class,
                        shop.id.as("shopId"),
                        shop.name.as("shopName"),
                        shop.icon.as("shopImg"),
                        shop.totalScore.as("score")
                ))
                .from(shop)
                .join(like).on(shop.id.eq(like.shop.id))
                .where(like.member.id.eq(memberId),
                        lastIdLt(request.getLastId()))
                .orderBy(like.id.desc())
                .limit(request.getLimit()==null ? 6L: request.getLimit()+1)
                .offset(request.getOffset()==null ? 0L : request.getOffset())
                .fetch();
    }

    @Override
    public List<ShopScrollResponse> scrollShopList(ShopScrollListRequest request) {
        OrderSpecifier<?> orderSpecifier = getOrderSpecifier(request.getSortOption().name());
        BooleanExpression cursorLt = getCursorLt(request);

        //subquery로 필터링된 shop_id 먼저 받기
        JPAQuery<Long> subQuery = jpaQueryFactory.select(shop.id)
                .from(shop);

        if(request.getCategory()!=null
            /*&& !request.getCategory().isEmpty()*/
            /*&& !request.getCategory().equals("신규맛집")*/) {
            subQuery.join(categoryShop).on(categoryShop.shop.id.eq(shop.id))
                    .join(category).on(categoryShop.category.id.eq(category.id));
        }

        List<Long> filteredShopId = subQuery.where(
                        categoryNameEq(request.getCategory().getCategoryKoreanName()),
                        deliveryPriceLt(request.getDeliveryPrice()),
                        leastOrderPriceLt(request.getLeastOrderPrice()),
                        isNewShop(request.getCategory().getCategoryKoreanName()),
                        cursorLt
                )
                .orderBy(orderSpecifier)
                .limit(request.getSize() == null ? 11L : request.getSize() + 1)
                .fetch();

        for (Long aLong : filteredShopId) {
            log.info(aLong+" ");
        }
        System.out.println();

        return jpaQueryFactory
                .select(Projections.fields(ShopScrollResponse.class,
                        shop.id.as("shopId"),
                        shop.name.as("shopName"),
                        shop.totalScore,
                        shop.orderNum,
                        shop.reviewNum,
                        getShopDistance(request.getLatitude(), request.getLongitude()).as("distance"),
                        shop.deliveryTime,
                        shop.minDeliveryPrice,
                        shop.maxDeliveryPrice,
                        shop.icon
                ))
                .from(shop)
                .where(shop.id.in(
                            filteredShopId
                        )
                )
                .orderBy(orderSpecifier, shop.id.desc())
                .fetch();
    }

    private BooleanExpression getCursorLt(ShopScrollListRequest request) {
        if(request.getSortOption().name().equals("REVIEW")){
            return request.getCursor()==null ?
                    shop.reviewNum.lt(Long.MAX_VALUE)
                    : shop.reviewNum.eq(request.getCursor().longValue()).and(shop.id.lt(request.getSubCursor())).or(shop.reviewNum.lt(request.getCursor().longValue()));
        }
        else if(request.getSortOption().name().equals("SCORE")){
            return request.getCursor()==null ?
                    shop.totalScore.lt(5.1)
                    : shop.totalScore.eq(request.getCursor()).and(shop.id.lt(request.getSubCursor())).or(shop.totalScore.lt(request.getCursor()));
        }else
        //"CLOSEST" 추가해야함
        return request.getCursor()==null ?
                shop.orderNum.lt(Long.MAX_VALUE)
                : shop.orderNum.eq(request.getCursor().longValue()).and(shop.id.lt(request.getSubCursor())).or(shop.orderNum.lt(request.getCursor().longValue()));
    }

    private OrderSpecifier<?> getOrderSpecifier(String sortOption) {
        if(sortOption.equals("REVIEW")){
            return new OrderSpecifier<>(Order.DESC, shop.reviewNum);
        }
        else if(sortOption.equals("SCORE")){
            return new OrderSpecifier<>(Order.DESC, shop.totalScore);
        }
        //"CLOSEST" 추가해야함
        return new OrderSpecifier<>(Order.DESC, shop.orderNum);
    }

    private BooleanExpression isNewShop(String category) {
        if(category == null || category.isEmpty()) return null;
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
        return leastOrderPrice == null ? null : shop.minOrderPrice.loe(leastOrderPrice);
    }

    private BooleanExpression deliveryPriceLt(Integer deliveryPrice) {
        return deliveryPrice == null ? null : shop.minDeliveryPrice.loe(deliveryPrice);
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

    private BooleanExpression categoryNameEq(String categoryName){
        if(categoryName == null) return null;
        return categoryName.isEmpty() | categoryName.equals("신규맛집")  ? null : category.name.eq(categoryName);
    }

    private static BooleanExpression lastIdLt(Long lastId) {
        return lastId == null ? null : shop.id.lt(lastId);
    }
}
