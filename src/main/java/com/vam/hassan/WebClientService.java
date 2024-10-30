package com.memberpatientdetails.epmp.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memberpatientdetails.epmp.constants.APIConstants;
import com.memberpatientdetails.epmp.request.ContextLookHemiRequest;
import com.memberpatientdetails.epmp.request.EpmpPreferencesRequest;
import com.memberpatientdetails.epmp.request.EpmpPreferencesRequestDTO;
import com.memberpatientdetails.epmp.response.ContextLookHemiResponse;
import com.memberpatientdetails.epmp.response.EpmpPreferencesResponse;
import com.memberpatientdetails.epmp.service.EPMPService;
import com.memberpatientdetails.rxcore.common.InputMetaData;
import com.memberpatientdetails.rxcore.common.getaccesstoken.GetAccessToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class EPMPServiceImpl implements EPMPService {

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private GetAccessToken getAccessToken;
    @Autowired
    private ObjectMapper objectMapper;

    private final Logger logger = LogManager.getLogger(this.getClass());

    @Value("${hemiAccessTokenEndPoint}")
    private String hemiAccessTokenEndPoint;
    @Value("${epmpPreferencesSearchEndpoint}")
    private String epmpPreferencesSearchEndpoint;
    @Value("${GetContextLookupEndpoint}")
    private String GetContextLookupEndpoint;

    @Override
    public EpmpPreferencesResponse getEpmpPreferences(EpmpPreferencesRequestDTO request, String profileType) {
        logger.info("Started getEpmpPreferences(): EPMPServiceImpl");

        if (isInvalidRequest(request)) {
            logger.error("Invalid request, one or more parameters are missing or invalid");
            return null;
        }

        try {
            String token = getAccessToken.getToken(stargateTokenEndPoint, stargateAccessNavTokenReq);
            EpmpPreferencesRequest requestBody = createRequestBody(request, profileType);

            if (requestBody == null) {
                logger.error("Invalid request body, profile type is missing or invalid");
                return null;
            }

            // Call getContextLookupHemiApi asynchronously and set timeout
            ContextLookHemiRequest contextRequest = createContextRequest(request, profileType);
            CompletableFuture<ContextLookHemiResponse> contextFuture = getContextLookupHemiApiAsync(contextRequest);

            // Wait for context lookup response with a timeout
            ContextLookHemiResponse contextResponse = contextFuture.get(1, TimeUnit.SECONDS);

            if (contextResponse != null && contextResponse.getContext() != null &&
                StringUtils.isNotBlank(contextResponse.getContext().getBrand())) {

                HttpHeaders headers = createHeaders(token);
                headers.set(APIConstants.BRAND_HEADER, contextResponse.getContext().getBrand());

                HttpEntity<EpmpPreferencesRequest> requestEntity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> responseEntity = restTemplate.exchange(
                        epmpPreferencesSearchEndpoint, HttpMethod.POST, requestEntity, String.class);

                EpmpPreferencesResponse result = new EpmpPreferencesResponse();
                handleResponse(responseEntity, result);
                return result;
            } else {
                logger.error("Brand not found in context response");
                return null;
            }
        } catch (Exception e) {
            logger.error("Error occurred while calling EPMP service, Error Message :: {}", e.getMessage());
        }

        return null;
    }

    private boolean isInvalidRequest(EpmpPreferencesRequestDTO request) {
        return StringUtils.isBlank(request.getCarrierId()) || StringUtils.isBlank(request.getAccountId()) ||
                StringUtils.isBlank(request.getGroupId());
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

    private EpmpPreferencesRequest createRequestBody(EpmpPreferencesRequestDTO request, String profileType) {
        if (StringUtils.equalsIgnoreCase(profileType, APIConstants.PBM_PROFILE_TYPE) && StringUtils.isNotBlank(request.getMemberId())
                && StringUtils.isNotBlank(request.getInstanceId())) {
            return EpmpPreferencesRequest.builder()
                    .idType(APIConstants.PBM_ID_TYPE)
                    .idValue(String.format("%s~%s~%s~%s~%s", request.getCarrierId(), request.getAccountId(), request.getGroupId(), request.getMemberId(), request.getInstanceId()))
                    .preferenceCategory(APIConstants.PREFERENCES_CATEGORY_VALUE)
                    .build();
        } else if (StringUtils.equalsIgnoreCase(profileType, APIConstants.PHARMACY_PROFILE_TYPE) && StringUtils.isNotBlank(request.getPatientId())) {
            return EpmpPreferencesRequest.builder()
                    .idType(APIConstants.PHARMACY_ID_TYPE)
                    .idValue(request.getPatientId())
                    .preferenceCategory(APIConstants.PREFERENCES_CATEGORY_VALUE)
                    .build();
        }
        return null;
    }

    private ContextLookHemiRequest createContextRequest(EpmpPreferencesRequestDTO request, String profileType) {
        ContextLookHemiRequest contextRequest = new ContextLookHemiRequest();
        contextRequest.setAccountId(request.getAccountId());
        contextRequest.setCarrierId(request.getCarrierId());
        contextRequest.setGroupId(request.getGroupId());
        contextRequest.setInputMetaData(new InputMetaData(request.getCorrelationId()));

        if (StringUtils.equalsIgnoreCase(profileType, APIConstants.PBM_PROFILE_TYPE)) {
            contextRequest.setId(request.getMemberId());
        } else if (StringUtils.equalsIgnoreCase(profileType, APIConstants.PHARMACY_PROFILE_TYPE)) {
            contextRequest.setId(request.getPatientId());
        }

        return contextRequest;
    }

    private void handleResponse(ResponseEntity<String> responseEntity, EpmpPreferencesResponse result) throws JsonProcessingException {
        if (responseEntity.getBody() != null && responseEntity.getStatusCode().is2xxSuccessful()) {
            JsonNode rootNode = objectMapper.readTree(responseEntity.getBody());
            result.setPronounsValue(getJsonNodeValue(rootNode, "preferences.Personal_Identifiers.preferenceTypes.Pronouns.value"));
            result.setChosenName(getJsonNodeValue(rootNode, "contactInfo.alternateName.chosenName"));
        } else if (responseEntity.getBody() != null) {
            logger.error("Error occurred while calling EPMP service, Error Message :: {}", StringEscapeUtils.escapeJava(responseEntity.getBody()));
        }
    }

    private String getJsonNodeValue(JsonNode rootNode, String path) {
        JsonNode node = rootNode.at("/" + path.replace(".", "/"));
        return node.isMissingNode() ? null : node.asText();
    }

    // Asynchronous method to fetch context lookup information from the HEMI API
    public CompletableFuture<ContextLookHemiResponse> getContextLookupHemiApiAsync(ContextLookHemiRequest contextLookHemiRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Start EPMPServiceImpl: getContextLookupHemiApiAsync method started");
                HttpHeaders requestHeaders = createHeadersForHemiApi(contextLookHemiRequest.getInputMetaData().getExternalCorrelationId());
                HttpEntity<ContextLookHemiRequest> requestEntity = new HttpEntity<>(contextLookHemiRequest, requestHeaders);

                ResponseEntity<ContextLookHemiResponse> response = restTemplate.exchange(
                        GetContextLookupEndpoint, HttpMethod.POST, requestEntity, ContextLookHemiResponse.class);
                return response.getBody();
            } catch (Exception e) {
                logger.error("Error fetching getContextLookupHemiApi API: {}", e.getMessage());
                return null;
            }
        });
    }

    private HttpHeaders createHeadersForHemiApi(String correlationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getAccessToken.getToken(hemiAccessTokenEndPoint, hemiAccessTokenReq));
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }
}
