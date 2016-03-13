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

/**
 * Интерфейс модуля аудита результатов работы системы обнаружения аномалий.
 *
 * @author Andrey Grigorov
 */
public interface AuditModule {

    /**
     * Записать в журнал аудита информацию о проведённой обработке SQL-запроса.
     *
     * @param userId     идентификатор пользователя
     * @param sqlQuery   текст SQL-запроса
     * @param date       время выполнения запроса
     * @param eventType  тип события
     * @param mode       режим работы
     * @param properties дополнительные сведения, связанные с оценкой данного SQL-запроса
     */
    void logEvent(UserId userId, String sqlQuery, Date date, EventType eventType, IDSMode mode, Map<String, String> properties);

    /**
     * Установить подписку на события в системе аудита.
     *
     * @param listener слушатель, которому будет передаваться информация о
     *                 произошедшем событии
     */
    void addListener(Listener listener);

    /**
     * Отписаться от подписки на события в системе аудита.
     *
     * @param listener слушатель, которого необходимо отписать от получения
     *                 информации о произходящих событиях в системе аудита
     */
    void removeListener(Listener listener);
}
