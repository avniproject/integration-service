package org.avni_integration_service.tanuh.model;

import com.sun.net.httpserver.HttpServer;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfig;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.avni_integration_service.tanuh.config.TanuhConfig;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.avni_integration_service.tanuh.domain.TanuhConceptsFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.http.client.config.RequestConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

// The client as the service builds it, against real sockets: the test above swaps the HTTP layer out, so it cannot see
// what the HTTP library does on its own. Tanuh's guide lists no redirect, and the ID token must never follow one.
public class OcshrClientRedirectTest {
    private final TanuhContextProvider context = new TanuhContextProvider();
    private final List<String> elsewhere = new CopyOnWriteArrayList<>();
    private HttpServer tanuh;
    private HttpServer other;

    @BeforeEach
    public void setUp() throws IOException {
        other = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        other.createContext("/", exchange -> {
            elsewhere.add(exchange.getRequestMethod() + " " + exchange.getRequestHeaders().getFirst("Authorization"));
            reply(exchange, 200, "{\"case_id\":\"s1\",\"status\":\"RECEIVED\"}");
        });
        other.start();
        tanuh = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        tanuh.createContext("/", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://127.0.0.1:" + other.getAddress().getPort() + "/elsewhere");
            reply(exchange, 303, "");
        });
        tanuh.start();
        context.set(config("ocshr_api_url", "http://127.0.0.1:" + tanuh.getAddress().getPort(), "avni_organisation_id", "1071"));
        context.setConcepts(TanuhConceptsFixture.defaults());
    }

    @AfterEach
    public void tearDown() {
        tanuh.stop(0);
        other.stop(0);
        TanuhContextProvider.clear();
    }

    private static void reply(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
        exchange.close();
    }

    private static TanuhConfig config(String... keyValues) {
        List<IntegrationSystemConfig> configs = new java.util.ArrayList<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            IntegrationSystemConfig c = new IntegrationSystemConfig();
            c.setKey(keyValues[i]);
            c.setValue(keyValues[i + 1]);
            configs.add(c);
        }
        IntegrationSystem integrationSystem = new IntegrationSystem();
        integrationSystem.setId(1);
        ReflectionTestUtils.setField(integrationSystem, "name", "tanuh_uat_local");
        return new TanuhConfig(new IntegrationSystemConfigCollection(configs), integrationSystem);
    }

    @Test
    public void aRedirectIsNotFollowedSoTheTokenStaysWithTanuh() {
        IdTokenSource tokens = new IdTokenSource() {
            @Override
            public String idToken(TanuhConfig config) {
                return "id-token";
            }

            @Override
            public String renewedIdToken(TanuhConfig config) {
                return "renewed-token";
            }
        };
        OcshrClient client = new OcshrClient(context, tokens);

        HighRiskModelException e = assertThrows(HighRiskModelException.class, () -> client.submit("s1", List.of("s3://b/k1.jpg"), null));

        assertTrue(e.getMessage().contains("303"), e.getMessage());
        assertEquals(List.of(), elsewhere, "nothing, and no token, reached the address the redirect named");
    }

    // A slow send must still end, so the screening waits for a retry rather than holding the run.
    @Test
    public void theTimeoutsStillReachTheHttpClient() {
        OcshrClient client = new OcshrClient(context, null);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        RequestConfig requestConfig = (RequestConfig) ReflectionTestUtils.getField(restTemplate.getRequestFactory(), "requestConfig");

        assertEquals(OcshrClient.CONNECT_TIMEOUT.toMillis(), requestConfig.getConnectTimeout());
        assertEquals(OcshrClient.READ_TIMEOUT.toMillis(), requestConfig.getSocketTimeout());
    }
}
