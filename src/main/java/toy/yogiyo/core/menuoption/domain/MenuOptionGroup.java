package toy.yogiyo.core.menuoption.domain;

import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import toy.yogiyo.common.dto.Visible;
import toy.yogiyo.core.menu.domain.Menu;
import toy.yogiyo.core.shop.domain.Shop;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = @Index(name = "idx_shop_id", columnList = "shop_id"))
@DynamicInsert
public class MenuOptionGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_option_group_id")
    private Long id;

    private String name;
    private Integer position;
    private Integer count;
    private Boolean isPossibleCount;

    @Enumerated(EnumType.STRING)
    private OptionType optionType;

    @ColumnDefault("'SHOW'")
    @Enumerated(EnumType.STRING)
    private Visible visible;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Shop shop;

    @Builder.Default
    @BatchSize(size = 100)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "menuOptionGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MenuOption> menuOptions = new ArrayList<>();

    @Builder.Default
    @BatchSize(size = 100)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "menuOptionGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OptionGroupLinkMenu> linkMenus = new ArrayList<>();

    public void createMenuOption(String content, int price) {
        MenuOption menuOption = MenuOption.builder()
                .content(content)
                .price(price)
                .menuOptionGroup(this)
                .build();

        getMenuOptions().add(menuOption);
    }

    public void updateLinkMenus(List<Menu> menus) {
        this.linkMenus.clear();
        menus.forEach(menu -> {
            OptionGroupLinkMenu linkMenu = OptionGroupLinkMenu.builder()
                    .menuOptionGroup(this)
                    .menu(menu)
                    .build();

            linkMenus.add(linkMenu);
        });
    }

    public void updatePosition(int position) {
        this.position = position;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateVisible(Visible visible) {
        this.visible = visible;
    }
}
