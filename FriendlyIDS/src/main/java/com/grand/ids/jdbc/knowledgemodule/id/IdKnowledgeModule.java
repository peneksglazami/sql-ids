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
package com.grand.ids.jdbc.knowledgemodule.id;

import com.grand.ids.UserId;
import com.grand.ids.jdbc.knowledgemodule.GenericKnowledgeModule;
import com.grand.ids.jdbc.knowledgemodule.LinkPredictionMode;
import com.grand.ids.model.Graph;
import com.grand.ids.model.Schema;
import com.grand.ids.model.Table;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;

/**
 * База знаний, строящая профили нормального поведения на основе информации
 * об идентификаторах записей, попадающих в результат выполнения запроса.
 * @author Andrey Grigorov
 */
public class IdKnowledgeModule extends GenericKnowledgeModule {

    private interface Action {

        void doAction(Session session);
    }
    private SessionFactory sessionFactory;

    public IdKnowledgeModule(String connectionUrl, String username, String password,
            Schema schema, String driverClassName, String dialect, boolean cleanKnowledgeDatabase,
            LinkPredictionMode linkPredictionMode) {
        this.schema = schema;
        this.sessionFactory = createSessionFactory(connectionUrl, username, password,
                driverClassName, dialect, cleanKnowledgeDatabase);
        this.linkPredictionMode = linkPredictionMode;
    }

    private SessionFactory createSessionFactory(String connectionUrl, String username, String password,
            String driverClassName, String dialect, boolean cleanDatabase) throws HibernateException {
        SessionFactory factory = new Configuration()//
                .setProperty("hibernate.dialect", dialect)//
                .setProperty("hibernate.connection.driver_class", driverClassName)//
                .setProperty("hibernate.connection.url", connectionUrl)//
                .setProperty("hibernate.connection.username", username)//
                .setProperty("hibernate.connection.password", password)//
                .setProperty("hibernate.hbm2ddl.auto", cleanDatabase ? "create" : "update")//
                //.setProperty("hibernate.show_sql", "true")//
                .addPackage("com.grand.ids.jdbc.knowledgemodule.id.")//
                .addAnnotatedClass(TableInfo.class)//
                .addAnnotatedClass(RecordInfo.class)//
                .buildSessionFactory();
        return factory;
    }

    private Session getSession() {
        return sessionFactory.openSession();
    }

    protected Graph getGraph(final Table table, ResultSet resultSet, final UserId userId) throws Exception {
        final List<List<Object>> ids = getObjectIds(resultSet, table);
        final Graph graph = new Graph(ids.size());
        doInTransaction(new Action() {

            public void doAction(Session session) {
                Set<String> idSet = new HashSet<String>();
                Map<String, Integer> nodesNum = new HashMap<String, Integer>();
                int num = 0;
                for (List<Object> id : ids) {
                    String stringId = getStringIdPresentation(id);
                    idSet.add(stringId);
                    nodesNum.put(stringId, num++);
                }
                Criteria criteria = session.createCriteria(RecordInfo.class);
                criteria.add(Restrictions.in("recordId", idSet));
                criteria.add(Restrictions.eq("tableName", table.getName()));
                criteria.add(Restrictions.eq("userId", userId.getUserId()));
                criteria.setFetchMode("friendlyRecordIds", FetchMode.JOIN);
                criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
                List<RecordInfo> recordInfos = criteria.list();

                Set<String> edges = new HashSet<String>();
                for (RecordInfo recordInfoA : recordInfos) {
                    Integer nodeId = nodesNum.get(recordInfoA.getRecordId());
                    Set<String> friendIds = recordInfoA.getFriendlyRecordIds();
                    for (RecordInfo recordInfoB : recordInfos) {
                        Integer friendNodeId = nodesNum.get(recordInfoB.getRecordId());
                        String edge = String.format("%d-%d", friendNodeId, nodeId);
                        if (!edges.contains(edge)) {
                            if (friendIds.contains(recordInfoB.getRecordId())) {
                                graph.addEdge(nodeId, friendNodeId, 1.0);
                            } else {
                                graph.addEdge(nodeId, friendNodeId, getLinkPredictionScore(table, userId, session, recordInfoA, recordInfoB));
                            }
                            edges.add(String.format("%d-%d", nodeId, friendNodeId));
                        }
                    }
                }
            }
        });
        return graph;
    }

    private double getLinkPredictionScore(Table table, UserId userId, Session session, RecordInfo recordInfoA, RecordInfo recordInfoB) {
        if (linkPredictionMode == LinkPredictionMode.ADAMIC_ADAR) {
            Set<String> intersection = new HashSet<String>(recordInfoA.getFriendlyRecordIds());
            intersection.retainAll(recordInfoB.getFriendlyRecordIds());
            if (intersection.size() > 0) {
                Criteria criteria = session.createCriteria(RecordInfo.class);
                criteria.add(Restrictions.in("recordId", intersection));
                criteria.add(Restrictions.eq("tableName", table.getName()));
                criteria.add(Restrictions.eq("userId", userId.getUserId()));
                criteria.setFetchMode("friendlyRecordIds", FetchMode.JOIN);
                criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
                List<RecordInfo> recordInfos = criteria.list();
                double sum = 0;
                for (RecordInfo recordInfo : recordInfos) {
                    sum += Math.log(2) / Math.log(recordInfo.getFriendlyRecordIds().size());
                }
                double score = sum / intersection.size();
                if ((score > 1.0) || (score < 0.0)) {
                    throw new RuntimeException("Error!");
                }
                return score;
            } else {
                return 0.0;
            }
        } else if (linkPredictionMode == LinkPredictionMode.JACCARD) {
            Set<String> friendsA = recordInfoA.getFriendlyRecordIds();
            Set<String> friendsB = recordInfoB.getFriendlyRecordIds();
            Set<String> union = new HashSet<String>(friendsA);
            union.addAll(friendsB);
            Set<String> intersection = new HashSet<String>(friendsA);
            intersection.retainAll(friendsB);
            System.out.println(intersection.size() + " " + union.size() + " : " + ((union.size() > 0) ? (double) intersection.size() / union.size() : 0.0));
            return (union.size() > 0) ? ((double) intersection.size()) / union.size() : 0.0;
        }
        return 0.0;
    }

