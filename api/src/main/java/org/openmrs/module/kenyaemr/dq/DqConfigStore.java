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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.kenyaemr.dq.rule.DqRuleTypes;
import org.openmrs.module.kenyaemr.reporting.air.ConfigurableAdxGenerationStrategy;
import org.openmrs.util.OpenmrsUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Source of the data quality configuration. It lives under {@code kenyaemr/khis} in the OpenMRS
 * application data directory so that each facility can set it, within the national M&E guidelines,
 * without a release:
 * <ul>
 * <li>{@code dq_config.json}: shared settings (override reasons, and any reports or indicators not
 * tied to one program)</li>
 * <li>{@code dq_programs/<name>.json}: one file per program area, holding the program, its rules,
 * and the expected reports and indicators it needs</li>
 * </ul>
 * A missing file is seeded from the default bundled in the module, and the files are re-read
 * whenever one changes. The bundled default each file came from is kept in {@code .dq_defaults},
 * so that when a new module version brings a different default, the file is upgraded to it while
 * keeping the facility's own settings ({@link DqConfigUpgrade}). A program file's reports and indicators replace those of the same name in
 * dq_config.json; all indicators form one library that every program's rules can use.
 */
public final class DqConfigStore {

	private static final Log log = LogFactory.getLog(DqConfigStore.class);

	static final String DEFAULT_RESOURCE = "khis/dq_config.default.json";

	static final String PROGRAMS_DIRECTORY = "dq_programs";

	/** Program files bundled in the module, seeded into dq_programs when missing */
	static final List<String> BUNDLED_PROGRAMS = Arrays.asList("malaria");

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private static volatile Cached cached;

	/** Whether the files have been checked against the bundled defaults since the module started */
	private static volatile boolean upgradeChecked;

	private DqConfigStore() {
	}

	/**
	 * @return the current configuration; the last good one if a file has become unreadable, or the
	 * bundled default if there has never been a good one
	 */
	public static DqConfig getConfig() {
		File file = getFile();
		try {
			ensureFile(DEFAULT_RESOURCE, file, new File(getBaseDirectory(), file.getName()));
			for (String name : BUNDLED_PROGRAMS) {
				ensureFile(programResource(name), new File(getProgramsDirectory(), name + ".json"),
						new File(new File(getBaseDirectory(), PROGRAMS_DIRECTORY), name + ".json"));
			}
			upgradeChecked = true;
			if (!file.exists()) {
				return getDefaultConfig();
			}

			List<File> programFiles = programFiles();
			String signature = signature(file, programFiles);
			Cached current = cached;
			if (current != null && current.signature.equals(signature)) {
				return current.config;
			}

			DqConfig config = objectMapper.readValue(file, DqConfig.class);
			for (File programFile : programFiles) {
				merge(config, objectMapper.readValue(programFile, ProgramFile.class));
			}
			for (String problem : validate(config)) {
				log.warn("Data quality configuration in " + file.getParent() + ": " + problem);
			}
			try {
				for (String problem : mappingProblems(config)) {
					log.warn("Data quality configuration: " + problem);
				}
			}
			catch (RuntimeException e) {
				log.warn("Could not check the KHIS mappings of the expected reports", e);
			}
			cached = new Cached(signature, config);
			return config;
		}
		catch (Exception e) {
			log.error("Error loading data quality configuration from " + file.getParent(), e);
			Cached current = cached;
			return current != null ? current.config : getDefaultConfig();
		}
	}

	/**
	 * @return the configuration bundled in the module, with its program files
	 */
	public static DqConfig getDefaultConfig() {
		DqConfig config = read(DEFAULT_RESOURCE, DqConfig.class);
		if (config == null) {
			return new DqConfig();
		}
		for (String name : BUNDLED_PROGRAMS) {
			ProgramFile programFile = read(programResource(name), ProgramFile.class);
			if (programFile != null) {
				merge(config, programFile);
			}
		}
		return config;
	}

