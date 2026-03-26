/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */

package org.openmrs.module.kenyaemr.nmlis;

import org.openmrs.api.context.Context;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.io.*;
import java.net.URL;

public class NlmisHttpClientService {

	private String getBaseUrl() {
		return Context.getAdministrationService()
				.getGlobalProperty("nlmis.base.url");
	}

	private String getToken() {
		return Context.getAdministrationService()
				.getGlobalProperty("nlmis.oauth.token");
	}

	private String buildUrl(String endpoint) {
		String baseUrl = getBaseUrl();
		if (baseUrl == null || endpoint == null) {
			throw new IllegalStateException("Missing NLMIS configuration");
		}
		return baseUrl + endpoint;
	}

	private HttpsURLConnection openConnection(String urlString) throws Exception {
		URL url = new URL(urlString);
		HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

		// Trust all certificates
		TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

					public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
					}

					public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
					}
				}
		};

		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());

		con.setSSLSocketFactory(sc.getSocketFactory());
		con.setHostnameVerifier((hostname, session) -> true);

		return con;
	}

	/**
	 * Generic GET Executor
	 */
	public ResponseEntity<String> executeGet(String endpoint) {

		HttpsURLConnection con = null;

		try {
			String completeUrl = buildUrl(endpoint);
			con = openConnection(completeUrl);
			con.setRequestMethod("GET");
			con.setRequestProperty("Authorization", "Bearer " + getToken());
			con.setRequestProperty("Accept", "application/json");
			con.setConnectTimeout(50000);
			con.setReadTimeout(50000);

			return handleResponse(con);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return ResponseEntity.badRequest()
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"status\":\"Error\"}");
	}

	/**
	 * GET with query params
	 */
	public ResponseEntity<String> executeGet(String endpoint, String queryString) {

		HttpsURLConnection con = null;

		try {
			String completeUrl = buildUrl(endpoint);

			if (queryString != null && !queryString.trim().isEmpty()) {
				completeUrl += "?" + queryString;
			}
			con = openConnection(completeUrl);
			con.setRequestMethod("GET");
			con.setRequestProperty("Authorization", "Bearer " + getToken());
			con.setRequestProperty("Accept", "application/json");
			con.setConnectTimeout(50000);
			con.setReadTimeout(50000);

			return handleResponse(con);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return ResponseEntity.badRequest()
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"status\":\"Error\"}");
	}

	/**
	 * Generic POST Executor
	 */
	public ResponseEntity<String> executePost(String endpoint, String payload) {

		HttpsURLConnection con = null;

		try {
			String completeUrl = buildUrl(endpoint);

			con = openConnection(completeUrl);
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setRequestProperty("Authorization", "Bearer " + getToken());
			con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			con.setRequestProperty("Accept", "application/json");
			con.setConnectTimeout(50000);
			con.setReadTimeout(50000);

			// Send payload
			PrintStream os = new PrintStream(con.getOutputStream());
			os.print(payload);
			os.flush();
			os.close();

			return handleResponse(con);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return ResponseEntity.badRequest()
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"status\":\"Error\"}");
	}

	/**
	 * Unified Response Handler
	 */
	private ResponseEntity<String> handleResponse(HttpsURLConnection con) throws IOException {

		int responseCode = con.getResponseCode();

		InputStream stream = (responseCode >= 200 && responseCode < 300)
				? con.getInputStream()
				: con.getErrorStream();

		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		StringBuilder response = new StringBuilder();
		String line;

		while ((line = reader.readLine()) != null) {
			response.append(line);
		}
		reader.close();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		return ResponseEntity.status(responseCode)
				.headers(headers)
				.body(response.toString());
	}
}
