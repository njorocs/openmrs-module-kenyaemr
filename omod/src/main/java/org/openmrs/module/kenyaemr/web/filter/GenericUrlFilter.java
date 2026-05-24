/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.web.filter;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

public class GenericUrlFilter implements Filter {

    private static final String EMR_PREFIX = "/emr/";
    private static final String KENYAEMR_PREFIX = "/kenyaemr/";
    private static final String FORWARDED_ATTR = "emr.internal.forward";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String contextPath = request.getContextPath();
        String path = request.getRequestURI().substring(contextPath.length());

        if (path.startsWith(EMR_PREFIX)) {
            String newPath = KENYAEMR_PREFIX + path.substring(EMR_PREFIX.length());
            request.setAttribute(FORWARDED_ATTR, Boolean.TRUE);

            final ClassLoader moduleClassLoader = this.getClass().getClassLoader();
            HttpServletRequest wrappedRequest = new QueryStringRewritingRequest(request, "emr.", "kenyaemr.");

            try {
                Thread.currentThread().setContextClassLoader(moduleClassLoader);
                wrappedRequest.getRequestDispatcher(newPath).forward(wrappedRequest, res);
            } finally {
                Thread.currentThread().setContextClassLoader(moduleClassLoader);
            }
            return;
        }

        if (path.startsWith(KENYAEMR_PREFIX)
                && request.getAttribute(FORWARDED_ATTR) == null
                && !path.contains(".action")) {

            String newPath = contextPath + EMR_PREFIX + path.substring(KENYAEMR_PREFIX.length());
            String qs = rewriteQueryString(request.getQueryString());
            String redirectUrl = newPath + (qs != null ? "?" + qs : "");
            response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
            response.setHeader("Location", redirectUrl);
            return;
        }

        chain.doFilter(req, res);
    }

    private String rewriteQueryString(String qs) {
        if (qs == null)
            return null;
        return qs
                .replaceAll("kenyaemr\\.", "emr.")
                .replace("%2Fkenyaemr%2F", "%2Femr%2F")
                .replace("/kenyaemr/", "/emr/");
    }

    private static class QueryStringRewritingRequest extends HttpServletRequestWrapper {
        private final String find;
        private final String replace;

        QueryStringRewritingRequest(HttpServletRequest request, String find, String replace) {
            super(request);
            this.find = find;
            this.replace = replace;
        }

        private String rewrite(String value) {
            if (value == null)
                return null;
            return value.replaceAll("(?<!kenya)emr\\.", "kenyaemr.");
        }

        @Override
        public String getQueryString() {
            return rewrite(super.getQueryString());
        }

        @Override
        public String getParameter(String name) {
            return rewrite(super.getParameter(name));
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null)
                return null;
            String[] rewritten = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                rewritten[i] = rewrite(values[i]);
            }
            return rewritten;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Map getParameterMap() {
            Map<String, String[]> original = (Map<String, String[]>) super.getParameterMap();
            Map<String, String[]> rewritten = new LinkedHashMap<String, String[]>();
            for (String key : original.keySet()) {
                String[] values = original.get(key);
                String[] newValues = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    newValues[i] = rewrite(values[i]);
                }
                rewritten.put(key, newValues);
            }
            return Collections.unmodifiableMap(rewritten);
        }

        @Override
        public Enumeration getParameterNames() {
            return super.getParameterNames();
        }
    }

    @Override
    public void init(FilterConfig fc) {
    }

    @Override
    public void destroy() {
    }
}