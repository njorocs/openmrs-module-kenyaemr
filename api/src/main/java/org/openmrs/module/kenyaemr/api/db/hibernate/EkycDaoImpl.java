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

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.openmrs.module.kenyaemr.api.model.BiometricVerification;
import org.openmrs.module.kenyaemr.api.model.OtpUseRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public class EkycDaoImpl implements EkycDao {
    private SessionFactory sessionFactory;


    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private org.hibernate.Session session() {
        return sessionFactory.getCurrentSession();
    }

    // BIOMETRIC VERIFICATION

    @Override
    public void saveVerification(BiometricVerification v) {
        session().save(v);
    }

    @Override
    public void updateVerification(BiometricVerification v) {
        session().update(v);
    }

    @Override
    public BiometricVerification getById(Integer id) {
        return session().get(BiometricVerification.class, id);
    }

    @Override
    public BiometricVerification getByRequestId(String requestId) {
        Query<BiometricVerification> q = session().createQuery(
                "from BiometricVerification where requestId = :rid", BiometricVerification.class);
        q.setParameter("rid", requestId);
        List<BiometricVerification> list = q.list();
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<BiometricVerification> getAllVerifications() {
        return session().createQuery(
                        "from BiometricVerification order by dateCreated desc", BiometricVerification.class)
                .list();
    }

    // OTP USE REQUEST
    @Override
    public void saveOtpRequest(OtpUseRequest r) {
        session().save(r);
    }

    @Override
    public OtpUseRequest getOtpRequestById(Integer id) {
        return session().get(OtpUseRequest.class, id);
    }

    @Override
    public List<OtpUseRequest> getOtpRequestsByVerificationId(Integer verificationId) {
        Query<OtpUseRequest> q = session().createQuery(
                "from OtpUseRequest where verificationId = :vid order by dateRequested desc",
                OtpUseRequest.class
        );
        q.setParameter("vid", verificationId);
        return q.list();
    }

    @Override
    public List<OtpUseRequest> getAllOtpRequests() {
        return session().createQuery(
                        "from OtpUseRequest order by dateRequested desc", OtpUseRequest.class)
                .list();
    }
}