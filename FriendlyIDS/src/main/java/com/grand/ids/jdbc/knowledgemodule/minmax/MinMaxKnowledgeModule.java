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
package com.grand.ids.jdbc.knowledgemodule.minmax;

import com.grand.ids.UserId;
import com.grand.ids.jdbc.knowledgemodule.GenericKnowledgeModule;
import com.grand.ids.jdbc.knowledgemodule.LinkPredictionMode;
import com.grand.ids.model.Column;
import com.grand.ids.model.Graph;
import com.grand.ids.model.Schema;
import com.grand.ids.model.Table;
import com.grand.ids.utils.XmlUtils;
import com.grand.ids.utils.sql.ScriptRunner;
import com.grand.ids.utils.xslt.XslTransformator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

/**
 * Класс, оценивающий результирующее множество, строя граф по правилу "MinMax".
 *
 * @author Andrey Grigorov
 */
public class MinMaxKnowledgeModule extends GenericKnowledgeModule {

    private final static String MIN_MAX_SCHEMA_XSL = "/" + MinMaxKnowledgeModule.class.getPackage().getName().replace(".", "/")
            + "/xslt/MySQLMinMaxSchema.xsl";
    private final static Logger LOG = Logger.getLogger(MinMaxKnowledgeModule.class);
    private Connection connection;
    private List<Map<String, Object>> allRecords = null;
    private List<Map<String, Object>> allRecordsTopBounds = null;
    private List<Map<String, Object>> allRecordsBottomBounds = null;

    /**
     * Хранилище данных, в котором производится хранение базы знаний.
     */
    private DataSource knowledgeDataSource;

    private static class RecordInfo {

        private long startTime;
        private long hits;

        public RecordInfo(long startTime, long hits) {
            this.startTime = startTime;
            this.hits = hits;
        }

        public long getHits() {
            return hits;
        }

        public long getStartTime() {
            return startTime;
        }
    }

    public MinMaxKnowledgeModule(DataSource knowledgeDataSource, Schema schema, boolean alwaysCreateNewTables,
                                 LinkPredictionMode linkPredictionMode) throws Exception {
        this.knowledgeDataSource = knowledgeDataSource;
        this.connection = this.knowledgeDataSource.getConnection();
        this.linkPredictionMode = linkPredictionMode;
        this.setSchema(schema, alwaysCreateNewTables);
    }

    /**
     * Указание защищаемых объектов базы данных.
     *
     * @param schema                описание защищаемых объектов базы данных
     * @param alwaysCreateNewTables флаг, показывающий выполнять или нет
     *                              создание в хранилище данных knowledgeDataSource специальных служебных
     *                              объектов в не зависимости от того, существуют эти объекты уже или нет.
     */
    private void setSchema(Schema schema, boolean alwaysCreateNewTables) throws Exception {
        this.schema = schema;
        String script = createSqlScript(schema, alwaysCreateNewTables);
        ScriptRunner scriptRunner = new ScriptRunner(knowledgeDataSource.getConnection(), false);
        scriptRunner.runScript(script);
    }

    /**
     * Получить скрипт создания служебных объектов (таблиц), использующихся
     * для хранения знаний.
     *
     * @param schema                описание защищаемых объектов базы данных
     * @param alwaysCreateNewTables флаг, показывающий создавать ли по новому
     *                              уже существующие таблицы или нет.
     * @return строка, представляющая собой SQL-скрипт из команд, создающих
     * служебные объекты (таблицы).
     * @throws Exception
     */
    private String createSqlScript(Schema schema, boolean alwaysCreateNewTables) throws Exception {
        StringBuilder script = new StringBuilder();
        if (alwaysCreateNewTables) {
            script.append(getDropTablesScript(schema));
        }
        InputStream xsl = getTransformationXslUrl().openStream();
        ByteArrayOutputStream schemaXml = new ByteArrayOutputStream();
        XmlUtils.marshal(schema, schemaXml, Schema.class);
        script.append(XslTransformator.transform(new ByteArrayInputStream(schemaXml.toByteArray()), xsl).toString());
        return script.toString();
    }

    /**
     * Получить указатель на ресурс, предоставляющий XSL-схему для трансформации.
     *
     * @return URL, указывающий на XSL-схему.
     * @throws Exception
     */
    protected URL getTransformationXslUrl() throws Exception {
        return this.getClass().getResource(MIN_MAX_SCHEMA_XSL);
    }

