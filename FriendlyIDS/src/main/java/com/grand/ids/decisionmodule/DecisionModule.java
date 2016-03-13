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
package com.grand.ids.decisionmodule;

import com.grand.ids.audit.AuditModule;
import com.grand.ids.jdbc.IDSMode;

/**
 * Интерфейс модуля принятия решений.
 *
 * @author Andrey Grigorov
 */
public interface DecisionModule {

    /**
     * Анализ события безопасности, полученного от сенсорной подсистемы.
     *
     * @param event событие безопасности
     * @return решение, принятое в результате анализа
     */
    AnalysisResult analyze(final Event event);

    /**
     * Установить режим работы модуля принятия решений.
     *
     * @param mode режим работы
     */
    void setMode(IDSMode mode);

    /**
     * Установить модуль аудита, который должен использовать данный модуль
     * принятия решений.
     *
     * @param auditModule модуль аудита
     */
    void setAuditModule(AuditModule auditModule);

    /**
     * Получить модуль аудита, который используется данным модулем
     * принятия решений.
     *
     * @return модуль аудита, который используется данным модулем
     * принятия решений
     */
    AuditModule getAuditModule();

    /**
     * Установить слушателя, которому будет передаваться сообщение в формате
     * IDMEF.
     *
     * @param listener слушатель, которому будет передаваться сообщение в
     *                 формате IDMEF.
     */
    void addIDMEFListener(IDMEFListener listener);

    /**
     * Удалить слушателя, которому передавались сообщения в формате IDMEF.
     *
     * @param listener слушателя, которому передавались сообщения в формате
     *                 IDMEF.
     */
    void removeIDMEFListener(IDMEFListener listener);
}
