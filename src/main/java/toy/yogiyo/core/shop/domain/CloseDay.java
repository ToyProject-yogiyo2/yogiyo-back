package toy.yogiyo.core.shop.domain;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CloseDay {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "close_day_Id")
    private Long id;
    private Integer weekNumOfMonth;
    @Enumerated(EnumType.STRING)
    private Days dayOfWeek;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Shop shop;

    public void setShop(Shop shop) {
        this.shop = shop;
    }

}
