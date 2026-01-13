package org.avni_integration_service.web;

import org.apache.log4j.Logger;
import org.avni_integration_service.goonj.dto.GoonjAdhocTaskDTO;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.repository.UserRepository;
import org.avni_integration_service.scheduler.AdhocTaskSchedulerService;
import org.avni_integration_service.service.GoonjAdhocTaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/goonj")
public class GoonjExternalController extends BaseController{

    private final Logger logger = Logger.getLogger(GoonjExternalController.class);
    private final GoonjAdhocTaskService goonjAdhocTaskService;
    private final AdhocTaskSchedulerService adhocTaskSchedulerService;

    public GoonjExternalController(UserRepository userRepository, GoonjAdhocTaskService goonjAdhocTaskService,
                                   AdhocTaskSchedulerService adhocTaskSchedulerService) {
        super(userRepository);
        this.goonjAdhocTaskService = goonjAdhocTaskService;
        this.adhocTaskSchedulerService = adhocTaskSchedulerService;
    }
    @GetMapping("/tasktype")
    public List<String> getValidTask(){
        return goonjAdhocTaskService.getValidTasks();
    }

    @PostMapping("/v1/task")
    public ResponseEntity<?> creatAdhocTask(@Validated @RequestBody GoonjAdhocTaskDTO goonjAdhocTaskDTO, Principal principal){
        logger.info("creating goonj adhoc task for "+goonjAdhocTaskDTO);
        
        // Get the user's working integration system
        IntegrationSystem integrationSystem = getCurrentIntegrationSystem(principal);
        logger.info(String.format("User %s creating task for integration system: %s (type: %s)", 
                principal.getName(), integrationSystem.getName(), integrationSystem.getSystemType()));
        
        // Validate that the user's working integration system is a Goonj system
        if (!IntegrationSystem.IntegrationSystemType.Goonj.equals(integrationSystem.getSystemType())) {
            logger.error(String.format("User %s attempted to create Goonj task but working integration system is not Goonj: %s (type: %s)", 
                    principal.getName(), integrationSystem.getName(), integrationSystem.getSystemType()));
            return new ResponseEntity<>("User's working integration system is not a Goonj system", HttpStatus.FORBIDDEN);
        }
        
        var savedTask = goonjAdhocTaskService.createAdhocTask(goonjAdhocTaskDTO);
        adhocTaskSchedulerService.schedule(savedTask, integrationSystem);
        var response = goonjAdhocTaskService.entityToDto(savedTask);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/v1/task")
    public ResponseEntity<?> getAdhocTasks(){
        logger.info("finding all non voided goonj adhoc task");
        var response = goonjAdhocTaskService.getAllAdhocTasks();
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    @GetMapping("/v1/task/{uuid}")
    public ResponseEntity<?> getAdhocTask(@PathVariable String uuid){
        logger.info("finding goonj adhoc task for "+uuid);
        var response = goonjAdhocTaskService.getAdhocTask(uuid);
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    @DeleteMapping("/v1/task/{uuid}")
    public ResponseEntity<?> deleteAdhocTask(@PathVariable String uuid){
        logger.info("deleting goonj adhoc task for "+uuid);
        var response = goonjAdhocTaskService.deleteAdhocTask(uuid);
        return new ResponseEntity<>(response,HttpStatus.ACCEPTED);
    }

}
