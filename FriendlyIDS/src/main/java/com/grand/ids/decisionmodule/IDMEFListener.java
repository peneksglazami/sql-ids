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

import edu.jhuapl.idmef.IDMEF_Message;

/**
 * Слушатель, которому модуль оценки результата выполнения запроса будет
 * передавать сообщение о произошедшем событии в формате IDMEF.
 *
 * @author Andrey Grigorov
 */
public interface IDMEFListener {

    void onEvent(IDMEF_Message message);
}
