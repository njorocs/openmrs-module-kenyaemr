/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.openmrs.GlobalProperty;
import org.openmrs.Privilege;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.module.kenyaemr.EmrActivator;

/** Internal credential access. Never expose these values through a REST resource. */
public final class SensitiveGlobalProperties {
    public static final String CREDENTIAL_PRIVILEGE = "Restricted: Manage KenyaEMR Credentials";

    /** Explicit allowlist of credentials configured by KenyaEMR. */
    private static final Set<String> MANAGED_PROPERTIES = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
        "kenyaemr.ndwh.nupi.client.id",
        "kenyaemr.ndwh.nupi.client.secret",
        "kenyaemr.sms.apiToken",
        "mail.smtp.username",
        "mail.smtp.password",
        "nlmis.oauth.token",
        "dhis.username",
        "dhis.password",
        "kenyaemr.client.registry.api.token",
        "kenyaemr.client.registry.oath2.client.id",
        "kenyaemr.client.registry.oath2.client.secret",
        "kenyaemr.sha.facilityregistry.get.api.user",
        "kenyaemr.sha.facilityregistry.get.api.secret",
        "kenyaemr.sha.healthworker.get.api.user",
        "kenyaemr.sha.healthworker.get.api.secret",
        "kenyaemr.sha.registry.get.api.user",
        "kenyaemr.sha.registry.get.api.secret",
        "kenyaemr.sha.jwt.token.username",
        "kenyaemr.sha.jwt.token.password",
        "kenyaemr.hie.il.mediator.client.id",
        "kenyaemr.hie.il.mediator.client.secret"
    )));

    private SensitiveGlobalProperties() {
    }

    /** Creates metadata credentials with access restrictions applied before installation. */
    public static GlobalProperty globalProperty(String name, String description, String defaultValue) {
        if (!MANAGED_PROPERTIES.contains(name)) {
            throw new IllegalArgumentException("Property is not registered for credential access");
        }
        GlobalProperty property = new GlobalProperty(name, defaultValue, description);
        restrict(property, credentialPrivilege(Context.getUserService()));
        return property;
    }

    private static Privilege credentialPrivilege(UserService users) {
        Privilege privilege = users.getPrivilege(CREDENTIAL_PRIVILEGE);
        if (privilege == null) {
            privilege = users.savePrivilege(new Privilege(CREDENTIAL_PRIVILEGE,
                "View and maintain credentials used by KenyaEMR integrations"));
        }
        return privilege;
    }

    private static void restrict(GlobalProperty property, Privilege privilege) {
        property.setViewPrivilege(privilege);
        property.setEditPrivilege(privilege);
        property.setDeletePrivilege(privilege);
    }

    public static String get(String property) {
        GlobalProperty value = getPropertyObject(property);
        return value == null ? null : value.getPropertyValue();
    }

    public static GlobalProperty getPropertyObject(String property) {
        if (!MANAGED_PROPERTIES.contains(property)) {
            throw new IllegalArgumentException("Property is not registered for credential access");
        }
        return asDaemon(() -> Context.getAdministrationService().getGlobalPropertyObject(property));
    }

    /** Applies to existing installations as well as properties created from config.xml. */
    public static void protectProperties() {
        asDaemon(() -> {
            protectProperties(Context.getAdministrationService(), Context.getUserService());
            return null;
        });
    }

    static void protectProperties(AdministrationService administration, UserService users) {
        Privilege privilege = credentialPrivilege(users);
        // Restrict only credentials configured by KenyaEMR, including its SMTP and DHIS settings.
        for (String name : MANAGED_PROPERTIES) {
            GlobalProperty property = administration.getGlobalPropertyObject(name);
            if (property != null) {
                restrict(property, privilege);
                administration.saveGlobalProperty(property);
            }
        }
    }

    private static <T> T asDaemon(Callable<T> operation) {
        try {
            if (Daemon.isDaemonThread()) {
                return operation.call();
            }
            if (EmrActivator.getDaemonToken() == null) {
                throw new IllegalStateException("KenyaEMR daemon token is unavailable");
            }
            return Daemon.runInDaemonThread(operation, EmrActivator.getDaemonToken()).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new APIException("Credential access interrupted", e);
        } catch (ExecutionException e) {
            throw new APIException("Credential access failed", e.getCause());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new APIException("Credential access failed", e);
        }
    }
}
