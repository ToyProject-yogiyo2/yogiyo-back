package toy.yogiyo.core.shop.domain;

import lombok.*;
import toy.yogiyo.common.converter.StringArrayConverter;
import toy.yogiyo.common.domain.BaseTimeEntity;
import toy.yogiyo.core.review.domain.Review;
import toy.yogiyo.core.category.domain.CategoryShop;
import toy.yogiyo.core.owner.domain.Owner;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shop extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shop_id")
    private Long id;

    private String name;

    private long orderNum;
    private long likeNum;
    private long ownerReplyNum;

    // 리뷰
    private long reviewNum;

    private double tasteScore;
    private double quantityScore;
    private double deliveryScore;
    private double totalScore;

    private String icon;
    private String banner;

    // 공지
    private String noticeTitle;
    private String ownerNotice;
    @Builder.Default
    @Convert(converter = StringArrayConverter.class)
    private List<String> noticeImages = new ArrayList<>();

    private String callNumber;
    private String address;

    private Double longitude;
    private Double latitude;

    private int minDeliveryPrice;
    private int minOrderPrice;
    private int deliveryTime;

    @Builder.Default
    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CategoryShop> categoryShop = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Owner owner;

    @Builder.Default
    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeliveryPriceInfo> deliveryPriceInfos = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BusinessHours> businessHours = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CloseDay> closeDays = new ArrayList<>();


    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    public void updateCallNumber(String callNumber) {
        this.callNumber = callNumber;
    }

    public void updateNotice(String title, String notice, List<String> images) {
        this.noticeTitle = title;
        this.ownerNotice = notice;
        this.noticeImages = images;
    }

    public void updateBusinessHours(List<BusinessHours> businessHours) {
        this.businessHours.clear();
        for (BusinessHours businessHour : businessHours) {
            this.businessHours.add(businessHour);
            businessHour.setShop(this);
        }
    }

    public void changeDeliveryPrices(List<DeliveryPriceInfo> deliveryPriceInfos) {
        if(deliveryPriceInfos != null && !deliveryPriceInfos.isEmpty()) {
            this.minDeliveryPrice = deliveryPriceInfos.get(deliveryPriceInfos.size()-1).getDeliveryPrice();
            this.minOrderPrice = deliveryPriceInfos.get(0).getOrderPrice();
        }
        this.deliveryPriceInfos.clear();
        for (DeliveryPriceInfo deliveryPriceInfo : deliveryPriceInfos) {
            this.deliveryPriceInfos.add(deliveryPriceInfo);
            deliveryPriceInfo.setShop(this);
        }
    }

    public void updateLatLng(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void updateCloseDays(List<CloseDay> closeDays) {
        this.closeDays.clear();
        for (CloseDay closeDay : closeDays) {
            this.closeDays.add(closeDay);
            closeDay.setShop(this);
        }
    }

    public void increaseOrderNum(){
        this.orderNum++;
    }

    public void addReview(Review review) {
        this.reviewNum++;
        this.tasteScore = (this.tasteScore*(this.reviewNum-1)+review.getTasteScore())/reviewNum;
        this.quantityScore = (this.quantityScore*(this.reviewNum-1)+review.getQuantityScore())/reviewNum;
        this.deliveryScore = (this.deliveryScore*(this.reviewNum-1)+review.getTasteScore())/reviewNum;
        this.totalScore = (this.totalScore*(this.reviewNum-1)+review.getTotalScore())/reviewNum;
    }

    public void decreaseLikeNum() {
        this.likeNum--;
    }

    public void increaseLikeNum() {
        this.likeNum++;
    }
}


