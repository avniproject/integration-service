package org.avni_integration_service.tanuh.model;

import com.google.auth.oauth2.AwsCredentials;
import com.google.auth.oauth2.IdToken;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfig;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.avni_integration_service.tanuh.config.TanuhConfig;
import org.avni_integration_service.util.ObjectJsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GoogleIdTokensTest {
    private static final String SERVICE_ACCOUNT = "sa-ocshr-avni@proj-model-hub.iam.gserviceaccount.com";

    private static String sample() throws IOException {
        return Files.readString(Path.of("src/test/resources/ocshr/partner-credential-config.sample.json"), StandardCharsets.UTF_8);
    }

    private static String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String jwt() {
        long now = System.currentTimeMillis() / 1000;
        return b64("{\"alg\":\"RS256\",\"typ\":\"JWT\"}") + "." + b64("{\"iat\":" + now + ",\"exp\":" + (now + 3600) + "}") + "." + b64("sig");
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
        system.setId(7);
        ReflectionTestUtils.setField(system, "name", "tanuh_uat_local");
        return new TanuhConfig(new IntegrationSystemConfigCollection(configs), system);
    }

    static class FakeProvider implements IdTokenProvider {
        final List<String> audiences = new ArrayList<>();
        final List<List<Option>> options = new ArrayList<>();
        final String token = jwt();

        @Override
        public IdToken idTokenWithAudience(String targetAudience, List<Option> opts) {
            audiences.add(targetAudience);
            options.add(opts);
            try {
                return IdToken.create(token);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private ObjectNode sampleNode() throws IOException {
        return (ObjectNode) ObjectJsonMapper.objectMapper.readTree(sample());
    }

    private void assertRefused(ObjectNode node, String field) {
        String json = node.toString();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> GoogleIdTokens.validate(json));
        assertTrue(e.getMessage().contains(field), e.getMessage());
        assertFalse(e.getMessage().contains(json));
        assertFalse(e.getMessage().contains("000000000000"));
    }

    @Test
    public void tanuhsConfigurationImpersonatesTheirServiceAccount() throws Exception {
        ImpersonatedCredentials credentials = GoogleIdTokens.impersonation(sample(), SERVICE_ACCOUNT);
        assertEquals(SERVICE_ACCOUNT, credentials.getAccount());
        assertTrue(credentials.getSourceCredentials() instanceof AwsCredentials);
    }

    @Test
    public void theSampleConfigurationIsAccepted() throws Exception {
        assertDoesNotThrow(() -> GoogleIdTokens.validate(sample()));
    }

    @Test
    public void theIdTokenAsksForTheAudienceAndTheCallersEmail() throws Exception {
        FakeProvider provider = new FakeProvider();
        IdTokenCredentials credentials = GoogleIdTokens.idTokens(provider, "medgemma-intake");
        credentials.refresh();
        assertEquals(List.of("medgemma-intake"), provider.audiences);
        assertTrue(provider.options.get(0).contains(IdTokenProvider.Option.INCLUDE_EMAIL));
        assertEquals(provider.token, credentials.getIdToken().getTokenValue());
    }

    @Test
    public void oneTokenServesManyCallsAndARenewalFetchesAnother() throws Exception {
        FakeProvider provider = new FakeProvider();
        GoogleIdTokens tokens = new GoogleIdTokens(c -> GoogleIdTokens.idTokens(provider, "medgemma-intake"));
        TanuhConfig config = config("ocshr_credential_config", "{}");
        assertEquals(provider.token, tokens.idToken(config));
        assertEquals(provider.token, tokens.idToken(config));
        assertEquals(1, provider.audiences.size());
        tokens.renewedIdToken(config);
        assertEquals(2, provider.audiences.size());
    }

    @Test
    public void aFailureToBuildIsNotCached() throws Exception {
        FakeProvider provider = new FakeProvider();
        int[] calls = {0};
        GoogleIdTokens tokens = new GoogleIdTokens(c -> {
            if (calls[0]++ == 0) throw new IllegalArgumentException("token_url must be https");
            return GoogleIdTokens.idTokens(provider, "medgemma-intake");
        });
        TanuhConfig config = config("ocshr_credential_config", "{}");
        HighRiskModelException e = assertThrows(HighRiskModelException.class, () -> tokens.idToken(config));
        assertTrue(e.getMessage().contains(SERVICE_ACCOUNT));
        assertEquals(provider.token, tokens.idToken(config));
    }

    @Test
    public void aConfigurationThatIsNotAnAwsFederationIsRefused() throws Exception {
        assertRefused(sampleNode().put("type", "service_account"), "type");
    }

    @Test
    public void aConfigurationThatRunsAProgramIsRefused() throws Exception {
        ObjectNode node = sampleNode();
        ((ObjectNode) node.get("credential_source")).putObject("executable").put("command", "distinctive-command-xyz");
        assertRefused(node, "credential_source.executable");
    }

    @Test
    public void aConfigurationThatExchangesTheTokenElsewhereIsRefused() throws Exception {
        assertRefused(sampleNode().put("token_url", "https://sts.example.org/v1/token"), "token_url");
    }

    @Test
    public void aConfigurationThatAlreadyImpersonatesIsRefused() throws Exception {
        assertRefused(sampleNode().put("service_account_impersonation_url", "https://iamcredentials.googleapis.com/x"), "service_account_impersonation_url");
    }

    @Test
    public void aConfigurationThatReadsAwsCredentialsElsewhereIsRefused() throws Exception {
        ObjectNode node = sampleNode();
        ((ObjectNode) node.get("credential_source")).put("url", "http://169.254.170.2/creds");
        assertRefused(node, "credential_source.url");
    }

    @Test
    public void noCredentialConfigurationMeansTheScreeningWaits() {
        GoogleIdTokens tokens = new GoogleIdTokens(c -> { throw new AssertionError("must not be built"); });
        HighRiskModelException e = assertThrows(HighRiskModelException.class, () -> tokens.idToken(config()));
        assertTrue(e.getMessage().contains("ocshr_credential_config"));
    }
}
