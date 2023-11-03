package org.avni_integration_service.glific.bigQuery.mapper;

import com.google.cloud.bigquery.TableResult;
import org.avni_integration_service.glific.bigQuery.builder.TableResultBuilder;
import org.avni_integration_service.glific.bigQuery.domain.FlowResult;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BigQueryResultsMapperTest {

    @Test
    public void shouldCleanlyIterateOverABigQueryResult() {
        TableResult tableResult = new TableResultBuilder().build();

        Iterator<FlowResult> flowResults = new BigQueryResultsMapper<FlowResult>().map(tableResult, new FlowResultMapper());

        FlowResult firstFlowResult = flowResults.next();
        assertEquals("919317217785", firstFlowResult.getContactPhone());
        assertEquals("919317217785", firstFlowResult.getContactPhone());
        assertEquals("Suresh", firstFlowResult.getInput("avni_first_name"));

        assertEquals(3, remainingItemsIn(flowResults));
    }

    private int remainingItemsIn(Iterator<FlowResult> iterator) {
        int count = 0;
        while (iterator.hasNext()) {
            count++;
            iterator.next();
        }
        return count;
    }

}
