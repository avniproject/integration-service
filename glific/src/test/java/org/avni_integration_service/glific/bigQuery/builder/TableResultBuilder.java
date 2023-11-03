package org.avni_integration_service.glific.bigQuery.builder;

import com.google.api.gax.paging.Page;
import com.google.cloud.PageImpl;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;

public class TableResultBuilder {
    public TableResult build() {
        FieldValueListBuilder fieldValueListBuilder = new FieldValueListBuilder();
        ImmutableList<FieldValueList> queryResults = ImmutableList.of(
                fieldValueListBuilder.buildFlowResult(
                        "919317217785",
                        "11041268",
                        "{\"avni_first_name\":{\"category\":\"^[A-Za-z ]+$\",\"input\":\"Suresh\",\"inserted_at\":\"2023-09-07T06:09:52.961071Z\",\"intent\":null},\"avni_last_name\":{\"category\":\"^[A-Za-z ]+$\",\"input\":\"Yadav\",\"inserted_at\":\"2023-09-07T06:09:59.008525Z\",\"intent\":null},\"avni_optin\":{\"category\":\"Yes\",\"input\":\"Yes\",\"inserted_at\":\"2023-09-07T06:09:44.764400Z\",\"intent\":null,\"interactive\":{\"id\":\"\",\"postbackText\":\"\",\"reply\":\"Yes 1\",\"title\":\"Yes\"}},\"child\":{\"Language\":{\"category\":\"English\",\"input\":\"English\",\"inserted_at\":\"2023-09-07T06:09:37.461253Z\",\"intent\":null,\"interactive\":{\"description\":\"\",\"id\":\"\",\"postbackText\":\"\",\"reply\":\"English 1\",\"title\":\"English\"}}},\"flow_keyword\":{\"category\":\"registerme\",\"input\":\"registerme\",\"inserted_at\":\"2023-09-07T06:09:28.125929Z\"}}"),
                fieldValueListBuilder.buildFlowResult(
                        "919873249733",
                        "11041211",
                        "{\"avni_first_name\":{\"category\":\"^[A-Za-z ]+$\",\"input\":\"Sameer\",\"inserted_at\":\"2023-09-07T05:58:34.653513Z\",\"intent\":null},\"avni_last_name\":{\"category\":\"^[A-Za-z ]+$\",\"input\":\"Kohlapuri\",\"inserted_at\":\"2023-09-07T05:58:51.431716Z\",\"intent\":null},\"avni_optin\":{\"category\":\"Yes\",\"input\":\"Yes\",\"inserted_at\":\"2023-09-07T05:58:23.609434Z\",\"intent\":null,\"interactive\":{\"id\":\"\",\"postbackText\":\"\",\"reply\":\"Yes 1\",\"title\":\"Yes\"}},\"child\":{\"Language\":{\"category\":\"English\",\"input\":\"English\",\"inserted_at\":\"2023-09-07T05:58:09.972092Z\",\"intent\":null,\"interactive\":{\"description\":\"\",\"id\":\"\",\"postbackText\":\"\",\"reply\":\"English 1\",\"title\":\"English\"}}},\"flow_keyword\":{\"category\":\"registerme\",\"input\":\"registerme\",\"inserted_at\":\"2023-09-07T05:57:46.237686Z\"}}"),
                fieldValueListBuilder.buildFlowResult(
                        "919900310515",
                        "10427672",
                        "{\"avni_first_name\":{\"category\":\"^[A-Za-z ]+$\",\"input\":\"Utkarsh\",\"inserted_at\":\"2023-08-21T08:46:34.140088Z\",\"intent\":null},\"avni_last_name\":{\"category\":\"^[A-Za-z ]+$\",\"input\":\"Hathi\",\"inserted_at\":\"2023-08-21T08:46:44.420595Z\",\"intent\":null},\"avni_optin\":{\"category\":\"Yes\",\"input\":\"Yes\",\"inserted_at\":\"2023-08-21T08:46:26.843587Z\",\"intent\":null,\"interactive\":{\"id\":\"\",\"postbackText\":\"\",\"reply\":\"Yes 1\",\"title\":\"Yes\"}},\"child\":{\"Language\":{\"category\":\"English\",\"input\":\"English\",\"inserted_at\":\"2023-08-21T08:46:17.715479Z\",\"intent\":null,\"interactive\":{\"description\":\"\",\"id\":\"\",\"postbackText\":\"\",\"reply\":\"English 1\",\"title\":\"English\"}}},\"flow_keyword\":{\"category\":\"registerme\",\"input\":\"registerme\",\"inserted_at\":\"2023-08-21T08:45:54.533500Z\"}}"),
                fieldValueListBuilder.buildFlowResult(
                        "919317217785",
                        "9843637",
                        "{\"avni_optin\":{\"category\":\"Other\",\"input\":\"skillsonwheels\",\"inserted_at\":\"2023-08-18T09:53:26.753549Z\",\"intent\":null},\"child\":{\"Language\":{\"category\":\"English\",\"input\":\"English\",\"inserted_at\":\"2023-08-08T05:35:17.430486Z\",\"intent\":null,\"interactive\":{\"description\":\"\",\"id\":\"\",\"postbackText\":\"\",\"reply\":\"English 1\",\"title\":\"English\"}}},\"flow_keyword\":{\"category\":\"registerme\",\"input\":\"registerme\",\"inserted_at\":\"2023-08-08T05:34:43.903087Z\"}}")
        );

        Page<FieldValueList> resultsPage = new PageImpl<>((PageImpl.NextPageFetcher<FieldValueList>) () -> null, null, queryResults);
        return new TableResult(new SchemaBuilder().flowResultSchema(), queryResults.size(), resultsPage);
    }
}
