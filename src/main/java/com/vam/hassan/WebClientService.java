import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
public class EPMPServiceImpl implements EPMPService {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private GetAccessToken getAccessToken;

    @Autowired
    private ObjectMapper objectMapper;

    private final Logger logger = LogManager.getLogger(this.getClass());

    private final Duration TIMEOUT_DURATION = Duration.ofSeconds(1);

    @Async
    public CompletableFuture<EpmpPreferencesResponse> getEpmpPreferences(EpmpPreferencesRequestDTO request, String profileType, String sessionId) {
        logger.info("Started getEpmpPreferences(): EPMPServiceImpl");
        EpmpPreferencesResponse result = new EpmpPreferencesResponse();

        // Check for a valid session before proceeding
        if (!isValidSession(sessionId)) {
            logger.error("Invalid session. Skipping API call.");
            return CompletableFuture.completedFuture(null);
        }

        if (isInvalidRequest(request)) {
            logger.error("Invalid request, one or more parameters are missing or invalid");
            return CompletableFuture.completedFuture(null);
        }

        try {
            String token = getAccessToken.getToken(stargateTokenEndPoint, stargateAccessNavTokenReq);
            EpmpPreferencesRequest requestBody = createRequestBody(request, profileType);

            if (requestBody == null) {
                logger.error("Invalid request body, profile type is missing or invalid");
                return CompletableFuture.completedFuture(null);
            }

            HttpHeaders headers = createHeaders(token);
            ContextLookHemiRequest contextRequest = createContextRequest(request, profileType);

            // Asynchronously fetch context lookup
            return getContextLookupHemiApi(contextRequest)
                    .thenCompose(contextResponse -> {
                        if (contextResponse != null && contextResponse.getContext() != null &&
                                StringUtils.isNotBlank(contextResponse.getContext().getBrand())) {

                            headers.set(APIConstants.BRAND_HEADER, contextResponse.getContext().getBrand());
                            HttpEntity<EpmpPreferencesRequest> requestEntity = new HttpEntity<>(requestBody, headers);

                            // Asynchronous call to EPMP preferences API
                            return webClientBuilder.build()
                                    .post()
                                    .uri(epmpPreferencesSearchEndpoint)
                                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                                    .bodyValue(requestBody)
                                    .retrieve()
                                    .bodyToMono(String.class)
                                    .timeout(TIMEOUT_DURATION)
                                    .toFuture()
                                    .thenApply(response -> {
                                        handleResponse(response, result);
                                        return result;
                                    });
                        } else {
                            logger.error("Brand not found in context response");
                            return CompletableFuture.completedFuture(null);
                        }
                    }).exceptionally(e -> {
                        logger.error("Error during getEpmpPreferences: {}", e.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            logger.error("Error occurred while calling EPMP service: {}", e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    @Async
    public CompletableFuture<ContextLookHemiResponse> getContextLookupHemiApi(ContextLookHemiRequest contextLookHemiRequest) {
        logger.info("Start EPMPServiceImpl: getContextLookupHemiApi method");

        String token = getAccessToken.getToken(hemiAccessTokenEndPoint, hemiAccessTokenReq);

        return webClientBuilder.build()
                .post()
                .uri(GetContextLookupEndpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(contextLookHemiRequest)
                .retrieve()
                .bodyToMono(ContextLookHemiResponse.class)
                .timeout(TIMEOUT_DURATION)
                .toFuture()
                .exceptionally(e -> {
                    logger.error("Error fetching getContextLookupHemiApi API: {}", e.getMessage());
                    return null;
                });
    }

    private void handleResponse(String responseBody, EpmpPreferencesResponse result) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            result.setPronounsValue(getJsonNodeValue(rootNode, "preferences.Personal_Identifiers.preferenceTypes.Pronouns.value"));
            result.setChosenName(getJsonNodeValue(rootNode, "contactInfo.alternateName.chosenName"));
        } catch (JsonProcessingException e) {
            logger.error("Error parsing response: {}", e.getMessage());
        }
    }

    private boolean isInvalidRequest(EpmpPreferencesRequestDTO request) {
        return StringUtils.isBlank(request.getCarrierId()) || StringUtils.isBlank(request.getAccountId()) ||
                StringUtils.isBlank(request.getGroupId());
    }

    private boolean isValidSession(String sessionId) {
        // Implement session validation logic here
        // For example, check if the session ID is not null and exists in the session store
        return sessionId != null && !sessionId.trim().isEmpty();
    }

    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set(APIConstants.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.set(APIConstants.ACTOR_HEADERS, APIConstants.ORX_AGENT);
        headers.set(APIConstants.USER_ID_HEADER, APIConstants.USER_ID_VALUE);
        headers.set(APIConstants.USER_TYPE_HEADER, APIConstants.USER_TYPE_VALUE);
        headers.set(APIConstants.INCLUDE_CONTACT_HEADER, APIConstants.INCLUDE_CONTACT);
        return headers;
    }

    private String getJsonNodeValue(JsonNode rootNode, String path) {
        JsonNode node = rootNode.at("/" + path.replace(".", "/"));
        return node.isMissingNode() ? null : node.asText();
    }
}
