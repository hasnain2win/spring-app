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
