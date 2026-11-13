/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.api.db.hibernate;

import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.module.kenyaemr.api.db.DqDao;
import org.openmrs.module.kenyaemr.model.dq.DqOverride;
import org.openmrs.module.kenyaemr.model.dq.DqReportPayload;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.kenyaemr.model.dq.DqResult;
import org.openmrs.module.kenyaemr.model.dq.DqRunStatus;
import org.openmrs.module.kenyaemr.model.dq.DqTicket;
import org.openmrs.module.kenyaemr.model.dq.DqTicketStatus;
import org.openmrs.module.kenyaemr.model.dq.DqValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Hibernate implementation of {@link DqDao}
 */
public class HibernateDqDao implements DqDao {

	/** Restricts a run alias "r" to the highest version of its report and period */
	private static final String LATEST_VERSION = "r.version = (select max(r2.version) from DqReportRun r2 "
			+ "where r2.reportName = r.reportName and r2.periodStart = r.periodStart and r2.periodEnd = r.periodEnd)";

	/**
	 * @return a condition true for a run of a whole month; the month queries leave out the weekly
	 * versions of a report sent a week at a time (MOH 505), even one starting on the 1st
	 */
	static String monthRun(String alias) {
		return "(day(" + alias + ".periodStart) = 1 and day(" + alias + ".periodEnd) >= 28)";
	}

	private SessionFactory sessionFactory;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	private Session session() {
		return sessionFactory.getCurrentSession();
	}

	@Override
	public DqReportRun saveReportRun(DqReportRun run) {
		session().saveOrUpdate(run);
		return run;
	}

	@Override
	public DqReportRun getReportRunByUuid(String uuid) {
		return (DqReportRun) session().createQuery("from DqReportRun where uuid = :uuid")
				.setParameter("uuid", uuid)
				.uniqueResult();
	}

	@Override
	public DqReportRun getLatestReportRun(String reportName, Date periodStart) {
		return (DqReportRun) session()
				.createQuery("from DqReportRun r where r.reportName = :reportName and r.periodStart = :periodStart and "
						+ monthRun("r") + " order by r.version desc")
				.setParameter("reportName", reportName)
				.setParameter("periodStart", periodStart)
				.setMaxResults(1)
				.uniqueResult();
	}

