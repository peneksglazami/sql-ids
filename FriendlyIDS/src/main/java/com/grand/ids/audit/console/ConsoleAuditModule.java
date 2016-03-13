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
package com.grand.ids.audit.console;

import com.grand.ids.UserId;
import com.grand.ids.audit.AbstractAuditModule;
import com.grand.ids.audit.EventType;
import com.grand.ids.jdbc.IDSMode;

import java.util.Date;
import java.util.Map;

/**
 * Реализация консольной версии модуля аудита. Лог пишется в
 * стандартный поток вывода.
 *
 * @author Andrey Grigorov
 */
public class ConsoleAuditModule extends AbstractAuditModule {

    public void logEvent(UserId userId, String sqlQuery, Date date, EventType eventType, IDSMode mode, Map<String, String> properties) {
        synchronized (this) {
            System.out.println(sqlQuery);
            System.out.println("userId = " + userId.getUserId());
            System.out.println("eventType = " + eventType.name());
            System.out.println("mode = " + mode.name());
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                System.out.println(entry.getKey() + " = " + entry.getValue());
            }
            System.out.println();
        }
    }
}
