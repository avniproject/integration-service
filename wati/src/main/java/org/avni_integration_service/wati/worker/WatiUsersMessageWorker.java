package org.avni_integration_service.wati.worker;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.SendMessageResponse;
import org.avni_integration_service.wati.config.WatiContextProvider;
import org.avni_integration_service.wati.dto.WatiUserRequestDTO;
import org.avni_integration_service.wati.service.WatiUserMessageErrorService;
import org.avni_integration_service.wati.service.WatiUserMessageService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WatiUsersMessageWorker {
    private static final Logger logger = Logger.getLogger(WatiUsersMessageWorker.class);

    private final WatiUserMessageService watiUserMessageService;
    private final WatiUserMessageErrorService watiUserMessageErrorService;
    private final WatiContextProvider watiContextProvider;

    public WatiUsersMessageWorker(WatiUserMessageService watiUserMessageService,
                                   WatiUserMessageErrorService watiUserMessageErrorService,
                                   WatiContextProvider watiContextProvider) {
        this.watiUserMessageService = watiUserMessageService;
        this.watiUserMessageErrorService = watiUserMessageErrorService;
        this.watiContextProvider = watiContextProvider;
    }

    public void processUsers() {
        watiContextProvider.get().getQueryToTemplateNameMap().forEach((queryName, defaultTemplateName) -> {
            logger.info(String.format("Processing query '%s'", queryName));
            List<WatiUserRequestDTO> users = watiUserMessageService.getUsersForQuery(queryName, defaultTemplateName);
            users.forEach(dto -> processUser(dto, queryName));
        });
    }

    private void processUser(WatiUserRequestDTO dto, String queryName) {
        String templateName = watiContextProvider.get().getTemplateName(queryName, dto.getLocale());
        logger.info(String.format("Sending template '%s' (locale: %s) to user %s", templateName, dto.getLocale(), dto.getUserId()));
        try {
            SendMessageResponse response = watiUserMessageService.sendMessage(dto, templateName);
            watiUserMessageErrorService.saveUserMessageStatus(dto.getUserId(), response);
        } catch (Exception exception) {
            watiUserMessageErrorService.saveUserMessageError(dto.getUserId(), exception);
        }
    }
}
