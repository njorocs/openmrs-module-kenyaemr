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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Brings a facility's data quality configuration file up to a new bundled default without losing
 * the facility's own settings. Next to each file the store keeps the bundled default it last came
 * from (the base). When the module ships a different default, the three are merged setting by
 * setting:
 * <ul>
 * <li>a setting the facility has not changed from the base takes the new default</li>
 * <li>a setting the facility has changed keeps the facility's value</li>
 * <li>a setting both have changed, differently, keeps the facility's value and is reported</li>
 * </ul>
 * Lists of rules, indicators, reports and parts are merged item by item, matched by "id", "code"
 * or "report"; other lists are treated as one setting. A rule the facility added stays, one it
 * removed stays removed, and one the new default drops goes unless the facility changed it.
 * <p>
 * A file seeded before bases were kept has no base, so what the facility changed is unknown: the
 * file is backed up and replaced by the new default.
 */
public final class DqConfigUpgrade {

	private static final Log log = LogFactory.getLog(DqConfigUpgrade.class);

	/** Keys that identify an item in a list, tried in order */
	static final List<String> IDENTITY_KEYS = Arrays.asList("id", "code", "report");

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private DqConfigUpgrade() {
	}

	/**
	 * Upgrades one configuration file to a new bundled default, backing it up first when it changes,
	 * and records the default as the file's new base
	 *
	 * @param bundled the bundled default, as JSON
	 * @param file the facility's file, which must exist
	 * @param baseFile where the base of the file is kept
	 * @return what was noted, empty when nothing needed attention; null when the file was already up
	 *         to date
	 */
	public static List<String> upgrade(String bundled, File file, File baseFile) throws IOException {
		JsonNode update = objectMapper.readTree(bundled);
		JsonNode base = baseFile.exists() ? objectMapper.readTree(baseFile) : null;
		if (base != null && base.equals(update)) {
			return null;
		}
		JsonNode facility = objectMapper.readTree(file);
		List<String> notes = new ArrayList<String>();
		JsonNode merged;
		if (base == null) {
			merged = update;
			if (!facility.equals(update)) {
				notes.add(file.getName() + " was set up before upgrades kept track of its default, so any changes made to it"
						+ " cannot be told apart: it has been replaced by the new default. Re-apply local settings from the backup.");
			}
		} else {
			merged = merge(facility, base, update, "", notes);
		}
		if (merged == null) {
			merged = JsonNodeFactory.instance.objectNode();
		}
		if (!merged.equals(facility)) {
			File backup = new File(file.getParentFile(), file.getName() + ".bak-"
					+ new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
			Files.copy(file.toPath(), backup.toPath());
			Files.write(file.toPath(), objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(merged));
			log.info("Upgraded data quality configuration " + file.getAbsolutePath() + " to the new default; the previous file is "
					+ backup.getName());
		}
		File dir = baseFile.getParentFile();
		if (dir != null && !dir.exists() && !dir.mkdirs()) {
			throw new IOException("Failed to create directory " + dir.getAbsolutePath());
		}
		Files.write(baseFile.toPath(), bundled.getBytes(StandardCharsets.UTF_8));
		for (String note : notes) {
			log.warn("Data quality configuration upgrade: " + note);
		}
		return notes;
	}

	/**
	 * Three-way merge of one setting. A null node means the setting is absent.
	 *
	 * @param facility the facility's value
	 * @param base the default the facility's file came from
	 * @param update the new default
	 * @param path where the setting is, for notes
	 * @param notes collects settings both sides changed
	 * @return the merged value, or null for absent
	 */
	static JsonNode merge(JsonNode facility, JsonNode base, JsonNode update, String path, List<String> notes) {
		if (same(facility, base)) {
			return update;
		}
		if (same(update, base) || same(facility, update)) {
			return facility;
		}
		if (facility instanceof ObjectNode && update instanceof ObjectNode && (base == null || base instanceof ObjectNode)) {
			return mergeObjects((ObjectNode) facility, (ObjectNode) base, (ObjectNode) update, path, notes);
		}
		if (facility instanceof ArrayNode && update instanceof ArrayNode && (base == null || base instanceof ArrayNode)) {
			String key = identityKey((ArrayNode) facility, (ArrayNode) base, (ArrayNode) update);
			if (key != null) {
				return mergeLists((ArrayNode) facility, (ArrayNode) base, (ArrayNode) update, key, path, notes);
			}
		}
		notes.add((path.isEmpty() ? "the file" : path) + ": kept the facility's "
				+ (facility == null ? "removal" : "value " + abbreviate(facility)) + "; the new default is "
				+ (update == null ? "to remove it" : abbreviate(update)));
		return facility;
	}

	private static ObjectNode mergeObjects(ObjectNode facility, ObjectNode base, ObjectNode update, String path,
	                                       List<String> notes) {
		Set<String> keys = new LinkedHashSet<String>();
		addFieldNames(keys, update);
		addFieldNames(keys, facility);
		addFieldNames(keys, base);
		ObjectNode merged = JsonNodeFactory.instance.objectNode();
		for (String key : keys) {
			JsonNode value = merge(facility.get(key), base != null ? base.get(key) : null, update.get(key),
					path.isEmpty() ? key : path + "." + key, notes);
			if (value != null) {
				merged.set(key, value);
			}
		}
		return merged;
	}

	private static ArrayNode mergeLists(ArrayNode facility, ArrayNode base, ArrayNode update, String key, String path,
	                                    List<String> notes) {
		Map<String, JsonNode> facilityItems = byIdentity(facility, key);
		Map<String, JsonNode> baseItems = base != null ? byIdentity(base, key) : new LinkedHashMap<String, JsonNode>();
		Map<String, JsonNode> updateItems = byIdentity(update, key);
		// The new default's order, then items only the facility has
		Set<String> identities = new LinkedHashSet<String>(updateItems.keySet());
		identities.addAll(facilityItems.keySet());
		identities.addAll(baseItems.keySet());
		ArrayNode merged = JsonNodeFactory.instance.arrayNode();
		for (String identity : identities) {
			JsonNode value = merge(facilityItems.get(identity), baseItems.get(identity), updateItems.get(identity),
					path + "[" + identity + "]", notes);
			if (value != null) {
				merged.add(value);
			}
		}
		return merged;
	}

	/**
	 * @return the key that identifies every item of the lists uniquely, or null when there is none
	 */
	static String identityKey(ArrayNode... lists) {
		for (String key : IDENTITY_KEYS) {
			boolean identifies = true;
			boolean any = false;
			for (ArrayNode list : lists) {
				if (list == null) {
					continue;
				}
				Set<String> seen = new LinkedHashSet<String>();
				for (JsonNode item : list) {
					any = true;
					JsonNode identity = item.get(key);
					if (!item.isObject() || identity == null || !identity.isValueNode() || !seen.add(identity.asText())) {
						identifies = false;
						break;
					}
				}
				if (!identifies) {
					break;
				}
			}
			if (identifies && any) {
				return key;
			}
		}
		return null;
	}

	private static Map<String, JsonNode> byIdentity(ArrayNode list, String key) {
		Map<String, JsonNode> items = new LinkedHashMap<String, JsonNode>();
		for (JsonNode item : list) {
			items.put(item.get(key).asText(), item);
		}
		return items;
	}

	private static void addFieldNames(Set<String> keys, ObjectNode node) {
		if (node == null) {
			return;
		}
		for (Iterator<String> names = node.fieldNames(); names.hasNext();) {
			keys.add(names.next());
		}
	}

	private static boolean same(JsonNode a, JsonNode b) {
		return a == null ? b == null : a.equals(b);
	}

	private static String abbreviate(JsonNode node) {
		String text = node.toString();
		return text.length() > 80 ? text.substring(0, 77) + "..." : text;
	}
}
