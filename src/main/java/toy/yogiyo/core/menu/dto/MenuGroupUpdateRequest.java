package toy.yogiyo.core.menu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import toy.yogiyo.core.menu.domain.MenuGroup;

import javax.validation.constraints.NotBlank;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuGroupUpdateRequest {

    @NotBlank
    private String name;
    @NotBlank
    private String content;

    public MenuGroup toMenuGroup() {
        return MenuGroup.builder()
                .name(name)
                .content(content)
                .build();
    }

    public MenuGroup toMenuGroup(Long menuGroupId) {
        return MenuGroup.builder()
                .id(menuGroupId)
                .name(name)
                .content(content)
                .build();
    }
}
