package org.avni_integration_service.tanuh;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {TanuhIntegrationService.class})
public class TanuhModuleSpringTest extends BaseTanuhSpringTest {

    @Autowired
    private TanuhIntegrationService dummyBean;

    @Test
    public void contextLoads() {
    }
}
