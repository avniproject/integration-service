package org.avni_integration_service.tanuh.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.auth.oauth2.AwsCredentials;
import com.google.auth.oauth2.ExternalAccountCredentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import com.google.auth.oauth2.ImpersonatedCredentials;
import org.avni_integration_service.tanuh.config.TanuhConfig;
import org.avni_integration_service.util.ObjectJsonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

// Signs in to Tanuh's model service. The AWS role this job runs as is exchanged for Google credentials using Tanuh's
// credential configuration, those credentials impersonate Tanuh's service account, and an ID token for the model
// service's audience is requested. One set of credentials is kept per integration system and refreshes itself when its
// token expires. The settings are read when the credentials are first built, so a change needs a restart.
@Component
public class GoogleIdTokens implements IdTokenSource {
    static final List<String> SCOPES = List.of("https://www.googleapis.com/auth/cloud-platform");
    private static final String GOOGLE_UNIVERSE = "googleapis.com";
    private static final String GOOGLE_TOKEN_URL = "https://sts.googleapis.com/v1/token";
    private static final List<String> METADATA_HOSTS = List.of("169.254.169.254", "[fd00:ec2::254]");
    private static final String IAM_CREDENTIALS = "https://iamcredentials.googleapis.com/v1/projects/-/serviceAccounts/";

    private final Map<Integer, IdTokenCredentials> credentials = new ConcurrentHashMap<>();
    private final Function<TanuhConfig, IdTokenCredentials> builder;

    @Autowired
    public GoogleIdTokens() {
        this(GoogleIdTokens::build);
    }

    GoogleIdTokens(Function<TanuhConfig, IdTokenCredentials> builder) {
        this.builder = builder;
    }

    @Override
    public String idToken(TanuhConfig config) throws HighRiskModelException {
        return token(config, false);
    }

    @Override
    public String renewedIdToken(TanuhConfig config) throws HighRiskModelException {
        return token(config, true);
    }

    private String token(TanuhConfig config, boolean renew) throws HighRiskModelException {
        if (config.getOcshrCredentialConfig() == null)
            throw new HighRiskModelException("ocshr_credential_config is not set, so the screening cannot be signed in to Tanuh's model service");
        try {
            IdTokenCredentials idTokenCredentials = credentialsFor(config);
            if (renew) idTokenCredentials.refresh();
            else idTokenCredentials.refreshIfExpired();
            return idTokenCredentials.getIdToken().getTokenValue();
        } catch (IOException | UncheckedIOException | IllegalArgumentException e) {
            // The message is Google's reason or ours; it never carries the token.
            throw new HighRiskModelException(String.format("Could not get a Google ID token as %s: %s",
                    config.getOcshrServiceAccount(), e.getMessage()), e);
        }
    }

    private IdTokenCredentials credentialsFor(TanuhConfig config) {
        Integer key = config.getIntegrationSystem().getId();
        IdTokenCredentials existing = credentials.get(key);
        if (existing != null) return existing;
        IdTokenCredentials built = builder.apply(config);
        IdTokenCredentials won = credentials.putIfAbsent(key, built);
        return won != null ? won : built;
    }

