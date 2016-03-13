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

import com.grand.ids.UserId;
import com.grand.ids.audit.AbstractAuditModule;
import com.grand.ids.audit.EventType;
import com.grand.ids.audit.db.domain.Event;
import com.grand.ids.jdbc.IDSMode;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.Date;
import java.util.Map;

/**
 * Модуль аудита. Хранение данных производится в БД, доступ к которой
 * осуществляется на базе ORM Hibernate.
 *
 * @author Andrey Grigorov
 */
public class HibernateAuditModule extends AbstractAuditModule {

    private final static Logger LOG = Logger.getLogger(HibernateAuditModule.class);
    private SessionFactory sessionFactory;

    public HibernateAuditModule(String connectionUrl, String username, String password,
                                String driverClassName, String dialect) {
        sessionFactory = HibernateUtils.createSessionFactory(connectionUrl, username, password, driverClassName, dialect);
    }

    @Override
    public void logEvent(UserId userId, String sqlQuery, Date date, EventType eventType, IDSMode mode, Map<String, String> properties) {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Event event = new Event();
            event.setUserId(userId.getUserId());
            event.setSqlQuery(sqlQuery);
            event.setEventDate(date);
            event.setEventType(eventType);
            event.setMode(mode);
            event.setProperties(properties);
            session.save(event);
            tx.commit();
        } catch (Exception ex) {
            LOG.error("При сохранении информации о событии в журнал аудита произошла ошибка", ex);
            if (tx != null) {
                tx.rollback();
            }
        } finally {
            session.close();
        }
    }
}
