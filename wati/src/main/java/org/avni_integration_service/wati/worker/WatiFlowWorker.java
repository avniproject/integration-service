package org.avni_integration_service.wati.worker;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.CustomQueryRequest;
import org.avni_integration_service.avni.domain.CustomQueryResponse;
import org.avni_integration_service.avni.repository.AvniQueryRepository;
import org.avni_integration_service.wati.config.WatiContextProvider;
import org.avni_integration_service.wati.config.WatiFlowConfig;
import org.avni_integration_service.wati.service.WatiMessageRequestService;
import org.avni_integration_service.util.ObjectJsonMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
            WatiFlowConfig flowConfig = watiContextProvider.get().getFlowConfig(flowName);
            if (!flowConfig.isEnabled()) {
                logger.info(String.format("Flow '%s' is disabled, skipping", flowName));
                return;
            }
            logger.info(String.format("Processing flow '%s' with query '%s'", flowName, queryName));
            processFlow(flowConfig, queryName);
        });
    }

    private void processFlow(WatiFlowConfig flowConfig, String queryName) {
        CustomQueryResponse response = avniQueryRepository.invokeCustomQuery(
                new CustomQueryRequest(queryName, flowConfig.getFlowName()));
        logger.info(String.format("Flow '%s': query returned %d rows", flowConfig.getFlowName(), response.getTotal()));
        // Isolate each row so one malformed row (e.g. a null column) cannot abort the whole flow.
        response.getData().forEach(row -> {
            try {
                processRow(row, flowConfig);
            } catch (Exception e) {
                logger.error(String.format("Flow '%s': skipping row after error: %s",
                        flowConfig.getFlowName(), e.getMessage()), e);
            }
        });
    }

    private void processRow(List<Object> row, WatiFlowConfig flowConfig) {
        if (row.isEmpty() || row.get(0) == null) {
            logger.warn(String.format("Flow '%s': skipping row with no phone number", flowConfig.getFlowName()));
            return;
        }
        String phoneNumber = row.get(0).toString();
        String locale = row.size() > 1 && row.get(1) != null ? row.get(1).toString() : null;
        String entityId = row.size() > 2 && row.get(2) != null ? row.get(2).toString() : phoneNumber;

        if (phoneNumber.isEmpty()) {
            logger.warn(String.format("Flow '%s': skipping entity %s — empty phone number",
                    flowConfig.getFlowName(), entityId));
            return;
        }

        if (!Character.isDigit(phoneNumber.charAt(0)) && !phoneNumber.startsWith("+")) {
            logger.warn(String.format("Flow '%s': phone '%s' for entity %s has unexpected format",
                    flowConfig.getFlowName(), phoneNumber, entityId));
        }

        if (watiMessageRequestService.isInCooldown(entityId, flowConfig.getFlowName(), flowConfig.getCooldownDays())) {
            logger.info(String.format("Flow '%s': skipping entity %s — in cooldown", flowConfig.getFlowName(), entityId));
            return;
        }

        String templateName = watiContextProvider.get().getTemplateName(flowConfig.getFlowName(), locale);
        if (!StringUtils.hasLength(templateName)) {
            logger.warn(String.format("Flow '%s': skipping entity %s — no template configured (locale %s)",
                    flowConfig.getFlowName(), entityId, locale));
            return;
        }
        String parametersJson = buildParametersJson(row, flowConfig.getTemplateParams());
        watiMessageRequestService.createRequest(phoneNumber, locale, entityId, templateName, parametersJson, flowConfig);
        logger.info(String.format("Flow '%s': created request for entity %s phone %s template %s",
                flowConfig.getFlowName(), entityId, phoneNumber, templateName));
    }

    private String buildParametersJson(List<Object> row, String[] paramNames) {
        if (paramNames == null || paramNames.length == 0) return null;
        List<Map<String, String>> params = new ArrayList<>();
        for (int i = 0; i < paramNames.length; i++) {
            String value = (row.size() > 3 + i && row.get(3 + i) != null) ? row.get(3 + i).toString() : "";
            Map<String, String> param = new LinkedHashMap<>();
            param.put("name", paramNames[i]);
            param.put("value", value);
            params.add(param);
        }
        // Use the shared mapper so values are escaped correctly (newlines/control chars/quotes).
        return ObjectJsonMapper.writeValueAsString(params);
    }
}
