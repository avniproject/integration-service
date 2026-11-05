package org.avni_integration_service.tanuh.dqs;

import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfig;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.avni_integration_service.tanuh.config.TanuhConfig;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
public class DqsClientTest {
    private static final String ROUTE = "https://dqs.example.org/v1/samvit/records/oral_screening";
    private static final String SCORED = "{\"evaluation\":{\"scored\":true,\"violations\":[],\"outcomes\":["
            + "{\"rule_id\":\"ST-001\",\"version\":\"1.0.0\",\"outcome\":\"pass\",\"evidence\":null,\"blocked_on\":[]},"
            + "{\"rule_id\":\"SL-001\",\"version\":\"1.0.0\",\"outcome\":\"fail\",\"evidence\":{\"message\":\"No location was captured for this record\"},\"blocked_on\":[]}]},"
            + "\"summary\":{\"outcomes\":{\"pass\":1,\"fail\":1}}}";
    private static final String UNREADABLE = "{\"evaluation\":{\"scored\":false,\"violations\":[{\"code\":\"IDENTITY_FIELD_MISSING\",\"level\":\"error\",\"message\":\"beneficiary_id identifies the record and is absent\"}]},\"summary\":{\"outcomes\":{}}}";

    @Mock
    private AvniHttpClient avniHttpClient;
    private final TanuhContextProvider context = new TanuhContextProvider();
    private final RestTemplate restTemplate = new RestTemplate();
    private MockRestServiceServer server;
    private DqsClient client;

    @BeforeEach
    public void setUp() {
        context.set(config("dqs_api_url", "https://dqs.example.org/", "dqs_tenant", "samvit"));
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new DqsClient(avniHttpClient, context, restTemplate);
        lenient().when(avniHttpClient.getIdToken()).thenReturn("token-1");
        lenient().when(avniHttpClient.renewIdToken()).thenReturn("token-2");
    }

    @AfterEach
    public void tearDown() {
        TanuhContextProvider.clear();
    }

