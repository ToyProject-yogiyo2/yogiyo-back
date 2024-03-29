package toy.yogiyo.api.owner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import toy.yogiyo.common.security.WithLoginOwner;
import toy.yogiyo.core.category.domain.Category;
import toy.yogiyo.core.category.domain.CategoryShop;
import toy.yogiyo.core.shop.domain.BusinessHours;
import toy.yogiyo.core.shop.domain.CloseDay;
import toy.yogiyo.core.shop.domain.Days;
import toy.yogiyo.core.shop.domain.Shop;
import toy.yogiyo.core.shop.dto.*;
import toy.yogiyo.core.shop.service.ShopService;
import toy.yogiyo.util.ConstrainedFields;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static toy.yogiyo.document.utils.DocumentLinkGenerator.DocUrl.DAYS;
import static toy.yogiyo.document.utils.DocumentLinkGenerator.generateLinkCode;

@WebMvcTest(ShopController.class)
@ExtendWith(RestDocumentationExtension.class)
class ShopControllerTest {

    @MockBean
    ShopService shopService;

    @Autowired
    MockMvc mockMvc;

    ObjectMapper objectMapper;

    final String jwt = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGdtYWlsLmNvbSIsInByb3ZpZGVyVHlwZSI6IkRFRkFVTFQiLCJleHAiOjE2OTQ5NjY4Mjh9.Ls1wnxU41I99ijXRyKfkYI2w3kd-Q_qA2QgCLgpDTKk";

