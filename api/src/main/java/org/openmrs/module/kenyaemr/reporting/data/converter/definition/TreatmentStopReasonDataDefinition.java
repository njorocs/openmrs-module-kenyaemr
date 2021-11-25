/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package reporting.data.converter.definition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.reporting.data.BaseDataDefinition;
import org.openmrs.module.reporting.data.person.definition.PersonDataDefinition;
import org.openmrs.module.reporting.definition.configuration.ConfigurationProperty;
import org.openmrs.module.reporting.definition.configuration.ConfigurationPropertyCachingStrategy;
import org.openmrs.module.reporting.evaluation.caching.Caching;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Treatment reason
 */
@Caching(strategy = ConfigurationPropertyCachingStrategy.class)
public class TreatmentStopReasonDataDefinition extends BaseDataDefinition implements PersonDataDefinition {

	static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	protected final Log log = LogFactory.getLog(getClass());

	@ConfigurationProperty
	private Integer txStopReasonConcept;

	public static final long serialVersionUID = 1L;

	/**
	 * Default Constructor
	 */
	public TreatmentStopReasonDataDefinition() {
		super();
	}

	public static long getSerialVersionUID() {
		return serialVersionUID;
	}

	/**
	 * Constructor to populate name,concept id
	 */
	public TreatmentStopReasonDataDefinition(String name, Integer stopReason) {
		super(name);
		this.txStopReasonConcept = stopReason;
	}
	
	//***** INSTANCE METHODS *****


	public Integer getTxStopReasonConcept() {
		return txStopReasonConcept;
	}

	public void setTxStopReasonConcept(Integer txStopReasonConcept) {
		this.txStopReasonConcept = txStopReasonConcept;
	}

	/**
	 * @see org.openmrs.module.reporting.data.DataDefinition#getDataType()
	 */
	public Class<?> getDataType() {
		return Date.class;
	}
}
