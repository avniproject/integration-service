package org.avni_integration_service.goonj.service;

import org.avni_integration_service.avni.repository.AvniMediaRepository;
import org.avni_integration_service.avni.service.AvniMediaService;
import org.avni_integration_service.goonj.BaseGoonjSpringTest;
import org.avni_integration_service.goonj.config.GoonjAvniSessionFactory;
import org.avni_integration_service.goonj.config.GoonjContextProvider;
import org.avni_integration_service.goonj.domain.Dispatch;
import org.avni_integration_service.goonj.domain.GoonjMedia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SpringBootTest(classes = {GoonjContextProvider.class, GoonjAvniSessionFactory.class, AvniMediaService.class})
public class GoonjMediaServiceTest extends BaseGoonjSpringTest {
    @Autowired
    private GoonjContextProvider goonjContextProvider;
    private RestTemplate restTemplate;
    private AvniMediaRepository avniMediaRepository;
    @Autowired
    private AvniMediaService avniMediaService;

    private GoonjMediaService goonjMediaService;
    private Dispatch dispatch;

    @BeforeEach
    public void init() {
        goonjMediaService = new GoonjMediaService(new RestTemplate(), avniMediaService, goonjContextProvider);
        //Init
        Map<String, Object> dispatchResponse = new HashMap<>();
        dispatch = Dispatch.from(dispatchResponse);
    }

    @Test
    public void test() {
        List<GoonjMedia> imageList = goonjMediaService.getSalesforceImageList(dispatch, GoonjMediaService.IMAGES_LINK);
        Assert.isTrue(imageList.size() ==0, "imageList should have been empty");
    }
}
