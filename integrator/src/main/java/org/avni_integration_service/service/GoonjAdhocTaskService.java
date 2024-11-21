package org.avni_integration_service.service;

import org.avni_integration_service.goonj.GoonjAdhocTaskSatus;
import org.avni_integration_service.goonj.config.GoonjConstants;
import org.avni_integration_service.goonj.domain.GoonjAdhocTask;
import org.avni_integration_service.goonj.dto.GoonjAdhocTaskDTO;
import org.avni_integration_service.goonj.exceptions.GoonjAdhocException;
import org.avni_integration_service.goonj.job.IntegrationTask;
import org.avni_integration_service.goonj.repository.GoonjAdhocTaskRepository;
import org.avni_integration_service.goonj.util.DateTimeUtil;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GoonjAdhocTaskService {
    public static final IntegrationSystem.IntegrationSystemType INTEGRATION_SYSTEM_TYPE = IntegrationSystem.IntegrationSystemType.Goonj;
    public static final String NO_ADHOC_TASK_ERROR_MESSAGE = "There is no Goonj Adhoc Task for: ";
    public static final String INTEGRATION_SYSTEM_NAME = "Goonj";
    private final GoonjAdhocTaskRepository goonjAdhocTaskRepository;
    private final IntegrationSystemRepository integrationSystemRepository;

    public GoonjAdhocTaskService(GoonjAdhocTaskRepository goonjAdhocTaskRepository, IntegrationSystemRepository integrationSystemRepository) {
        this.goonjAdhocTaskRepository = goonjAdhocTaskRepository;
        this.integrationSystemRepository = integrationSystemRepository;
    }

    public List<String> getValidTasks() {
        return Arrays.stream(IntegrationTask.values()).filter(task -> !IntegrationTask.isInvalidTask(task)).map(Enum::name).collect(Collectors.toList());
    }

    public void dtoToEntity(GoonjAdhocTaskDTO goonjAdhocTaskDTO, GoonjAdhocTask goonjAdhocTask) {
        goonjAdhocTask.setIntegrationTask(goonjAdhocTaskDTO.getTask());
        goonjAdhocTask.setTriggerDateTime(goonjAdhocTaskDTO.getTriggerDateTime());
        goonjAdhocTask.setTaskConfig(goonjAdhocTaskDTO.getTaskConfig());
        goonjAdhocTask.setCutOffDateTime(goonjAdhocTaskDTO.getCutOffDateTime());
    }

    public GoonjAdhocTaskDTO entityToDto(GoonjAdhocTask goonjAdhocTask) {
        GoonjAdhocTaskDTO goonjAdhocTaskDTO = new GoonjAdhocTaskDTO();
        goonjAdhocTaskDTO.setUuid(goonjAdhocTask.getUuid());
        goonjAdhocTaskDTO.setTriggerDateTime(goonjAdhocTask.getTriggerDateTime());
        goonjAdhocTaskDTO.setTaskConfig(goonjAdhocTask.getTaskConfig());
        goonjAdhocTaskDTO.setCutOffDateTime(goonjAdhocTask.getCutOffDateTime());
        goonjAdhocTaskDTO.setTask(goonjAdhocTask.getIntegrationTask());
        goonjAdhocTaskDTO.setIsVoided(goonjAdhocTask.isVoided());
        goonjAdhocTaskDTO.setStatus(goonjAdhocTask.getGoonjAdhocTaskSatus().name());
        return goonjAdhocTaskDTO;
    }

    public GoonjAdhocTask createAdhocTask(GoonjAdhocTaskDTO goonjAdhocTaskDTO) {
        validateDTO(goonjAdhocTaskDTO);
        IntegrationSystem integrationSystem = integrationSystemRepository.findBySystemTypeAndName(INTEGRATION_SYSTEM_TYPE, INTEGRATION_SYSTEM_NAME);
        GoonjAdhocTask goonjAdhocTask = new GoonjAdhocTask();
        dtoToEntity(goonjAdhocTaskDTO, goonjAdhocTask);
        goonjAdhocTask.setUuid(UUID.randomUUID().toString());
        goonjAdhocTask.setVoided(false);
        goonjAdhocTask.setIntegrationSystem(integrationSystem);
        goonjAdhocTask.setGoonjAdhocTaskSatus(GoonjAdhocTaskSatus.CREATED);
        return goonjAdhocTaskRepository.save(goonjAdhocTask);
    }

    public GoonjAdhocTaskDTO getAdhocTask(String uuid) {
        GoonjAdhocTask goonjAdhocTask = findAdhocTask(uuid);
        return entityToDto(goonjAdhocTask);
    }

    private GoonjAdhocTask findAdhocTask(String uuid) {
        return goonjAdhocTaskRepository.findByUuid(uuid).orElseThrow(() ->
                new GoonjAdhocException(NO_ADHOC_TASK_ERROR_MESSAGE + uuid, HttpStatus.NOT_FOUND));
    }

    public GoonjAdhocTaskDTO deleteAdhocTask(String uuid) {
        GoonjAdhocTask goonjAdhocTask = findAdhocTask(uuid);
        goonjAdhocTask.setGoonjAdhocTaskSatus(GoonjAdhocTaskSatus.DELETED);
        goonjAdhocTask.setVoided(true);
        GoonjAdhocTask updateGoonjAdhocTask = goonjAdhocTaskRepository.save(goonjAdhocTask);
        return entityToDto(updateGoonjAdhocTask);
    }

    public List<GoonjAdhocTaskDTO> getAllAdhocTasks() {
        List<GoonjAdhocTask> goonjAdhocTasks = goonjAdhocTaskRepository.findAllByVoidedIsFalse();
        return goonjAdhocTasks.stream().map(this::entityToDto).collect(Collectors.toList());
    }

    private void validateDTO(GoonjAdhocTaskDTO goonjAdhocTaskDTO) {
        List<String> errorMessageList = new LinkedList<>();
        validateTrigger(goonjAdhocTaskDTO, errorMessageList);
        validateTask(goonjAdhocTaskDTO, errorMessageList);
        validateTaskConfig(goonjAdhocTaskDTO, errorMessageList);
        if (errorMessageList.size() > 0) {
            throw new GoonjAdhocException(String.join(" & ", errorMessageList), HttpStatus.BAD_REQUEST);
        }
    }

    private void validateTaskConfig(GoonjAdhocTaskDTO goonjAdhocTaskDTO, List<String> errorMessageList) {
        Map<String, Object> taskConfigs = goonjAdhocTaskDTO.getTaskConfig();
        if (taskConfigs != null) {
            if (IntegrationTask.isSalesforceSourceTask(goonjAdhocTaskDTO.getTask())) {
                boolean find = taskConfigs.keySet().stream().anyMatch(key -> !GoonjConstants.GOONJ_API_FILTER.contains(key));
                if (find) {
                    errorMessageList.add(String.format("please enter valid task config value from %s", GoonjConstants.GOONJ_API_FILTER));
                }
                taskConfigs.keySet().stream().forEach(key -> {
                    try {
                        URLEncoder.encode(String.valueOf(taskConfigs.get(key)), "UTF-8");
                    } catch (Exception e) {
                        errorMessageList.add(String.format("please enter valid task config entry value for key %s", key));
                    }
                });
            } else if (IntegrationTask.isAvniSourceTask(goonjAdhocTaskDTO.getTask())) {
                boolean find = taskConfigs.keySet().stream().anyMatch(key -> !GoonjConstants.AVNI_API_FILTER.contains(key));
                if (find) {
                    errorMessageList.add(String.format("please enter valid task config value from %s", GoonjConstants.AVNI_API_FILTER));
                }
            }
        }
    }

    private void validateTask(GoonjAdhocTaskDTO goonjAdhocTaskDTO, List<String> errorMessageList) {
        if (IntegrationTask.isInvalidTask(goonjAdhocTaskDTO.getTask())) {
            errorMessageList.add(String.format("%s is not valid task", goonjAdhocTaskDTO.getTask()));
        }
    }

    private void validateTrigger(GoonjAdhocTaskDTO goonjAdhocTaskDTO, List<String> errorMessageList) {
        Date date = DateTimeUtil.offsetTimeZone(DateTime.now().toDate(), DateTimeUtil.UTC, DateTimeUtil.IST);
        int days = Days.daysBetween(new DateTime(date), new DateTime(goonjAdhocTaskDTO.getTriggerDateTime())).getDays();
        if (days < -1 || days > 1) {
            errorMessageList.add(String.format("Trigger DateTime should be within range of +/- 1 day", goonjAdhocTaskDTO.getTriggerDateTime()));
        }
    }
}
