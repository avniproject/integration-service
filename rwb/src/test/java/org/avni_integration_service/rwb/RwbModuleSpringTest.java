package org.avni_integration_service.rwb;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {RwbIntegrationService.class})
public class RwbModuleSpringTest extends BaseRwbSpringTest {

    @Autowired
    private RwbIntegrationService dummyBean;

    @Test
    public void contextLoads() {
    }
}
