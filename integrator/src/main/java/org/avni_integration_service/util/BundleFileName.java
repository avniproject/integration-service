package org.avni_integration_service.util;

public enum BundleFileName {
    MAPPING_TYPES("mappingTypes.json"),
    MAPPING_GROUPS("mappingGroups.json"),
    MAPPING_METADATA("mappingMetadata.json"),
    ERROR_TYPES("errorTypes.json");

    private final String bundleFileName;

    public String getBundleFileName() {
        return bundleFileName;
    }

    BundleFileName(String bundleFileName) {
        this.bundleFileName = bundleFileName;
    }
}
