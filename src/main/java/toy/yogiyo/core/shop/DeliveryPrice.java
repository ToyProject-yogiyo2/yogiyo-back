package toy.yogiyo.core.shop;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
public class DeliveryPrice {

    @Id
    @GeneratedValue
    @Column(name = "delivery_price_info_id")
    private Long id;

    private int orderPrice;
    private int deliveryPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    private Shop shop;

    public DeliveryPrice() {
    }

    public DeliveryPrice(int orderPrice, int deliveryPrice) {
        this.orderPrice = orderPrice;
        this.deliveryPrice = deliveryPrice;
    }

    public void setShop(Shop shop) {
        this.shop = shop;
    }

}
