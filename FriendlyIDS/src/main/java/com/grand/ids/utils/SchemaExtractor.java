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
package com.grand.ids.utils;

import com.grand.ids.jdbc.SchemaExplorer;
import com.grand.ids.model.Schema;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Утилита для получения описания защищаемых объектов.
 *
 * @author Andrey Grigorov
 */
public class SchemaExtractor {

    /**
     * Запустить процесс экспорта описания защищаемых объектов.
     *
     * @param args список параметров<br>
     * args[0] имя файла, куда надо произвести экспорт<br>
     * args[1] строка соединения с БД<br>
     * args[2] имя пользователя<br>
     * args[3] пароль<br>
     * args[4] имя класса jdbc-драйвера<br>
     */
    public static void main(String[] args) throws Exception {
        String filename = args[0];
        String jdbcConnectionString = args[1];
        String username = args[2];
        String password = args[3];
        String driverClassName = args[4];

        Class.forName(driverClassName);
        Connection connection = DriverManager.getConnection(jdbcConnectionString, username, password);
        SchemaExplorer schemaExplorer = new SchemaExplorer(connection);
        Schema schema = schemaExplorer.extractSchema();
        XmlUtils.marshal(schema, new FileOutputStream(filename), Schema.class);
        connection.close();
    }
}
