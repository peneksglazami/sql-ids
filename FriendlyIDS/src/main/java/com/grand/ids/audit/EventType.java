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

/**
 * Типы событий, которые обрабатывает система аудита.
 *
 * @author Andrey Grigorov
 */
public enum EventType {

    /**
     * Выполненные запрос признан аномальным
     */
    ANOMALY,
    /**
     * Выполненный запрос признан нормальным
     */
    NORMAL,
    /**
     * В результате оценки запроса не было принято решение относительно
     * того, нормальный он или аномальный.
     */
    NO_DECISION,
    /**
     * Во время оценки запроса произошла ошибка.
     */
    ERROR,
    /**
     * Запрос выполнялся в режиме обучения и учитывался при формировании базы
     * знаний.
     */
    TRAINING_QUERY,
    /**
     * Оценка запроса не производилась
     */
    NO_OBSERVATION
}