	@Override
	public DqReportRun getLatestReportRun(String reportName, Date periodStart, Date periodEnd) {
		return (DqReportRun) session()
				.createQuery("from DqReportRun r where r.reportName = :reportName and r.periodStart = :periodStart "
						+ "and r.periodEnd = :periodEnd order by r.version desc")
				.setParameter("reportName", reportName)
				.setParameter("periodStart", periodStart)
				.setParameter("periodEnd", periodEnd)
				.setMaxResults(1)
				.uniqueResult();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DqReportRun> getLatestWeeklyRuns(Date fromPeriod, Date toPeriod) {
		return session()
				.createQuery("from DqReportRun r where r.periodStart >= :fromPeriod and r.periodStart <= :toPeriod and not "
						+ monthRun("r") + " and " + LATEST_VERSION + " order by r.periodStart, r.reportName")
				.setParameter("fromPeriod", fromPeriod)
				.setParameter("toPeriod", toPeriod)
				.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DqReportRun> getLatestReportRuns(Date periodStart) {
		return session()
				.createQuery("from DqReportRun r where r.periodStart = :periodStart and " + monthRun("r") + " and "
						+ LATEST_VERSION + " order by r.reportName")
				.setParameter("periodStart", periodStart)
				.list();
	}

	@Override
	public boolean hasReportRuns() {
		return !session().createQuery("select r.id from DqReportRun r").setMaxResults(1).list().isEmpty();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DqReportRun> getLatestReportRuns(String reportName, Date fromPeriod, Date toPeriod) {
		return session()
				.createQuery("from DqReportRun r where r.reportName = :reportName "
						+ "and r.periodStart >= :fromPeriod and r.periodStart <= :toPeriod and " + monthRun("r") + " and "
						+ LATEST_VERSION + " order by r.periodStart")
				.setParameter("reportName", reportName)
				.setParameter("fromPeriod", fromPeriod)
				.setParameter("toPeriod", toPeriod)
				.list();
	}

	@Override
	public void saveValues(Collection<DqValue> values) {
		Session session = session();
		int count = 0;
		for (DqValue value : values) {
			session.save(value);
			// A report such as MOH 705A has a few thousand cells; keep the session small
			if (++count % 200 == 0) {
				session.flush();
			}
		}
		session.flush();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DqValue> getValues(DqReportRun run) {
		return session().createQuery("from DqValue v where v.reportRun = :run order by v.id")
				.setParameter("run", run)
				.list();
	}

	@Override
	public DqReportPayload savePayload(DqReportPayload payload) {
		session().saveOrUpdate(payload);
		return payload;
	}

	@Override
	public DqReportPayload getPayload(DqReportRun run) {
		return (DqReportPayload) session().createQuery("from DqReportPayload p where p.reportRun = :run")
				.setParameter("run", run)
				.uniqueResult();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DqValue> getLatestValues(Collection<String> reportNames, Date fromPeriod, Date toPeriod) {
		if (reportNames == null || reportNames.isEmpty()) {
			return Collections.emptyList();
		}
		return session()
				.createQuery("select v from DqValue v join v.reportRun r where r.reportName in (:reportNames) "
						+ "and r.periodStart >= :fromPeriod and r.periodStart <= :toPeriod and " + monthRun("r") + " and "
						+ LATEST_VERSION)
				.setParameterList("reportNames", reportNames)
				.setParameter("fromPeriod", fromPeriod)
				.setParameter("toPeriod", toPeriod)
				.list();
	}

	@Override
	public DqResult saveResult(DqResult result) {
		session().saveOrUpdate(result);
		return result;
	}

	@Override
	public DqResult getResultByUuid(String uuid) {
		return (DqResult) session().createQuery("from DqResult where uuid = :uuid")
				.setParameter("uuid", uuid)
				.uniqueResult();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DqResult> getResults(DqReportRun run) {
		return session().createQuery("from DqResult r where r.reportRun = :run order by r.ruleId, r.indicatorCode")
				.setParameter("run", run)
				.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DqResult> getResults(String ruleId, Date fromPeriod, Date toPeriod) {
		return session()
				.createQuery("select r from DqResult r left join r.reportRun run where r.ruleId = :ruleId "
						+ "and r.periodStart >= :fromPeriod and r.periodStart <= :toPeriod and (run is null or "
						+ monthRun("run") + ") order by r.periodStart, r.evaluatedAt")
				.setParameter("ruleId", ruleId)
				.setParameter("fromPeriod", fromPeriod)
				.setParameter("toPeriod", toPeriod)
				.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DqResult> getProgramResults(String programCode, String ruleId, Date fromPeriod, Date toPeriod) {
		Query query = session().createQuery("from DqResult r left join fetch r.reportRun run "
				+ "where r.periodStart >= :fromPeriod and r.periodStart <= :toPeriod "
				+ "and (run is null or (run.status <> :superseded and " + monthRun("run") + "))"
				+ (programCode != null ? " and r.programCode = :programCode" : "")
				+ (ruleId != null ? " and r.ruleId = :ruleId" : "")
				+ " order by r.periodStart, r.ruleId, r.indicatorCode");
		query.setParameter("fromPeriod", fromPeriod).setParameter("toPeriod", toPeriod)
				.setParameter("superseded", DqRunStatus.SUPERSEDED);
		if (programCode != null) {
			query.setParameter("programCode", programCode);
		}
		if (ruleId != null) {
			query.setParameter("ruleId", ruleId);
		}
		return query.list();
	}

	@Override
	public DqOverride saveOverride(DqOverride override) {
		session().saveOrUpdate(override);
		return override;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DqOverride> getOverrides(DqReportRun run) {
		return session().createQuery("select o from DqOverride o where o.result.reportRun = :run")
				.setParameter("run", run)
				.list();
	}

	@Override
	public DqTicket saveTicket(DqTicket ticket) {
		session().saveOrUpdate(ticket);
		return ticket;
	}

	@Override
	public DqTicket getTicketByUuid(String uuid) {
		return (DqTicket) session().createQuery("from DqTicket where uuid = :uuid")
				.setParameter("uuid", uuid)
				.uniqueResult();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DqTicket> getTickets(DqTicketStatus status) {
		Query query;
		if (status == null) {
			query = session().createQuery("from DqTicket t order by t.openedAt desc");
		} else {
			query = session().createQuery("from DqTicket t where t.status = :status order by t.openedAt desc")
					.setParameter("status", status);
		}
		return query.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DqTicket> getUnresolvedTickets(String ruleId, Date periodStart) {
		return session().createQuery("from DqTicket t where t.ruleId = :ruleId and t.periodStart = :periodStart "
				+ "and t.status <> :resolved order by t.id")
				.setParameter("ruleId", ruleId)
				.setParameter("periodStart", periodStart)
				.setParameter("resolved", DqTicketStatus.RESOLVED)
				.list();
	}

	@Override
	public List<DqTicket> getTickets(String ruleId, Date periodStart) {
		return session().createQuery("from DqTicket t where t.ruleId = :ruleId and t.periodStart = :periodStart "
				+ "order by t.id desc")
				.setParameter("ruleId", ruleId)
				.setParameter("periodStart", periodStart)
				.list();
	}

	@Override
	public int executeSqlUpdate(String sql, Map<String, Object> parameters) {
		SQLQuery query = session().createSQLQuery(sql);
		for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
			query.setParameter(parameter.getKey(), parameter.getValue());
		}
		return query.executeUpdate();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> executeSqlQuery(String sql, Map<String, Object> parameters) {
		SQLQuery query = session().createSQLQuery(sql);
		if (parameters != null) {
			for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
				if (parameter.getValue() instanceof Collection) {
					query.setParameterList(parameter.getKey(), (Collection<?>) parameter.getValue());
				} else {
					query.setParameter(parameter.getKey(), parameter.getValue());
				}
			}
		}
		List<Object[]> rows = new ArrayList<Object[]>();
		for (Object row : query.list()) {
			rows.add(row instanceof Object[] ? (Object[]) row : new Object[] { row });
		}
		return rows;
	}
}
