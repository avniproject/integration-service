package org.avni_integration_service.wati.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.CustomQueryRequest;
import org.avni_integration_service.avni.domain.CustomQueryResponse;
import org.avni_integration_service.avni.domain.SendMessageResponse;
import org.avni_integration_service.wati.dto.WatiUserRequestDTO;
import org.avni_integration_service.wati.repository.AvniWatiUserMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WatiUserMessageService {

    private static final Logger logger = Logger.getLogger(WatiUserMessageService.class);

    private final AvniWatiUserMessageRepository avniWatiUserMessageRepository;

    @Autowired
    public WatiUserMessageService(AvniWatiUserMessageRepository avniWatiUserMessageRepository) {
        this.avniWatiUserMessageRepository = avniWatiUserMessageRepository;
    }

    /**
     * Executes the named custom query (passing templateName as query param) and returns
     * a list of DTOs. Each row is expected to have: [user_id, param1, param2, ...]
     */
    public List<WatiUserRequestDTO> getUsersForQuery(String queryName, String templateName) {
        CustomQueryRequest request = new CustomQueryRequest(queryName, templateName);
        CustomQueryResponse response = avniWatiUserMessageRepository.executeCustomQuery(request);
        logger.info(String.format("Query '%s' returned %d users", queryName, response.getTotal()));
        return response.getData().stream()
                .map(row -> {
                    String userId = row.get(0).toString();
                    String locale = row.size() > 1 ? row.get(1).toString() : null;
                    String[] parameters = row.subList(2, row.size()).stream()
                            .map(Object::toString)
                            .toArray(String[]::new);
                    return new WatiUserRequestDTO(userId, locale, parameters);
                })
                .collect(Collectors.toList());
    }

    public SendMessageResponse sendMessage(WatiUserRequestDTO dto, String templateName) {
        return avniWatiUserMessageRepository.sendMessage(dto, templateName);
    }
}
