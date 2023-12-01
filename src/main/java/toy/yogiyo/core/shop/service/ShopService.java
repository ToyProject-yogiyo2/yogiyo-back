package toy.yogiyo.core.shop.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import toy.yogiyo.common.dto.scroll.Scroll;
import toy.yogiyo.core.shop.dto.ShopScrollListRequest;
import toy.yogiyo.core.shop.dto.ShopScrollResponse;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopService {

    private final ShopRepository shopRepository;
    private final ImageFileHandler imageFileHandler;
    private final CategoryService categoryService;

    public Long register(ShopRegisterRequest request, MultipartFile icon, MultipartFile banner, Owner owner) throws IOException {
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
    public ShopDeliveryPriceResponse getDeliveryPrice(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SHOP_NOT_FOUND));

        return ShopDeliveryPriceResponse.from(shop);
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
    public void updateNotice(Long shopId, Owner owner, ShopNoticeUpdateRequest request, List<MultipartFile> imageFiles) throws IOException {
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
    public void updateDeliveryPrice(Long shopId, Owner owner, DeliveryPriceUpdateRequest request) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SHOP_NOT_FOUND));

        validatePermission(owner, shop);

        shop.changeDeliveryPrices(request.toDeliveryPriceInfos());
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

        shopRepository.delete(shop);
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

    public Scroll<ShopScrollResponse> getList(ShopScrollListRequest request) {
        List<ShopScrollResponse> shops = shopRepository.scrollShopList(request);
        boolean hasNext = shops.size() >= request.getLimit()+1;
        if(hasNext) shops.remove(shops.size()-1);
        long nextOffset = request.getOffset() + shops.size();

        return new Scroll<>(shops, nextOffset, hasNext);
    }
}
