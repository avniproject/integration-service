package org.avni_integration_service.goonj.domain;

import org.avni_integration_service.avni.domain.AvniMedia;

public class GoonjMedia implements AvniMedia
{
    private String externalDownloadLink;
    private String externalId;
    private String avniUrl;
    private String uuid;

    public GoonjMedia(String externalDownloadLink, String externalId, String avniUrl, String uuid) {
        this.externalDownloadLink = externalDownloadLink;
        this.externalId = externalId;
        this.avniUrl = avniUrl;
        this.uuid = uuid;
    }

    public String getExternalDownloadLink() {
        return externalDownloadLink;
    }

    public void setExternalDownloadLink(String externalDownloadLink) {
        this.externalDownloadLink = externalDownloadLink;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getAvniUrl() {
        return avniUrl;
    }

    @Override
    public void setAvniUrl(String avniUrl) {
        this.avniUrl = avniUrl;
    }

    @Override
    public String toString() {
        return "GoonjMedia{" +
                "externalDownloadLink='" + externalDownloadLink + '\'' +
                ", externalId='" + externalId + '\'' +
                ", avniUrl='" + avniUrl + '\'' +
                ", uuid='" + uuid + '\'' +
                '}';
    }
}