    public UpdateResult updateKnowledge(ResultSet resultSet, UserId userId, String sqlQuery) throws Exception {
        Connection queryConnection = resultSet.getStatement().getConnection();
        if (queryConnection == null) {
            // невозможно определить то соединение с базой данных, по которому
            // производилась выборка данных
            return new UpdateResult() {

                public boolean isUseful() {
                    return false;
                }

                public Map<String, String> getProperties() {
                    Map<String, String> properties = new HashMap<String, String>();
                    properties.put("description", "NO_CONNECTION");
                    return Collections.unmodifiableMap(properties);
                }
            };
        }
        List<Table> tables = recognizeTables(resultSet, sqlQuery);
        if (tables.isEmpty()) {
            return new UpdateResult() {

                public boolean isUseful() {
                    return false;
                }

                public Map<String, String> getProperties() {
                    Map<String, String> properties = new HashMap<String, String>();
                    properties.put("description", "TABLES_ARE_NOT_RECOGNIZED");
                    return Collections.unmodifiableMap(properties);
                }
            };
        }
        for (Table table : tables) {
            // обновляем общую таблицу
            updateHittingTableInfo(table.getName(), userId);
            // определили какие объекты выбрали в данной выборке
            List<List<Object>> ids = getObjectIds(resultSet, table);
            if (ids.isEmpty()) {
                // результат выборки - пустое множество
                return new UpdateResult() {

                    public boolean isUseful() {
                        return true;
                    }

                    public Map<String, String> getProperties() {
                        Map<String, String> properties = new HashMap<String, String>();
                        properties.put("description", "EMPTY_RESULT_SET");
                        return Collections.unmodifiableMap(properties);
                    }
                };
            }
            updateRecordInfos(table.getName(), ids, userId);
        }

        return new UpdateResult() {

            public boolean isUseful() {
                return true;
            }

            public Map<String, String> getProperties() {
                return Collections.EMPTY_MAP;
            }
        };
    }

    private void updateHittingTableInfo(final String tableName, final UserId userId) {
        doInTransaction(new Action() {

            public void doAction(Session session) {
                Criteria criteria = session.createCriteria(TableInfo.class);
                criteria.add(Restrictions.eq("tableName", tableName));
                criteria.add(Restrictions.eq("userId", userId.getUserId()));
                List<TableInfo> tables = criteria.list();
                if (tables.isEmpty()) {
                    TableInfo tableInfo = new TableInfo();
                    tableInfo.setTableName(tableName);
                    tableInfo.setHits(1L);
                    tableInfo.setUserId(userId.getUserId());
                    session.save(tableInfo);
                } else {
                    TableInfo tableInfo = tables.get(0);
                    tableInfo.setHits(tableInfo.getHits() + 1);
                    session.update(tableInfo);
                }
            }
        });
    }

    private void updateRecordInfos(final String tableName, final List<List<Object>> ids, final UserId userId) {
        doInTransaction(new Action() {

            public void doAction(Session session) {
                Set<String> idSet = new HashSet<String>();
                for (List<Object> id : ids) {
                    idSet.add(getStringIdPresentation(id));
                }
                Criteria criteria = session.createCriteria(RecordInfo.class);
                criteria.add(Restrictions.in("recordId", idSet));
                criteria.add(Restrictions.eq("tableName", tableName));
                criteria.add(Restrictions.eq("userId", userId.getUserId()));
                criteria.setFetchMode("friendlyRecordIds", FetchMode.JOIN);
                criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
                List<RecordInfo> recordInfos = criteria.list();
                Set<String> nonExistIds = new HashSet<String>(idSet);
                for (RecordInfo recordInfo : recordInfos) {
                    recordInfo.getFriendlyRecordIds().addAll(idSet);
                    recordInfo.setHits(recordInfo.getHits() + 1);
                    session.update(recordInfo);
                    nonExistIds.remove(recordInfo.getRecordId());
                }
                for (String id : nonExistIds) {
                    RecordInfo recordInfo = new RecordInfo();
                    recordInfo.setRecordId(id);
                    recordInfo.setTableName(tableName);
                    recordInfo.setFriendlyRecordIds(idSet);
                    recordInfo.setHits(1L);
                    recordInfo.setUserId(userId.getUserId());
                    session.save(recordInfo);
                }
            }
        });
    }

    private String getStringIdPresentation(List<Object> id) {
        StringBuilder res = new StringBuilder();
        for (Object part : id) {
            if (res.length() > 0) {
                res.append("$");
            }
            res.append(part.toString());
        }
        return res.toString();
    }

    private void doInTransaction(Action action) {
        Session session = getSession();
        Transaction trn = null;
        try {
            trn = session.beginTransaction();
            action.doAction(session);
            trn.commit();
        } catch (Exception ex) {
            if (trn != null) {
                trn.rollback();
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