    private static TanuhConfig config(String... keyValues) {
        List<IntegrationSystemConfig> configs = new ArrayList<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            IntegrationSystemConfig c = new IntegrationSystemConfig();
            c.setKey(keyValues[i]);
            c.setValue(keyValues[i + 1]);
            configs.add(c);
        }
        IntegrationSystem system = new IntegrationSystem();
        system.setId(1);
        ReflectionTestUtils.setField(system, "name", "tanuh_uat_local");
        return new TanuhConfig(new IntegrationSystemConfigCollection(configs), system);
    }

    private static Map<String, Object> payload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("record_id", "s1");
        payload.put("beneficiary_id", "p1");
        return payload;
    }

    @Test
    public void aScoredRecordIsPostedWithTheJobUsersTokenAndItsOutcomesRead() {
        server.expect(requestTo(ROUTE)).andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token-1"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.record_id").value("s1"))
                .andRespond(withSuccess(SCORED, MediaType.APPLICATION_JSON));

        DqsResult result = client.submit(DqsClient.ORAL_SCREENING, payload());

        assertEquals(200, result.status());
        assertTrue(result.scored());
        assertEquals(List.of(new DqsResult.RuleOutcome("ST-001", "pass"), new DqsResult.RuleOutcome("SL-001", "fail")), result.rules());
        server.verify();
    }

    @Test
    public void aRegistrationIsPostedToItsOwnRoute() {
        server.expect(requestTo("https://dqs.example.org/v1/samvit/records/registration")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.record_id").value("s1"))
                .andRespond(withSuccess(SCORED, MediaType.APPLICATION_JSON));

        assertTrue(client.submit(DqsClient.REGISTRATION, payload()).scored());
        server.verify();
    }

    @Test
    public void anUnreadableRecordIsStoredNotScoredAndItsViolationsAreRead() {
        server.expect(requestTo(ROUTE)).andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).body(UNREADABLE).contentType(MediaType.APPLICATION_JSON));

        DqsResult result = client.submit(DqsClient.ORAL_SCREENING, payload());

        assertEquals(422, result.status());
        assertFalse(result.scored());
        assertEquals(List.of("error IDENTITY_FIELD_MISSING: beneficiary_id identifies the record and is absent"), result.violations());
    }

    @Test
    public void anExpiredTokenIsRenewedOnceAndTheRecordSentAgain() {
        server.expect(requestTo(ROUTE)).andExpect(header("Authorization", "Bearer token-1"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"detail\":\"the token has expired\"}").contentType(MediaType.APPLICATION_JSON));
        server.expect(requestTo(ROUTE)).andExpect(header("Authorization", "Bearer token-2"))
                .andRespond(withSuccess(SCORED, MediaType.APPLICATION_JSON));

        assertTrue(client.submit(DqsClient.ORAL_SCREENING, payload()).scored());
        verify(avniHttpClient).renewIdToken();
        server.verify();
    }

    @Test
    public void aTokenRefusedTwiceFailsTheSend() {
        server.expect(requestTo(ROUTE)).andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"detail\":\"the token was issued to another client\"}").contentType(MediaType.APPLICATION_JSON));
        server.expect(requestTo(ROUTE)).andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"detail\":\"the token was issued to another client\"}").contentType(MediaType.APPLICATION_JSON));

        DqsException e = assertThrows(DqsException.class, () -> client.submit(DqsClient.ORAL_SCREENING, payload()));
        assertTrue(e.getMessage().contains("401") && e.getMessage().contains("issued to another client"), e.getMessage());
    }

    @Test
    public void aCallerTheTenantDoesNotAllowFailsTheSendNamingTheFix() {
        server.expect(requestTo(ROUTE)).andRespond(withStatus(HttpStatus.FORBIDDEN).body("{\"detail\":\"this caller is not allowed for tenant samvit\"}").contentType(MediaType.APPLICATION_JSON));

        DqsException e = assertThrows(DqsException.class, () -> client.submit(DqsClient.ORAL_SCREENING, payload()));
        assertTrue(e.getMessage().contains("403") && e.getMessage().contains("authentication file"), e.getMessage());
        verify(avniHttpClient, never()).renewIdToken();
    }

    @Test
    public void aMissingRouteAndAServiceOutageFailTheSend() {
        server.expect(requestTo(ROUTE)).andRespond(withStatus(HttpStatus.NOT_FOUND).body("{\"detail\":\"Not Found\"}").contentType(MediaType.APPLICATION_JSON));
        DqsException notFound = assertThrows(DqsException.class, () -> client.submit(DqsClient.ORAL_SCREENING, payload()));
        assertTrue(notFound.getMessage().contains("404") && notFound.getMessage().contains("oral_screening"), notFound.getMessage());

        server.reset();
        server.expect(requestTo(ROUTE)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("{\"detail\":\"cannot check tokens right now\"}").contentType(MediaType.APPLICATION_JSON));
        DqsException outage = assertThrows(DqsException.class, () -> client.submit(DqsClient.ORAL_SCREENING, payload()));
        assertTrue(outage.getMessage().contains("503") && outage.getMessage().contains("cannot check tokens"), outage.getMessage());

        server.reset();
        server.expect(requestTo(ROUTE)).andRespond(withStatus(HttpStatus.BAD_GATEWAY).body("<html>bad gateway</html>"));
        DqsException gateway = assertThrows(DqsException.class, () -> client.submit(DqsClient.ORAL_SCREENING, payload()));
        assertTrue(gateway.getMessage().contains("502") && gateway.getMessage().contains("bad gateway"), gateway.getMessage());
    }

    // A 200 that does not say how the record was scored is not a pass with no failed rules: the screening waits.
    @Test
    public void aStoredAnswerThatDoesNotSayHowItWasScoredFailsTheSend() {
        for (String body : List.of("<html>signed out</html>", "", "{}",
                "{\"evaluation\":{\"scored\":true}}",
                "{\"evaluation\":{\"scored\":true},\"summary\":{\"outcomes\":{\"fail\":1}}}",
                "{\"evaluation\":{\"scored\":true,\"outcomes\":[{\"rule_id\":\"SL-001\"}]}}")) {
            server.reset();
            server.expect(requestTo(ROUTE)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
            DqsException e = assertThrows(DqsException.class, () -> client.submit(DqsClient.ORAL_SCREENING, payload()), body);
            assertTrue(e.getMessage().contains("does not say how it was scored"), body + ": " + e.getMessage());
        }
    }

    @Test
    public void anUnreadableRecordWhoseAnswerCannotBeReadIsStillAFail() {
        server.expect(requestTo(ROUTE)).andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).body("<html>unprocessable</html>"));

        DqsResult result = client.submit(DqsClient.ORAL_SCREENING, payload());

        assertFalse(result.scored());
        assertFalse(result.passed(Set.of()));
    }

    // The job user's token goes with every record, so an address that is not https sends nothing.
    @Test
    public void anAddressThatIsNotHttpsSendsNothing() {
        context.set(config("dqs_api_url", "http://dqs.example.org", "dqs_tenant", "samvit"));

        DqsException e = assertThrows(DqsException.class, () -> client.submit(DqsClient.ORAL_SCREENING, payload()));

        assertTrue(e.getMessage().contains("is not https"), e.getMessage());
        verify(avniHttpClient, never()).getIdToken();
        server.verify();
    }

    @Test
    public void aServiceOnThisMachineMayBePlainHttp() {
        context.set(config("dqs_api_url", "http://localhost:8000", "dqs_tenant", "samvit"));
        server.expect(requestTo("http://localhost:8000/v1/samvit/records/oral_screening"))
                .andRespond(withSuccess(SCORED, MediaType.APPLICATION_JSON));

        assertTrue(client.submit(DqsClient.ORAL_SCREENING, payload()).scored());
        server.verify();
    }

    @Test
    public void aServiceThatCannotBeReachedFailsTheSend() {
        DqsClient unreachable = new DqsClient(avniHttpClient, context, new RestTemplate());
        context.set(config("dqs_api_url", "http://127.0.0.1:1", "dqs_tenant", "samvit"));
        DqsException e = assertThrows(DqsException.class, () -> unreachable.submit(DqsClient.ORAL_SCREENING, payload()));
        assertTrue(e.getMessage().contains("did not answer"), e.getMessage());
    }

    @Test
    public void noTokenIsSentWhenTheSessionDoesNotSignInToAvni() {
        when(avniHttpClient.getIdToken()).thenReturn(null);
        server.expect(requestTo(ROUTE)).andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withSuccess(SCORED, MediaType.APPLICATION_JSON));

        assertTrue(client.submit(DqsClient.ORAL_SCREENING, payload()).scored());
        server.verify();
    }

    @Test
    public void anOrganisationWithoutAnAddressCannotSend() {
        context.set(config("dqs_tenant", "samvit"));
        DqsException e = assertThrows(DqsException.class, () -> client.submit(DqsClient.ORAL_SCREENING, payload()));
        assertTrue(e.getMessage().contains("dqs_api_url"), e.getMessage());
    }
}
