package org.avni_integration_service.goonj.service;

import org.avni_integration_service.avni.client.AvniHttpClient;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {GoonjContextProvider.class, GoonjAvniSessionFactory.class})
public class GoonjMediaServiceTest extends BaseGoonjSpringTest {
    public static final String IMAGE_ID_KEY = GoonjMediaService.IMAGE_ID;
    public static final String LINK_KEY = GoonjMediaService.LINK;
    public static final String IMAGES_LINK = GoonjMediaService.IMAGES_LINK;
    public static final String LOADING_AND_TRUCK_IMAGES = GoonjMediaService.LOADING_AND_TRUCK_IMAGES;
    public static final String IMAGE = AvniMediaConstants.IMAGE;
    public static final String IMAGE_ID_1 = "abc";
    public static final String IMAGE_ID_2 = "def";
    public static final String IMAGE_ID_3 = "ghi";
    public static final String IMAGE_ID_1_LINK = "www.abc.com";
    public static final String IMAGE_ID_2_LINK = "www.def.com";
    public static final String IMAGE_ID_3_LINK = "www.ghi.com";
    public static final String IMAGE_ID_1_AVNI_URL = "https://s3.com/env-media/org-media/a9993e36-4706-516a-ba3e-25717850c26c.jpg";
    public static final String IMAGE_ID_2_AVNI_URL = "https://s3.com/env-media/org-media/589c2233-5a38-5f12-ad12-9225f5c0ba30.jpg";
    public static final String DUMMY = "dummy";

    //Manually initialized
    private Dispatch dispatch;
    private List<GoonjMedia> imageList;
    private List<String> storedMediaUrls;
    private Map<String, String> goonjMedia;
    private Map<String, Object> observation;
    private Map<String, Object> dispatchResponse;
    private Map<GoonjMedia, Boolean> goonjMediaDownloadAndUploadResultMap;

    //Mocked
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private AvniHttpClient avniHttpClient;
    @Mock
    private AvniMediaRepository avniMediaRepository;
    @Mock
    private GoonjContextProvider goonjContextProvider;

    //Injected with Mocks
    private AvniMediaService avniMediaService;
    private GoonjMediaService goonjMediaService;

    @BeforeEach
    public void init() {
        avniMediaService = new AvniMediaService(avniMediaRepository);
        goonjMediaService = new GoonjMediaService(restTemplate, avniMediaService, goonjContextProvider);
        dispatch = Dispatch.from(dispatchResponse);
        imageList = new ArrayList<>();
        storedMediaUrls = new ArrayList<>();
        goonjMedia = new HashMap<>();
        observation = new HashMap<>();
        dispatchResponse = new HashMap<>();

        reset(restTemplate, avniHttpClient, avniMediaRepository, goonjContextProvider);
    }

    @DisplayName("getSalesforceImageList")
    @Test
    public void test_getSalesforceImageList() {
        // TODO: 14/10/24 Break into smaller separate tests
        imageList = goonjMediaService.getSalesforceImageList(dispatch, GoonjMediaService.IMAGES_LINK);
        Assert.isTrue(imageList.size() ==0, "imageList should have been empty for empty data in dispatch");

        goonjMedia.put(IMAGE_ID_KEY, IMAGE_ID_1);
        goonjMedia.put(LINK_KEY, IMAGE_ID_1_LINK);

        dispatchResponse.put(IMAGES_LINK,List.of(goonjMedia));
        dispatch = Dispatch.from(dispatchResponse);
        imageList = goonjMediaService.getSalesforceImageList(dispatch, IMAGES_LINK);
        Assert.isTrue(imageList.size() == 1, "imageList should have been give size after convert to goonjmedia");

        dispatchResponse.clear();
        dispatchResponse.put(IMAGES_LINK, IMAGE_ID_1);
        dispatch = Dispatch.from(dispatchResponse);
        imageList = goonjMediaService.getSalesforceImageList(dispatch, IMAGES_LINK);
        Assert.isTrue(imageList.size() ==0, "imageList should have been empty for IMAGES_LINK is not in proper format in dispatch");

        dispatchResponse.clear();
        goonjMedia.clear();
        goonjMedia.put(IMAGE_ID_1, IMAGE_ID_1);
        dispatchResponse.put(IMAGES_LINK,List.of(goonjMedia));
        dispatch = Dispatch.from(dispatchResponse);
        imageList = goonjMediaService.getSalesforceImageList(dispatch, IMAGES_LINK);
        Assert.isTrue(imageList.size() ==0, "imageList give size of which have IMAGE_ID and LINK");
    }

