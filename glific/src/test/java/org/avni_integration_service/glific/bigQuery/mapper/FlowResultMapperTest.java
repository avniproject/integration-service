package org.avni_integration_service.glific.bigQuery.mapper;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Schema;
import org.avni_integration_service.glific.bigQuery.builder.FieldValueListBuilder;
import org.avni_integration_service.glific.bigQuery.builder.SchemaBuilder;
import org.avni_integration_service.glific.bigQuery.domain.FlowResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FlowResultMapperTest {

    @Test
    public void shouldMapBigQueryResultsToFlowResult() {
        Schema schema = new SchemaBuilder().flowResultSchema();
        FieldValueList fieldValues = new FieldValueListBuilder().buildFlowResult(
                "913652322176",
                "23318031",
                "{\"avni_academic_year\":{\"category\":\"2023\",\"input\":\"2023-24\",\"inserted_at\":\"2023-10-20T09:08:58.566681Z\",\"intent\":null,\"interactive\":{\"description\":\"\",\"id\":\"\",\"postbackText\":\"\",\"reply\":\"2023-24 1\",\"title\":\"2023-24\"}},\"avni_alternate_contact\":{\"category\":\"All Responses\",\"input\":\"92\",\"inserted_at\":\"2023-10-20T09:09:25.764105Z\",\"intent\":null},\"avni_city_name\":{\"category\":\"^[A-Za-z ]+$\",\"input\":\"asld\",\"inserted_at\":\"2023-10-20T09:08:31.285480Z\",\"intent\":null},\"avni_date_of_birth\":{\"category\":\"dateofbirth\",\"input\":\"01-01-1890\",\"inserted_at\":\"2023-10-20T09:06:54.709856Z\",\"intent\":null},\"avni_district_name\":{\"category\":\"^[A-Za-z ]+$\",\"input\":\"sidtict\",\"inserted_at\":\"2023-10-20T09:08:27.573088Z\",\"intent\":null},\"avni_email\":{\"category\":\"Has Email\",\"input\":\"asdf@asdfa.com\",\"inserted_at\":\"2023-10-20T09:09:31.479192Z\",\"intent\":null},\"avni_father_name\":{\"category\":\"^[A-Za-z ]+$\",\"input\":\"fat\",\"inserted_at\":\"2023-10-20T09:07:06.684665Z\",\"intent\":null},\"avni_first_name\":{\"category\":\"^[A-Za-z ]+$\",\"input\":\"first name\",\"inserted_at\":\"2023-10-20T09:06:37.679357Z\",\"intent\":null},\"avni_gender\":{\"category\":\"Female\",\"input\":\"female\",\"inserted_at\":\"2023-10-20T09:07:01.398781Z\",\"intent\":null},\"avni_highest_qualification\":{\"category\":\"Graduation\",\"input\":\"Post Graduation\",\"inserted_at\":\"2023-10-20T09:08:47.861303Z\",\"intent\":null,\"interactive\":{\"description\":\"\",\"id\":\"\",\"postbackText\":\"\",\"reply\":\"Post Graduation 1\",\"title\":\"Post Graduation\"}},\"avni_last_name\":{\"category\":\"^[A-Za-z ]+$\",\"input\":\"Singh\",\"inserted_at\":\"2023-10-20T09:06:41.676437Z\",\"intent\":null},\"avni_optin\":{\"category\":\"Yes\",\"input\":\"yes\",\"inserted_at\":\"2023-10-20T09:06:23.045052Z\",\"intent\":null},\"avni_qualification_status\":{\"category\":\"Result\",\"input\":\"Result Awaited\",\"inserted_at\":\"2023-10-20T09:09:10.078292Z\",\"intent\":null,\"interactive\":{\"description\":\"\",\"id\":\"\",\"postbackText\":\"\",\"reply\":\"Result Awaited 1\",\"title\":\"Result Awaited\"}},\"avni_school_name\":{\"category\":\"All Responses\",\"input\":\"asdf. asdf.\",\"inserted_at\":\"2023-10-20T09:08:36.995705Z\",\"intent\":null},\"avni_state\":{\"category\":\"Telangana\",\"input\":\"telangana\",\"inserted_at\":\"2023-10-20T09:08:15.943382Z\",\"intent\":null},\"avni_vocational\":{\"category\":\"Non-Vocational\",\"input\":\"non-vocational\",\"inserted_at\":\"2023-10-20T09:09:19.441700Z\",\"intent\":null},\"child\":{\"Language\":{\"category\":\"English\",\"input\":\"english\",\"inserted_at\":\"2023-10-20T09:06:14.078443Z\",\"intent\":null}},\"flow_keyword\":{\"category\":\"Register me\",\"input\":\"registerme\",\"inserted_at\":\"2023-10-20T09:05:56.385111Z\"}}"
        );

        FlowResult flowResult = new FlowResultMapper().map(schema, fieldValues);

        assertEquals("913652322176", flowResult.getContactPhone());
        assertEquals("Singh", flowResult.getInput("avni_last_name"));
        assertEquals("Telangana", flowResult.getCategory("avni_state"));
    }
}
