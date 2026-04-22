package org.avni_integration_service.wati.worker;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.CustomQueryRequest;
import org.avni_integration_service.avni.domain.CustomQueryResponse;
import org.avni_integration_service.avni.repository.AvniQueryRepository;
import org.avni_integration_service.wati.config.WatiContextProvider;
import org.avni_integration_service.wati.config.WatiFlowConfig;
import org.avni_integration_service.wati.service.WatiMessageRequestService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class WatiFlowWorker {

    private static final Logger logger = Logger.getLogger(WatiFlowWorker.class);

    private final WatiContextProvider watiContextProvider;
    private final AvniQueryRepository avniQueryRepository;
    private final WatiMessageRequestService watiMessageRequestService;

    public WatiFlowWorker(WatiContextProvider watiContextProvider,
                          AvniQueryRepository avniQueryRepository,
                          WatiMessageRequestService watiMessageRequestService) {
        this.watiContextProvider = watiContextProvider;
        this.avniQueryRepository = avniQueryRepository;
        this.watiMessageRequestService = watiMessageRequestService;
    }

    public void processAllFlows() {
        Map<String, String> flowToQueryMap = watiContextProvider.get().getFlowToQueryMap();
        logger.info(String.format("Processing %d flows", flowToQueryMap.size()));
        flowToQueryMap.forEach((flowName, queryName) -> {
            logger.info(String.format("Processing flow '%s' with query '%s'", flowName, queryName));
            WatiFlowConfig flowConfig = watiContextProvider.get().getFlowConfig(flowName);
            processFlow(flowConfig, queryName);
        });
    }

    private void processFlow(WatiFlowConfig flowConfig, String queryName) {
        CustomQueryResponse response = avniQueryRepository.invokeCustomQuery(
                new CustomQueryRequest(queryName, flowConfig.getFlowName()));
        logger.info(String.format("Flow '%s': query returned %d rows", flowConfig.getFlowName(), response.getTotal()));
        response.getData().forEach(row -> processRow(row, flowConfig));
    }

    private void processRow(List<Object> row, WatiFlowConfig flowConfig) {
        String phoneNumber = row.get(0).toString();
        String locale = row.size() > 1 && row.get(1) != null ? row.get(1).toString() : null;
        String entityId = row.size() > 2 ? row.get(2).toString() : phoneNumber;

        if (watiMessageRequestService.isInCooldown(entityId, flowConfig.getFlowName(), flowConfig.getCooldownDays())) {
            logger.info(String.format("Flow '%s': skipping entity %s — in cooldown", flowConfig.getFlowName(), entityId));
            return;
        }

        String templateName = watiContextProvider.get().getTemplateName(flowConfig.getFlowName(), locale);
        watiMessageRequestService.createRequest(phoneNumber, locale, entityId, templateName, flowConfig);
        logger.info(String.format("Flow '%s': created request for entity %s phone %s template %s",
                flowConfig.getFlowName(), entityId, phoneNumber, templateName));
    }
}
