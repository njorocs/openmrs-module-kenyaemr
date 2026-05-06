/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.WildcardTypePermission;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.ModuleActivator;
import org.openmrs.module.kenyacore.CoreContext;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.util.ServerInformation;
import org.openmrs.serialization.OpenmrsSerializer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * This class contains the logic that is run every time this module is either
 * started or stopped.
 */
public class EmrActivator implements ModuleActivator {

	protected static final Log log = LogFactory.getLog(EmrActivator.class);

	static {
		LogManager.getLogger("org.openmrs.module.kenyacore").setLevel(Level.INFO);
		LogManager.getLogger("org.openmrs.module.kenyaemr").setLevel(Level.INFO);
	}

	/**
	 * @see ModuleActivator#willRefreshContext()
	 */
	public void willRefreshContext() {
		log.info("KenyaEMR context refreshing...");
		// Remove stale report requests — they embed serialized XML that XStream 1.4.21
		// (used in reporting 2.1.0) can no longer deserialize due to security
		// restrictions
		Context.getAdministrationService().executeSQL(
				"DELETE FROM reporting_report_request", false);
	}

	/**
	 * @see ModuleActivator#willStart()
	 */
	public void willStart() {
		log.info("KenyaEMR starting...");
	}

	/**
	 * @see ModuleActivator#contextRefreshed()
	 */
	public void contextRefreshed() {
		Configuration.configure();

		// Configure XStream security BEFORE CoreContext.refresh() so that
		// report definitions can be serialized/deserialized correctly.
		// XStream 1.4.21 (reporting 2.1.0) blocks all classes by default —
		// we must explicitly whitelist org.openmrs.** classes here.
		configureXStreamSecurity();

		try {
			CoreContext.getInstance().refresh();
		} catch (Exception ex) {
			log.error("Unable to refresh core context", ex);
		}
	}

	/**
	 * @see ModuleActivator#started()
	 */
	public void started() {
		log.info("KenyaEMR started");
		AdministrationService administrationService = Context.getAdministrationService();
		administrationService.executeSQL("UPDATE form SET published = 1 WHERE retired = 0", false);

		Map<String, Object> kenyaemrInfo = ServerInformation.getKenyaemrInformation();
		String moduleVersion = (String) kenyaemrInfo.get("version");
		GlobalProperty gp = administrationService.getGlobalPropertyObject(CommonMetadata.GP_KENYAEMR_VERSION);
		if (gp == null) {
			gp = new GlobalProperty();
			gp.setProperty(CommonMetadata.GP_KENYAEMR_VERSION);
		}
		gp.setPropertyValue(moduleVersion);
		administrationService.saveGlobalProperty(gp);
	}

	/**
	 * @see ModuleActivator#willStop()
	 */
	public void willStop() {
		log.info("KenyaEMR stopping...");
	}

	/**
	 * @see ModuleActivator#stopped()
	 */
	public void stopped() {
		log.info("KenyaEMR stopped");
	}

	/**
	 * Configures XStream security to allow deserialization of all OpenMRS module
	 * classes.
	 *
	 * XStream 1.4.21 (reporting 2.1.0) enforces a strict class whitelist by
	 * default.
	 * Two separate XStream instances must be patched:
	 *
	 * 1. XStreamSerializer (serialization.xstream module) — used during report
	 * definition
	 * build and save at startup via CoreContext.refresh().
	 *
	 * 2. ReportingSerializer (reporting 2.1.0) — has its OWN internal XStream
	 * instance
	 * used by MappedDefinitionType when loading ReportRequests from the DB, and by
	 * getDefinitionByUuid() at runtime. Without patching this instance,
	 * ForbiddenClassException still occurs at runtime even after patching
	 * XStreamSerializer, because the two serializers are completely independent.
	 *
	 * This method must be called before CoreContext.refresh().
	 */
	private void configureXStreamSecurity() {
		String[] allowedPackages = new String[] {
				"org.openmrs.**",
				"java.util.**",
				"java.lang.**",
				"org.apache.commons.logging.**",
				"org.apache.commons.**",
				"org.slf4j.**", 
				"ch.qos.logback.**",
				"org.apache.log4j.**",
				"org.apache.logging.**"
		};

		int patchCount = 0;

		try {
			for (OpenmrsSerializer serializer : Context.getSerializationService().getSerializers()) {
				if (!serializer.getClass().getName().contains("XStream")) {
					continue;
				}
				XStream xstream = resolveXStreamInstance(serializer);
				if (xstream != null) {
					xstream.addPermission(new WildcardTypePermission(allowedPackages));
					log.info("KenyaEMR: XStream permissions set on XStreamSerializer: "
							+ serializer.getClass().getSimpleName());
					patchCount++;
				}
			}
		} catch (Exception e) {
			log.error("KenyaEMR: Failed to patch XStreamSerializer", e);
		}

		// --- Patch 2: ReportingSerializer (reporting module 2.1.0) ---
		// This serializer is separate from XStreamSerializer and holds its own
		// XStream instance. It is used by MappedDefinitionType.nullSafeGet() when
		// Hibernate loads ReportRequest rows, and by getDefinitionByUuid() at runtime.
		// Without patching this, ForbiddenClassException still occurs even after
		// patching XStreamSerializer above.
		try {
			OpenmrsSerializer reportingSerializer = Context.getSerializationService()
					.getSerializer(org.openmrs.module.reporting.serializer.ReportingSerializer.class);

			if (reportingSerializer != null) {
				XStream xstream = resolveXStreamInstance(reportingSerializer);

				if (xstream == null) {
					xstream = resolveXStreamDeep(reportingSerializer);
				}

				if (xstream != null) {
					xstream.addPermission(new WildcardTypePermission(allowedPackages));
					log.info("KenyaEMR: XStream permissions set on ReportingSerializer");
					patchCount++;
				} else {
					log.warn("KenyaEMR: Could not resolve XStream instance from ReportingSerializer. "
							+ "ForbiddenClassException may still occur when loading ReportRequests.");
				}
			} else {
				log.warn("KenyaEMR: ReportingSerializer not found in SerializationService. "
						+ "Is the reporting module loaded?");
			}
		} catch (Exception e) {
			log.error("KenyaEMR: Failed to patch ReportingSerializer", e);
		}

		if (patchCount == 0) {
			log.warn("KenyaEMR: No XStream instances were patched. ");
					
		} else {
			log.info("KenyaEMR: Successfully patched " + patchCount + " XStream instance(s).");
		}
	}

