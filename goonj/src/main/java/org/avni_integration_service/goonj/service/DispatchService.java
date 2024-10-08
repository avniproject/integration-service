package org.avni_integration_service.goonj.service;

import org.avni_integration_service.avni.domain.QuestionGroupObservations;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.goonj.config.GoonjContextProvider;
import org.avni_integration_service.goonj.domain.Dispatch;
import org.avni_integration_service.goonj.domain.DispatchLineItem;
import org.avni_integration_service.goonj.domain.GoonjMedia;
import org.avni_integration_service.goonj.repository.DispatchRepository;
import org.avni_integration_service.integration_data.repository.MappingMetaDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.avni_integration_service.goonj.config.GoonjMappingDbConstants.MappingGroup_Dispatch;
import static org.avni_integration_service.goonj.config.GoonjMappingDbConstants.MappingGroup_Dispatch_LineItem;

@Service
public class DispatchService extends BaseGoonjService {
    public static final String MATERIALS_DISPATCHED = "Materials Dispatched";
    private final DispatchRepository dispatchRepository;
    private final GoonjMediaService goonjMediaService;

    @Autowired
    public DispatchService(DispatchRepository dispatchRepository, GoonjMediaService goonjMediaService, MappingMetaDataRepository mappingMetaDataRepository, GoonjContextProvider goonjContextProvider) {
        super(mappingMetaDataRepository, goonjContextProvider);
        this.dispatchRepository = dispatchRepository;
        this.goonjMediaService = goonjMediaService;
    }

    public HashMap<String, Object> getDispatch(String uuid) {
        return dispatchRepository.getDispatch(uuid);
    }

    public void populateObservations(Subject subject , Dispatch dispatch) {
        populateObservations(subject, dispatch, MappingGroup_Dispatch);
        List<DispatchLineItem> lineItems = dispatch.getLineItems();
        List<Map<String, Object>> avniLineItems = new ArrayList<>();
        for (DispatchLineItem lineItem : lineItems) {
            QuestionGroupObservations questionGroupObservations = new QuestionGroupObservations();
            populateObservations(questionGroupObservations, lineItem, MappingGroup_Dispatch_LineItem);
            avniLineItems.add(questionGroupObservations.getObservations());
        }
        subject.addObservation(MATERIALS_DISPATCHED, avniLineItems);
    }

    public void populateImageConcepts(Subject oldSubject, Subject subject, Dispatch dispatch) {
        processImages(oldSubject, subject, dispatch, GoonjMediaService.IMAGES_LINK, GoonjMediaService.LOADING_AND_TRUCK_IMAGES, GoonjMediaService.INVALID_PHOTOGRAPH_URLS_RECEIVED);
    }

    private void processImages(Subject oldSubject, Subject subject, Dispatch dispatch, String goonjImagesFieldName, String avniImagesConceptName, String invalidPhotographUrlsReceived){
        List<GoonjMedia> imageList = goonjMediaService.getSalesforceImageList(dispatch, goonjImagesFieldName);
        Map<GoonjMedia, Boolean> goonjMediaBooleanMap = goonjMediaService.processMedia(oldSubject, imageList, MediaType.IMAGE_PNG, avniImagesConceptName);
        subject.addObservation(avniImagesConceptName, goonjMediaService.fetchListOfAvniUrlsToBeStoredAsConceptValue(imageList, avniImagesConceptName, goonjMediaBooleanMap));
        subject.addObservation(invalidPhotographUrlsReceived, goonjMediaService.hasAtleastOneInvalidImagesLink(goonjMediaBooleanMap).toString());
    }

}
