/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.dq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.util.OpenmrsUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Source of the data quality configuration. It lives in {@code kenyaemr/khis/dq_config.json} under
 * the OpenMRS application data directory so that M&E can edit it without a release. A missing file
 * is seeded from the default bundled in the module, and the file is re-read whenever it changes.
 */
public final class DqConfigStore {

	private static final Log log = LogFactory.getLog(DqConfigStore.class);

	static final String DEFAULT_RESOURCE = "khis/dq_config.default.json";

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private static volatile Cached cached;

	private DqConfigStore() {
	}

	/**
	 * @return the current configuration; the last good one if the file has become unreadable, or the
	 * bundled default if there has never been a good one
	 */
	public static DqConfig getConfig() {
		File file = getFile();
		try {
			if (!file.exists()) {
				seedFile(file);
			}
			if (!file.exists()) {
				return getDefaultConfig();
			}

			long lastModified = file.lastModified();
			Cached current = cached;
			if (current != null && current.lastModified == lastModified) {
				return current.config;
			}

			DqConfig config = objectMapper.readValue(file, DqConfig.class);
			for (String problem : validate(config)) {
				log.warn("Data quality configuration " + file.getAbsolutePath() + ": " + problem);
			}
			cached = new Cached(lastModified, config);
			return config;
		}
		catch (Exception e) {
			log.error("Error loading data quality configuration from " + file.getAbsolutePath(), e);
			Cached current = cached;
			return current != null ? current.config : getDefaultConfig();
		}
	}

	/**
	 * @return the configuration bundled in the module
	 */
	public static DqConfig getDefaultConfig() {
		InputStream in = DqConfigStore.class.getClassLoader().getResourceAsStream(DEFAULT_RESOURCE);
		if (in == null) {
			log.error("Bundled data quality configuration not found: " + DEFAULT_RESOURCE);
			return new DqConfig();
		}
		try {
			return objectMapper.readValue(in, DqConfig.class);
		}
		catch (IOException e) {
			log.error("Failed to read bundled data quality configuration: " + DEFAULT_RESOURCE, e);
			return new DqConfig();
		}
		finally {
			try {
				in.close();
			}
			catch (IOException ignored) {
			}
		}
	}

	/**
	 * Checks that the configuration hangs together
	 *
	 * @return a description of each problem found, empty when there are none
	 */
	public static List<String> validate(DqConfig config) {
		List<String> problems = new ArrayList<String>();
		Set<String> reports = new HashSet<String>();
		for (DqConfig.ExpectedReport report : config.getExpectedReports()) {
			if (StringUtils.isBlank(report.getReport())) {
				problems.add("an expected report has no name");
			}
			reports.add(report.getReport());
		}

		Set<String> codes = new HashSet<String>();
		for (DqConfig.Indicator indicator : config.getIndicators()) {
			if (StringUtils.isBlank(indicator.getCode())) {
				problems.add("an indicator has no code");
				continue;
			}
			if (!codes.add(indicator.getCode())) {
				problems.add("indicator " + indicator.getCode() + " is defined more than once");
			}
			if (indicator.getParts().isEmpty()) {
				problems.add("indicator " + indicator.getCode() + " has no parts");
			}
			for (DqConfig.Part part : indicator.getParts()) {
				if (!reports.contains(part.getReport())) {
					problems.add("indicator " + indicator.getCode() + " reads from " + part.getReport()
							+ ", which is not an expected report, so it is never generated");
				}
				if (StringUtils.isBlank(part.getDataset()) || part.getColumns().isEmpty()) {
					problems.add("indicator " + indicator.getCode() + " has a part with no dataset or columns");
				}
			}
		}

		for (Map.Entry<String, Map<String, Object>> rule : config.getRules().entrySet()) {
			for (String key : new String[] { "numerator", "denominator" }) {
				Object code = rule.getValue().get(key);
				if (code != null && !codes.contains(code.toString())) {
					problems.add("rule " + rule.getKey() + " refers to unknown indicator " + code);
				}
			}
			DqRuleSettings settings = new DqRuleSettings(rule.getValue());
			for (Map<String, Object> pair : settings.getObjectList("pairs")) {
				for (Object code : pair.values()) {
					if (code != null && !codes.contains(code.toString())) {
						problems.add("rule " + rule.getKey() + " pairs refer to unknown indicator " + code);
					}
				}
			}
		}
		return problems;
	}

	static File getFile() {
		return new File(OpenmrsUtil.getApplicationDataDirectory() + File.separator + "kenyaemr" + File.separator
				+ "khis" + File.separator + "dq_config.json");
	}

	private static synchronized void seedFile(File file) {
		if (file.exists()) {
			return;
		}
		InputStream in = DqConfigStore.class.getClassLoader().getResourceAsStream(DEFAULT_RESOURCE);
		if (in == null) {
			return;
		}
		try {
			File dir = file.getParentFile();
			if (dir != null && !dir.exists() && !dir.mkdirs()) {
				log.error("Failed to create directory: " + dir.getAbsolutePath());
				return;
			}
			Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			log.info("Seeded data quality configuration: " + file.getAbsolutePath());
		}
		catch (IOException e) {
			log.error("Failed to seed data quality configuration: " + file.getAbsolutePath(), e);
		}
		finally {
			try {
				in.close();
			}
			catch (IOException ignored) {
			}
		}
	}

	private static final class Cached {

		private final long lastModified;

		private final DqConfig config;

		private Cached(long lastModified, DqConfig config) {
			this.lastModified = lastModified;
			this.config = config;
		}
	}
}
