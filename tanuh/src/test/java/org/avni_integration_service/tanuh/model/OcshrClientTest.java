package org.avni_integration_service.tanuh.model;

import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfig;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.avni_integration_service.tanuh.config.TanuhConfig;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.avni_integration_service.tanuh.domain.TanuhConcepts;
import org.avni_integration_service.tanuh.domain.TanuhConceptsFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class OcshrClientTest {
    private static final String URL = "https://ocshr.example.org/v1/cases";
    private static final String BODY = "{\"case_id\":\"s1\",\"tenant_id\":1071,\"encounter_type\":\"Oral Screening\",\"images\":[{\"uri\":\"s3://b/k1.jpg\"},{\"uri\":\"s3://b/k2.jpg\"}]}";
    private static final List<String> IMAGES = List.of("s3://b/k1.jpg", "s3://b/k2.jpg");
    private static final String RECEIVED = "{\"case_id\":\"s1\",\"status\":\"RECEIVED\",\"image_count\":2,\"error_code\":null,\"error_detail\":null}";

    private final AtomicInteger tokenRequests = new AtomicInteger();
    private final IdTokenSource tokens = new IdTokenSource() {
        @Override
        public String idToken(TanuhConfig config) {
            tokenRequests.incrementAndGet();
            return "id-token";
        }

        @Override
        public String renewedIdToken(TanuhConfig config) {
            tokenRequests.incrementAndGet();
            return "renewed-token";
        }
    };
    private final TanuhContextProvider context = new TanuhContextProvider();
    private final RestTemplate restTemplate = new RestTemplate();
    private MockRestServiceServer server;
    private OcshrClient client;

    @BeforeEach
    public void setUp() {
        context.set(config("ocshr_api_url", "https://ocshr.example.org/", "avni_organisation_id", "1071"));
        context.setConcepts(TanuhConceptsFixture.defaults());
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new OcshrClient(context, tokens, restTemplate);
    }

    private static TanuhConfig config(String... keyValues) {
        List<IntegrationSystemConfig> configs = new ArrayList<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            IntegrationSystemConfig c = new IntegrationSystemConfig();
            c.setKey(keyValues[i]);
            c.setValue(keyValues[i + 1]);
            configs.add(c);
        }
        IntegrationSystem integrationSystem = new IntegrationSystem();
        integrationSystem.setId(1);
        ReflectionTestUtils.setField(integrationSystem, "name", "Tanuh_UAT_Local");
        return new TanuhConfig(new IntegrationSystemConfigCollection(configs), integrationSystem);
    }

    private Submission submit() throws HighRiskModelException {
        return client.submit("s1", IMAGES);
    }

    private void answer(HttpStatus status, String body) {
        server.expect(requestTo(URL)).andRespond(withStatus(status).contentType(MediaType.APPLICATION_JSON).body(body));
    }

    @Test
    public void postsTheCaseIdAndTheImagesInOrderWithTheIdToken() throws Exception {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer id-token"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(BODY, true))
                .andRespond(withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body(RECEIVED));
        assertEquals(Submission.Outcome.ACCEPTED, submit().outcome());
        server.verify();
    }

    @Test
    public void theEncounterTypeIsTheNameTheOrganisationGivesTheOralScreening() throws Exception {
        TanuhConcepts renamed = TanuhConceptsFixture.editedBy(rows -> {
            rows.replaceAll(row -> row.key().equals("oral_screening") ? new TanuhConcepts.Mapping("oral_screening", "Mouth Screening") : row);
            return rows;
        });
        context.setConcepts(renamed);
        server.expect(requestTo(URL))
                .andExpect(content().json("{\"case_id\":\"s1\",\"tenant_id\":1071,\"encounter_type\":\"Mouth Screening\",\"images\":[{\"uri\":\"s3://b/k1.jpg\"},{\"uri\":\"s3://b/k2.jpg\"}]}", true))
                .andRespond(withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body(RECEIVED));
        assertEquals(Submission.Outcome.ACCEPTED, submit().outcome());
        server.verify();
    }

    // Tanuh's service tells cases apart by the organisation they came from, so a case without it is not sent: the
    // screening waits, with nothing written, until the setting is put right.
    @Test
    public void noOrganisationIdMeansTheScreeningWaits() {
        context.set(config("ocshr_api_url", "https://ocshr.example.org/"));
        HighRiskModelException e = assertThrows(HighRiskModelException.class, this::submit);
        assertTrue(e.getMessage().contains("avni_organisation_id"), e.getMessage());
        server.verify();
        assertEquals(0, tokenRequests.get());
    }

    @Test
    public void anOrganisationIdThatIsNotAWholeNumberMeansTheScreeningWaits() {
        for (String value : List.of("tanuh_uat", "0", "-1071", "1071.0", "  ")) {
            context.set(config("ocshr_api_url", "https://ocshr.example.org/", "avni_organisation_id", value));
            HighRiskModelException e = assertThrows(HighRiskModelException.class, this::submit, value);
            assertTrue(e.getMessage().contains("avni_organisation_id"), e.getMessage());
        }
        server.verify();
        assertEquals(0, tokenRequests.get());
    }

    @Test
    public void theSameRequestAgainIsAccepted() throws Exception {
        answer(HttpStatus.OK, "{\"case_id\":\"s1\",\"status\":\"QUEUED\"}");
        assertEquals(Submission.Outcome.ACCEPTED, submit().outcome());
    }

    @Test
    public void anAcceptanceThatDoesNotNameTheCaseWaitsForARetry() {
        server.expect(requestTo(URL)).andRespond(withSuccess("<html>ok</html>", MediaType.TEXT_HTML));
        assertThrows(HighRiskModelException.class, this::submit);
        server.reset();
        answer(HttpStatus.ACCEPTED, "{\"case_id\":\"other\",\"status\":\"RECEIVED\"}");
        assertThrows(HighRiskModelException.class, this::submit);
        server.reset();
        answer(HttpStatus.ACCEPTED, "{\"case_id\":\"s1\"}");
        assertThrows(HighRiskModelException.class, this::submit);
    }

    @Test
    public void aCaseHeldWithOtherPhotosIsAConflict() throws Exception {
        String body = "{\"error\":\"CASE_EXISTS_WITH_DIFFERENT_MANIFEST\",\"case_id\":\"s1\"}";
        answer(HttpStatus.CONFLICT, body);
        Submission submission = submit();
        assertEquals(Submission.Outcome.CONFLICT, submission.outcome());
        assertEquals(body, submission.detail());
    }

    @Test
    public void aCaseThatBreaksTheRulesIsRefused() throws Exception {
        answer(HttpStatus.UNPROCESSABLE_ENTITY, "{\"error\":\"INVALID_MANIFEST\",\"problems\":[\"image 2: same uri as image 1\"]}");
        Submission submission = submit();
        assertEquals(Submission.Outcome.REFUSED, submission.outcome());
        assertTrue(submission.detail().contains("image 2"));
    }

    @Test
    public void theDetailIsCutTo300Characters() throws Exception {
        answer(HttpStatus.UNPROCESSABLE_ENTITY, "{\"error\":\"INVALID_MANIFEST\",\"problems\":[\"" + "x".repeat(500) + "\"]}");
        assertEquals(300, submit().detail().length());
    }

    @Test
    public void aRequestTheServiceCannotReadWaitsForARetry() {
        answer(HttpStatus.UNPROCESSABLE_ENTITY, "{\"detail\":[{\"type\":\"extra_forbidden\",\"loc\":[\"body\",\"images\",0,\"sha_256\"]}]}");
        assertThrows(HighRiskModelException.class, this::submit);
    }

    @Test
    public void aRefusedTokenIsRenewedOnce() throws Exception {
        server.expect(ExpectedCount.once(), requestTo(URL)).andExpect(header("Authorization", "Bearer id-token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(ExpectedCount.once(), requestTo(URL)).andExpect(header("Authorization", "Bearer renewed-token"))
                .andExpect(content().json(BODY, true))
                .andRespond(withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body(RECEIVED));
        assertEquals(Submission.Outcome.ACCEPTED, submit().outcome());
        server.verify();

        server.reset();
        server.expect(ExpectedCount.times(2), requestTo(URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        HighRiskModelException e = assertThrows(HighRiskModelException.class, this::submit);
        assertTrue(e.getMessage().contains("401"));
        server.verify();
    }

    @Test
    public void refusedOrBusyOrBrokenServicesWaitForARetry() {
        for (HttpStatus status : List.of(HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND, HttpStatus.TOO_MANY_REQUESTS,
                HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.SERVICE_UNAVAILABLE)) {
            server.reset();
            answer(status, "{\"error\":\"X\"}");
            HighRiskModelException e = assertThrows(HighRiskModelException.class, this::submit);
            assertTrue(e.getMessage().contains(String.valueOf(status.value())), e.getMessage());
        }
        server.reset();
        answer(HttpStatus.CONFLICT, "{\"error\":\"SOMETHING_ELSE\"}");
        HighRiskModelException e = assertThrows(HighRiskModelException.class, this::submit);
        assertTrue(e.getMessage().contains("409"));
    }

    @Test
    public void noAnswerWaitsForARetry() {
        server.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("Read timed out")));
        assertThrows(HighRiskModelException.class, this::submit);
    }

    @Test
    public void noAddressMeansTheScreeningWaits() {
        context.set(config());
        HighRiskModelException e = assertThrows(HighRiskModelException.class, this::submit);
        assertTrue(e.getMessage().contains("ocshr_api_url"));
        server.verify();
        assertEquals(0, tokenRequests.get());
    }

    @Test
    public void theTokenNeverTravelsInTheClear() throws Exception {
        context.set(config("ocshr_api_url", "http://ocshr.example.org", "avni_organisation_id", "1071"));
        assertThrows(HighRiskModelException.class, this::submit);
        server.verify();
        assertEquals(0, tokenRequests.get());

        context.set(config("ocshr_api_url", "http://localhost:8099", "avni_organisation_id", "1071"));
        server.expect(requestTo("http://localhost:8099/v1/cases"))
                .andRespond(withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body(RECEIVED));
        assertEquals(Submission.Outcome.ACCEPTED, submit().outcome());
    }

    @Test
    public void theTokenIsInNoMessage() {
        server.expect(ExpectedCount.times(2), requestTo(URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        HighRiskModelException e = assertThrows(HighRiskModelException.class, this::submit);
        assertFalse(e.getMessage().contains("id-token"));
        assertFalse(e.getMessage().contains("renewed-token"));
    }
}