    @DisplayName("getStoredMediaUrls")
    @Test
    public void test_getStoredMediaUrls(){
        // TODO: 14/10/24 Break into smaller separate tests
        storedMediaUrls = goonjMediaService.getStoredMediaUrls(null, LOADING_AND_TRUCK_IMAGES);
        Assert.isTrue(storedMediaUrls.size() ==0, "storedMediaUrls should give size 0 after if there is no image");

        observation.put(LOADING_AND_TRUCK_IMAGES, IMAGE_ID_1);

        Subject subject = new Subject();
        subject.setObservations(observation);
        storedMediaUrls = goonjMediaService.getStoredMediaUrls(subject, LOADING_AND_TRUCK_IMAGES);
        Assert.isTrue(storedMediaUrls.size() ==0, "storedMediaUrls should give size 0 if unable to convert");

        List<String> urls = List.of(IMAGE_ID_1_LINK, IMAGE_ID_2_LINK);
        observation.put(LOADING_AND_TRUCK_IMAGES,urls);
        subject.setObservations(observation);
        storedMediaUrls = goonjMediaService.getStoredMediaUrls(subject, LOADING_AND_TRUCK_IMAGES);
        Assert.isTrue(storedMediaUrls.size() ==2, "storedMediaUrls should give size as observations");
    }

    @DisplayName("processMedia ignoreForAlreadyPresentImage")
    @Test
    public void test_processMedia_ignoreForAlreadyPresentImage() {
        Subject subject = new Subject();
        List<String> urls = List.of(IMAGE_ID_1_AVNI_URL,
                IMAGE_ID_2_AVNI_URL);
        observation.put(LOADING_AND_TRUCK_IMAGES, urls);
        subject.setObservations(observation);

        // test for image already present
        dispatchResponse.clear();
        goonjMedia.clear();
        goonjMedia.put(IMAGE_ID_KEY, IMAGE_ID_1);
        goonjMedia.put(LINK_KEY, IMAGE_ID_1_LINK);

        dispatchResponse.put(IMAGES_LINK, List.of(goonjMedia));
        dispatch = Dispatch.from(dispatchResponse);

        imageList = goonjMediaService.getSalesforceImageList(dispatch, GoonjMediaService.IMAGES_LINK);
        storedMediaUrls = goonjMediaService.getStoredMediaUrls(subject, LOADING_AND_TRUCK_IMAGES);

        goonjMediaDownloadAndUploadResultMap = goonjMediaService.processMedia(storedMediaUrls, imageList, AvniMediaConstants.IMAGE);

        Assert.isTrue(goonjMediaDownloadAndUploadResultMap.size() == 0, "goonjMediaDownloadAndUploadResultMap should have size 0");

    }

    @DisplayName("processMedia successfulDownloadAndUploadOfMissingImage")
    @Test
    public void test_processMedia_successfulDownloadAndUploadOfMissingImage() {
        Subject subject = new Subject();
        List<String> urls = List.of(IMAGE_ID_1_AVNI_URL,
                IMAGE_ID_2_AVNI_URL);
        observation.put(LOADING_AND_TRUCK_IMAGES, urls);
        subject.setObservations(observation);

        dispatchResponse.clear();
        goonjMedia.clear();
        goonjMedia.put(IMAGE_ID_KEY, IMAGE_ID_1);
        goonjMedia.put(LINK_KEY, IMAGE_ID_1_LINK);
        goonjMedia.put(IMAGE_ID_KEY, IMAGE_ID_3);
        goonjMedia.put(LINK_KEY, IMAGE_ID_3_LINK);

        dispatchResponse.put(IMAGES_LINK, List.of(goonjMedia));
        dispatch = Dispatch.from(dispatchResponse);

        imageList = goonjMediaService.getSalesforceImageList(dispatch, GoonjMediaService.IMAGES_LINK);
        storedMediaUrls = goonjMediaService.getStoredMediaUrls(subject, LOADING_AND_TRUCK_IMAGES);

        when(restTemplate.execute(any(), any(), any(), any())).thenReturn(true); //mocking upload and download
        when(avniMediaRepository.generateUploadUrl(anyString())).thenReturn(DUMMY); //mocking generateUploadUrl
        when(avniMediaRepository.addMedia(anyString(), anyString(), anyString(), any())).thenReturn(true); //mocking addMedia

        goonjMediaDownloadAndUploadResultMap = goonjMediaService.processMedia(storedMediaUrls, imageList, AvniMediaConstants.IMAGE);

        Assert.isTrue(goonjMediaDownloadAndUploadResultMap.size() == 1, "goonjMediaDownloadAndUploadResultMap should have size 1");
        Assert.isTrue(goonjMediaDownloadAndUploadResultMap.values().stream().findFirst().get(), "goonjMediaDownloadAndUploadResultMap value should have been true");

        verify(restTemplate).execute(any(), any(), any(), any());
        verifyNoMoreInteractions(restTemplate);
        verify(avniMediaRepository).generateUploadUrl(anyString());
        verify(avniMediaRepository).addMedia(anyString(), anyString(), anyString(), any());
        verifyNoMoreInteractions(avniMediaRepository);

    }

