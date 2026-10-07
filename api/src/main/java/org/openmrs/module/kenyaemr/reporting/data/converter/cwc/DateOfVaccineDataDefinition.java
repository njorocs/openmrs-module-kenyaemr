/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.data.converter.cwc;

import org.openmrs.module.reporting.data.BaseDataDefinition;
import org.openmrs.module.reporting.data.encounter.definition.EncounterDataDefinition;
import org.openmrs.module.reporting.definition.configuration.ConfigurationProperty;
import org.openmrs.module.reporting.definition.configuration.ConfigurationPropertyCachingStrategy;
import org.openmrs.module.reporting.evaluation.caching.Caching;

import java.util.Date;

/**
 * Visit ID Column
 */
@Caching(strategy=ConfigurationPropertyCachingStrategy.class)
public class DateOfVaccineDataDefinition extends BaseDataDefinition implements EncounterDataDefinition {

    public static final long serialVersionUID = 1L;

    /** vaccine concept id as stored in etl_immunization.vaccine */
    @ConfigurationProperty
    private Integer vaccine;

    /** dose number as stored in etl_immunization.sequence, null for any dose */
    @ConfigurationProperty
    private Integer sequence;

    /**
     * Default Constructor
     */
    public DateOfVaccineDataDefinition() {
        super();
    }

    /**
     * Constructor to populate name only
     */
    public DateOfVaccineDataDefinition(String name) {

        super(name);
    }

    //***** INSTANCE METHODS *****


    public Integer getVaccine() {
        return vaccine;
    }

    public void setVaccine(Integer vaccine) {
        this.vaccine = vaccine;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public DateOfVaccineDataDefinition(String name, Integer vaccine, Integer sequence) {
        super(name);
        this.vaccine = vaccine;
        this.sequence = sequence;
    }

    /**
     * @see org.openmrs.module.reporting.data.DataDefinition#getDataType()
     */
    public Class<?> getDataType() {
        return Date.class;
    }
}