	/**
	 * Adds a program file to the configuration: its reports and indicators replace those with the
	 * same name, and its program replaces one with the same code
	 */
	static void merge(DqConfig config, ProgramFile programFile) {
		for (DqConfig.ExpectedReport report : programFile.getExpectedReports()) {
			DqConfig.ExpectedReport existing = config.getExpectedReport(report.getReport());
			if (existing != null) {
				config.getExpectedReports().set(config.getExpectedReports().indexOf(existing), report);
			} else {
				config.getExpectedReports().add(report);
			}
		}
		for (DqConfig.Indicator indicator : programFile.getIndicators()) {
			DqConfig.Indicator existing = config.getIndicator(indicator.getCode());
			if (existing != null) {
				config.getIndicators().set(config.getIndicators().indexOf(existing), indicator);
			} else {
				config.getIndicators().add(indicator);
			}
		}
		DqConfig.Program program = programFile.getProgram();
		if (program != null) {
			DqConfig.Program existing = config.getProgram(program.getCode());
			if (existing != null) {
				config.getPrograms().remove(existing);
			}
			config.getPrograms().add(program);
		}
	}

	/**
	 * Checks that the configuration hangs together. A typing mistake in a code or report name would
	 * otherwise make a check silently read zero.
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

		Set<String> programCodes = new HashSet<String>();
		Set<String> ruleIds = new HashSet<String>();
		for (DqConfig.Program program : config.getPrograms()) {
			String name = "program " + program.getCode();
			if (StringUtils.isBlank(program.getCode())) {
				problems.add("a program has no code");
				continue;
			}
			if (!programCodes.add(program.getCode().toUpperCase())) {
				problems.add(name + " is defined more than once");
			}
			for (String code : program.getIndicators()) {
				if (!codes.contains(code)) {
					problems.add(name + " watches unknown indicator " + code);
				}
			}
			for (String report : program.getOwns()) {
				if (!reports.contains(report)) {
					problems.add(name + " owns " + report + ", which is not an expected report");
				}
			}
			if (program.getDashboard() != null) {
				List<String> dashboardCodes = new ArrayList<String>();
				for (DqConfig.CrossCheck check : program.getDashboard().getCrossChecks()) {
					dashboardCodes.add(check.getNumerator());
					dashboardCodes.add(check.getDenominator());
				}
				if (program.getDashboard().getCascade() != null) {
					for (DqConfig.CascadeStep step : program.getDashboard().getCascade().getSteps()) {
						dashboardCodes.add(step.getIndicator());
					}
				}
				if (program.getDashboard().getConsistency() != null) {
					dashboardCodes.add(program.getDashboard().getConsistency().getIndicator());
				}
				for (String code : dashboardCodes) {
					if (code == null || !codes.contains(code)) {
						problems.add(name + "'s dashboard refers to unknown indicator " + code);
					}
				}
			}
			for (Map<String, Object> values : program.getRules()) {
				DqConfig.RuleDefinition rule = new DqConfig.RuleDefinition(program, values);
				String ruleName = "rule " + (rule.getId() != null ? rule.getId() : "(no id)") + " of " + name;
				if (StringUtils.isBlank(rule.getId())) {
					problems.add("a rule of " + name + " has no id");
				} else if (!ruleIds.add(rule.getId())) {
					problems.add("rule id " + rule.getId() + " is used more than once; rule ids must be unique across programs");
				}
				if (!DqRuleTypes.isKnown(rule.getType())) {
					problems.add(ruleName + " has type " + rule.getType() + "; it must be one of " + DqRuleTypes.TYPES);
				}
				for (String code : rule.getIndicatorCodes()) {
					if (!codes.contains(code)) {
						problems.add(ruleName + " refers to unknown indicator " + code);
					}
				}
				for (String report : rule.getSettings().getReportNames()) {
					if (!reports.contains(report)) {
						problems.add(ruleName + " checks " + report + ", which is not an expected report");
					}
				}
				Object tolerance = rule.getValues().get("tolerance");
				if (tolerance != null) {
					Double value = rule.getSettings().getDouble("tolerance", null);
					if (value == null || value < 0 || value >= 1) {
						problems.add(ruleName + " has tolerance " + tolerance
								+ "; write it as a fraction from 0 to below 1, e.g. 0.05 for 5%");
					}
				}
				if (rule.getGates() != null && !program.getOwns().contains(rule.getGates())) {
					problems.add(ruleName + " gates " + rule.getGates() + ", which " + name
							+ " does not own, so it is shown as a warning");
				}
			}
		}

		if (!config.getPrograms().isEmpty() && !config.getRules().isEmpty()) {
			problems.add("dq_config.json still has a \"rules\" section; rules now live in the program files in "
					+ PROGRAMS_DIRECTORY + ", and an entry there wins over one here");
		}
		return problems;
	}

	/**
	 * @return a problem for each expected KHIS report with no KHIS mapping: it is generated and
	 * checked, but cannot be sent, and the administrator is alerted each month it is approved
	 */
	public static List<String> mappingProblems(DqConfig config) {
		List<String> problems = new ArrayList<String>();
		ConfigurableAdxGenerationStrategy mappings = new ConfigurableAdxGenerationStrategy();
		for (DqConfig.ExpectedReport report : config.getExpectedReports()) {
			if ("KHIS".equalsIgnoreCase(report.getTarget()) && report.getReport() != null
					&& mappings.getConfigurationForReport(report.getReport()) == null) {
				problems.add(report.getReport() + " is sent to KHIS but has no KHIS mapping in khis_dataset_mapping.json, "
						+ "so it is checked but cannot be sent");
			}
		}
		return problems;
	}

