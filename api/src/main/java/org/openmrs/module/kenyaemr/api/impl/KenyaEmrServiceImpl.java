/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.api.impl;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONObject;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.LocationAttributeType;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Visit;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.idgen.AutoGenerationOption;
import org.openmrs.module.idgen.IdentifierSource;
import org.openmrs.module.idgen.SequentialIdentifierGenerator;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.idgen.validator.LuhnModNIdentifierValidator;
import org.openmrs.module.kenyacore.identifier.IdentifierManager;
import org.openmrs.module.kenyaemr.EmrConstants;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaemr.api.db.KenyaEmrDAO;
import org.openmrs.module.kenyaemr.api.events.EventsBroadcaster;
import org.openmrs.module.kenyaemr.api.impl.token.TokenCache;
import org.openmrs.module.kenyaemr.api.impl.token.TokenService;
import org.openmrs.module.kenyaemr.api.model.BiometricVerification;
import org.openmrs.module.kenyaemr.api.model.OtpUseRequest;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.FacilityMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.util.EmrUtils;
import org.openmrs.module.kenyaemr.util.RowMapper;
import org.openmrs.module.kenyaemr.util.SqlQueryHelper;
import org.openmrs.module.kenyaemr.wrapper.Facility;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.util.DatabaseUpdater;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementations of business logic methods for KenyaEMR
 */
public class KenyaEmrServiceImpl extends BaseOpenmrsService implements KenyaEmrService {

	protected static final Log log = LogFactory.getLog(KenyaEmrServiceImpl.class);

	protected static final String OPENMRS_MEDICAL_RECORD_NUMBER_NAME = "Kenya EMR - OpenMRS Medical Record Number";
	protected static final String HIV_UNIQUE_PATIENT_NUMBER_NAME = "Kenya EMR - OpenMRS HIV Unique Patient Number";

	@Autowired
	private IdentifierManager identifierManager;

	@Autowired
	private LocationService locationService;

	private boolean setupRequired = true;

	private KenyaEmrDAO dao;

	/**
	 * Method used to inject the data access object.
	 * @param dao the data access object.
	 */
	public void setKenyaEmrDAO(KenyaEmrDAO dao) {
		this.dao = dao;
	}

	/**
	 * @see org.openmrs.module.kenyaemr.api.KenyaEmrService#isSetupRequired()
	 */
	@Override
	public boolean isSetupRequired() {
		// Assuming that it's not possible to _un_configure after having configured, i.e. after the first
		// time we return true we can save time by not re-checking things
		if (!setupRequired) {
			return false;
		}

		boolean defaultLocationConfigured = getDefaultLocation() != null;
		boolean mrnConfigured = identifierManager.getIdentifierSource(MetadataUtils.existing(PatientIdentifierType.class, CommonMetadata._PatientIdentifierType.OPENMRS_ID)) != null;
		boolean upnConfigured = identifierManager.getIdentifierSource(MetadataUtils.existing(PatientIdentifierType.class, HivMetadata._PatientIdentifierType.UNIQUE_PATIENT_NUMBER)) != null;

		setupRequired = !(defaultLocationConfigured && mrnConfigured && upnConfigured);
		return setupRequired;
	}

	/**
	 * @see org.openmrs.module.kenyaemr.api.KenyaEmrService#setDefaultLocation(org.openmrs.Location)
	 */
	@Override
	public void setDefaultLocation(Location location) {
		GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(EmrConstants.GP_DEFAULT_LOCATION);
		gp.setValue(location);
		Context.getAdministrationService().saveGlobalProperty(gp);
	}

