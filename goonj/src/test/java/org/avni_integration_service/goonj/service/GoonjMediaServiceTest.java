package org.avni_integration_service.goonj.service;

import org.avni_integration_service.avni.domain.AvniMediaConstants;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.avni.repository.AvniMediaRepository;
import org.avni_integration_service.avni.service.AvniMediaService;
import org.avni_integration_service.goonj.BaseGoonjSpringTest;
import org.avni_integration_service.goonj.config.GoonjAvniSessionFactory;
import org.avni_integration_service.goonj.config.GoonjContextProvider;
import org.avni_integration_service.goonj.domain.Dispatch;
import org.avni_integration_service.goonj.domain.GoonjMedia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SpringBootTest(classes = {GoonjContextProvider.class, GoonjAvniSessionFactory.class, AvniMediaService.class})
public class GoonjMediaServiceTest extends BaseGoonjSpringTest {
    public static final String IMAGE_ID = GoonjMediaService.IMAGE_ID;
    public static final String LINK = GoonjMediaService.LINK;
    public static final String IMAGES_LINK = GoonjMediaService.IMAGES_LINK;
    public static final String LOADING_AND_TRUCK_IMAGES = GoonjMediaService.LOADING_AND_TRUCK_IMAGES;
    public static final String IMAGE = AvniMediaConstants.IMAGE;
    @Autowired
    private GoonjContextProvider goonjContextProvider;
    @Mock
    private RestTemplate restTemplateMock;
    private AvniMediaRepository avniMediaRepository;
    @Autowired
    private AvniMediaService avniMediaService;

    private GoonjMediaService goonjMediaService;
    private Dispatch dispatch;

    @BeforeEach
    public void init() {
        goonjMediaService = new GoonjMediaService(restTemplateMock, avniMediaService, goonjContextProvider);
        Map<String, Object> dispatchResponse = new HashMap<>();
        dispatch = Dispatch.from(dispatchResponse);
    }

    @DisplayName("getSalesforceImageList")
    @Test
    public void test_getSalesforceImageList() {
        List<GoonjMedia> imageList = goonjMediaService.getSalesforceImageList(dispatch, GoonjMediaService.IMAGES_LINK);
        Assert.isTrue(imageList.size() ==0, "imageList should have been empty for empty data in dispatch");

        Map<String,String> goonjMedia = new HashMap<>();
        goonjMedia.put(IMAGE_ID,"abc");
        goonjMedia.put(LINK,"www.abc.com");

        Map<String, Object> dispatchResponse = new HashMap<>();
        dispatchResponse.put(IMAGES_LINK,List.of(goonjMedia));
        dispatch = Dispatch.from(dispatchResponse);
        imageList = goonjMediaService.getSalesforceImageList(dispatch, IMAGES_LINK);
        Assert.isTrue(imageList.size() == 1, "imageList should have been give size after convert to goonjmedia");

        dispatchResponse.clear();
        dispatchResponse.put(IMAGES_LINK,"abc");
        dispatch = Dispatch.from(dispatchResponse);
        imageList = goonjMediaService.getSalesforceImageList(dispatch, IMAGES_LINK);
        Assert.isTrue(imageList.size() ==0, "imageList should have been empty for IMAGES_LINK is not in proper format in dispatch");

        dispatchResponse.clear();
        goonjMedia.clear();
        goonjMedia.put("abc","abc");
        dispatchResponse.put(IMAGES_LINK,List.of(goonjMedia));
        dispatch = Dispatch.from(dispatchResponse);
        imageList = goonjMediaService.getSalesforceImageList(dispatch, IMAGES_LINK);
        Assert.isTrue(imageList.size() ==0, "imageList give size of which have IMAGE_ID and LINK");
    }

    @DisplayName("getStoredMediaUrls")
    @Test
    public void test_getStoredMediaUrls(){
        List<String> storedMediaUrls = goonjMediaService.getStoredMediaUrls(null, LOADING_AND_TRUCK_IMAGES);
        Assert.isTrue(storedMediaUrls.size() ==0, "storedMediaUrls should give size 0 after if there is no image");

        Map<String, Object> observation = new HashMap<>();
        observation.put(LOADING_AND_TRUCK_IMAGES,"abc");

        Subject subject = new Subject();
        subject.setObservations(observation);
        storedMediaUrls = goonjMediaService.getStoredMediaUrls(subject, LOADING_AND_TRUCK_IMAGES);
        Assert.isTrue(storedMediaUrls.size() ==0, "storedMediaUrls should give size 0 if unable to convert");

        List<String> urls = List.of("www.abc.com","www.def.com");
        observation.put(LOADING_AND_TRUCK_IMAGES,urls);
        subject.setObservations(observation);
        storedMediaUrls = goonjMediaService.getStoredMediaUrls(subject, LOADING_AND_TRUCK_IMAGES);
        Assert.isTrue(storedMediaUrls.size() ==2, "storedMediaUrls should give size as observations");
    }
}
