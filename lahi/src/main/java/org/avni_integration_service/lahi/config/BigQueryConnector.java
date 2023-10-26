package org.avni_integration_service.lahi.config;

import com.google.cloud.bigquery.BigQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BigQueryConnector {

    private final Logger log = LoggerFactory.getLogger(BigQueryConnector.class);

    private static final String DATASET_LOCATION = "US";

    private final BigQuery bigQuery;

    private final String dataSetName;

    public BigQueryConnector(BigQuery bigQuery,
                             @Value("${spring.cloud.gcp.bigquery.dataset-name}") String dataSetName) {
        this.bigQuery = bigQuery;
        this.dataSetName = dataSetName;
    }

    public BigQuery getBigQuery() {
        return bigQuery;
    }

    public String getDataSetName() {
        return dataSetName;
    }
}