	/**
	 * @see org.openmrs.module.kenyaemr.api.KenyaEmrService#getDefaultLocation()
	 */
	@Override
	public Location getDefaultLocation() {
		try {
			Context.addProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
			Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);

			GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(EmrConstants.GP_DEFAULT_LOCATION);
			return gp != null ? ((Location) gp.getValue()) : null;
		}
		finally {
			Context.removeProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
			Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
		}
	}

	/**
	 * @see org.openmrs.module.kenyaemr.api.KenyaEmrService#getDefaultLocationMflCode()
	 */
	@Override
	public String getDefaultLocationMflCode() {
		try {
			Context.addProxyPrivilege(PrivilegeConstants.GET_LOCATION_ATTRIBUTE_TYPES);

			Location location = getDefaultLocation();
			return (location != null) ? new Facility(location).getMflCode() : null;
		}
		finally {
			Context.removeProxyPrivilege(PrivilegeConstants.GET_LOCATION_ATTRIBUTE_TYPES);
		}
	}

	/**
	 * @see org.openmrs.module.kenyaemr.api.KenyaEmrService#getLocationByMflCode(String)
	 */
	@Override
	public Location getLocationByMflCode(String mflCode) {
		LocationAttributeType mflCodeAttrType = MetadataUtils.existing(LocationAttributeType.class, FacilityMetadata._LocationAttributeType.MASTER_FACILITY_CODE);
		Map<LocationAttributeType, Object> attrVals = new HashMap<LocationAttributeType, Object>();
		attrVals.put(mflCodeAttrType, mflCode);

		List<Location> locations = locationService.getLocations(null, null, attrVals, false, null, null);

		return locations.size() > 0 ? locations.get(0) : null;
	}

	/**
	 * @see org.openmrs.module.kenyaemr.api.KenyaEmrService#getNextHivUniquePatientNumber(String)
	 */
	@Override
	public String getNextHivUniquePatientNumber(String comment) {
		if (comment == null) {
			comment = "KenyaEMR Service";
		}

		PatientIdentifierType upnType = MetadataUtils.existing(PatientIdentifierType.class, HivMetadata._PatientIdentifierType.UNIQUE_PATIENT_NUMBER);
		IdentifierSource source = identifierManager.getIdentifierSource(upnType);

		String prefix = Context.getService(KenyaEmrService.class).getDefaultLocationMflCode();
		String sequentialNumber = Context.getService(IdentifierSourceService.class).generateIdentifier(source, comment);
		return prefix+sequentialNumber;
	}

	/**
	 * @see KenyaEmrService#getVisitsByPatientAndDay(org.openmrs.Patient, java.util.Date)
	 */
	@Override
	public List<Visit> getVisitsByPatientAndDay(Patient patient, Date date) {
		Date startOfDay = OpenmrsUtil.firstSecondOfDay(date);
		Date endOfDay = OpenmrsUtil.getLastMomentOfDay(date);

		// look for visits that started before endOfDay and ended after startOfDay
		List<Visit> visits = Context.getVisitService().getVisits(null, Collections.singleton(patient), null, null, null, endOfDay, startOfDay, null, null, true, false);
		Collections.reverse(visits); // We want by date asc
		return visits;
	}

	/**
	 * @see KenyaEmrService#setupMrnIdentifierSource(String)
	 */
	@Override
	public void setupMrnIdentifierSource(String startFrom) {
		PatientIdentifierType idType = MetadataUtils.existing(PatientIdentifierType.class, CommonMetadata._PatientIdentifierType.OPENMRS_ID);
		setupIdentifierSource(idType, startFrom, OPENMRS_MEDICAL_RECORD_NUMBER_NAME, null, "M");
	}

	/**
	 * @see KenyaEmrService#setupHivUniqueIdentifierSource(String)
	 */
	@Override
	public void setupHivUniqueIdentifierSource(String startFrom) {
		PatientIdentifierType idType = MetadataUtils.existing(PatientIdentifierType.class, HivMetadata._PatientIdentifierType.UNIQUE_PATIENT_NUMBER);
		setupIdentifierSource(idType, startFrom, HIV_UNIQUE_PATIENT_NUMBER_NAME, "0123456789", null);
	}

	/**
	 * Setup an identifier source
	 * @param idType the patient identifier type
	 * @param startFrom the base identifier to start from
	 * @param name the identifier source name
	 * @param baseCharacterSet the base character set
	 * @param prefix the prefix
	 */
	protected void setupIdentifierSource(PatientIdentifierType idType, String startFrom, String name, String baseCharacterSet, String prefix) {
		if (identifierManager.getIdentifierSource(idType) != null) {
			throw new APIException("Identifier source is already exists for " + idType.getName());
		}

		String validatorClass = idType.getValidator();
		LuhnModNIdentifierValidator validator = null;
		if (validatorClass != null) {
			try {
				validator = (LuhnModNIdentifierValidator) Context.loadClass(validatorClass).newInstance();
			} catch (Exception e) {
				throw new APIException("Unexpected Identifier Validator (" + validatorClass + ") for " + idType.getName(), e);
			}
		}

		if (startFrom == null) {
			if (validator != null) {
				startFrom = validator.getBaseCharacters().substring(0, 1);
			} else {
				throw new RuntimeException("startFrom is required if this isn't using a LuhnModNIdentifierValidator");
			}
		}

		if (baseCharacterSet == null) {
			baseCharacterSet = validator.getBaseCharacters();
		}

		IdentifierSourceService idService = Context.getService(IdentifierSourceService.class);

		SequentialIdentifierGenerator idGen = new SequentialIdentifierGenerator();
		idGen.setPrefix(prefix);
		idGen.setName(name);
		idGen.setDescription("Identifier Generator for " + idType.getName());
		idGen.setIdentifierType(idType);
		idGen.setBaseCharacterSet(baseCharacterSet);
		idGen.setFirstIdentifierBase(startFrom);
		idService.saveIdentifierSource(idGen);

		AutoGenerationOption auto = new AutoGenerationOption(idType, idGen, true, true);
		idService.saveAutoGenerationOption(auto);
	}

	@Override
	public List<Object> executeSqlQuery(String query, Map<String, Object> substitutions) {
		return dao.executeSqlQuery(query, substitutions);
	}

	@Override
	public List<Object> executeHqlQuery(String query, Map<String, Object> substitutions) {
		return dao.executeHqlQuery(query, substitutions);
	}

	/**
	 * @param queryId
	 * @param params
	 * @return
	 */
	@Override
	public List<SimpleObject> search(String queryId, Map<String, String[]> params) {
		Map<String, String[]> updatedParams = conditionallyAddVisitLocation(params);
		List<SimpleObject> results = new ArrayList<SimpleObject>();
		SqlQueryHelper sqlQueryHelper = new SqlQueryHelper();
		String query = getSql(queryId);
		try(Connection conn = DatabaseUpdater.getConnection();
			PreparedStatement statement = sqlQueryHelper.constructPreparedStatement(query,updatedParams,conn);
			ResultSet resultSet = statement.executeQuery()) {

			RowMapper rowMapper = new RowMapper();
			while (resultSet.next()) {
				results.add(rowMapper.mapRow(resultSet));
			}
			return results;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String getSql(String queryId) {
		String query = Context.getAdministrationService().getGlobalProperty(queryId);
		if (query == null) throw new RuntimeException("No such query:" + queryId);
		return query;
	}

	private Map<String, String[]> conditionallyAddVisitLocation(Map<String, String[]> params) {
		Map<String, String[]> updatedParams = new HashMap<String, String[]>(params);
		if (params.containsKey("location_uuid")) {
			String locationUuid = params.get("location_uuid")[0];
			String[] visitLocationValue = {locationUuid};
			updatedParams.put("visit_location_uuid", visitLocationValue);
		}
		return updatedParams;
	}

	@Override
	public SimpleObject sendKenyaEmrSms(String recipient, String message, String nationalId) {
		SimpleObject response = new SimpleObject();
		GlobalProperty gpOTPSource = Context.getAdministrationService().getGlobalPropertyObject(CommonMetadata.GP_HEI_OPT_SOURCE);
		if (gpOTPSource != null) {
			if (gpOTPSource.getPropertyValue().equalsIgnoreCase("hie")) {
				try {
					response.add("response", _HIEILMediatorSms(recipient, nationalId));
				} catch (Exception e) {
					response.add("response", "Failed to send SMS " + e.getMessage());
					throw new RuntimeException(e);
				}
			} else if (gpOTPSource.getPropertyValue().equalsIgnoreCase("kehmis")) {
				try {
					response.add("response", _Sms(recipient, message));
				} catch (Exception e) {
					response.add("response", "Failed to send SMS " + e.getMessage());
					throw new RuntimeException(e);
				}
			}
		}

		return response;
	}
	public static String _Sms(String recipient, String message) throws Exception {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		String responseMsg = null;
		try {
			AdministrationService administrationService = Context.getAdministrationService();
			HttpPost postRequest = new HttpPost(administrationService.getGlobalProperty("kenyaemr.sms.url"));
			postRequest.setHeader("api-token", administrationService.getGlobalProperty("kenyaemr.sms.apiToken"));
			JSONObject json = new JSONObject();
			json.put("destination", recipient);
			json.put("msg", message);
			json.put("sender_id", administrationService.getGlobalProperty("kenyaemr.sms.senderId"));
			json.put("gateway", administrationService.getGlobalProperty("kenyaemr.sms.gateway") );

			StringEntity entity = new StringEntity(json.toString());
			postRequest.setEntity(entity);
			postRequest.setHeader("Content-Type", "application/json");

			try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
				int statusCode = response.getStatusLine().getStatusCode();
				String responseBody = EntityUtils.toString(response.getEntity());

				if (statusCode == 200) {
					responseMsg = "SMS sent successfully" + responseBody;
					System.out.println("SMS sent successfully \n" + responseBody);
				} else {
					responseMsg = "Failed to send SMS " + responseBody;
					System.err.println("Failed to send SMS \n" + responseBody);
				}
			}
		} finally {
			httpClient.close();
		}
		return responseMsg;
	}
	/**
	 * SMS service for HIE IL Mediator
	 *
	 * @return
	 * @throws IOException
	 */

	public static String _HIEILMediatorSms(String recipient, String nationalId) throws Exception {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		String bearerToken = getHIEILMediatorAuthToken();
		String responseMsg = null;
		try {
			AdministrationService administrationService = Context.getAdministrationService();
			//FID
			GlobalProperty globalGetUrl = Context.getAdministrationService()
				.getGlobalPropertyObject(CommonMetadata.GP_SHA_FACILITY_REGISTRY_CODE);
			String fid = globalGetUrl.getPropertyValue();

			HttpPost postRequest = new HttpPost(administrationService.getGlobalProperty("kenyaemr.sms.url"));
			postRequest.setHeader("Authorization", "Bearer " + bearerToken);

			// Create the JSON using ObjectMapper with proper handling
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> jsonMap = new HashMap<>();
			jsonMap.put("identifierType", "National ID");
			jsonMap.put("identifierNumber", nationalId);
			jsonMap.put("phoneNumber", recipient);
			jsonMap.put("facility", fid);
			jsonMap.put("scope", Arrays.asList("CLIENT_REGISTRY"));

			String jsonPayload = mapper.writeValueAsString(jsonMap);
			StringEntity entity = new StringEntity(jsonPayload, "UTF-8");
			postRequest.setEntity(entity);
			postRequest.setHeader("Content-Type", "application/json");

			try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
				int statusCode = response.getStatusLine().getStatusCode();
				System.out.println("SMS Payload Status Code ==>" + statusCode);
				String responseBody = EntityUtils.toString(response.getEntity());
				if (statusCode == 200) {
					responseMsg = "SMS sent successfully" + responseBody;
					System.out.println("SMS sent successfully \n" + responseBody);
					} else {
					responseMsg = "Failed to send SMS " + responseBody;
					System.err.println("Failed to send SMS \n" + responseBody);
				}
			}
		} finally {
			httpClient.close();
		}
		return responseMsg;
	}
	/**
	 * Gets the HIE IL Mediator auth token
	 *
	 * @return
	 * @throws IOException
	 */
	public static String getHIEILMediatorAuthToken() throws IOException {
		String ret = null;
		GlobalProperty globalGetHIEILMediatorTokenUrl = Context.getAdministrationService()
			.getGlobalPropertyObject(CommonMetadata.GP_HEI_IL_MEDIATOR_TOKEN_POST_ENDPOINT);
		String hieILMedTokenUrl = globalGetHIEILMediatorTokenUrl.getPropertyValue();
		if (hieILMedTokenUrl == null || hieILMedTokenUrl.trim().isEmpty()) {
			System.out.println("HIE IL Mediator token url configs not updated: ");
		}
		GlobalProperty globalGetHieILMediatorClientID = Context.getAdministrationService()
			.getGlobalPropertyObject(CommonMetadata.GP_HEI_IL_MEDIATOR_TOKEN_CLIENT_ID);
		String globalHieILMediatorClientID = globalGetHieILMediatorClientID.getPropertyValue();
		if (globalHieILMediatorClientID == null || globalHieILMediatorClientID.trim().isEmpty()) {
			System.out.println("HIE IL Mediator client ID not updated: ");
		}
		GlobalProperty globalGetHieILMediatorClientSecret = Context.getAdministrationService()
			.getGlobalPropertyObject(CommonMetadata.GP_HEI_IL_MEDIATOR_TOKEN_CLIENT_SECRET);
		String globalHieILMediatorClientSecret = globalGetHieILMediatorClientSecret.getPropertyValue();
		if (globalHieILMediatorClientSecret == null || globalHieILMediatorClientSecret.trim().isEmpty()) {
			System.out.println("HIE IL Mediator client secret not updated: ");
		}
		try {
			OkHttpClient client = new OkHttpClient().newBuilder().build();
			okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/x-www-form-urlencoded");
			okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, "client_id="+ globalHieILMediatorClientID +"&client_secret="+ globalHieILMediatorClientSecret +"&grant_type=client_credentials");
			Request request = new Request.Builder()
				.url(hieILMedTokenUrl)
				.method("POST", body)
				.addHeader("Content-Type", "application/x-www-form-urlencoded")
				.build();
			Response response = client.newCall(request).execute();		
			if (!response.isSuccessful()) {
				System.err.println("Get HIE IL Mediator Auth: ERROR: Request failed: " + response.code() + " - " + response.message());
			} else {
				System.err.println("Get HIE IL Mediator Auth: Request successful: " + response.code() + " - " + response.message());
				//Extract token
				ObjectMapper mapper = new ObjectMapper();
				JsonNode node = mapper.readTree(response.body().string());
				 ret = node.get("access_token").asText();			
			}
		} catch (Exception ex) {
			System.err.println("Get HIE IL Mediator HIE Auth: ERROR: Request failed: " + ex.getMessage());
			ex.printStackTrace();
		}
		return (ret);
	}

    private TokenService tokenService;
    private EventsBroadcaster events;

    public void setDao(KenyaEmrDAO dao) {
        this.dao = dao;
    }

    public void setTokenService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public void setEvents(EventsBroadcaster events) {
        this.events = events;
    }

    private final RestTemplate rest = new RestTemplate();

    @Override
    public BiometricVerification startVerification(
            String patientUuid,
            String subjectId,
            String subjectIdType,
            String agentId,
            String agentIdType,
            String reason,
            //String locationName,
            String verificationContext
    ) throws Exception {

        String maxAttemptsGp = EmrUtils.getGlobalPropertyValue("kenyaemrekyc.maxAttempts");
        String callbackUrl = EmrUtils.getGlobalPropertyValue("kenyaemrekyc.callbackUrl");
        String requestUrl = EmrUtils.getGlobalPropertyValue("kenyaemrekyc.requestUrl");
        String requestExpiryDuration = EmrUtils.getGlobalPropertyValue("kenyaemr.ekyc.requestExpirySeconds");

        int maxAttempts;
        try {
            maxAttempts = Integer.parseInt(maxAttemptsGp);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid configuration for kenyaemrekyc.maxAttempts.", ex);
        }
        int requestExpirySeconds = 3600;
        try {
            requestExpirySeconds = Integer.parseInt(requestExpiryDuration);
        } catch (Exception e) {
            System.err.println("Invalid format for requestExpirySeconds: " + requestExpiryDuration + ". Using default: " + requestExpirySeconds);
        }

        BiometricVerification v = new BiometricVerification();
        v.setPatientUuid(patientUuid);
        v.setAttemptsUsed(0);
        v.setTotalAttempts(maxAttempts);
        v.setCompleted(false);
        v.setVerificationContext(verificationContext);
        v.setDateCreated(new Date());
        dao.saveVerification(v);

        String accessToken = tokenService.getValidToken();
        String tokenType = TokenCache.getTokenType();

        org.json.JSONObject payload = new org.json.JSONObject();
        payload.put("notification_callback_url", callbackUrl);
        payload.put("reason", reason);
        payload.put("relying_party_agent_id_number", agentId);
        payload.put("relying_party_agent_id_type", agentIdType);
        payload.put("subject_id_number", subjectId);
        payload.put("subject_id_type", subjectIdType);
        payload.put("relying_party_request_id", v.getId().toString());
        payload.put("expiry_in_seconds", requestExpirySeconds);
        payload.put("service_id", "medical-care");
        payload.put("total_attempts", maxAttempts);
        payload.put("location_name", "123-Test location");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", tokenType + " " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);

        if (requestUrl == null || requestUrl.isEmpty()) {
            throw new IllegalStateException("eKYC kenyaemr.ekyc.requestUrl is not configured");
        }
        ResponseEntity<String> resp = rest.postForEntity(requestUrl, entity, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("eKYC request failed: " + resp.getStatusCode());
        }
        org.json.JSONObject r = new org.json.JSONObject(resp.getBody());
        v.setRequestId(r.getString("request_id"));
        v.setRelyingPartyRequestId(payload.getString("relying_party_request_id"));
        v.setStatus("pending");
        v.setDateUpdated(new Date());
        dao.updateVerification(v);

        return v;
    }

    @Override
    public void handleCallback(String callbackJson) {
        org.json.JSONObject o = new org.json.JSONObject(callbackJson);

        String requestId = o.optString("request_id");
        String status = o.optString("status");
        String result = o.optString("result");
        String statusCode = o.optString("status_code");

        BiometricVerification v = dao.getByRequestId(requestId);
        if (v == null) return;

        v.setLastCallbackRaw(callbackJson);
        v.setStatus(status);
        v.setStatusCode(statusCode);
        v.setResult(result);
        v.setDateUpdated(new Date());

        boolean isFinal = false;

        if ("match".equals(result) && "accepted".equals(status)) {
            isFinal = true;
            v.setFinalResult("match");
            v.setCompleted(true);
            Date now = new Date();
            v.setVerificationCompletedAt(now);
            v.setVerificationExpiry(computeExpiry(v.getVerificationContext(), now));
        }
        else if ("biometrics_not_found".equals(result)) {
            isFinal = true;
            v.setFinalResult("biometrics_not_found");
            v.setCompleted(true);
            Date now = new Date();
            v.setVerificationCompletedAt(now);
            v.setVerificationExpiry(computeExpiry(v.getVerificationContext(), now));
        }
        else if ("no_match".equals(result)) {
            int verificationAttemptsUsed = Optional.ofNullable(v.getAttemptsUsed()).orElse(0) + 1;
            v.setAttemptsUsed(verificationAttemptsUsed);

            if (verificationAttemptsUsed < v.getTotalAttempts()) { //todo validate this
                isFinal = false;
                Calendar c = Calendar.getInstance();
                c.setTime(new Date());
                c.add(Calendar.SECOND, 300); // todo get from a global property
                v.setVerificationExpiry(c.getTime());
            } else {
                isFinal = true;
                v.setFinalResult("no_match");
                v.setCompleted(true);
                Date now = new Date();
                v.setVerificationCompletedAt(now);
                v.setVerificationExpiry(computeExpiry(v.getVerificationContext(), now));
            }
        }
        else if (Arrays.asList("capture_failed", "expired", "failed", "canceled").contains(status)) {
            isFinal = true;
            v.setFinalResult(status);
            v.setCompleted(true);
            Date now = new Date();
            v.setVerificationCompletedAt(now);
            v.setVerificationExpiry(computeExpiry(v.getVerificationContext(), now));
        }
        else if ("captured".equals(status)) {
            isFinal = true;
            v.setFinalResult("captured");
            v.setCompleted(true);
            Date now = new Date();
            v.setVerificationCompletedAt(now);
            v.setVerificationExpiry(computeExpiry(v.getVerificationContext(), now));
        }

        dao.updateVerification(v);

        Map<String, Object> data = new HashMap<>();
        data.put("requestId", v.getRequestId());
        data.put("status", v.getStatus());
        data.put("statusCode", v.getStatusCode());
        data.put("result", v.getResult());
        data.put("attemptsUsed", v.getAttemptsUsed());
        data.put("totalAttempts", v.getTotalAttempts());
        data.put("completed", v.getCompleted());
        data.put("finalResult", v.getFinalResult());
        data.put("verificationContext", v.getVerificationContext());
        data.put("verificationCompletedAt", v.getVerificationCompletedAt());
        data.put("verificationExpiry", v.getVerificationExpiry());

        events.sendStatusEvent(v.getRequestId(), data, isFinal ? "final-status" : "status-update");
    }

    private Date computeExpiry(String context, Date now) {
        String checkinValidityMins = EmrUtils.getGlobalPropertyValue("kenyaemr.ekyc.checkin.validity.mins");
        String claimValidityMins = EmrUtils.getGlobalPropertyValue("kenyaemr.ekyc.claim.validity.mins");

        int mins;
        int defaultValidity = 1440; // todo get proper fallback
        try {
            if ("CLAIM".equalsIgnoreCase(context)) {
                mins = parseValidity(claimValidityMins, defaultValidity);
            } else if ("CHECKIN".equalsIgnoreCase(context)) {
                mins = parseValidity(checkinValidityMins, defaultValidity);
            } else {
                mins = parseValidity(checkinValidityMins, defaultValidity);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid eKYC expiry configuration", ex);
        }
        Calendar c = Calendar.getInstance();
        c.setTime(now);
        c.add(Calendar.MINUTE, mins);
        return c.getTime();
    }
    private int parseValidity(String value, int defaultValue) {
        if (value != null && !value.trim().isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // todo Use default if parsing fails
            }
        }
        return defaultValue;
    }
    /* ---------------------------------------------
       REQUEST OTP APPROVAL (Pesaflow)
       --------------------------------------------- */

    //TODO Get actual request/response payloads
    @Override
    public void requestOtpUse(String requestId,
                              String reason,
                              String otpContext,
                              String subjectId,
                              String subjectIdType,
                              String agentId,
                              String locationName) throws Exception {

        BiometricVerification v = dao.getByRequestId(requestId);
        if (v == null) {
            throw new IllegalArgumentException("Unknown requestId: " + requestId);
        }

        // Acquire token
        String token = tokenService.getValidToken();
        String tokenType = TokenCache.getTokenType();

        org.json.JSONObject payload = new org.json.JSONObject();
        payload.put("request_id", v.getRequestId());
        payload.put("reason", reason);
        payload.put("otp_context", otpContext);
        payload.put("subject_id_number", subjectId);
        payload.put("subject_id_type", subjectIdType);
        payload.put("relying_party_agent_id_number", agentId);
        payload.put("location_name", locationName);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", tokenType + " " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);

        String otpApprovalUrl = EmrUtils.getGlobalPropertyValue("kenyaemr.ekyc.getOTPUseApproval.url");

        ResponseEntity<String> resp;
        try {
            resp = rest.postForEntity(otpApprovalUrl, entity, String.class);
        } catch (Exception ex) {
            log.error("Failed to call OTP approval endpoint", ex);
            // persist rejected approval and notify FE
            OtpUseRequest req = new OtpUseRequest();
            req.setVerificationId(v.getId());
            req.setPatientUuid(v.getPatientUuid());
            req.setReason(reason);
            req.setOtpContext(otpContext);
            req.setRequestedBy(agentId); // identifying the agent as the requestor
            req.setResponseStatus("rejected");
            req.setApproved(false);
            req.setRejectionReason("remote_call_failed: " + ex.getMessage());
            req.setDateRequested(new Date());
            dao.saveOtpRequest(req);

            Map<String, Object> d = new HashMap<>();
            d.put("requestId", v.getRequestId());
            d.put("status", "rejected");
            d.put("reason", req.getRejectionReason());
            events.sendStatusEvent(v.getRequestId(), d, "otp-rejected");
            return;
        }

        if (!resp.getStatusCode().is2xxSuccessful()) {
            log.warn("OTP approval call returned non-2xx: " + resp.getStatusCode());
            OtpUseRequest req = new OtpUseRequest();
            req.setVerificationId(v.getId());
            req.setPatientUuid(v.getPatientUuid());
            req.setReason(reason);
            req.setOtpContext(otpContext);
            req.setRequestedBy(agentId);
            req.setResponseStatus("rejected");
            req.setApproved(false);
            req.setRejectionReason("remote_request_failed: " + resp.getStatusCode());
            req.setDateRequested(new Date());
            dao.saveOtpRequest(req);

            Map<String,Object> d = new HashMap<>();
            d.put("requestId", v.getRequestId());
            d.put("status", "rejected");
            d.put("reason", resp.getStatusCode().toString());
            events.sendStatusEvent(v.getRequestId(), d, "otp-rejected");
            return;
        }

        org.json.JSONObject j = new org.json.JSONObject(resp.getBody());
        String status = j.optString("status", "rejected");

        OtpUseRequest req = new OtpUseRequest();
        req.setVerificationId(v.getId());
        req.setPatientUuid(v.getPatientUuid());
        req.setReason(reason);
        req.setOtpContext(otpContext);
        req.setRequestedBy(agentId);
        req.setResponseStatus(status);
        req.setDateRequested(new Date());

        if ("approved".equalsIgnoreCase(status)) {
            req.setApproved(true);
            req.setApprovalId(j.optString("approval_id", null));
            int expiresIn = j.optInt("expires_in", 0);
            if (expiresIn > 0) {
                Calendar c = Calendar.getInstance();
                c.add(Calendar.SECOND, expiresIn);
                req.setExpiresAt(c.getTime());
            }
            dao.saveOtpRequest(req);

            Map<String,Object> d = new HashMap<>();
            d.put("requestId", v.getRequestId());
            d.put("approvalId", req.getApprovalId());
            d.put("status", "approved");
            d.put("expiresIn", j.optInt("expires_in", 0));
            events.sendStatusEvent(v.getRequestId(), d, "otp-approved");
        } else {
            req.setApproved(false);
            req.setRejectionReason(j.optString("reason", "rejected"));
            dao.saveOtpRequest(req);

            Map<String,Object> d = new HashMap<>();
            d.put("requestId", v.getRequestId());
            d.put("status", "rejected");
            d.put("reason", req.getRejectionReason());
            events.sendStatusEvent(v.getRequestId(), d, "otp-rejected");
        }
    }

}
