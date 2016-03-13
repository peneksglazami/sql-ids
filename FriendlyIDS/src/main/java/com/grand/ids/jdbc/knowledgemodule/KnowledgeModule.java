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
package com.grand.ids.jdbc.knowledgemodule;

import com.grand.ids.UserId;
import com.grand.ids.model.Graph;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

/**
 * Интерфейс модуля хранения знаний.
 *
 * @author Andrey Grigorov
 */
public interface KnowledgeModule {

    /**
     * Результат выполнения обновления.
     */
    interface UpdateResult {

        /**
         * Был ли рассмотренный результат выполнения запроса использован
         * для формирования базы знаний.
         *
         * @return true - результат выполнения запроса использовался для
         * формирования базы знананий; false - результат не использовался.
         */
        boolean isUseful();

        /**
         * Получить дополнительную информации о выполненном обучении.
         *
         * @return дополнительная информация о выполненном обучении.
         */
        Map<String, String> getProperties();
    }

    /**
     * Обновить базу знаний.
     *
     * @param resultSet результат выборки, на котором необходимо произвести
     *                  обучение
     * @param userId    идентификатор пользователя
     * @param sqlQuery  выполненный SQL-запрос
     * @return результат выполнения обновления базы знаний
     * @throws Exception
     */
    UpdateResult updateKnowledge(final ResultSet resultSet, final UserId userId, final String sqlQuery) throws Exception;

    /**
     * Получить графы, описывающие взаимоотношения записей в представленном
     * результате выполнения запроса на выборку данных.
     *
     * @param resultSet результат выборки, для которого необходимо построить
     *                  графы
     * @param userId    идентификатор пользователя
     * @param sqlQuery  текст выполненного SQL-запроса
     * @return графы, описывающие взаимоотношения между записями, попавшими в
     * результат выполнения запроса
     * @throws Exception
     */
    List<Graph> getRelationGraphs(final ResultSet resultSet, final UserId userId, final String sqlQuery) throws Exception;
}
