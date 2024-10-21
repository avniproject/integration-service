package org.avni_integration_service.web;

import org.apache.log4j.Logger;
import org.avni_integration_service.goonj.dto.GoonjAdhocTaskDTO;
import org.avni_integration_service.goonj.service.GoonjAdhocTaskService;
import org.avni_integration_service.integration_data.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/goonj")
public class GoonjExternalController extends BaseController{

    private final Logger logger = Logger.getLogger(GoonjExternalController.class);
    private final GoonjAdhocTaskService goonjAdhocTaskService;
    public GoonjExternalController(UserRepository userRepository, GoonjAdhocTaskService goonjAdhocTaskService) {
        super(userRepository);
        this.goonjAdhocTaskService = goonjAdhocTaskService;
    }
    @GetMapping("/tasktype")
    public List<String> getValidTask(){
        return goonjAdhocTaskService.getValidTasks();
    }

    @PostMapping("/v1/task")
    public ResponseEntity<?> creatAdhocTask(@Validated  @RequestBody GoonjAdhocTaskDTO goonjAdhocTaskDTO){
        logger.info("creating goonj adhoc task for "+goonjAdhocTaskDTO);
        var response = goonjAdhocTaskService.createAdhocTask(goonjAdhocTaskDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/v1/task")
    public ResponseEntity<?> getAdhocTasks(){
        logger.info("finding all non voided goonj adhoc task");
        var response = goonjAdhocTaskService.getAdhocTasks();
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
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

}
