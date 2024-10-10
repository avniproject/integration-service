package org.avni_integration_service.goonj.domain;

import org.avni_integration_service.avni.domain.AvniMedia;
import org.springframework.http.MediaType;

public class GoonjMedia implements AvniMedia
{
    private String externalDownloadLink;
    private String externalId;
    private String avniUrl;
    private String uuid;
    private String extention;
    private MediaType contentType;

    public GoonjMedia(String externalDownloadLink, String externalId, String avniUrl, String uuid,String extention,MediaType contentType) {
        this.externalDownloadLink = externalDownloadLink;
        this.externalId = externalId;
        this.avniUrl = avniUrl;
        this.uuid = uuid;
        this.extention = extention;
        this.contentType = contentType;
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

    public String getExtention() {
        return extention;
    }

    public void setExtention(String extention) {
        this.extention = extention;
    }

    public MediaType getContentType() {
        return contentType;
    }

    public void setContentType(MediaType contentType) {
        this.contentType = contentType;
    }

    @Override
    public String toString() {
        return "GoonjMedia{" +
                "externalDownloadLink='" + externalDownloadLink + '\'' +
                ", externalId='" + externalId + '\'' +
                ", avniUrl='" + avniUrl + '\'' +
                ", uuid='" + uuid + '\'' +
                ", extention='" + extention + '\'' +
                ", contentType=" + contentType +
                '}';
    }
}
