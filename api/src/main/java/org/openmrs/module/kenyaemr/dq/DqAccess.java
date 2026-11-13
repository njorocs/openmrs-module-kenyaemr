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

import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.api.DqService;
import org.openmrs.util.PrivilegeConstants;

/**
 * Who may do what with the data quality checks. A super admin and the roles in
 * {@link DqService#RUN_ROLES} (the Health Record Officer and the system administrators) may do
 * everything; anyone else needs the privilege for the action.
 */
public final class DqAccess {

	private DqAccess() {
	}

	/**
	 * @return whether the authenticated user is a super admin or has one of {@link DqService#RUN_ROLES},
	 *         inherited roles included
	 */
	public static boolean hasDqRole() {
		User user = Context.getAuthenticatedUser();
		if (user == null) {
			return false;
		}
		if (user.isSuperUser()) {
			return true;
		}
		for (String role : DqService.RUN_ROLES) {
			if (user.hasRole(role)) {
				return true;
			}
		}
		return false;
	}

	/** Run the checks now, generate reports and work the tickets; or with Manage Scheduler */
	public static boolean mayRun() {
		return hasDqRole() || Context.hasPrivilege(PrivilegeConstants.MANAGE_SCHEDULER);
	}

	/** Approve a report version for KHIS; or with "Approve Data Quality Reports" */
	public static boolean mayApprove() {
		return hasDqRole() || Context.hasPrivilege(DqService.PRIVILEGE_APPROVE);
	}

	/** Set the rules' thresholds for the facility; or with "Manage Data Quality Settings" */
	public static boolean mayManageSettings() {
		return hasDqRole() || Context.hasPrivilege(DqService.PRIVILEGE_MANAGE_SETTINGS);
	}
}