	private XStream resolveXStreamInstance(OpenmrsSerializer serializer) {
		for (String methodName : new String[] { "getXstream", "getXStream" }) {
			try {
				Method method = serializer.getClass().getMethod(methodName);
				Object result = method.invoke(serializer);
				if (result instanceof XStream) {
					log.debug("KenyaEMR: Resolved XStream via method: " + methodName);
					return (XStream) result;
				}
			} catch (NoSuchMethodException ignored) {
			} catch (Exception e) {
				log.warn("KenyaEMR: Failed to invoke " + methodName + " on serializer", e);
			}
		}

		Class<?> clazz = serializer.getClass();
		while (clazz != null && clazz != Object.class) {
			for (Field field : clazz.getDeclaredFields()) {
				if (XStream.class.isAssignableFrom(field.getType())) {
					try {
						field.setAccessible(true);
						Object value = field.get(serializer);
						if (value instanceof XStream) {
							log.debug("KenyaEMR: Resolved XStream via field: " + field.getName()
									+ " on " + clazz.getSimpleName());
							return (XStream) value;
						}
					} catch (Exception e) {
						log.warn("KenyaEMR: Failed to access field: " + field.getName(), e);
					}
				}
			}
			clazz = clazz.getSuperclass();
		}

		return null;
	}

	/**
	 * Deep search for an XStream instance across the full object graph of a
	 * serializer.
	 * Used for ReportingSerializer which may store its XStream inside an anonymous
	 * inner class, delegate, or wrapper object rather than as a direct field.
	 *
	 * Searches one level deep into non-primitive, non-JDK member objects.
	 *
	 * @param serializer the serializer to deep-search
	 * @return the XStream instance, or null if not found
	 */
	private XStream resolveXStreamDeep(OpenmrsSerializer serializer) {
		Class<?> clazz = serializer.getClass();
		while (clazz != null && clazz != Object.class) {
			for (Field field : clazz.getDeclaredFields()) {
				try {
					field.setAccessible(true);
					Object value = field.get(serializer);

					// Direct XStream field
					if (value instanceof XStream) {
						log.debug("KenyaEMR: Deep-resolved XStream directly from field: "
								+ field.getName());
						return (XStream) value;
					}

					// One level deeper — inspect non-null, non-primitive, non-JDK members
					if (value != null
							&& !field.getType().isPrimitive()
							&& !field.getType().isArray()
							&& !field.getType().getName().startsWith("java.")
							&& !field.getType().getName().startsWith("javax.")) {
						for (Field inner : value.getClass().getDeclaredFields()) {
							if (XStream.class.isAssignableFrom(inner.getType())) {
								try {
									inner.setAccessible(true);
									Object innerValue = inner.get(value);
									if (innerValue instanceof XStream) {
										log.debug("KenyaEMR: Deep-resolved XStream from "
												+ field.getName() + "." + inner.getName());
										return (XStream) innerValue;
									}
								} catch (Exception ignored) {
								}
							}
						}
					}
				} catch (Exception ignored) {
				}
			}
			clazz = clazz.getSuperclass();
		}
		return null;
	}
}