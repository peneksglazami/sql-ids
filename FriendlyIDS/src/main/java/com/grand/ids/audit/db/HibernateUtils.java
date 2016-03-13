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
package com.grand.ids.audit.db;

import com.grand.ids.audit.db.domain.Event;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Утилитарный класс для создания фабрики сессий для подключения в
 * подсистеме аудита.
 *
 * @author Andrey Grigorov
 */
public class HibernateUtils {

    public static SessionFactory createSessionFactory(String connectionUrl, String username, String password,
                                                      String driverClassName, String dialect) throws HibernateException {
        SessionFactory factory = new Configuration()
                .setProperty("hibernate.dialect", dialect)
                .setProperty("hibernate.connection.driver_class", driverClassName)
                .setProperty("hibernate.connection.url", connectionUrl)
                .setProperty("hibernate.connection.username", username)
                .setProperty("hibernate.connection.password", password)
                .setProperty("hibernate.hbm2ddl.auto", "update")
                        //.setProperty("hibernate.show_sql", "true")
                .addPackage("com.grand.ids.audit.db.domain")
                .addAnnotatedClass(Event.class)
                .buildSessionFactory();

        return factory;
    }
}