    @BeforeEach
    void beforeEach(WebApplicationContext context, RestDocumentationContextProvider restDocumentationContextProvider) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentationContextProvider)
                        .operationPreprocessors()
                        .withRequestDefaults(prettyPrint())
                        .withResponseDefaults(prettyPrint()))
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }


    @Test
    @DisplayName("가게 입점")
    @WithLoginOwner
    void register() throws Exception {
        // given
        ShopRegisterRequest registerRequest = ShopRegisterRequest.builder()
                .name("롯데리아")
                .callNumber("010-1234-5678")
                .address("서울 강남구 영동대로 513")
                .latitude(36.674648)
                .longitude(127.448544)
                .categories(Arrays.asList("치킨", "한식", "중국집"))
                .build();

        MockMultipartFile requestJson = new MockMultipartFile(
                "shopData",
                "jsonData",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsString(registerRequest).getBytes());
        MockMultipartFile icon = givenIcon();
        MockMultipartFile banner = givenBanner();
        when(shopService.register(any(), any(), any(), any())).thenReturn(1L);

        // when
        ResultActions result = mockMvc.perform(multipart("/owner/shop/register")
                .file(icon)
                .file(banner)
                .file(requestJson)
                .header(HttpHeaders.AUTHORIZATION, jwt)
                .contentType(MediaType.MULTIPART_FORM_DATA));

        // then
        ConstrainedFields fields = new ConstrainedFields(ShopRegisterRequest.class);
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andDo(print())
                .andDo(document("shop/register",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Access token")
                        ),
                        requestParts(
                                partWithName("icon").description("아이콘"),
                                partWithName("banner").description("배너 이미지"),
                                partWithName("shopData").description("가게 정보")
                        ),
                        requestPartFields("shopData",
                                fields.withPath("name").type(JsonFieldType.STRING).description("가게명"),
                                fields.withPath("callNumber").type(JsonFieldType.STRING).description("전화번호"),
                                fields.withPath("address").type(JsonFieldType.STRING).description("주소"),
                                fields.withPath("latitude").type(JsonFieldType.NUMBER).description("위도"),
                                fields.withPath("longitude").type(JsonFieldType.NUMBER).description("경도"),
                                fields.withPath("categories").type(JsonFieldType.ARRAY).description("카테고리 Array")
                        ),
                        responseFields(
                                fieldWithPath("id").type(JsonFieldType.NUMBER).description("가게 ID")
                        )
                ));
    }


    @Test
    @DisplayName("가게 정보 조회")
    void getInfo() throws Exception {
        // given
        ShopInfoResponse response = ShopInfoResponse.from(givenShop());
        when(shopService.getInfo(anyLong())).thenReturn(response);

        // when
        ResultActions result = mockMvc.perform(RestDocumentationRequestBuilders.get("/owner/shop/{shopId}/info", 1L));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.getId()))
                .andExpect(jsonPath("$.name").value(response.getName()))
                .andExpect(jsonPath("$.callNumber").value(response.getCallNumber()))
                .andExpect(jsonPath("$.address").value(response.getAddress()))
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.categories.length()").value(response.getCategories().size()))
                .andDo(print())
                .andDo(document("shop/get-info",
                        pathParameters(
                                parameterWithName("shopId").description("가게 ID")
                        ),
                        responseFields(
                                fieldWithPath("id").type(JsonFieldType.NUMBER).description("가게 ID"),
                                fieldWithPath("name").type(JsonFieldType.STRING).description("가게명"),
                                fieldWithPath("callNumber").type(JsonFieldType.STRING).description("전화번호"),
                                fieldWithPath("address").type(JsonFieldType.STRING).description("주소"),
                                fieldWithPath("categories").type(JsonFieldType.ARRAY).description("카테고리명 리스트")
                        )
                ));
    }

    @Test
    @DisplayName("사장님 공지 조회")
    void getNotice() throws Exception {
        // given
        ShopNoticeResponse response = ShopNoticeResponse.builder()
                .title("공지 제목")
                .notice("공지 내용")
                .images(List.of("/images/image1.png", "/images/image2.png"))
                .build();
        when(shopService.getNotice(anyLong())).thenReturn(response);

        // when
        ResultActions result = mockMvc.perform(RestDocumentationRequestBuilders.get("/owner/shop/{shopId}/notice", 1));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("공지 제목"))
                .andExpect(jsonPath("$.notice").value("공지 내용"))
                .andExpect(jsonPath("$.images[0]").value("/images/image1.png"))
                .andExpect(jsonPath("$.images[1]").value("/images/image2.png"))
                .andDo(print())
                .andDo(document("shop/get-notice",
                        pathParameters(
                                parameterWithName("shopId").description("가게 ID")
                        ),
                        responseFields(
                                fieldWithPath("title").type(JsonFieldType.STRING).description("사장님 공지 제목"),
                                fieldWithPath("notice").type(JsonFieldType.STRING).description("사장님 공지 내용"),
                                fieldWithPath("images").type(JsonFieldType.ARRAY).description("사장님 공지 사진 리스트")
                        )
                ));
    }

    @Test
    @DisplayName("영업 시간 조회")
    void getBusinessHours() throws Exception {
        // given
        ShopBusinessHourResponse response = ShopBusinessHourResponse.builder()
                .businessHours(List.of(
                        new ShopBusinessHourResponse.BusinessHoursDto(BusinessHours.builder()
                                .dayOfWeek(Days.EVERYDAY)
                                .isOpen(true)
                                .openTime(LocalTime.of(10, 0))
                                .closeTime(LocalTime.of(20, 0))
                                .build()
                        )
                ))
                .build();
        when(shopService.getBusinessHours(anyLong())).thenReturn(response);

        // when
        ResultActions result = mockMvc.perform(RestDocumentationRequestBuilders.get("/owner/shop/{shopId}/business-hours", 1));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.businessHours.length()").value(1))
                .andExpect(jsonPath("$.businessHours[0].dayOfWeek").value("EVERYDAY"))
                .andExpect(jsonPath("$.businessHours[0].openTime").value("10:00:00"))
                .andDo(print())
                .andDo(document("shop/get-business-hours",
                        pathParameters(
                                parameterWithName("shopId").description("가게 ID")
                        ),
                        responseFields(
                                fieldWithPath("businessHours").type(JsonFieldType.ARRAY).description("영업 시간"),
                                fieldWithPath("businessHours[].dayOfWeek").type(JsonFieldType.STRING).description(generateLinkCode(DAYS)),
                                fieldWithPath("businessHours[].isOpen").type(JsonFieldType.BOOLEAN).description("영업일"),
                                fieldWithPath("businessHours[].openTime").type(JsonFieldType.STRING).description("오픈 시간"),
                                fieldWithPath("businessHours[].closeTime").type(JsonFieldType.STRING).description("마감 시간"),
                                fieldWithPath("businessHours[].breakTimeStart").type(JsonFieldType.STRING).description("휴게 시간 (시작)").optional(),
                                fieldWithPath("businessHours[].breakTimeEnd").type(JsonFieldType.STRING).description("휴게 시간 (종료)").optional()
                        )
                ));
    }

    @Test
    @DisplayName("휴무일 조회")
    void getCloseDays() throws Exception {
        // given
        when(shopService.getCloseDays(anyLong())).thenReturn(ShopCloseDayResponse.builder()
                .closeDays(List.of(
                        new ShopCloseDayResponse.CloseDayDto(CloseDay.builder().weekNumOfMonth(1).dayOfWeek(Days.MONDAY).build()),
                        new ShopCloseDayResponse.CloseDayDto(CloseDay.builder().weekNumOfMonth(3).dayOfWeek(Days.MONDAY).build())
                ))
                .build());

        // when
        ResultActions result = mockMvc.perform(RestDocumentationRequestBuilders.get("/owner/shop/{shopId}/close-day", 1));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.closeDays[0].weekNumOfMonth").value(1))
                .andExpect(jsonPath("$.closeDays[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.closeDays[1].weekNumOfMonth").value(3))
                .andExpect(jsonPath("$.closeDays[1].dayOfWeek").value("MONDAY"))
                .andDo(print())
                .andDo(document("shop/get-close-day",
                        pathParameters(
                                parameterWithName("shopId").description("가게 ID")
                        ),
                        responseFields(
                                fieldWithPath("closeDays").type(JsonFieldType.ARRAY).description("휴무일 리스트"),
                                fieldWithPath("closeDays[].weekNumOfMonth").type(JsonFieldType.NUMBER).description("(1~4)번째 주"),
                                fieldWithPath("closeDays[].dayOfWeek").type(JsonFieldType.STRING).description(generateLinkCode(DAYS))
                        )
                ));
    }

    @Test
    @DisplayName("가게 전화번호 수정")
    @WithLoginOwner
    void updateCallNumber() throws Exception {
        // given
        ShopUpdateCallNumberRequest updateRequest = ShopUpdateCallNumberRequest.builder()
                .callNumber("010-1234-5678")
                .build();

        doNothing().when(shopService).updateCallNumber(anyLong(), any(), any());

        // when
        ResultActions result = mockMvc.perform(RestDocumentationRequestBuilders.patch("/owner/shop/{shopId}/call-number/update", 1L)
                .header(HttpHeaders.AUTHORIZATION, jwt)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(updateRequest)));

        // then
        ConstrainedFields fields = new ConstrainedFields(ShopUpdateCallNumberRequest.class);
        result.andExpect(status().isNoContent())
                .andDo(print())
                .andDo(document("shop/update-call-number",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Access token")
                        ),
                        pathParameters(
                                parameterWithName("shopId").description("가게 ID")
                        ),
                        requestFields(
                                fields.withPath("callNumber").type(JsonFieldType.STRING).description("전화번호")
                        )
                ));
    }

    @Test
    @DisplayName("사장님 공지 수정")
    @WithLoginOwner
    void updateNotice() throws Exception {
        // given
        doNothing().when(shopService).updateNotice(anyLong(), any(), any(), anyList());
        ShopNoticeUpdateRequest request = ShopNoticeUpdateRequest.builder()
                .title("공지 제목")
                .notice("사장님 공지")
                .build();
        MockMultipartFile image1 = new MockMultipartFile("images", "image1.png", MediaType.IMAGE_PNG_VALUE, "<<image.png>>".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("images", "image2.png", MediaType.IMAGE_PNG_VALUE, "<<image.png>>".getBytes());
        MockMultipartFile image3 = new MockMultipartFile("images", "image3.png", MediaType.IMAGE_PNG_VALUE, "<<image.png>>".getBytes());
        MockMultipartFile requestJson = new MockMultipartFile(
                "noticeData",
                "jsonData",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsString(request).getBytes());


        // when
        ConstrainedFields fields = new ConstrainedFields(ShopNoticeUpdateRequest.class);
        ResultActions result = mockMvc.perform(RestDocumentationRequestBuilders.multipart("/owner/shop/{shopId}/notice/update", 1)
                .file(image1)
                .file(image2)
                .file(image3)
                .file(requestJson)
                .header(HttpHeaders.AUTHORIZATION, jwt)
                .contentType(MediaType.MULTIPART_FORM_DATA));

        // then
        result.andExpect(status().isNoContent())
                .andDo(print())
                .andDo(document("shop/update-notice",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Access token")
                        ),
                        pathParameters(
                                parameterWithName("shopId").description("가게 ID")
                        ),
                        requestParts(
                                partWithName("images").description("첨부 사진"),
                                partWithName("noticeData").description("공지 Json")
                        ),
                        requestPartFields("noticeData",
                                fields.withPath("title").type(JsonFieldType.STRING).description("공지 제목"),
                                fields.withPath("notice").type(JsonFieldType.STRING).description("공지 내용")
                        )
                ));
    }

    @Test
    @DisplayName("영업 시간 수정")
    @WithLoginOwner
    void updateBusinessHours() throws Exception {
        // given
        doNothing().when(shopService).updateBusinessHours(anyLong(), any(), any());
        ShopBusinessHourUpdateRequest request = ShopBusinessHourUpdateRequest.builder()
                .businessHours(List.of(
                        new ShopBusinessHourUpdateRequest.BusinessHoursDto(
                                Days.EVERYDAY,
                                true,
                                LocalTime.of(10, 0),
                                LocalTime.of(20, 0),
                                null,
                                null)
                ))
                .build();

        // when
        ResultActions result = mockMvc.perform(RestDocumentationRequestBuilders.patch("/owner/shop/{shopId}/business-hours/update", 1)
                .header(HttpHeaders.AUTHORIZATION, jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .characterEncoding("utf8"));

        // then
        ConstrainedFields fields = new ConstrainedFields(ShopBusinessHourUpdateRequest.class);
        result.andExpect(status().isNoContent())
                .andDo(print())
                .andDo(document("shop/update-business-hours",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Access token")
                        ),
                        pathParameters(
                                parameterWithName("shopId").description("가게 ID")
                        ),
                        requestFields(
                                fields.withPath("businessHours").type(JsonFieldType.ARRAY).description("영업 시간"),
                                fields.withPath("businessHours[].dayOfWeek").type(JsonFieldType.STRING).description(generateLinkCode(DAYS)),
                                fields.withPath("businessHours[].isOpen").type(JsonFieldType.BOOLEAN).description("영업일"),
                                fields.withPath("businessHours[].openTime").type(JsonFieldType.STRING).description("오픈 시간"),
                                fields.withPath("businessHours[].closeTime").type(JsonFieldType.STRING).description("마감 시간"),
                                fields.withPath("businessHours[].breakTimeStart").type(JsonFieldType.STRING).description("휴게 시간 (시작)").optional(),
                                fields.withPath("businessHours[].breakTimeEnd").type(JsonFieldType.STRING).description("휴게 시간 (종료)").optional()
                        )
                ));
    }

    @Test
    @DisplayName("휴무일 수정")
    void updateCloseDays() throws Exception {
        // given
        doNothing().when(shopService).updateCloseDays(anyLong(), any(), any());
        ShopCloseDayUpdateRequest request = ShopCloseDayUpdateRequest.builder()
                .closeDays(List.of(
                        new ShopCloseDayUpdateRequest.CloseDayDto(1, Days.MONDAY),
                        new ShopCloseDayUpdateRequest.CloseDayDto(3, Days.MONDAY)
                ))
                .build();

        // when
        ResultActions result = mockMvc.perform(RestDocumentationRequestBuilders.patch("/owner/shop/{shopId}/close-day/update", 1)
                .header(HttpHeaders.AUTHORIZATION, jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        ConstrainedFields fields = new ConstrainedFields(ShopCloseDayUpdateRequest.class);
        result.andExpect(status().isNoContent())
                .andDo(print())
                .andDo(document("shop/update-close-day",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Access token")
                        ),
                        pathParameters(
                                parameterWithName("shopId").description("가게 ID")
                        ),
                        requestFields(
                                fields.withPath("closeDays").type(JsonFieldType.ARRAY).description("휴무일 리스트"),
                                fields.withPath("closeDays[].weekNumOfMonth").type(JsonFieldType.NUMBER).description("(1~4)번째 주"),
                                fields.withPath("closeDays[].dayOfWeek").type(JsonFieldType.STRING).description(generateLinkCode(DAYS))
                        )
                ));
    }

    @Test
    @DisplayName("가게 삭제")
    @WithLoginOwner
    void deleteShop() throws Exception {
        // given
        doNothing().when(shopService).delete(anyLong(), any());

        // when
        ResultActions result = mockMvc.perform(RestDocumentationRequestBuilders.delete("/owner/shop/{shopId}/delete", 1L)
                .header(HttpHeaders.AUTHORIZATION, jwt));

        // then
        verify(shopService).delete(anyLong(), any());
        result.andExpect(status().isNoContent())
                .andDo(print())
                .andDo(document("shop/delete",
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Access token")
                        ),
                        pathParameters(
                                parameterWithName("shopId").description("가게 ID")
                        )
                ));
    }

    @Test
    @DisplayName("가게 일시중지")
    void tempClose() throws Exception {
        // given
        doNothing().when(shopService).tempClose(anyLong(), any(), any());
        ShopTempCloseRequest request = ShopTempCloseRequest.builder()
                .closeUntil(LocalDateTime.of(2024, 2, 10, 22, 0, 0))
                .build();

        // when
        ResultActions result = mockMvc.perform(RestDocumentationRequestBuilders.patch("/owner/shop/{shopId}/temp-close", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)));

        // then
        ConstrainedFields fields = new ConstrainedFields(ShopTempCloseRequest.class);
        result.andExpect(status().isNoContent())
                .andDo(document("shop/temp-close",
                        pathParameters(
                                parameterWithName("shopId").description("가게 ID")
                        ),
                        requestFields(
                                fields.withPath("closeUntil").type(JsonFieldType.STRING).description("일시중지 종료 시간").optional(),
                                fields.withPath("today").type(JsonFieldType.BOOLEAN).description("오늘 하루 일시중지").optional()
                        )
                ));
    }

    @Test
    @DisplayName("점주가 소유한 가게 목록 조회")
    void getOwnerShops() throws Exception {
        // given
        when(shopService.getOwnerShops(any())).thenReturn(
                List.of(OwnerShopResponse.builder().id(1L).name("가게 1").icon("/images/image.jpg").build(),
                        OwnerShopResponse.builder().id(2L).name("가게 1").icon("/images/image.jpg").build())
        );

        // when
        ResultActions result = mockMvc.perform(RestDocumentationRequestBuilders.get("/owner/shop"));

        // then
        result.andExpect(status().isOk())
                .andDo(document("shop/owner-shops",
                        responseFields(
                                fieldWithPath("[].id").type(JsonFieldType.NUMBER).description("가게 ID"),
                                fieldWithPath("[].name").type(JsonFieldType.STRING).description("가게명"),
                                fieldWithPath("[].icon").type(JsonFieldType.STRING).description("아이콘")
                        )
                ));
    }

    private Shop givenShop() {
        Shop shop = Shop.builder()
                .id(1L)
                .name("롯데리아")
                .icon("692c0741-f234-448e-ba3f-35b5a394f33d.png")
                .banner("692c0741-f234-448e-ba3f-35b5a394f33d.png")
                .ownerNotice("사장님 공지")
                .callNumber("010-1234-5678")
                .address("서울 강남구 영동대로 513")
                .categoryShop(Arrays.asList(
                        CategoryShop.builder().category(Category.builder().name("카테고리1").build()).build(),
                        CategoryShop.builder().category(Category.builder().name("카테고리2").build()).build(),
                        CategoryShop.builder().category(Category.builder().name("카테고리3").build()).build(),
                        CategoryShop.builder().category(Category.builder().name("카테고리4").build()).build()
                ))
                .build();

        return shop;
    }

    private MockMultipartFile givenIcon() throws IOException {
        return new MockMultipartFile("icon", "images.png", MediaType.IMAGE_PNG_VALUE, "<<image png>>".getBytes());
    }

    private MockMultipartFile givenBanner() throws IOException {
        return new MockMultipartFile("banner", "images.png", MediaType.IMAGE_PNG_VALUE, "<<image png>>".getBytes());
    }
}