	/**
	 * Sets thresholds of a rule in the program file that holds it, as the settings screen does. The
	 * file is backed up first, and the configuration is read again at once. The facility's values
	 * are kept when a new module version brings a different default ({@link DqConfigUpgrade}).
	 *
	 * @param ruleId the rule
	 * @param changes threshold to value, see {@link DqRuleThresholds#apply}
	 * @return a description of each problem; when not empty, nothing was changed
	 * @throws IllegalArgumentException when no program file holds the rule
	 */
	public static synchronized List<String> updateRuleThresholds(String ruleId, Map<String, Object> changes)
			throws IOException {
		getConfig();
		for (File file : programFiles()) {
			JsonNode tree = objectMapper.readTree(file);
			ObjectNode rule = findRule(tree, ruleId);
			if (rule == null) {
				continue;
			}
			List<String> problems = DqRuleThresholds.apply(rule, changes);
			if (!problems.isEmpty()) {
				return problems;
			}
			File backup = new File(file.getParentFile(), file.getName() + ".bak-"
					+ new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
			Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
			File written = new File(file.getParentFile(), "." + file.getName() + ".tmp");
			Files.write(written.toPath(), objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(tree));
			Files.move(written.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			cached = null;
			log.info("Data quality: thresholds of " + ruleId + " set to " + changes + " in " + file.getAbsolutePath()
					+ "; the previous file is " + backup.getName());
			return problems;
		}
		throw new IllegalArgumentException("No program file in " + getProgramsDirectory() + " has rule " + ruleId);
	}

	/**
	 * @return the rule's entry in a program file, or null when the file does not have it
	 */
	static ObjectNode findRule(JsonNode programFile, String ruleId) {
		for (JsonNode rule : programFile.path("program").path("rules")) {
			if (rule instanceof ObjectNode && ruleId.equals(rule.path("id").asText(null))) {
				return (ObjectNode) rule;
			}
		}
		return null;
	}

	static File getFile() {
		return new File(getKhisDirectory(), "dq_config.json");
	}

	static File getProgramsDirectory() {
		return new File(getKhisDirectory(), PROGRAMS_DIRECTORY);
	}

	/**
	 * @return where the bundled default each file came from is kept, for upgrades
	 */
	static File getBaseDirectory() {
		return new File(getKhisDirectory(), ".dq_defaults");
	}

	private static File getKhisDirectory() {
		return new File(OpenmrsUtil.getApplicationDataDirectory() + File.separator + "kenyaemr" + File.separator + "khis");
	}

	static String programResource(String name) {
		return "khis/" + PROGRAMS_DIRECTORY + "/" + name + ".default.json";
	}

	/**
	 * @return the program files, in name order so that the result does not depend on the file system
	 */
	private static List<File> programFiles() {
		File[] files = getProgramsDirectory().listFiles();
		List<File> programFiles = new ArrayList<File>();
		if (files != null) {
			for (File file : files) {
				if (file.isFile() && file.getName().endsWith(".json")) {
					programFiles.add(file);
				}
			}
		}
		Collections.sort(programFiles);
		return programFiles;
	}

	private static String signature(File file, List<File> programFiles) {
		StringBuilder signature = new StringBuilder(file.getName()).append(':').append(file.lastModified());
		for (File programFile : programFiles) {
			signature.append(';').append(programFile.getName()).append(':').append(programFile.lastModified());
		}
		return signature.toString();
	}

	private static <T> T read(String resource, Class<T> type) {
		InputStream in = DqConfigStore.class.getClassLoader().getResourceAsStream(resource);
		if (in == null) {
			log.error("Bundled data quality configuration not found: " + resource);
			return null;
		}
		try {
			return objectMapper.readValue(in, type);
		}
		catch (IOException e) {
			log.error("Failed to read bundled data quality configuration: " + resource, e);
			return null;
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
	 * Seeds a missing file from the bundled default, or, once after the module starts, upgrades an
	 * existing one to a changed default
	 */
	private static synchronized void ensureFile(String resource, File file, File baseFile) {
		if (file.exists() && upgradeChecked) {
			return;
		}
		String bundled = readText(resource);
		if (bundled == null) {
			return;
		}
		try {
			if (!file.exists()) {
				write(file, bundled);
				write(baseFile, bundled);
				log.info("Seeded data quality configuration: " + file.getAbsolutePath());
			} else {
				DqConfigUpgrade.upgrade(bundled, file, baseFile);
			}
		}
		catch (IOException e) {
			log.error("Failed to seed or upgrade data quality configuration: " + file.getAbsolutePath(), e);
		}
	}

	private static void write(File file, String content) throws IOException {
		File dir = file.getParentFile();
		if (dir != null && !dir.exists() && !dir.mkdirs()) {
			throw new IOException("Failed to create directory " + dir.getAbsolutePath());
		}
		Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
	}

	private static String readText(String resource) {
		InputStream in = DqConfigStore.class.getClassLoader().getResourceAsStream(resource);
		if (in == null) {
			log.error("Bundled data quality configuration not found: " + resource);
			return null;
		}
		try {
			return IOUtils.toString(in, StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			log.error("Failed to read bundled data quality configuration: " + resource, e);
			return null;
		}
		finally {
			IOUtils.closeQuietly(in);
		}
	}

	/**
	 * One program's file: the program, and the expected reports and indicators it brings
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ProgramFile {

		private DqConfig.Program program;

		private List<DqConfig.ExpectedReport> expectedReports = new ArrayList<DqConfig.ExpectedReport>();

		private List<DqConfig.Indicator> indicators = new ArrayList<DqConfig.Indicator>();

		public DqConfig.Program getProgram() {
			return program;
		}

		public void setProgram(DqConfig.Program program) {
			this.program = program;
		}

		public List<DqConfig.ExpectedReport> getExpectedReports() {
			return expectedReports;
		}

		public void setExpectedReports(List<DqConfig.ExpectedReport> expectedReports) {
			this.expectedReports = expectedReports != null ? expectedReports : new ArrayList<DqConfig.ExpectedReport>();
		}

		public List<DqConfig.Indicator> getIndicators() {
			return indicators;
		}

		public void setIndicators(List<DqConfig.Indicator> indicators) {
			this.indicators = indicators != null ? indicators : new ArrayList<DqConfig.Indicator>();
		}
	}

	private static final class Cached {

		private final String signature;

		private final DqConfig config;

		private Cached(String signature, DqConfig config) {
			this.signature = signature;
			this.config = config;
		}
	}
}
