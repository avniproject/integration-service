package org.avni_integration_service.wati;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {WatiIntegrationService.class})
public class WatiModuleSpringTest extends BaseWatiSpringTest {

    @Autowired
    private WatiIntegrationService dummyBean;

    @Test
    public void contextLoads() {
    }
}
