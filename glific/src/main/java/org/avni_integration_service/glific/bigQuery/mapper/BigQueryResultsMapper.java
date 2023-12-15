package org.avni_integration_service.glific.bigQuery.mapper;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;

import java.util.Iterator;

public class BigQueryResultsMapper<T> {
    public Iterator<T> map(TableResult response, BigQueryResultMapper<T> resultMapper) {
        Schema schema = response.getSchema();
        Iterator<FieldValueList> iterator = response.iterateAll().iterator();

        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return resultMapper.map(schema, iterator.next());
            }
        };
    }
}
