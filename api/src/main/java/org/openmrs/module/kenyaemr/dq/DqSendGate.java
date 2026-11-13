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

import org.openmrs.module.kenyaemr.model.dq.DqReportRun;

import java.util.ArrayList;
import java.util.List;

/**
 * Whether a report, as generated on the reports page, may be sent to KHIS. A report the data
 * quality checks cover is sent only once its figures have been checked, nothing blocks it and
 * every warning has been reviewed. Viewing and downloading the report are never held back.
 */
public class DqSendGate {

	public enum State {
		/** The checks do not cover this report or period, so it is sent as before */
		NOT_CHECKED_HERE(true),
		/** The checks are running on these figures */
		CHECKING(false),
		/** These figures have not been checked */
		NOT_CHECKED(false),
		/** A newer version of the report, with other figures, has been generated since */
		NOT_LATEST(false),
		/** A report sent a week at a time, generated for a period that is not a whole week or month that has ended */
		WRONG_PERIOD(false),
		/** A hard block is open */
		BLOCKED(false),
		/** A warning needs a reason on the Data quality page */
		NEEDS_REVIEW(false),
		/** Nothing blocks and nothing waits for review */
		READY(true);

		private final boolean sendable;

		State(boolean sendable) {
			this.sendable = sendable;
		}

		public boolean isSendable() {
			return sendable;
		}
	}

	private final State state;

	private final DqReportRun run;

	private final String message;

	private final List<String> problems;

	private List<String> blockers = new ArrayList<String>();

	private List<String> warnings = new ArrayList<String>();

	private List<String> tickets = new ArrayList<String>();

	private List<String> resolvedTickets = new ArrayList<String>();

	public DqSendGate(State state, DqReportRun run, String message, List<String> problems) {
		this.state = state;
		this.run = run;
		this.message = message;
		this.problems = problems != null ? problems : new ArrayList<String>();
	}

	/**
	 * Sets the checks of the version that did not pass, by what they need: each "rule: message"
	 *
	 * @param blockers block sending until the records are corrected
	 * @param warnings need a reason, given when the report is approved
	 * @param tickets do not hold sending, but are followed up as tickets
	 */
	public DqSendGate withFindings(List<String> blockers, List<String> warnings, List<String> tickets) {
		this.blockers = blockers;
		this.warnings = warnings;
		this.tickets = tickets;
		return this;
	}

	/**
	 * @param resolvedTickets those of the tickets whose ticket has been resolved
	 */
	public DqSendGate withResolvedTickets(List<String> resolvedTickets) {
		this.resolvedTickets = resolvedTickets;
		return this;
	}

	public static DqSendGate notCheckedHere() {
		return new DqSendGate(State.NOT_CHECKED_HERE, null, null, null);
	}

	public State getState() {
		return state;
	}

	public boolean isSendable() {
		return state.isSendable();
	}

	/**
	 * @return the version the figures were checked as, or for NOT_LATEST the newer version
	 */
	public DqReportRun getRun() {
		return run;
	}

	/**
	 * @return what the user reads beside the send button; null when nothing holds the report back
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @return the open blocks, or the warnings waiting for review
	 */
	public List<String> getProblems() {
		return problems;
	}

	/**
	 * @return the checks that block sending
	 */
	public List<String> getBlockers() {
		return blockers;
	}

	/**
	 * @return the warnings, which need a reason when the report is approved
	 */
	public List<String> getWarnings() {
		return warnings;
	}

	/**
	 * @return the checks flagged for follow-up as tickets, which do not hold sending
	 */
	public List<String> getTickets() {
		return tickets;
	}

	/**
	 * @return those of {@link #getTickets()} whose ticket has been resolved
	 */
	public List<String> getResolvedTickets() {
		return resolvedTickets;
	}
}
