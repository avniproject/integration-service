package org.avni_integration_service.web;

import org.apache.log4j.Logger;
import org.avni_integration_service.goonj.dto.GoonjAdhocTaskDTO;
import org.avni_integration_service.integration_data.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/goonj")
public class GoonjExternalController extends BaseController{

    private final Logger logger = Logger.getLogger(GoonjExternalController.class);
    public GoonjExternalController(UserRepository userRepository) {
        super(userRepository);
    }


    @PostMapping("/task")
    public ResponseEntity<GoonjAdhocTaskDTO> creatAdhocTask(@RequestBody GoonjAdhocTaskDTO goonjAdhocTaskDTO){
        logger.info("calling goonj adhoc task for"+goonjAdhocTaskDTO);
        // TODO: 16/10/24 provide logic 
        return ResponseEntity.ok(goonjAdhocTaskDTO);
    }
}
