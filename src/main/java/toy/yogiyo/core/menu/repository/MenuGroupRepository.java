package toy.yogiyo.core.menu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import toy.yogiyo.core.menu.domain.MenuGroup;

import java.util.List;


public interface MenuGroupRepository extends JpaRepository<MenuGroup, Long> {

    @Query("select mg from MenuGroup mg where mg.shop.id = :shopId order by mg.position")
    List<MenuGroup> findAllByShopId(@Param("shopId") Long shopId);

    @Query("select max(mg.position) from MenuGroup mg where mg.shop.id = :shopId")
    Integer findMaxOrder(@Param("shopId") Long shopId);

}
