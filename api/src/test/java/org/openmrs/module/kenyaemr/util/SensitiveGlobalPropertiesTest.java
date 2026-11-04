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

import org.junit.Test;
import org.mockito.MockedStatic;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.kenyaemr.EmrActivator;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import org.openmrs.GlobalProperty;
import org.openmrs.Privilege;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.UserService;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SensitiveGlobalPropertiesTest {
    @Test
    public void protectsExistingCredentialsWithoutChangingValuesOrUnrelatedProperties() {
        AdministrationService administration = mock(AdministrationService.class);
        UserService users = mock(UserService.class);
        Privilege privilege = new Privilege(SensitiveGlobalProperties.CREDENTIAL_PRIVILEGE, "Credentials");
        when(users.getPrivilege(privilege.getPrivilege())).thenReturn(privilege);
        GlobalProperty credential = new GlobalProperty("kenyaemr.hie.il.mediator.client.secret", "existing-value");
        when(administration.getGlobalPropertyObject(credential.getProperty())).thenReturn(credential);

        SensitiveGlobalProperties.protectProperties(administration, users);
        SensitiveGlobalProperties.protectProperties(administration, users);

        assertEquals("existing-value", credential.getPropertyValue());
        assertEquals(privilege, credential.getViewPrivilege());
        assertEquals(privilege, credential.getEditPrivilege());
        assertEquals(privilege, credential.getDeletePrivilege());
        verify(administration, times(2)).saveGlobalProperty(credential);
        verify(administration, never()).getGlobalPropertyObject("kenyaemr.sms.url");
        verify(users, never()).savePrivilege(any(Privilege.class));
    }

    @Test
    public void createsRestrictedPrivilegeOnFirstStartup() {
        AdministrationService administration = mock(AdministrationService.class);
        UserService users = mock(UserService.class);
        SensitiveGlobalProperties.protectProperties(administration, users);
        verify(users).savePrivilege(argThat(p -> SensitiveGlobalProperties.CREDENTIAL_PRIVILEGE.equals(p.getPrivilege())));
        verify(administration, never()).saveGlobalProperty(any(GlobalProperty.class));
    }

    @Test
    public void readsCredentialsThroughModuleDaemonWithoutElevatingCaller() {
        AdministrationService administration = mock(AdministrationService.class);
        DaemonToken token = mock(DaemonToken.class);
        when(administration.getGlobalPropertyObject("dhis.password"))
            .thenReturn(new GlobalProperty("dhis.password", "configured-value"));
        try (MockedStatic<Context> context = mockStatic(Context.class);
             MockedStatic<Daemon> daemon = mockStatic(Daemon.class);
             MockedStatic<EmrActivator> activator = mockStatic(EmrActivator.class)) {
            activator.when(EmrActivator::getDaemonToken).thenReturn(token);
            context.when(Context::getAdministrationService).thenReturn(administration);
            daemon.when(() -> Daemon.runInDaemonThread(any(Callable.class), eq(token)))
                .thenAnswer(invocation -> {
                    Callable<GlobalProperty> operation = invocation.getArgument(0);
                    verifyNoInteractions(administration);
                    return CompletableFuture.completedFuture(operation.call());
                });
            assertEquals("configured-value", SensitiveGlobalProperties.get("dhis.password"));
            daemon.verify(() -> Daemon.runInDaemonThread(any(Callable.class), eq(token)));
            context.verify(() -> Context.addProxyPrivilege(SensitiveGlobalProperties.CREDENTIAL_PRIVILEGE), never());
            context.verify(() -> Context.removeProxyPrivilege(SensitiveGlobalProperties.CREDENTIAL_PRIVILEGE), never());
        }
    }

    @Test
    public void readsDirectlyWhenAlreadyOnDaemonThread() {
        AdministrationService administration = mock(AdministrationService.class);
        try (MockedStatic<Context> context = mockStatic(Context.class);
             MockedStatic<Daemon> daemon = mockStatic(Daemon.class)) {
            daemon.when(Daemon::isDaemonThread).thenReturn(true);
            context.when(Context::getAdministrationService).thenReturn(administration);
            assertNull(SensitiveGlobalProperties.get("dhis.password"));
            verify(administration).getGlobalPropertyObject("dhis.password");
            daemon.verify(() -> Daemon.runInDaemonThread(any(Callable.class), any(DaemonToken.class)), never());
        }
    }

    @Test
    public void propagatesDaemonReadFailuresWithoutRetryingAsCaller() {
        DaemonToken token = mock(DaemonToken.class);
        IllegalStateException failure = new IllegalStateException("Read failed");
        CompletableFuture<GlobalProperty> result = new CompletableFuture<>();
        result.completeExceptionally(failure);
        try (MockedStatic<Context> context = mockStatic(Context.class);
             MockedStatic<Daemon> daemon = mockStatic(Daemon.class);
             MockedStatic<EmrActivator> activator = mockStatic(EmrActivator.class)) {
            activator.when(EmrActivator::getDaemonToken).thenReturn(token);
            daemon.when(() -> Daemon.runInDaemonThread(any(Callable.class), eq(token))).thenReturn(result);
            try {
                SensitiveGlobalProperties.get("dhis.password");
                fail("Expected daemon failure");
            } catch (org.openmrs.api.APIException expected) {
                assertSame(failure, expected.getCause());
                context.verifyNoInteractions();
            }
        }
    }

    @Test(expected = IllegalStateException.class)
    public void refusesCallerThreadFallbackWhenDaemonTokenIsMissing() {
        try (MockedStatic<Context> context = mockStatic(Context.class);
             MockedStatic<Daemon> daemon = mockStatic(Daemon.class);
             MockedStatic<EmrActivator> activator = mockStatic(EmrActivator.class)) {
            try {
                SensitiveGlobalProperties.get("dhis.password");
            } finally {
                context.verifyNoInteractions();
            }
        }
    }

    @Test
    public void restrictsCodeDeclaredCredentialsBeforeTheyAreSaved() {
        UserService users = mock(UserService.class);
        Privilege privilege = new Privilege(SensitiveGlobalProperties.CREDENTIAL_PRIVILEGE, "Credentials");
        when(users.getPrivilege(privilege.getPrivilege())).thenReturn(privilege);
        try (MockedStatic<Context> context = mockStatic(Context.class)) {
            context.when(Context::getUserService).thenReturn(users);
            GlobalProperty property = SensitiveGlobalProperties.globalProperty(
                "kenyaemr.hie.il.mediator.client.secret", "Mediator secret", "");
            assertEquals("Mediator secret", property.getDescription());
            assertEquals("", property.getPropertyValue());
            assertEquals(privilege, property.getViewPrivilege());
            assertEquals(privilege, property.getEditPrivilege());
            assertEquals(privilege, property.getDeletePrivilege());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnregisteredPropertiesBeforeAccessingContext() {
        SensitiveGlobalProperties.get("kenyaemr.sms.url");
    }
}
