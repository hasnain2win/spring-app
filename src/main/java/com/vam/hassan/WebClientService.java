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

@Service
public class EPMPServiceImpl implements EPMPService {
   
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private GetAccessToken getAccessToken;
    @Autowired
    private ObjectMapper objectMapper;

    private final Logger logger = LogManager.getLogger(this.getClass());

    @Override
    public EpmpPreferencesResponse getEpmpPreferences(EpmpPreferencesRequestDTO request, String profileType) {
        logger.info("Started getEpmpPreferences(): EPMPServiceImpl");
        EpmpPreferencesResponse result = new EpmpPreferencesResponse();

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

            HttpHeaders headers = createHeaders(token);
            ContextLookHemiResponse contextResponse = getContextLookupHemiApi(createContextRequest(request, profileType));

            if (contextResponse != null && contextResponse.getContext() != null && StringUtils.isNotBlank(contextResponse.getContext().getBrand())) {
                logger.info("Brand found in context response: {}", StringEscapeUtils.escapeJava(contextResponse.getContext().getBrand()));
                headers.set(APIConstants.BRAND_HEADER, contextResponse.getContext().getBrand());
            } else {
                logger.error("Brand not found in context response");
                return null;
            }

            HttpEntity<EpmpPreferencesRequest> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(epmpPreferencesSearchEndpoint, HttpMethod.POST, requestEntity, String.class);
            handleResponse(responseEntity, result);
        } catch (Exception e) {
            logger.info("Error occurred while calling EPMP service, Error Message :: {}", e.getMessage());
        }

        logger.info("Completed getEpmpPreferences(): EPMPServiceImpl");
        return result;
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

    /**
     * Creates an EpmpPreferencesRequest based on the provided member patient details and profile type.
     *
     * @param request     the details of the member patient
     * @param profileType the type of profile (e.g., PBM or Pharmacy)
     * @return an EpmpPreferencesRequest containing the EPMP preferences for the given member patient, or null if the profile type is invalid
     */
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

    /**
     * Fetches context lookup information from the HEMI API.
     *
     * @param contextLookHemiRequest the request object containing the necessary details for the context lookup
     * @return the response from the HEMI API containing context lookup information
     */

    public ContextLookHemiResponse getContextLookupHemiApi(ContextLookHemiRequest contextLookHemiRequest) {
        logger.info("Start EPMPServiceImpl: getContextLookupHemiApi method started");
        LocalDateTime initialTime = LocalDateTime.now();
        String correlationId = StringEscapeUtils.escapeJava(contextLookHemiRequest.getInputMetaData().getExternalCorrelationId());

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        String token = getAccessToken.getToken(hemiAccessTokenEndPoint, hemiAccessTokenReq);
        requestHeaders.set("Authorization", "Bearer " + token);
        requestHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        InputMetaData inputMetaData = new InputMetaData();
        inputMetaData.setConsumerAppId(this.consumerAppId);
        inputMetaData.setConsumerAppType(this.consumerAppType);
        inputMetaData.setConsumerType(this.consumerType);
        inputMetaData.setExternalCorrelationId(correlationId);

        ContextLookHemiRequest requestBody = ContextLookHemiRequest.builder()
                .accountId(contextLookHemiRequest.getAccountId())
                .carrierId(contextLookHemiRequest.getCarrierId())
                .command(APIConstants.EPMP_UI_COMMAND)
                .groupId(contextLookHemiRequest.getGroupId())
                .id(contextLookHemiRequest.getId())
                .idType(APIConstants.EPMP_UI_ID_TYPE)
                .inputMetaData(inputMetaData)
                .build();

        HttpEntity<ContextLookHemiRequest> requestEntity = new HttpEntity<>(requestBody, requestHeaders);
        ResponseEntity<ContextLookHemiResponse> contextLookHemiResponse = null;

        try {
            contextLookHemiResponse = restTemplate.exchange(GetContextLookupEndpoint, HttpMethod.POST, requestEntity, ContextLookHemiResponse.class);
            logger.info("getContextLookupHemiApi method completed in: {}", ChronoUnit.MILLIS.between(initialTime, LocalDateTime.now()));
        } catch (Exception exception) {
            logger.error("Error fetching getContextLookupHemiApi API: {} and CorrelationId: {}", exception.getMessage(), correlationId);
        }

        return contextLookHemiResponse.getBody();
    }
}
