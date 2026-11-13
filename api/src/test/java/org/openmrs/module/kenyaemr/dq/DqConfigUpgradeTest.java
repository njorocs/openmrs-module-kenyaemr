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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Upgrading a facility's configuration file to a new bundled default
 */
public class DqConfigUpgradeTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private final ObjectMapper mapper = new ObjectMapper();

	private File file;

	private File baseFile;

	private static final String BASE = "{'program': {'code': 'MALARIA', 'rules': ["
			+ "{'id': 'T22-07', 'report': 'MOH-743 Report', 'windowDays': 14},"
			+ "{'id': 'G-RPT-02', 'tolerance': 0.05},"
			+ "{'id': 'T22-05', 'tolerance': 0.1},"
			+ "{'id': 'OLD', 'severity': 'WARN'}]},"
			+ "'indicators': [{'code': 'MAL_TREATED', 'parts': [{'report': 'MOH-743 Report', 'dataset': 'ACT'},"
			+ "{'report': 'MOH-743 Report', 'dataset': 'INJ'}]}]}";

	private static final String UPDATE = "{'program': {'code': 'MALARIA', 'rules': ["
			+ "{'id': 'T22-07', 'reports': [{'report': 'MOH 705A', 'maxAgeYears': 5}], 'windowDays': 14},"
			+ "{'id': 'G-RPT-02', 'compare': 'ALL_VALUES', 'tolerance': 0.05},"
			+ "{'id': 'T22-05', 'tolerance': 0.08},"
			+ "{'id': 'X-RPT-01', 'min': 0.9}]},"
			+ "'indicators': [{'code': 'MAL_TREATED', 'parts': [{'report': 'MOH-743 Report', 'dataset': 'DQ_TREATMENT'}]}]}";

	@Before
	public void setUp() {
		file = new File(folder.getRoot(), "malaria.json");
		baseFile = new File(new File(folder.getRoot(), ".dq_defaults"), "malaria.json");
	}

	@Test
	public void upgrade_shouldTakeTheNewDefaultWhereTheFacilityChangedNothing() throws Exception {
		write(file, BASE);
		write(baseFile, BASE);

		List<String> notes = DqConfigUpgrade.upgrade(json(UPDATE), file, baseFile);

		Assert.assertTrue(notes.toString(), notes.isEmpty());
		Assert.assertEquals(mapper.readTree(json(UPDATE)), mapper.readTree(file));
		Assert.assertEquals(json(UPDATE), new String(Files.readAllBytes(baseFile.toPath()), StandardCharsets.UTF_8));
		Assert.assertEquals(1, backups().length);
	}

	@Test
	public void upgrade_shouldKeepTheFacilitysOwnSettings() throws Exception {
		write(baseFile, BASE);
		// The facility tightened G-RPT-02, removed T22-07 and added a rule of its own
		write(file, "{'program': {'code': 'MALARIA', 'rules': ["
				+ "{'id': 'G-RPT-02', 'tolerance': 0.03},"
				+ "{'id': 'T22-05', 'tolerance': 0.1},"
				+ "{'id': 'OLD', 'severity': 'WARN'},"
				+ "{'id': 'LOCAL-01', 'severity': 'WARN'}]},"
				+ "'indicators': [{'code': 'MAL_TREATED', 'parts': [{'report': 'MOH-743 Report', 'dataset': 'ACT'},"
				+ "{'report': 'MOH-743 Report', 'dataset': 'INJ'}]}]}");

		List<String> notes = DqConfigUpgrade.upgrade(json(UPDATE), file, baseFile);

		JsonNode rules = mapper.readTree(file).get("program").get("rules");
		Assert.assertEquals("G-RPT-02", rules.get(0).get("id").asText());
		Assert.assertEquals(0.03, rules.get(0).get("tolerance").asDouble(), 0);
		Assert.assertEquals("ALL_VALUES", rules.get(0).get("compare").asText());
		// Unchanged by the facility, so the new default
		Assert.assertEquals(0.08, rules.get(1).get("tolerance").asDouble(), 0);
		Assert.assertEquals("X-RPT-01", rules.get(2).get("id").asText());
		Assert.assertEquals("LOCAL-01", rules.get(3).get("id").asText());
		// T22-07 stays removed, OLD goes with the default
		Assert.assertEquals(4, rules.size());
		// Parts share a report, so the list is replaced as a whole
		JsonNode parts = mapper.readTree(file).get("indicators").get(0).get("parts");
		Assert.assertEquals(1, parts.size());
		Assert.assertEquals("DQ_TREATMENT", parts.get(0).get("dataset").asText());
		Assert.assertEquals(1, notes.size());
		Assert.assertTrue(notes.get(0), notes.get(0).startsWith("program.rules[T22-07]: kept the facility's removal"));
	}

	@Test
	public void upgrade_shouldKeepTheFacilitysValueWhenBothChangedASetting() throws Exception {
		write(baseFile, BASE);
		write(file, BASE.replace("'tolerance': 0.1", "'tolerance': 0.15"));

		List<String> notes = DqConfigUpgrade.upgrade(json(UPDATE), file, baseFile);

		JsonNode rules = mapper.readTree(file).get("program").get("rules");
		Assert.assertEquals(0.15, rules.get(2).get("tolerance").asDouble(), 0);
		Assert.assertEquals(1, notes.size());
		Assert.assertEquals("program.rules[T22-05].tolerance: kept the facility's value 0.15; the new default is 0.08",
				notes.get(0));
	}

	@Test
	public void upgrade_shouldDoNothingWhenTheDefaultIsUnchanged() throws Exception {
		write(baseFile, BASE);
		write(file, BASE.replace("0.05", "0.03"));

		Assert.assertNull(DqConfigUpgrade.upgrade(json(BASE), file, baseFile));
		Assert.assertEquals(0.03, mapper.readTree(file).get("program").get("rules").get(1).get("tolerance").asDouble(), 0);
		Assert.assertEquals(0, backups().length);
	}

	@Test
	public void upgrade_shouldReplaceAndBackUpAFileWithNoBase() throws Exception {
		write(file, BASE.replace("0.05", "0.03"));

		List<String> notes = DqConfigUpgrade.upgrade(json(UPDATE), file, baseFile);

		Assert.assertEquals(mapper.readTree(json(UPDATE)), mapper.readTree(file));
		Assert.assertTrue(baseFile.exists());
		Assert.assertEquals(1, notes.size());
		Assert.assertTrue(notes.get(0), notes.get(0).contains("replaced by the new default"));
		File[] backups = backups();
		Assert.assertEquals(1, backups.length);
		Assert.assertEquals(0.03, mapper.readTree(backups[0]).get("program").get("rules").get(1).get("tolerance").asDouble(), 0);
	}

	@Test
	public void upgrade_shouldReadTheBundledMalariaProgram() throws Exception {
		InputStream in = getClass().getClassLoader().getResourceAsStream(DqConfigStore.programResource("malaria"));
		String bundled = IOUtils.toString(in, StandardCharsets.UTF_8);
		in.close();
		write(baseFile, BASE);
		write(file, BASE);

		Assert.assertTrue(DqConfigUpgrade.upgrade(bundled, file, baseFile).isEmpty());
		Assert.assertEquals(mapper.readTree(bundled), mapper.readTree(file));
	}

	private File[] backups() {
		return folder.getRoot().listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("malaria.json.bak-");
			}
		});
	}

	private static String json(String singleQuoted) {
		return singleQuoted.replace('\'', '"');
	}

	private static void write(File target, String singleQuoted) throws Exception {
		target.getParentFile().mkdirs();
		Files.write(target.toPath(), json(singleQuoted).getBytes(StandardCharsets.UTF_8));
	}
}
