package org.avni_integration_service.avni.domain;

import org.springframework.http.MediaType;

import java.util.Set;

public class AvniMediaConstants {
    public static final String IMAGE = "image";
    public static final MediaType MediaType_IMAGE_JPG = new MediaType("image", "jpg");
    public static final Set<MediaType> SUPPORTED_IMAGE_MEDIA_TYPE_SET = Set.of(MediaType.IMAGE_PNG, MediaType.IMAGE_JPEG, MediaType_IMAGE_JPG);
}
