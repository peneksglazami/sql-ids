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

import com.grand.ids.UserId;

import java.sql.ResultSet;
import java.util.Date;

/**
 * Описание события системы безопасности, которое передаётся системе
 * принятия решения от сенсорной подсистемы.
 *
 * @author Andrey Grigorov
 */
public class Event {

    private String sqlQuery;
    private UserId userId;
    private ResultSet resultSet;
    private Date date;

    public Event(String sqlQuery, UserId userId, ResultSet resultSet) {
        this.sqlQuery = sqlQuery;
        this.userId = userId;
        this.resultSet = resultSet;
        this.date = new Date();
    }

    public ResultSet getResultSet() {
        return resultSet;
    }

    public String getSqlQuery() {
        return sqlQuery;
    }

    public UserId getUserId() {
        return userId;
    }

    public Date getDate() {
        return date;
    }
}
