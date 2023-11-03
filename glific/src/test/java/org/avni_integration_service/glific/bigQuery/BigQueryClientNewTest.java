package org.avni_integration_service.glific.bigQuery;

import com.google.cloud.bigquery.*;
import org.avni_integration_service.glific.bigQuery.builder.TableResultBuilder;
import org.avni_integration_service.glific.bigQuery.config.BigQueryConnector;
import org.avni_integration_service.glific.bigQuery.domain.FlowResult;
import org.avni_integration_service.glific.bigQuery.mapper.FlowResultMapper;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BigQueryClientNewTest {

    @Test
    public void shouldMapResponseToMapper() throws InterruptedException {
        FlowResultMapper flowResultMapper = new FlowResultMapper();
        TableResult tableResult = new TableResultBuilder().build();
        BigQueryConnector bqConnector = mock(BigQueryConnector.class);
        BigQuery bigQuery = mock(BigQuery.class);
        when(bqConnector.getBigQuery()).thenReturn(bigQuery);
        Job job = mock(Job.class);
        when(bigQuery.create(any(JobInfo.class))).thenReturn(job);
        when(job.getQueryResults()).thenReturn(tableResult);
        when(job.waitFor()).thenReturn(job);
        JobStatus jobStatus = mock(JobStatus.class);
        when(job.getStatus()).thenReturn(jobStatus);
        when(jobStatus.getError()).thenReturn(null);

        Iterator<FlowResult> results = new BigQueryClientNew(bqConnector).getResults("query", "2023-01-01", 5, flowResultMapper);

        FlowResult firstFlowResult = results.next();
        assertEquals("919317217785", firstFlowResult.getContactPhone());
        assertEquals("919317217785", firstFlowResult.getContactPhone());
        assertEquals("Suresh", firstFlowResult.getInput("avni_first_name"));
    }
}
