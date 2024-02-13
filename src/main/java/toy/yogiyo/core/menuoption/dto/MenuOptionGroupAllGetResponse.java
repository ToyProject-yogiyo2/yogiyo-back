package toy.yogiyo.core.menuoption.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import toy.yogiyo.common.dto.Visible;
import toy.yogiyo.core.menuoption.domain.MenuOption;
import toy.yogiyo.core.menuoption.domain.MenuOptionGroup;
import toy.yogiyo.core.menuoption.domain.OptionType;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class MenuOptionGroupAllGetResponse {

    private List<MenuOptionGroupDto> menuOptionGroups;

    public static MenuOptionGroupAllGetResponse from(List<MenuOptionGroup> menuOptionGroups) {
        return MenuOptionGroupAllGetResponse.builder()
                .menuOptionGroups(menuOptionGroups.stream()
                        .map(MenuOptionGroupDto::new)
                        .collect(Collectors.toList()))
                .build();

    }

    @Getter
    private static class MenuOptionGroupDto {
        private Long id;
        private String name;
        private Integer position;
        private Integer count;
        private boolean isPossibleCount;
        private OptionType optionType;
        private Visible visible;
        private List<MenuOptionDto> menuOptions;
        private List<String> menus;

        public MenuOptionGroupDto(MenuOptionGroup menuOptionGroup) {
            this.id = menuOptionGroup.getId();
            this.name = menuOptionGroup.getName();
            this.position = menuOptionGroup.getPosition();
            this.count = menuOptionGroup.getCount();
            this.isPossibleCount = menuOptionGroup.getIsPossibleCount();
            this.optionType = menuOptionGroup.getOptionType();
            this.visible = menuOptionGroup.getVisible();
            this.menuOptions = menuOptionGroup.getMenuOptions().stream()
                    .map(MenuOptionDto::new)
                    .collect(Collectors.toList());
            this.menus = menuOptionGroup.getLinkMenus().stream()
                    .map(menu -> menu.getMenu().getName())
                    .collect(Collectors.toList());
        }

        @JsonProperty("isPossibleCount")
        public boolean isPossibleCount() {
            return isPossibleCount;
        }
    }

    @Getter
    private static class MenuOptionDto {

        private Long id;
        private String content;
        private Integer price;
        private Integer position;
        private Visible visible;

        public MenuOptionDto(MenuOption menuOption) {
            this.id = menuOption.getId();
            this.content = menuOption.getContent();
            this.price = menuOption.getPrice();
            this.position = menuOption.getPosition();
            this.visible = menuOption.getVisible();
        }

    }
}
