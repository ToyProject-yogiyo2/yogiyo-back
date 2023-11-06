package toy.yogiyo.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import toy.yogiyo.core.category.domain.Category;
import toy.yogiyo.core.category.domain.CategoryShop;
import toy.yogiyo.core.category.dto.CategoryCreateRequest;
import toy.yogiyo.core.category.dto.CategoryResponse;
import toy.yogiyo.core.category.dto.CategoryShopCondition;
import toy.yogiyo.core.category.dto.CategoryShopResponse;
import toy.yogiyo.core.category.service.CategoryService;
import toy.yogiyo.core.category.service.CategoryShopService;
import toy.yogiyo.core.shop.domain.DeliveryPriceInfo;
import toy.yogiyo.core.shop.domain.Shop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @MockBean
    CategoryService categoryService;

    @MockBean
    CategoryShopService categoryShopService;

    @Autowired
    MockMvc mockMvc;

    ObjectMapper objectMapper;

    @BeforeEach
    void beforeEach(WebApplicationContext context) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("카테고리 추가")
    void create() throws Exception {
        // given
        CategoryCreateRequest request = givenCreateRequest();
        when(categoryService.createCategory(any())).thenReturn(1L);

        // when
        ResultActions result = mockMvc.perform(
                multipart("/category/create")
                        .param("name", request.getName()));

        // then
        result.andExpect(status().isOk())
                .andExpect(content().string("1"))
                .andDo(print());
        verify(categoryService).createCategory(any());
    }

    @Test
    @DisplayName("카테고리 단건 조회")
    void findOne() throws Exception {
        // given
        Category category = givenCategory();
        when(categoryService.getCategory(category.getId())).thenReturn(category);


        // when
        ResultActions result = mockMvc.perform(
                get("/category/{categoryId}", category.getId()));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(category.getId()))
                .andExpect(jsonPath("$.name").value(category.getName()))
                .andDo(print());
        verify(categoryService).getCategory(category.getId());
    }

    @Test
    @DisplayName("모든 카테고리 조회")
    void findAll() throws Exception {
        // given
        List<CategoryResponse> categoryResponses = givenCategories().stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
        when(categoryService.getCategories()).thenReturn(categoryResponses);

        // when
        ResultActions result = mockMvc.perform(
                get("/category/all"));

        // then
        for (int i = 0; i < categoryResponses.size(); i++) {
            result.andExpect(jsonPath("$.[%s].id", i).value(categoryResponses.get(i).getId()));
            result.andExpect(jsonPath("$.[%s].name", i).value(categoryResponses.get(i).getName()));
        }
        result.andDo(print());
        verify(categoryService).getCategories();
    }

    @Test
    @DisplayName("카테고리 수정")
    void update() throws Exception {
        // given
        doNothing().when(categoryService).update(anyLong(), any());

        // when
        ResultActions result = mockMvc.perform(
                patch("/category/{categoryId}", 1L));

        // then
        result.andExpect(status().isOk())
                .andExpect(content().string("success"))
                .andDo(print());
        verify(categoryService).update(anyLong(), any());
    }

    @Test
    @DisplayName("카테고리 삭제")
    void deleteCategory() throws Exception {
        // given
        doNothing().when(categoryService).delete(anyLong());

        // when
        ResultActions result = mockMvc.perform(delete("/category/{categoryId}", 1L));

        // then
        result.andExpect(status().isOk())
                .andExpect(content().string("success"))
                .andDo(print());
        verify(categoryService).delete(anyLong());
    }

    @Test
    @DisplayName("카테고리 상점 조회")
    void findAroundShop() throws Exception {
        // given
        CategoryShopCondition condition = new CategoryShopCondition(36.6732, 127.4491, null);

        List<CategoryShopResponse> categoryShopResponses = new ArrayList<>();
        Shop shop = givenShop();
        Category category = givenCategory();
        for (int i = 0; i < 10; i++) {
            categoryShopResponses.add(CategoryShopResponse.from(
                    new CategoryShop((long) i, category, shop), 168));
        }

        PageRequest pageRequest = PageRequest.of(0, 10);
        when(categoryShopService.findShop(anyLong(), any(), any()))
                .thenReturn(new SliceImpl<>(categoryShopResponses, pageRequest, false));

        // when
        ResultActions result = mockMvc.perform(
                get("/category/{categoryId}/shop", 1L)
                        .param("page", String.valueOf(pageRequest.getPageNumber()))
                        .param("size", String.valueOf(pageRequest.getPageSize())) // 기본값 : 20

                        .param("latitude", String.valueOf(condition.getLatitude()))
                        .param("longitude", String.valueOf(condition.getLongitude())));

        // then
        for (int i = 0; i < 10; i++) {
            result.andExpect(jsonPath("$.content[%s].name", i).value(categoryShopResponses.get(i).getName()));
            result.andExpect(jsonPath("$.content[%s].icon", i).value(categoryShopResponses.get(i).getIcon()));
            result.andExpect(jsonPath("$.content[%s].distance", i).value(categoryShopResponses.get(i).getDistance()));
            result.andExpect(jsonPath("$.content[%s].stars", i).value(categoryShopResponses.get(i).getStars()));
            result.andExpect(jsonPath("$.content[%s].reviewNum", i).value(categoryShopResponses.get(i).getReviewNum()));
            result.andExpect(jsonPath("$.content[%s].deliveryTime", i).value(categoryShopResponses.get(i).getDeliveryTime()));
            result.andExpect(jsonPath("$.content[%s].deliveryPriceInfos", i).isArray());
        }

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(pageRequest.getPageSize()))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.numberOfElements").value(10))
                .andDo(print());
    }

    private Category givenCategory() {
        return new Category(1L, "분식");
    }


    private List<Category> givenCategories() {
        return List.of(
                new Category(1L, "치킨"),
                new Category(2L, "한식"),
                new Category(3L, "중국집"),
                new Category(4L, "버거"),
                new Category(5L, "피자/양식"),
                new Category(6L, "분식"),
                new Category(7L, "족발/보쌈"),
                new Category(8L, "카페/디저트"),
                new Category(9L, "일식/돈까스"),
                new Category(10L, "찜/탕")
        );
    }

    private CategoryCreateRequest givenCreateRequest() {
        CategoryCreateRequest createRequest = new CategoryCreateRequest();
        createRequest.setName("치킨");

        return createRequest;
    }

    private Shop givenShop() {
        Shop shop = Shop.builder()
                .name("롯데리아")
                .icon("692c0741-f234-448e-ba3f-35b5a394f33d.png")
                .banner("692c0741-f234-448e-ba3f-35b5a394f33d.png")
                .ownerNotice("사장님 공지")
                .callNumber("010-1234-5678")
                .address("서울 강남구 영동대로 513")
                .deliveryTime(30)
                .build();

        shop.changeDeliveryPrices(Arrays.asList(
                new DeliveryPriceInfo(10000, 5000),
                new DeliveryPriceInfo(20000, 4000),
                new DeliveryPriceInfo(30000, 3000)));

        shop.updateLatLng(36.674648, 127.448544);

        return shop;
    }
}