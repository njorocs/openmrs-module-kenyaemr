/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.data.converter;

import org.openmrs.api.context.Context;
import org.openmrs.Location;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.reporting.data.converter.DataConverter;

/**
 * Converts an MFL code (siteCode) into a human-friendly location name using KenyaEmrService.
 */
public class LocationNameConverter implements DataConverter {

    @Override
    public Object convert(Object original) {
        if (original == null) return null;
        try {
            String siteCode = original.toString();
            KenyaEmrService svc = Context.getService(KenyaEmrService.class);
            Location loc = svc.getLocationByMflCode(siteCode);
            if (loc != null) return siteCode + ": " + loc.getName();
            return siteCode;
        } catch (Exception e) {
            return original;
        }
    }

    @Override
    public Class<?> getInputDataType() {
        return Object.class;
    }

    @Override
    public Class<?> getDataType() {
        return String.class;
    }
}

