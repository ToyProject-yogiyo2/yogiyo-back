package toy.yogiyo.core.menuoption.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import toy.yogiyo.core.menuoption.domain.MenuOption;

import java.util.List;

public interface MenuOptionRepository extends JpaRepository<MenuOption, Long> {

    @Query("select max(mo.position) from MenuOption mo where mo.menuOptionGroup.id = :menuOptionGroupId")
    Integer findMaxOrder(@Param("menuOptionGroupId") Long menuOptionGroupId);

    @Query("select o from MenuOption o where o.menuOptionGroup.id = :menuOptionGroupId order by o.position asc")
    List<MenuOption> findAllByMenuOptionGroupId(@Param("menuOptionGroupId") Long menuOptionGroupId);

    @Modifying
    @Query("delete MenuOption mo where mo.menuOptionGroup.id = :menuOptionGroupId")
    int deleteAllByGroupId(@Param("menuOptionGroupId") Long menuOptionGroupId);
}
