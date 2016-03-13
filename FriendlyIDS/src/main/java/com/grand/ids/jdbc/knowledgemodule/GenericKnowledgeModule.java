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
import com.grand.ids.model.Column;
import com.grand.ids.model.Graph;
import com.grand.ids.model.Schema;
import com.grand.ids.model.Table;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Абстрактный модуль знаний, использующий в качестве базового класса для других
 * модулей знаний. Реализует общие механизмы обработки запросов к модулям знаний.
 *
 * @author Andrey Grigorov
 */
public abstract class GenericKnowledgeModule implements KnowledgeModule {

    private final static Logger LOG = Logger.getLogger(GenericKnowledgeModule.class);

    /**
     * Описание защищаемых объектов базы данных
     */
    protected Schema schema;
    protected LinkPredictionMode linkPredictionMode;
    private Map<String, Set<String>> tableNamesSets = new HashMap<String, Set<String>>();
    private Map<String, Set<String>> columnNamesSets = new HashMap<String, Set<String>>();

    /**
     * Определение таблиц, в которой хранятся объекты, выбираемые запросом.
     *
     * @param resultSet результирующее множество.
     * @return список таблиц, объекты из которых были выбраны запросом.
     */
    protected List<Table> recognizeTables(final ResultSet resultSet, final String sqlQuery) {
        List<Table> res = new ArrayList<Table>();
        try {
            ResultSetMetaData resultSetMetadata = resultSet.getMetaData();
            int colCount = resultSetMetadata.getColumnCount();
            String[] columnTableNames = new String[colCount];
            String[] columnNames = new String[colCount];
            List<String> tableNames = new ArrayList<String>();
            for (int i = 1; i <= colCount; i++) {
                String tableName = resultSetMetadata.getTableName(i);
                // проверим, существует ли в БД таблица с таким именем;
                // если нет, то значит текущее значение tableName - алиас;
                if (!isTableExists(tableName, resultSet.getStatement().getConnection())) {
                    // попробуем распознать таблицу по алиасу
                    Matcher matcher = Pattern.compile("(\\w+)(\\s)+" + tableName + "(,|\\s)").matcher(sqlQuery);
                    if (matcher.find()) {
                        String tempTableName = matcher.group(1);
                        if (isTableExists(tempTableName, resultSet.getStatement().getConnection())) {
                            tableName = tempTableName;
                        }
                    }
                }
                columnTableNames[i - 1] = tableName;
                if (!tableNames.contains(tableName)) {
                    tableNames.add(tableName);
                }
                String columnName = resultSetMetadata.getColumnName(i);
                // проверим, настоящее ли имя поля храниться в columnName или алиас
                if (!isColumnExists(columnName, tableName, resultSet.getStatement().getConnection())) {
                    // это алиас, попробуем распознать имя поля по алиасу
                    Matcher matcher = Pattern.compile("(\\w+)(\\s)+AS(\\s)+" + columnName + "(,|\\s)").matcher(sqlQuery);
                    if (matcher.find()) {
                        String tempColumnName = matcher.group(1);
                        if (isColumnExists(tempColumnName, tableName, resultSet.getStatement().getConnection())) {
                            columnName = tempColumnName;
                        }
                    }
                }
                columnNames[i - 1] = columnName;
            }
            for (String tableName : tableNames) {
                for (Table table : schema.getTables()) {
                    if (table.getName().equalsIgnoreCase(tableName)) {
                        boolean ok = true;
                        for (String primaryKeyName : table.getPrimaryKeys()) {
                            boolean found = false;
                            for (int i = 1; i <= colCount; i++) {
                                if (columnTableNames[i - 1].equalsIgnoreCase(tableName)
                                        && columnNames[i - 1].equalsIgnoreCase(primaryKeyName)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                ok = false;
                                break;
                            }
                        }
                        if (ok) {
                            res.add(table);
                        }
                    }
                }
            }
        } catch (Exception ex) {
        }
        return res;
    }

    /**
     * Проверка существования в БД таблицы с указанным именем.
     *
     * @param tableName  имя таблицы
     * @param connection соединение с базой данных
     * @return true - таблица существует; false - таблица не существует
     */
    private boolean isTableExists(String tableName, Connection connection) {
        try {
            String setId = connection.getMetaData().getURL() + connection.getMetaData().getUserName();
            if (tableNamesSets.get(setId) == null) {
                Set<String> tableNames = new TreeSet<String>();
                ResultSet resultSet = connection.getMetaData().getTables(null, null, "", new String[]{"TABLE", "VIEW"});
                while (resultSet.next()) {
                    tableNames.add(resultSet.getString("TABLE_NAME").toUpperCase());
                }
                resultSet.close();
                tableNamesSets.put(setId, tableNames);
            }
            return tableNamesSets.get(setId).contains(tableName.toUpperCase());
        } catch (Exception ex) {
        }
        return false;
    }

    /**
     * Проверка существования в таблице поля с указанным именем.
     *
     * @param columnName имя поля
     * @param tableName  имя таблицы
     * @param connection соединение с базой данных
     * @return true - поле существует; false - поле не существует
     */
    private boolean isColumnExists(String columnName, String tableName, Connection connection) {
        try {
            String setId = connection.getMetaData().getURL() + connection.getMetaData().getUserName() + tableName;
            if (columnNamesSets.get(setId) == null) {
                Set<String> columnNames = new TreeSet<String>();
                ResultSet resultSet = connection.getMetaData().getPrimaryKeys(null, null, tableName);
                while (resultSet.next()) {
                    columnNames.add(resultSet.getString("COLUMN_NAME").toUpperCase());
                }
                resultSet.close();
                columnNamesSets.put(setId, columnNames);
            }
            return columnNamesSets.get(setId).contains(columnName.toUpperCase());
        } catch (Exception ex) {
        }
        return false;
    }

    /**
     * Получение списка идентификаторов объектов, попавших в результирующее множество.
     *
     * @param resultSet результирующее множество
     * @param table     описание таблицы
     * @return список идентификаторов объектов
     */
    protected List<List<Object>> getObjectIds(final ResultSet resultSet, final Table table) {
        List<List<Object>> ids = new ArrayList<List<Object>>();
        try {
            while (resultSet.next()) {
                List<Object> key = new ArrayList<Object>();
                for (String columnName : table.getPrimaryKeys()) {
                    key.add(resultSet.getObject(columnName));
                }
                ids.add(key);
            }
            resultSet.beforeFirst();
        } catch (Exception ex) {
        }
        return ids;
    }

    /**
     * Определяем, достаточно ли данных в выборке, чтобы выполнить операцию
     * обновления служебных таблиц.
     *
     * @param resultSet множество выбранных данных
     * @param table     описание защищаемой таблицы
     * @return true - данных достаточно, false - данных недостаточно
     */
    protected boolean resultSetIsComplete(final ResultSet resultSet, final Table table) throws Exception {
        for (Column column : table.getColumns()) {
            boolean found = false;
            for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                if (resultSet.getMetaData().getColumnName(i).equalsIgnoreCase(column.getName())
                        && resultSet.getMetaData().getTableName(i).equalsIgnoreCase(table.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /**
     * Получение полных данных для рассматриваемой таблицы.
     *
     * @param table      описание защищаемой таблицы
     * @param ids        список идентификаторов объектов, которые необходимо извлечь;
     *                   значения элементов ключа должны соответствовать порядку, определённому
     *                   в описании защищаемой таблицы
     * @param connection соединение с базой данных, для которой производится
     *                   мониторинг запросов
     */
    protected ResultSet getCompleteResultSet(Table table, List<List<Object>> ids, Connection connection) throws Exception {
        // FIXME: сейчас НЕ работает с составными первичными ключами
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
        query.append(" where ");
        query.append(table.getPrimaryKeys().get(0));
        query.append(" in (");
        for (int i = 0; i < ids.size(); i++) {
            if (i != 0) {
                query.append(",");
            }
            query.append("?");
        }
        query.append(")");
        LOG.debug("Выполнение запроса за полными данными: " + query.toString());
        PreparedStatement preparedStatement = connection.prepareStatement(query.toString());
        for (int i = 0; i < ids.size(); i++) {
            preparedStatement.setObject(i + 1, ids.get(i).get(0));
        }
        return preparedStatement.executeQuery();
    }

    @Override
    public List<Graph> getRelationGraphs(final ResultSet resultSet, final UserId userId, final String sqlQuery) throws Exception {
        // определить в каких таблица БД хранятся объекты, выбранные в результате выполнения запроса
        List<Table> tables = recognizeTables(resultSet, sqlQuery);
        if (tables.isEmpty()) {
            // невозможно однозначно определить таблицу
            return null;
        }
        List<Graph> graphs = new ArrayList<Graph>();
        for (Table table : tables) {
            // определили какие объекты выбрали в данной выборке
            List<List<Object>> ids = getObjectIds(resultSet, table);
            if (ids.isEmpty()) {
                // результат выборки - пустое множество; аномалий не обнаружено
                graphs.add(new Graph(0));
                return graphs;
            }

            final ResultSet completeResultSet;
            if (!resultSetIsComplete(resultSet, table)) {
                // данных в анализируемой выборке недостаточно, придётся делать ещё один запрос
                Connection connection = resultSet.getStatement().getConnection();
                if (connection != null) {
                    completeResultSet = getCompleteResultSet(table, ids, connection);
                } else {
                    // невозможно восстановить соединение с базой данных, из которой выбирались данные
                    return null;
                }
            } else {
                completeResultSet = resultSet;
            }
            graphs.add(getGraph(table, completeResultSet, userId));
        }
        return graphs;
    }

    /**
     * Получить граф, описывающий взаимосвязи между записями, представленными в
     * результате выборки.
     *
     * @param table     описание таблицы
     * @param resultSet результат выборки
     * @param userId    идентификатор текущего пользователя
     * @return граф, описывающий взаимосвязи между записями, представленными
     * в результате выборки.
     */
    protected abstract Graph getGraph(Table table, ResultSet resultSet, UserId userId) throws Exception;
}
