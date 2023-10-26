package org.avni_integration_service.lahi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {LahiIntegrationService.class})
public class LahiModuleSpringTest extends BaseLahiSpringTest {
    @Autowired
    private LahiIntegrationService dummyBean;

    @Test
    public void contextLoads() {
    }
}