    @DisplayName("processMedia FailedDownloadAndSkippedUploadOfMissingImage")
    @Test
    public void test_processMedia_FailedDownloadAndSkippedUploadOfMissingImage(){
        Subject subject = new Subject();
        List<String> urls = List.of(IMAGE_ID_1_AVNI_URL,
                IMAGE_ID_2_AVNI_URL);
        observation.put(LOADING_AND_TRUCK_IMAGES, urls);
        subject.setObservations(observation);

        dispatchResponse.clear();
        goonjMedia.clear();
        goonjMedia.put(IMAGE_ID_KEY, IMAGE_ID_1);
        goonjMedia.put(LINK_KEY, IMAGE_ID_1_LINK);
        goonjMedia.put(IMAGE_ID_KEY, IMAGE_ID_3);
        goonjMedia.put(LINK_KEY, IMAGE_ID_3_LINK);

        dispatchResponse.put(IMAGES_LINK,List.of(goonjMedia));
        dispatch = Dispatch.from(dispatchResponse);

        imageList = goonjMediaService.getSalesforceImageList(dispatch, GoonjMediaService.IMAGES_LINK);
        storedMediaUrls = goonjMediaService.getStoredMediaUrls(subject, LOADING_AND_TRUCK_IMAGES);

        when(restTemplate.execute(any(), any(), any(), any())).thenReturn(false); //mocking upload and download
        when(avniMediaRepository.generateUploadUrl(anyString())).thenReturn(DUMMY); //mocking generateUploadUrl
        when(avniMediaRepository.addMedia(anyString(), anyString(), anyString(), any())).thenReturn(true); //mocking addMedia

        goonjMediaDownloadAndUploadResultMap = goonjMediaService.processMedia(storedMediaUrls, imageList, AvniMediaConstants.IMAGE);

        Assert.isTrue(goonjMediaDownloadAndUploadResultMap.size() == 1, "goonjMediaDownloadAndUploadResultMap should have size 1");
        Assert.isTrue(!goonjMediaDownloadAndUploadResultMap.values().stream().findFirst().get(), "goonjMediaDownloadAndUploadResultMap entry value should have been false");

        verify(restTemplate).execute(any(), any(), any(), any());
        verifyNoMoreInteractions(restTemplate);
        verify(avniMediaRepository, never()).generateUploadUrl(anyString());
        verify(avniMediaRepository, never()).addMedia(anyString(), anyString(), anyString(), any());
        verifyNoMoreInteractions(avniMediaRepository);

    }

    // TODO: 14/10/24 1. Create unit tests for  goonjMediaService.fetchListOfAvniUrlsToBeStoredAsConceptValue(imageList,goonjMediaDownloadAndUploadResultMap)
    @DisplayName("fetchListOfAvniUrlsToBeStoredAsConceptValue ifNotAnyDownload")
    @Test
    public void test_fetchListOfAvniUrlsToBeStoredAsConceptValue_ifNotAnyDownload(){
        Subject subject = new Subject();
        List<String> urls = List.of(IMAGE_ID_1_AVNI_URL);
        observation.put(LOADING_AND_TRUCK_IMAGES, urls);
        subject.setObservations(observation);

        dispatchResponse.clear();
        goonjMedia.clear();
        goonjMedia.put(IMAGE_ID_KEY, IMAGE_ID_1);
        goonjMedia.put(LINK_KEY, IMAGE_ID_1_LINK);

        dispatchResponse.put(IMAGES_LINK,List.of(goonjMedia));
        dispatch = Dispatch.from(dispatchResponse);
        imageList = goonjMediaService.getSalesforceImageList(dispatch, IMAGES_LINK);
        storedMediaUrls = goonjMediaService.getStoredMediaUrls(subject, LOADING_AND_TRUCK_IMAGES);
        goonjMediaDownloadAndUploadResultMap = goonjMediaService.processMedia(storedMediaUrls, imageList, IMAGE);
        List<String> conceptValue = goonjMediaService.fetchListOfAvniUrlsToBeStoredAsConceptValue(imageList, goonjMediaDownloadAndUploadResultMap);
        Assert.isTrue(urls.get(0).equals(conceptValue.get(0)) ,"give concept value as already stored url");
    }

    // TODO: 14/10/24 2. Create unit tests for  goonjMediaService.hasAtleastOneInvalidImagesLink(goonjMediaDownloadAndUploadResultMap)
    // TODO: 14/10/24 3. Create unit tests for  goonjMediaService.getDownloadMediaResponseExtractor() (For validImage, expiredImage, anyOtherError)

}