    /**
     * Создать скрипт удаления ранее созданных таблиц в базе знаний.
     *
     * @param schema описание защищаемых объектов.
     * @return скрипт удаления служебных объектов (таблиц).
     */
    protected String getDropTablesScript(Schema schema) {
        StringBuilder script = new StringBuilder();
        for (Table table : schema.getTables()) {
            script.append("DROP TABLE IF EXISTS ");
            script.append(table.getName());
            script.append("_min;\n");
            script.append("DROP TABLE IF EXISTS ");
            script.append(table.getName());
            script.append("_max;\n");
            script.append("DROP TABLE IF EXISTS ");
            script.append(table.getName());
            script.append("_cnt;\n");
        }
        script.append("DROP TABLE IF EXISTS GENERAL_TABLE;");
        return script.toString();
    }

    @Override
    public UpdateResult updateKnowledge(final ResultSet resultSet, final UserId userId, final String sqlQuery) throws Exception {
        allRecords = null;
        allRecordsTopBounds = null;
        allRecordsBottomBounds = null;
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
            updateGeneralTable(table.getName(), userId);
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
            final ResultSet completeResultSet;
            if (!resultSetIsComplete(resultSet, table)) {
                // данных в анализируемой выборке недостаточно, придётся делать ещё один запрос
                completeResultSet = getCompleteResultSet(table, ids, queryConnection);
            } else {
                completeResultSet = resultSet;
            }
            // находим верхнюю и нижнюю границы значений
            Map<String, Object> bottomBound = getBottomBoundOfResultSet(completeResultSet, table);
            Map<String, Object> topBound = getTopBoundOfResultSet(completeResultSet, table);
            // обновляем границы
            updateBottomBound(ids, table, userId, bottomBound);
            updateTopBound(ids, table, userId, topBound);
            // обновляем таблицу-счётчик
            updateCountTable(ids, table, userId);
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

    /**
     * Получить граф, описывающий взаимосвязи между записями, представленными в
     * результате выборки
     *
     * @param table     описание таблицы
     * @param resultSet результат выборки
     * @param userId    идентификатор текущего прользователя
     * @return значение модульности
     */
    @Override
    protected Graph getGraph(final Table table, final ResultSet resultSet, final UserId userId) throws Exception {
        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> bottomBounds = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> topBounds = new ArrayList<Map<String, Object>>();

        // FIXME: сделать быстрое получение границ записи
        while (resultSet.next()) {
            Map<String, Object> record = new HashMap<String, Object>();
            for (Column column : table.getColumns()) {
                record.put(column.getName(), resultSet.getObject(column.getName()));
            }
            List<Object> id = new ArrayList<Object>();
            for (String key : table.getPrimaryKeys()) {
                id.add(resultSet.getObject(key));
            }
            records.add(record);
            Map<String, Object> bottomBound = getBottomBound(table, id, userId);
//            if (bottomBound == null) {
//                // В служебных таблицах нет записи о граничных значениях, так
//                // как запись попала в результат выборки в первый раз. В
//                // качестве граничных значений возьмём значения атрибутов данной
//                // записи.
//                // FIXME: Возможно неправильно таким образом определять
//                // граничные значения для записи, которая попадает в
//                // результирующее множество впервые. Наверное, возможны и
//                // другие варианты...
//                bottomBound = new HashMap<String, Object>();
//                for (Column column : table.getNonkeyColumns()) {
//                    bottomBound.put(column.getName(), record.get(column.getName()));
//                }
//            }
            bottomBounds.add(bottomBound);
            Map<String, Object> topBound = getTopBound(table, id, userId);
//            if (topBound == null) {
//                topBound = new HashMap<String, Object>();
//                for (Column column : table.getNonkeyColumns()) {
//                    topBound.put(column.getName(), record.get(column.getName()));
//                }
//            }
            topBounds.add(topBound);
        }
        resultSet.beforeFirst();

        Graph graph = createGraph(table, userId, records, bottomBounds, topBounds, resultSet.getStatement().getConnection());
        return graph;
    }

    /**
     * Получение нижней границы "дружественных" значений атрибутов для указанной
     * записи
     *
     * @param table  имя таблицы
     * @param id     значение идентификатора объекта
     * @param userId идентификатор текущего пользователя
     * @return нижняя граница "дружественных" значений атрибутов для указанной
     * записи
     * @throws Exception
     */
    private Map<String, Object> getBottomBound(Table table, List<Object> id, UserId userId) throws Exception {
        return getBound(table, id, userId, "_min");
    }

    /**
     * Получение верхней границы "дружественных" значений атрибутов для
     * указанной записи
     *
     * @param table  имя таблицы
     * @param id     значение идентификатора объекта
     * @param userId идентификатор текущего пользователя
     * @return верхняя граница "дружественных" значений атрибутов для указанной
     * записи
     * @throws Exception
     */
    private Map<String, Object> getTopBound(Table table, List<Object> id, UserId userId) throws Exception {
        return getBound(table, id, userId, "_max");
    }

    private Map<String, Object> getBound(Table table, List<Object> id, UserId userId, String postfix) throws Exception {
        StringBuilder query = new StringBuilder();
        query.append("select * from ");
        query.append(table.getName());
        query.append(postfix);
        query.append(" where ");
        for (int i = 0; i < table.getPrimaryKeys().size(); i++) {
            if (i != 0) {
                query.append(" and ");
            }
            query.append(table.getPrimaryKeys().get(i));
            query.append(" = ?");
        }
        query.append(" and IDS_USER_ID = ?");

        PreparedStatement preparedStatement = connection.prepareStatement(query.toString());
        for (int i = 0; i < table.getPrimaryKeys().size(); i++) {
            preparedStatement.setObject(i + 1, id.get(i));
        }
        preparedStatement.setObject(table.getPrimaryKeys().size() + 1, userId.getUserId());
        ResultSet resultSet = preparedStatement.executeQuery();

        Map<String, Object> bound = null;
        if (resultSet.next()) {
            // В служебных таблицах найдены данные про граничные значения
            // атрибутов данной записи.
            bound = new HashMap<String, Object>();
            for (Column column : table.getColumns()) {
                bound.put(column.getName(), resultSet.getObject(column.getName()));
            }
        } else {
            // В служебных таблицах нет данных о граничных значениях для данной
            // записи. Это означает, что данная запись никогда ранее не попадала
            // в результат выполнения запроса.
        }
        resultSet.close();
        preparedStatement.close();
        return bound;
    }

    private boolean between(Object value, Object bottom, Object top) {
        if (value == null) {
            return true;
        }
        if (bottom == null && top == null) {
            return false;
        }
        if (bottom != null) {
            if (((Comparable) value).compareTo(bottom) < 0) {
                return false;
            }
        }
        if (top != null) {
            if (((Comparable) value).compareTo(top) > 0) {
                return false;
            }
        }
        return true;
    }

    private boolean areFriends(Table table, Map<String, Object> record1, Map<String, Object> record2,
                               Map<String, Object> rec1BottomBound, Map<String, Object> rec1TopBound,
                               Map<String, Object> rec2BottomBound, Map<String, Object> rec2TopBound) {
        if (rec1BottomBound == null || rec1TopBound == null
                || rec2BottomBound == null || rec2TopBound == null) {
            return false;
        }
        List<Column> nonkeyColumns = table.getNonkeyColumns();
        for (Column column : nonkeyColumns) {
            if (!(between(record2.get(column.getName()), rec1BottomBound.get(column.getName()), rec1TopBound.get(column.getName()))
                    && between(record1.get(column.getName()), rec2BottomBound.get(column.getName()), rec2TopBound.get(column.getName())))) {
                return false;
            }
        }
        return true;
    }

    private Graph createGraph(Table table, UserId userId,
                              List<Map<String, Object>> records,
                              List<Map<String, Object>> bottomBounds,
                              List<Map<String, Object>> topBounds,
                              Connection connection) throws Exception {
        if ((linkPredictionMode != LinkPredictionMode.NONE)
                && ((allRecords == null)
                || (allRecordsBottomBounds == null)
                || (allRecordsTopBounds == null))) {
            // получаем все записи
            allRecords = getAllRecords(table, userId, connection);
            // получаем min-max границы для всех записей
            List<List<Object>> ids = new ArrayList<List<Object>>();
            for (Map<String, Object> record : allRecords) {
                List<Object> id = new ArrayList<Object>();
                for (String key : table.getPrimaryKeys()) {
                    id.add(record.get(key));
                }
                ids.add(id);
            }
            allRecordsTopBounds = getTopBounds(table, ids, userId);
            allRecordsBottomBounds = getTopBounds(table, ids, userId);
//            for (int i = 0; i < allRecords.size(); i++) {
//                if (allRecordsTopBounds.get(i) == null) {
//                    Map<String, Object> recordTopBound = new HashMap<String, Object>();
//                    for (Column column : table.getNonkeyColumns()) {
//                        recordTopBound.put(column.getName(), allRecords.get(i).get(column.getName()));
//                    }
//                    allRecordsTopBounds.set(i, recordTopBound);
//                }
//                if (allRecordsBottomBounds.get(i) == null) {
//                    Map<String, Object> recordBottomBound = new HashMap<String, Object>();
//                    for (Column column : table.getNonkeyColumns()) {
//                        recordBottomBound.put(column.getName(), allRecords.get(i).get(column.getName()));
//                    }
//                    allRecordsBottomBounds.set(i, recordBottomBound);
//                }
//            }
        }

        Graph graph = new Graph(records.size());
        for (int i = 0; i < records.size(); i++) {
            for (int j = i; j < records.size(); j++) { // если i = j, то для вершины строится ребро в саму себя
                if (areFriends(table, records.get(i), records.get(j),
                        bottomBounds.get(i), topBounds.get(i),
                        bottomBounds.get(j), topBounds.get(j))) {
                    // Записи точно являются "друзьями", поэтому вес ребра между ними равен 1.
                    graph.addEdge(i, j, 1);
                } else {
                    // Ещё точно не установлено, являются записи друзьями или нет.
                    // Установим в качестве веса ребра между записями i и j "предсказанный" вес
                    graph.addEdge(i, j, getLinkPredictionScore(records.get(i), records.get(j),
                            bottomBounds.get(i), topBounds.get(i),
                            bottomBounds.get(j), topBounds.get(j),
                            allRecords, allRecordsTopBounds, allRecordsBottomBounds,
                            table));
                }
            }
        }
        return graph;
    }

    private double getLinkPredictionScore(Map<String, Object> recordA, Map<String, Object> recordB,
                                          Map<String, Object> recordABottomBound, Map<String, Object> recordATopBound,
                                          Map<String, Object> recordBBottomBound, Map<String, Object> recordBTopBound,
                                          List<Map<String, Object>> allRecords,
                                          List<Map<String, Object>> allRecordsTopBounds,
                                          List<Map<String, Object>> allRecordsBottomBounds,
                                          Table table) throws Exception {
        if (linkPredictionMode == LinkPredictionMode.ADAMIC_ADAR) {
            List<Integer> intersection = new ArrayList<Integer>();
            for (int i = 0; i < allRecords.size(); i++) {
                boolean a = areFriends(table, allRecords.get(i), recordA, allRecordsBottomBounds.get(i), allRecordsTopBounds.get(i), recordABottomBound, recordATopBound);
                boolean b = areFriends(table, allRecords.get(i), recordB, allRecordsBottomBounds.get(i), allRecordsTopBounds.get(i), recordBBottomBound, recordBTopBound);
                if (a && b) {
                    intersection.add(i);
                }
            }
            if (intersection.size() > 0) {
                double sum = 0.0;
                for (Integer index : intersection) {
                    Map<String, Object> record = allRecords.get(index);
                    int friendsAmount = 0;
                    for (int i = 0; i < allRecords.size(); i++) {
                        if (areFriends(table, allRecords.get(index), allRecords.get(i),
                                allRecordsBottomBounds.get(index), allRecordsTopBounds.get(index),
                                allRecordsBottomBounds.get(i), allRecordsTopBounds.get(i))) {
                            friendsAmount++;
                        }
                    }
                    sum += Math.log(2) / Math.log(friendsAmount);
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
            // определяем друзей первой и второй используя areFriends
            int unionSize = 0;
            int intersectionSize = 0;
            for (int i = 0; i < allRecords.size(); i++) {
                boolean a = areFriends(table, allRecords.get(i), recordA, allRecordsBottomBounds.get(i), allRecordsTopBounds.get(i), recordABottomBound, recordATopBound);
                boolean b = areFriends(table, allRecords.get(i), recordB, allRecordsBottomBounds.get(i), allRecordsTopBounds.get(i), recordBBottomBound, recordBTopBound);
                if (a || b) {
                    unionSize++;
                }
                if (a && b) {
                    intersectionSize++;
                }
            }
            System.out.println(unionSize + " " + intersectionSize + " : " + ((unionSize > 0) ? (double) intersectionSize / unionSize : 0.0));
            return (unionSize > 0) ? (double) intersectionSize / unionSize : 0.0;
        }
        return 0.0;
    }

    private List<Map<String, Object>> getAllRecords(Table table, UserId userId, Connection connection) throws Exception {
        StringBuilder query = new StringBuilder();
        query.append("select ");
        for (int i = 0; i < table.getColumns().size(); i++) {
            if (i != 0) {
                query.append(", ");
            }
            query.append(table.getColumns().get(i).getName());
        }
        query.append(" from ");
        query.append(table.getName());

        LOG.debug("Выполнение запроса всеми записями: " + query.toString());
        PreparedStatement preparedStatement = connection.prepareStatement(query.toString());
        ResultSet resultSet = preparedStatement.executeQuery();

        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
        while (resultSet.next()) {
            Map<String, Object> record = new HashMap<String, Object>();
            for (Column column : table.getColumns()) {
                record.put(column.getName(), resultSet.getObject(column.getName()));
            }
            records.add(record);
        }

        return records;
    }

    private List<Map<String, Object>> getTopBounds(Table table, List<List<Object>> ids, UserId userId) throws Exception {
        List<Map<String, Object>> topBounds = new ArrayList<Map<String, Object>>();
        for (List<Object> id : ids) {
            topBounds.add(getTopBound(table, id, userId));
        }
        return topBounds;
    }

    private List<Map<String, Object>> getBottomBounds(Table table, List<List<Object>> ids, UserId userId) throws Exception {
        List<Map<String, Object>> bottomBounds = new ArrayList<Map<String, Object>>();
        for (List<Object> id : ids) {
            bottomBounds.add(getTopBound(table, id, userId));
        }
        return bottomBounds;
    }

    private long getTableCurrentTime(String tableName) throws Exception {
        long tableTime = 0;
        PreparedStatement preparedStatement = connection.prepareStatement("select * from general_table where table_name = ?");
        preparedStatement.setString(1, tableName);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()) {
            tableTime = resultSet.getLong("TABLE_TIME");
        }
        resultSet.close();
        preparedStatement.close();
        return tableTime;
    }

    /**
     * Получить информацию о текущем состоянии записей, попавших в результат
     * выборки
     *
     * @param table   описание таблицы, к которой выполнялся запрос
     * @param userId  идентификатор текущего пользователя
     * @param records выбранные записи
     * @return список информации о текущем состоянии записей (порядок
     * соответствует порядку, определённому в records)
     */
    private List<RecordInfo> getRecordInfos(Table table, UserId userId, List<Map<String, Object>> records) throws Exception {
        if (table.getPrimaryKeys().size() == 1) {
            // у таблицы всего один первичный ключ
            return getRecordInfosFromTableWithSinglePrimaryKey(table, userId, records);
        } else {
            // у таблицы составной первичный ключ
            throw new UnsupportedOperationException("Пока не работает");
        }
    }

    private List<RecordInfo> getRecordInfosFromTableWithSinglePrimaryKey(Table table, UserId userId, List<Map<String, Object>> records) throws Exception {
        String keyColumnName = table.getPrimaryKeys().get(0);
        StringBuilder query = new StringBuilder();
        query.append("select * from ").append(table.getName()).append("_cnt where ").append(keyColumnName).append(" in (");
        for (int i = 0; i < records.size(); i++) {
            if (i != 0) {
                query.append(", ");
            }
            query.append("?");
        }
        query.append(") and ids_user_id = ?");

        PreparedStatement preparedStatement = connection.prepareStatement(query.toString());
        for (int i = 0; i < records.size(); i++) {
            preparedStatement.setObject(i + 1, records.get(i).get(keyColumnName));
        }
        preparedStatement.setObject(records.size() + 1, userId.getUserId());
        ResultSet resultSet = preparedStatement.executeQuery();
        Map<Object, RecordInfo> resMap = new HashMap<Object, RecordInfo>();
        while (resultSet.next()) {
            Object keyValue = resultSet.getObject(keyColumnName);
            long hits = resultSet.getLong("hits");
            long startTime = resultSet.getLong("start_time");
            resMap.put(keyValue, new RecordInfo(startTime, hits));
        }
        resultSet.close();
        preparedStatement.close();
        List<RecordInfo> res = new ArrayList<RecordInfo>();
        for (Map<String, Object> record : records) {
            RecordInfo recordInfo = resMap.get(record.get(keyColumnName));
            if (recordInfo == null) {
                // FIXME: возможно, это должно быть сделано при инициализации базы знаний
                recordInfo = new RecordInfo(0, 0);
            }
            res.add(recordInfo);
        }
        return res;
    }

    private Map<String, Object> getBoundOfResultSet(final ResultSet resultSet, final Table table, int sign) throws Exception {
        Map<String, Object> res = new HashMap<String, Object>();
        while (resultSet.next()) {
            for (Column column : table.getColumns()) {
                Object curValue = res.get(column.getName());
                Object value = resultSet.getObject(column.getName());
                if (curValue == null) {
                    res.put(column.getName(), value);
                } else if (value != null) {
                    if (sign * ((Comparable) value).compareTo(curValue) < 0) {
                        res.put(column.getName(), value);
                    }
                }
            }
        }
        resultSet.beforeFirst();
        return res;
    }

    /**
     * Определение нижней границы для защищаемых неключевых полей таблицы
     *
     * @param resultSet результат выборки
     * @return нижние границы значений полей
     */
    private Map<String, Object> getBottomBoundOfResultSet(final ResultSet resultSet, final Table table) throws Exception {
        return getBoundOfResultSet(resultSet, table, 1);
    }

    /**
     * Определение верхней границы для защищаемых неключевых полей таблицы
     *
     * @param resultSet результат выборки
     * @return нижние границы значений полей
     */
    private Map<String, Object> getTopBoundOfResultSet(final ResultSet resultSet, final Table table) throws Exception {
        return getBoundOfResultSet(resultSet, table, -1);
    }

    private void updateBound(final List<List<Object>> ids, final Table table, final UserId userId, final Map<String, Object> bound, String postfix, int sign) throws Exception {
        // FIXME: сделать обновление быстрее
        StringBuilder query = new StringBuilder();
        query.append("select * from ");
        query.append(table.getName());
        query.append(postfix);
        query.append(" where ids_user_id = ?");
        for (int i = 0; i < table.getPrimaryKeys().size(); i++) {
            query.append(" and ");
            query.append(table.getPrimaryKeys().get(i));
            query.append(" = ?");
        }

        StringBuilder updateQuery = new StringBuilder();
        updateQuery.append("update ");
        updateQuery.append(table.getName());
        updateQuery.append(postfix);
        updateQuery.append(" set ");
        List<Column> nonkeyColumns = table.getNonkeyColumns();
        for (int i = 0; i < nonkeyColumns.size(); i++) {
            if (i != 0) {
                updateQuery.append(", ");
            }
            updateQuery.append(nonkeyColumns.get(i).getName());
            updateQuery.append(" = ?");
        }
        updateQuery.append(" where ids_user_id = ?");
        for (int i = 0; i < table.getPrimaryKeys().size(); i++) {
            updateQuery.append(" and ");
            updateQuery.append(table.getPrimaryKeys().get(i));
            updateQuery.append(" = ?");
        }

        StringBuilder insertQuery = new StringBuilder();
        insertQuery.append("insert ");
        insertQuery.append(table.getName());
        insertQuery.append(postfix);
        insertQuery.append(" (ids_user_id");
        for (int i = 0; i < table.getPrimaryKeys().size(); i++) {
            insertQuery.append(", ");
            insertQuery.append(table.getPrimaryKeys().get(i));
        }
        for (Column column : nonkeyColumns) {
            insertQuery.append(", ");
            insertQuery.append(column.getName());
        }
        insertQuery.append(") values (?");
        for (int i = 0, size = table.getColumns().size(); i < size; i++) {
            insertQuery.append(", ?");
        }
        insertQuery.append(")");

        PreparedStatement preparedStatement = connection.prepareStatement(query.toString());
        PreparedStatement updatePreparedStatement = connection.prepareStatement(updateQuery.toString());
        PreparedStatement insertPreparedStatement = connection.prepareStatement(insertQuery.toString());
        for (List<Object> id : ids) {
            preparedStatement.setObject(1, userId.getUserId());
            for (int i = 0; i < table.getPrimaryKeys().size(); i++) {
                preparedStatement.setObject(i + 2, id.get(i));
            }
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                // в таблице _min/_max уже существует запись об этом объекте, нужно её обновить
                Map<String, Object> newBound = new HashMap<String, Object>();
                for (Column column : nonkeyColumns) {
                    Object curValue = resultSet.getObject(column.getName());
                    Object value = bound.get(column.getName());
                    if (curValue == null) {
                        newBound.put(column.getName(), value);
                    } else {
                        if (value == null) {
                            newBound.put(column.getName(), curValue);
                        } else {
                            if (sign * ((Comparable) value).compareTo(curValue) < 0) {
                                newBound.put(column.getName(), value);
                            } else {
                                newBound.put(column.getName(), curValue);
                            }
                        }
                    }
                }
                // обновляем запись в таблице _min/_max
                for (int i = 0; i < nonkeyColumns.size(); i++) {
                    updatePreparedStatement.setObject(i + 1, newBound.get(nonkeyColumns.get(i).getName()));
                }
                updatePreparedStatement.setObject(nonkeyColumns.size() + 1, userId.getUserId());
                for (int i = 0; i < table.getPrimaryKeys().size(); i++) {
                    updatePreparedStatement.setObject(i + nonkeyColumns.size() + 2, id.get(i));
                }
                updatePreparedStatement.execute();
            } else {
                // в таблице _min/_max ещё нет записи об этом объекте
                insertPreparedStatement.setObject(1, userId.getUserId());
                for (int i = 0; i < table.getPrimaryKeys().size(); i++) {
                    insertPreparedStatement.setObject(i + 2, id.get(i));
                }
                for (int i = 0; i < nonkeyColumns.size(); i++) {
                    insertPreparedStatement.setObject(i + id.size() + 2, bound.get(nonkeyColumns.get(i).getName()));
                }
                insertPreparedStatement.execute();
            }
            resultSet.close();
        }
        preparedStatement.close();
        updatePreparedStatement.close();
        insertPreparedStatement.close();
    }

    /**
     * Обновление данных о нижних границах для множества объектов, попавших в
     * результат выполнения запроса
     *
     * @param ids    список идентификаторов объектов
     * @param table  описание таблицы
     * @param userId идентификатор текущего пользователя
     * @param bound  нижняя граница для множества записей попавших в результат
     *               выполнения запроса
     * @throws Exception
     */
    private void updateBottomBound(final List<List<Object>> ids, final Table table, final UserId userId, final Map<String, Object> bound) throws Exception {
        updateBound(ids, table, userId, bound, "_min", 1);
    }

    /**
     * Обновление данных о верхних границах для множества объектов, попавших в
     * результат выполнения запроса
     *
     * @param ids    список идентификаторов объектов
     * @param table  описание таблицы
     * @param userId идентификатор текущего пользователя
     * @param bound  верхняя граница для множества записей попавших в результат
     *               выполнения запроса
     * @throws Exception
     */
    private void updateTopBound(final List<List<Object>> ids, final Table table, final UserId userId, final Map<String, Object> bound) throws Exception {
        updateBound(ids, table, userId, bound, "_max", -1);
    }

    /**
     * Обновление данных в таблице-счётчике. Для каждой записи, значения первичного
     * ключа которой представлены в ids, значение счётчика увеличивается на единицу.
     *
     * @param ids   список идентификаторов объектов
     * @param table описание таблицы
     * @throws Exception
     */
    private void updateCountTable(final List<List<Object>> ids, final Table table, final UserId userId) throws Exception {
        if (table.getPrimaryKeys().size() == 1) {
            updateCountTableWithSinglePrimaryKey(ids, table, userId);
        } else {
            updateCountTableWithCompositePrimaryKey(ids, table, userId);
        }
    }

    private void updateCountTableWithSinglePrimaryKey(final List<List<Object>> ids, final Table table, final UserId userId) throws Exception {
        String keyColumnName = table.getPrimaryKeys().get(0);
        StringBuilder updateQuery = new StringBuilder();
        updateQuery.append("update ");
        updateQuery.append(table.getName());
        updateQuery.append("_cnt set hits = hits + 1 where ");
        updateQuery.append(keyColumnName);
        updateQuery.append(" in (");
        for (int i = 0; i < ids.size(); i++) {
            if (i != 0) {
                updateQuery.append(", ");
            }
            updateQuery.append("?");
        }
        updateQuery.append(") and ids_user_id = ?");
        PreparedStatement updatePreparedStatement = connection.prepareStatement(updateQuery.toString());
        for (int i = 0; i < ids.size(); i++) {
            updatePreparedStatement.setObject(i + 1, ids.get(i).get(0));
        }
        updatePreparedStatement.setObject(ids.size() + 1, userId.getUserId());

        int updatedRowCount = updatePreparedStatement.executeUpdate();
        if (updatedRowCount < ids.size()) {
            // для каких-то записей нет соответствующих записей в таблице _cnt;
            // добавим для каждой такой записи соответствующую запись в таблицу _cnt.
            // FIXME: Проблема в том, что, когда обновляется сразу несколько записей,
            // нельзя точно сказать какой записи ещё нет в таблице. Вариант с
            // обновлением каждой записи в отдельности весьма затратен.
            StringBuilder selectQuery = new StringBuilder();
            selectQuery.append("select ");
            selectQuery.append(keyColumnName);
            selectQuery.append(" from ");
            selectQuery.append(table.getName());
            selectQuery.append("_cnt where ");
            selectQuery.append(keyColumnName);
            selectQuery.append(" in (");
            for (int i = 0; i < ids.size(); i++) {
                if (i != 0) {
                    selectQuery.append(", ");
                }
                selectQuery.append("?");
            }
            selectQuery.append(") and ids_user_id = ?");
            PreparedStatement selectPreparedStatement = connection.prepareStatement(selectQuery.toString());
            for (int i = 0; i < ids.size(); i++) {
                selectPreparedStatement.setObject(i + 1, ids.get(i).get(0));
            }
            selectPreparedStatement.setObject(ids.size() + 1, userId.getUserId());
            ResultSet resultSet = selectPreparedStatement.executeQuery();
            Set<Object> idSet = new HashSet<Object>();
            while (resultSet.next()) {
                idSet.add(resultSet.getObject(keyColumnName));
            }
            resultSet.close();
            selectPreparedStatement.close();

            StringBuilder insertQuery = new StringBuilder();
            insertQuery.append("insert into ");
            insertQuery.append(table.getName());
            insertQuery.append("_cnt (ids_user_id, ");
            insertQuery.append(keyColumnName);
            insertQuery.append(", hits, start_time) values (?, ?, ?, ?)");

            PreparedStatement insertPreparedStatement = connection.prepareStatement(insertQuery.toString());
            for (List<Object> id : ids) {
                if (!idSet.contains(id.get(0))) {
                    // указываем имя текущего пользователя
                    insertPreparedStatement.setObject(1, userId.getUserId());
                    // таблице _cnt ещё нет соответствующей записи; создадим её
                    insertPreparedStatement.setObject(2, id.get(0));
                    // устанавливаем начальное значение cnt
                    insertPreparedStatement.setObject(3, 1);
                    // устанавливаем начальное значение для start_time
                    insertPreparedStatement.setObject(4, 0);
                    insertPreparedStatement.execute();
                }
            }
            insertPreparedStatement.close();
        }
        updatePreparedStatement.close();
    }

    private void updateCountTableWithCompositePrimaryKey(final List<List<Object>> ids, final Table table, final UserId userId) throws Exception {
        StringBuilder updateQuery = new StringBuilder();
        updateQuery.append("update ");
        updateQuery.append(table.getName());
        updateQuery.append("_cnt set hits = hits + 1 where ");
        for (int i = 0; i < table.getPrimaryKeys().size(); i++) {
            if (i != 0) {
                updateQuery.append(" and");
            }
            updateQuery.append(" ");
            updateQuery.append(table.getPrimaryKeys().get(i));
            updateQuery.append(" = ?");
        }
        updateQuery.append(" and ids_user_id = ?");

        StringBuilder insertQuery = new StringBuilder();
        insertQuery.append("insert into ");
        insertQuery.append(table.getName());
        insertQuery.append("_cnt (ids_user_id, ");
        for (int i = 0; i < table.getPrimaryKeys().size(); i++) {
            if (i != 0) {
                insertQuery.append(", ");
            }
            insertQuery.append(table.getPrimaryKeys().get(i));
        }
        insertQuery.append(", hits, start_time) values (?, ");
        for (int i = 0, size = table.getPrimaryKeys().size(); i < size; i++) {
            if (i != 0) {
                insertQuery.append(", ");
            }
            insertQuery.append("?");
        }
        insertQuery.append(", ?, ?)");

        PreparedStatement updatePreparedStatement = connection.prepareStatement(updateQuery.toString());
        PreparedStatement insertPreparedStatement = connection.prepareStatement(insertQuery.toString());
        for (List<Object> id : ids) {
            for (int i = 0; i < id.size(); i++) {
                updatePreparedStatement.setObject(i + 1, id.get(i));
            }
            updatePreparedStatement.setObject(id.size() + 1, userId.getUserId());
            int updatedRowCount = updatePreparedStatement.executeUpdate();
            if (updatedRowCount == 0) {
                // в таблице _cnt не существует записи, соответствующей данному
                // значению первичного ключа; добавим запись в таблицу
                insertPreparedStatement.setObject(1, userId.getUserId());
                for (int i = 0; i < id.size(); i++) {
                    insertPreparedStatement.setObject(i + 2, id.get(i));
                }
                // устанавливаем начальное значение cnt
                insertPreparedStatement.setObject(id.size() + 3, 1);
                // устанавливаем начальное значение для start_time
                insertPreparedStatement.setObject(id.size() + 4, 0);
                insertPreparedStatement.execute();
            }
        }
        updatePreparedStatement.close();
        insertPreparedStatement.close();
    }

    /**
     * Обновление информации в таблице GENERAL_TABLE. Для таблицы,
     * имя которой указывается в параметре <code>tableName</code>,
     * значение счётчика обращений пользователя, идентификатор которого
     * указывается в параметре <code>userId</code>, увеличивается на 1.
     *
     * @param tableName имя таблицы
     * @param userId    идентификатор пользователя, выполняющего запрос
     * @throws Exception
     */
    private void updateGeneralTable(String tableName, UserId userId) throws Exception {
        PreparedStatement preparedStatement = connection.prepareStatement("select * from general_table where table_name = ? and ids_user_id = ?");
        preparedStatement.setString(1, tableName);
        preparedStatement.setString(2, userId.getUserId());
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()) {
            // запись для данной таблицы с заданным именем уже существует в таблице GENERAL_TABLE
            PreparedStatement updateStatement = connection.prepareStatement("update general_table set table_time = table_time + 1 where table_name = ? and ids_user_id = ?");
            updateStatement.setString(1, tableName);
            updateStatement.setString(2, userId.getUserId());
            updateStatement.executeUpdate();
            updateStatement.close();
        } else {
            // записи для данной таблицы с заданным именем ещё не существует в таблице GENERAL_TABLE
            PreparedStatement insertStatement = connection.prepareStatement("insert into general_table (table_name, ids_user_id, table_time) values (?, ?, 1)");
            insertStatement.setString(1, tableName);
            insertStatement.setString(2, userId.getUserId());
            insertStatement.executeUpdate();
            insertStatement.close();
        }
        resultSet.close();
        preparedStatement.close();
    }
}
