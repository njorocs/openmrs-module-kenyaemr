/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.calculation.converter;

import org.openmrs.module.reporting.data.converter.DataConverter;

/**
 * converter for boolean results
 * Boolean results
 */
public class VLDoneWithinIntervalBooleanResultsConverter implements DataConverter {

    @Override
    public Object convert(Object obj) {

        if (obj == null) {
            return "N/A";
        }

        String value =  obj.toString();

        if(value == null) {
            return  "N/A";
        }

        if(value.equalsIgnoreCase("true")) {
            return "Y";
        }

        if(value.equalsIgnoreCase("false")) {
            return "N";
        }
        return  "N";

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