    private static IdTokenCredentials build(TanuhConfig config) {
        try {
            return credentials(config.getOcshrCredentialConfig(), config.getOcshrServiceAccount(), config.getOcshrAudience());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // The production path from the settings to the credentials, with no network call.
    static IdTokenCredentials credentials(String credentialConfigJson, String serviceAccount, String audience) throws IOException {
        String json = withoutOwnImpersonation(credentialConfigJson, serviceAccount);
        validate(json);
        return idTokens(impersonation(json, serviceAccount), audience);
    }

    // Google's tool writes the service account impersonation into the file it issues, and Tanuh's file has it. This
    // job impersonates that account itself, to ask for an ID token, so the address is accepted only when it is
    // Google's IAM credentials address for the configured service account, and is then dropped. Any other value is
    // refused, and the message never carries it.
    static String withoutOwnImpersonation(String credentialConfigJson, String serviceAccount) {
        JsonNode root = parse(credentialConfigJson);
        if (!root.has("service_account_impersonation_url")) return credentialConfigJson;
        if (!(IAM_CREDENTIALS + serviceAccount + ":generateAccessToken").equals(text(root, "service_account_impersonation_url")))
            throw new IllegalArgumentException("ocshr_credential_config: service_account_impersonation_url must be Google's IAM credentials address for the configured service account");
        ((ObjectNode) root).remove("service_account_impersonation_url");
        return root.toString();
    }

    // Only an AWS federation that reads this job's own role and exchanges it at Google is accepted: the configuration
    // is a setting an operator pastes in, and a different shape could make the job run a program or send its
    // credentials to another address. Messages name the field and never the JSON.
    static void validate(String credentialConfigJson) {
        JsonNode root = parse(credentialConfigJson);
        if (!"external_account".equals(text(root, "type")))
            throw new IllegalArgumentException("ocshr_credential_config: type must be external_account");
        if (root.has("service_account_impersonation_url"))
            throw new IllegalArgumentException("ocshr_credential_config: service_account_impersonation_url must not be set");
        if (root.has("universe_domain") && !GOOGLE_UNIVERSE.equals(text(root, "universe_domain")))
            throw new IllegalArgumentException("ocshr_credential_config: universe_domain must be absent or googleapis.com");
        if (root.has("token_url") && !GOOGLE_TOKEN_URL.equals(text(root, "token_url")))
            throw new IllegalArgumentException("ocshr_credential_config: token_url must be absent or " + GOOGLE_TOKEN_URL);
        JsonNode source = root.get("credential_source");
        if (source == null || !source.isObject())
            throw new IllegalArgumentException("ocshr_credential_config: credential_source is missing");
        if (source.has("executable"))
            throw new IllegalArgumentException("ocshr_credential_config: credential_source.executable must not be set");
        if (!"aws1".equals(text(source, "environment_id")))
            throw new IllegalArgumentException("ocshr_credential_config: credential_source.environment_id must be aws1");
        for (String field : List.of("region_url", "url", "imdsv2_session_token_url")) {
            if (!source.has(field)) continue;
            String host = uri(text(source, field), "credential_source." + field).getHost();
            if (host == null || !METADATA_HOSTS.contains(host))
                throw new IllegalArgumentException("ocshr_credential_config: credential_source." + field + " must be the AWS metadata address");
        }
    }

    private static JsonNode parse(String credentialConfigJson) {
        JsonNode root;
        try {
            root = ObjectJsonMapper.objectMapper.readTree(credentialConfigJson);
        } catch (IOException e) {
            throw new IllegalArgumentException("ocshr_credential_config is not valid JSON");
        }
        if (root == null || !root.isObject()) throw new IllegalArgumentException("ocshr_credential_config is not a JSON object");
        return root;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private static URI uri(String value, String field) {
        try {
            if (value == null) throw new IllegalArgumentException();
            return new URI(value);
        } catch (java.net.URISyntaxException | IllegalArgumentException e) {
            throw new IllegalArgumentException("ocshr_credential_config: " + field + " is not a valid address");
        }
    }

    // Checks what the library built as well as what the file said, so a field nobody thought of cannot move the
    // sign-in off Google's own addresses. Messages never carry values from the file.
    static ImpersonatedCredentials impersonation(String credentialConfigJson, String serviceAccount) throws IOException {
        ExternalAccountCredentials external = ExternalAccountCredentials
                .fromStream(new ByteArrayInputStream(credentialConfigJson.getBytes(StandardCharsets.UTF_8)));
        if (!(external instanceof AwsCredentials))
            throw new IllegalArgumentException("ocshr_credential_config is not an AWS federation");
        if (!GOOGLE_UNIVERSE.equals(external.getUniverseDomain()))
            throw new IllegalArgumentException("ocshr_credential_config: universe_domain must be absent or googleapis.com");
        if (external.getServiceAccountImpersonationUrl() != null)
            throw new IllegalArgumentException("ocshr_credential_config: service_account_impersonation_url must not be set");
        if (!GOOGLE_TOKEN_URL.equals(external.getTokenUrl()))
            throw new IllegalArgumentException("ocshr_credential_config: token_url must be absent or " + GOOGLE_TOKEN_URL);
        GoogleCredentials source = external.createScoped(SCOPES);
        return ImpersonatedCredentials.create(source, serviceAccount, null, SCOPES, 3600);
    }

    static IdTokenCredentials idTokens(IdTokenProvider provider, String audience) {
        return IdTokenCredentials.newBuilder()
                .setIdTokenProvider(provider)
                .setTargetAudience(audience)
                .setOptions(List.of(IdTokenProvider.Option.INCLUDE_EMAIL))
                .build();
    }
}
