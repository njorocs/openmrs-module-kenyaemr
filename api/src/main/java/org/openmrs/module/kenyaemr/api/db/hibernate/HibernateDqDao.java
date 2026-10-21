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
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.kenyaemr.model.dq.DqResult;
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
			+ "where r2.reportName = r.reportName and r2.periodStart = r.periodStart)";

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
				.createQuery("from DqReportRun r where r.reportName = :reportName and r.periodStart = :periodStart "
						+ "order by r.version desc")
				.setParameter("reportName", reportName)
				.setParameter("periodStart", periodStart)
				.setMaxResults(1)
				.uniqueResult();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DqReportRun> getLatestReportRuns(Date periodStart) {
		return session()
				.createQuery("from DqReportRun r where r.periodStart = :periodStart and " + LATEST_VERSION
						+ " order by r.reportName")
				.setParameter("periodStart", periodStart)
				.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DqReportRun> getLatestReportRuns(String reportName, Date fromPeriod, Date toPeriod) {
		return session()
				.createQuery("from DqReportRun r where r.reportName = :reportName "
						+ "and r.periodStart >= :fromPeriod and r.periodStart <= :toPeriod and " + LATEST_VERSION
						+ " order by r.periodStart")
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
	@SuppressWarnings("unchecked")
	public List<DqValue> getLatestValues(Collection<String> reportNames, Date fromPeriod, Date toPeriod) {
		if (reportNames == null || reportNames.isEmpty()) {
			return Collections.emptyList();
		}
		return session()
				.createQuery("select v from DqValue v join v.reportRun r where r.reportName in (:reportNames) "
						+ "and r.periodStart >= :fromPeriod and r.periodStart <= :toPeriod and " + LATEST_VERSION)
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
				.createQuery("from DqResult r where r.ruleId = :ruleId and r.periodStart >= :fromPeriod "
						+ "and r.periodStart <= :toPeriod order by r.periodStart, r.evaluatedAt")
				.setParameter("ruleId", ruleId)
				.setParameter("fromPeriod", fromPeriod)
				.setParameter("toPeriod", toPeriod)
				.list();
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
