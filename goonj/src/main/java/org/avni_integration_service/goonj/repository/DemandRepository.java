package org.avni_integration_service.goonj.repository;

import org.avni_integration_service.goonj.config.GoonjConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;

@Component
public class DemandRepository extends BaseRepository {
    @Autowired
    public DemandRepository(RestTemplate restTemplate, GoonjConfig goonjConfig) {
        super(restTemplate, goonjConfig);
    }

    public HashMap<String, Object>[] getDemands(LocalDateTime dateTime) {
        return super.getResponse( dateTime, "DemandService/getDemands");
    }
}
