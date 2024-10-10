package org.avni_integration_service.avni.domain;

import org.springframework.http.MediaType;

import java.util.Set;

public class AvniMediaConstants {
    public static final String IMAGE = "image";
    public static final Set<MediaType> SUPPORTED_IMAGE_MEDIATYPE_SET = Set.of(MediaType.IMAGE_PNG,MediaType.IMAGE_JPEG);
}
