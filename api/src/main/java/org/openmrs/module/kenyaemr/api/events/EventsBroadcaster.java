/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.api.events;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Component
public class EventsBroadcaster {

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void register(String requestId, SseEmitter emitter) {

        emitters.computeIfAbsent(requestId, k ->
                Collections.synchronizedList(new ArrayList<>())
        ).add(emitter);

        emitter.onCompletion(() -> emitters.getOrDefault(requestId, new ArrayList<>()).remove(emitter));
        emitter.onTimeout(() -> emitters.getOrDefault(requestId, new ArrayList<>()).remove(emitter));
        emitter.onError(e -> emitters.getOrDefault(requestId, new ArrayList<>()).remove(emitter));
    }

    public void sendStatusEvent(String requestId, Map<String,Object> data, String eventName) {
        List<SseEmitter> list = emitters.get(requestId);
        if (list == null) return;

        Iterator<SseEmitter> it = list.iterator();
        while (it.hasNext()) {
            SseEmitter e = it.next();
            try {
                e.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception ex) {
                it.remove();
            }
        }
    }
}
