/*
 * Copyright 2009-2016 Andrey Grigorov
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.grand.ids.audit;

import com.grand.ids.UserId;
import com.grand.ids.jdbc.IDSMode;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Абстрактная реализация модуля аудирования.
 *
 * @author Andrey Grigorov
 */
public abstract class AbstractAuditModule implements AuditModule {

    private final Set<Listener> listeners = new CopyOnWriteArraySet<Listener>();

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void logEvent(UserId userId, String sqlQuery, Date date, EventType eventType, IDSMode mode, Map<String, String> properties) {
        for (Listener listener : listeners) {
            listener.onEvent(userId, sqlQuery, date, eventType, mode, properties);
        }
    }
}
