package toy.yogiyo.core.shop.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import toy.yogiyo.core.deliveryplace.service.DeliveryPlaceService;
import toy.yogiyo.core.member.domain.Member;
import toy.yogiyo.core.menu.service.MenuGroupService;
import toy.yogiyo.core.menu.service.SignatureMenuService;
import toy.yogiyo.core.menuoption.service.MenuOptionGroupService;
import toy.yogiyo.core.shop.dto.scroll.ShopScrollListRequest;
import toy.yogiyo.core.shop.dto.scroll.ShopScrollListResponse;
import toy.yogiyo.core.shop.dto.scroll.ShopScrollResponse;
import toy.yogiyo.common.exception.*;
import toy.yogiyo.common.file.ImageFileHandler;
import toy.yogiyo.common.file.ImageFileUtil;
import toy.yogiyo.core.category.domain.Category;
import toy.yogiyo.core.category.domain.CategoryShop;
import toy.yogiyo.core.category.service.CategoryService;
import toy.yogiyo.core.owner.domain.Owner;
import toy.yogiyo.core.shop.domain.Shop;
import toy.yogiyo.core.shop.dto.*;
import toy.yogiyo.core.shop.repository.ShopRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class ShopService {

    private final ShopRepository shopRepository;
    private final ImageFileHandler imageFileHandler;
    private final CategoryService categoryService;
    private final MenuGroupService menuGroupService;
    private final SignatureMenuService signatureMenuService;
    private final MenuOptionGroupService menuOptionGroupService;
    private final DeliveryPlaceService deliveryPlaceService;

    public Long register(ShopRegisterRequest request, MultipartFile icon, MultipartFile banner, Owner owner) {
        validateDuplicateName(request.getName());

        String iconStoredName = "";
        String bannerStoredName = "";
        Shop shop;

        try {
            iconStoredName = ImageFileUtil.getFilePath(imageFileHandler.store(icon));
            bannerStoredName = ImageFileUtil.getFilePath(imageFileHandler.store(banner));

            shop = request.toShop(iconStoredName, bannerStoredName, owner);

            request.getCategories().forEach(categoryName -> {
                Category category = categoryService.getCategory(categoryName);
                shop.getCategoryShop().add(CategoryShop.builder()
                        .category(category)
                        .shop(shop)
                        .build());
            });

            shopRepository.save(shop);
        } catch (Exception e) {
            // 저장 도중 문제가 발생 하면 저장 했던 이미지 삭제
            imageFileHandler.remove(ImageFileUtil.extractFilename(iconStoredName));
            imageFileHandler.remove(ImageFileUtil.extractFilename(bannerStoredName));
            throw e;
        }

        return shop.getId();
    }

    @Transactional(readOnly = true)
    public ShopInfoResponse getInfo(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SHOP_NOT_FOUND));

        return ShopInfoResponse.from(shop);
    }

    @Transactional(readOnly = true)
    public ShopNoticeResponse getNotice(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SHOP_NOT_FOUND));

        return ShopNoticeResponse.from(shop);
    }

    @Transactional(readOnly = true)
    public ShopBusinessHourResponse getBusinessHours(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SHOP_NOT_FOUND));

        return ShopBusinessHourResponse.from(shop);
    }

    @Transactional(readOnly = true)
    public ShopCloseDayResponse getCloseDays(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SHOP_NOT_FOUND));

        return ShopCloseDayResponse.from(shop);
    }

    @Transactional
    public void updateCallNumber(Long shopId, Owner owner, ShopUpdateCallNumberRequest request) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SHOP_NOT_FOUND));

        validatePermission(owner, shop);

        shop.updateCallNumber(request.getCallNumber());
    }

    @Transactional
    public void updateNotice(Long shopId, Owner owner, ShopNoticeUpdateRequest request, List<MultipartFile> imageFiles) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SHOP_NOT_FOUND));

        validatePermission(owner, shop);

        // 기존에 있던 이미지 삭제
        for (String noticeImage : shop.getNoticeImages()) {
            if(!imageFileHandler.remove(ImageFileUtil.extractFilename(noticeImage))){
                throw new FileIOException(ErrorCode.FILE_NOT_REMOVED);
            }
        }

        // 새로운 이미지 저장
        List<String> storedImages = new ArrayList<>();
        for (MultipartFile imageFile : imageFiles) {
            String storedFilePath = ImageFileUtil.getFilePath(imageFileHandler.store(imageFile));
            storedImages.add(storedFilePath);
        }

        shop.updateNotice(request.getTitle(), request.getNotice(), storedImages);
    }

    @Transactional
    public void updateBusinessHours(Long shopId, Owner owner, ShopBusinessHourUpdateRequest request) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SHOP_NOT_FOUND));

        validatePermission(owner, shop);

        shop.updateBusinessHours(request.toBusinessHours());
    }

    @Transactional
    public void updateCloseDays(Long shopId, Owner owner, ShopCloseDayUpdateRequest request) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SHOP_NOT_FOUND));

        validatePermission(owner, shop);

        shop.updateCloseDays(request.toCloseDays());
    }

    @Transactional
    public void delete(Long shopId, Owner owner) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SHOP_NOT_FOUND));

        validatePermission(owner, shop);

        if (!imageFileHandler.remove(ImageFileUtil.extractFilename(shop.getIcon()))) {
            throw new FileIOException(ErrorCode.FILE_NOT_REMOVED);
        }
        if (!imageFileHandler.remove(ImageFileUtil.extractFilename(shop.getBanner()))) {
            throw new FileIOException(ErrorCode.FILE_NOT_REMOVED);
        }

        signatureMenuService.deleteAll(shopId);
        menuOptionGroupService.getAll(shopId)
                .forEach(menuOptionGroup -> menuOptionGroupService.delete(menuOptionGroup.getId()));
        menuGroupService.getMenuGroups(shopId)
                .forEach(menuGroupService::delete);
        deliveryPlaceService.deleteAll(shopId);

        shopRepository.delete(shop);
    }


    public ShopScrollListResponse getList(ShopScrollListRequest request) {
        List<ShopScrollResponse> shops = shopRepository.scrollShopList(request);

        if(shops.isEmpty()) {
            return ShopScrollListResponse.builder()
                    .content(shops)
                    .hasNext(false)
                    .build();
        }

        boolean hasNext = request.getSize()==null ? shops.size() >= 11L : shops.size() >= request.getSize()+1;
        if(hasNext) shops.remove(shops.size()-1);
        BigDecimal nextCursor = getCursor(request.getSortOption(), shops.get(shops.size()-1));
        long nextSubCursor = shops.get(shops.size()-1).getShopId();

        return ShopScrollListResponse.builder()
                .content(shops)
                .nextCursor(nextCursor)
                .nextSubCursor(nextSubCursor)
                .hasNext(hasNext)
                .build();
    }

    public List<ShopScrollResponse> getRecentList(Member member, ShopRecentRequest request) {
        if(member.getId() == null) throw new AuthenticationException(ErrorCode.MEMBER_UNAUTHORIZATION);

        return shopRepository.recentOrder(member.getId(), request);
    }

    @Transactional
    public void tempClose(Long shopId, Owner owner, ShopTempCloseRequest request) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SHOP_NOT_FOUND));
        validatePermission(owner, shop);

        if (request.getToday() != null && request.getToday()) {
            LocalDateTime tomorrowOpenTime = shop.getOpenTime(LocalDateTime.now().plusDays(1));
            shop.updateCloseUntil(tomorrowOpenTime);
        } else {
            shop.updateCloseUntil(request.getCloseUntil());
        }
    }

    @Transactional(readOnly = true)
    public List<OwnerShopResponse> getOwnerShops(Owner owner) {
        if(owner.getId() == null) {
            throw new AuthenticationException(ErrorCode.OWNER_UNAUTHORIZATION);
        }

        List<Shop> shops = shopRepository.findByOwner(owner);
        return shops.stream()
                .map(OwnerShopResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    @Scheduled(cron = "0 0/10 * * * *")
    public void tempCloseCheck() {
        shopRepository.updateAllTempCloseFinishByDateTime(LocalDateTime.now());
    }

    private void validateDuplicateName(String name) {
        if (shopRepository.existsByName(name)) {
            throw new EntityExistsException(ErrorCode.SHOP_ALREADY_EXIST);
        }
    }

    private void validatePermission(Owner owner, Shop shop) {
        if (!Objects.equals(shop.getOwner().getId(), owner.getId())) {
            throw new AccessDeniedException(ErrorCode.SHOP_ACCESS_DENIED);
        }
    }

    private BigDecimal getCursor(ShopScrollListRequest.SortOption sortOption, ShopScrollResponse shop) {
        switch (sortOption){
            case REVIEW: return BigDecimal.valueOf(shop.getReviewNum());
            case SCORE: return shop.getTotalScore();
            case CLOSEST: return BigDecimal.valueOf(shop.getDistance());
            default: return BigDecimal.valueOf(shop.getOrderNum());
        }
    }
}
