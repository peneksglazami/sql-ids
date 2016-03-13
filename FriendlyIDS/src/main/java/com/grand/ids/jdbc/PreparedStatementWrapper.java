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
package com.grand.ids.jdbc;

import com.grand.ids.UserId;
import com.grand.ids.decisionmodule.AnalysisResult;
import com.grand.ids.decisionmodule.DecisionModule;
import com.grand.ids.decisionmodule.Event;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Обёртка для класса PreparedStatement.
 *
 * @author Andrey Grigorov
 */
public class PreparedStatementWrapper implements PreparedStatement {

    private final static Logger LOG = Logger.getLogger(PreparedStatementWrapper.class);
    private PreparedStatement preparedStatement;
    private DecisionModule decisionModule;
    private String sqlQuery;
    private UserId userId;
    private Map<Integer, String> parametersValues = new HashMap<Integer, String>();

    public PreparedStatementWrapper(PreparedStatement preparedStatement,
                                    DecisionModule decisionModule, String sqlQuery,
                                    UserId userId) {
        this.preparedStatement = preparedStatement;
        this.decisionModule = decisionModule;
        this.sqlQuery = sqlQuery;
        this.userId = userId;
    }

    private ResultSet postProcessResultSet(final ResultSet resultSet) throws SQLException {
        Event event = new Event(getRealSqlQuery(), userId, resultSet);
        AnalysisResult analysisResult = decisionModule.analyze(event);
        if (analysisResult.needBlockAction()) {
            throw new SQLException("Anomaly query");
        } else {
            return resultSet;
        }
    }

    private String getRealSqlQuery() {
        String realSqlQuery = sqlQuery;
        try {
            int parameterCount = preparedStatement.getParameterMetaData().getParameterCount();
            for (int i = 1; i <= parameterCount; i++) {
                if (parametersValues.get(i) != null) {
                    realSqlQuery = realSqlQuery.replaceFirst("\\?", parametersValues.get(i));
                } else {
                    System.out.println("Не удалось установить значение для параметра " + i + " в запросе\n" + sqlQuery);
                    LOG.warn("Не удалось установить значение для параметра " + i + " в запросе " + sqlQuery);
                }
            }
        } catch (Exception ex) {
            LOG.error("Не удалось сформировать текст \"реального\" SQL-запроса.", ex);
        }
        return realSqlQuery;
    }

    public ResultSet executeQuery() throws SQLException {
        ResultSet resultSet = preparedStatement.executeQuery();
        return postProcessResultSet(resultSet);
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        sqlQuery = sql;
        ResultSet resultSet = preparedStatement.executeQuery(sql);
        return postProcessResultSet(resultSet);
    }

    public int executeUpdate() throws SQLException {
        return preparedStatement.executeUpdate();
    }

    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        preparedStatement.setNull(parameterIndex, sqlType);
        parametersValues.put(parameterIndex, "null");
    }

    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        preparedStatement.setBoolean(parameterIndex, x);
        parametersValues.put(parameterIndex, x ? "true" : "false");
    }

    public void setByte(int parameterIndex, byte x) throws SQLException {
        preparedStatement.setByte(parameterIndex, x);
        parametersValues.put(parameterIndex, String.valueOf(x));
    }

    public void setShort(int parameterIndex, short x) throws SQLException {
        preparedStatement.setShort(parameterIndex, x);
        parametersValues.put(parameterIndex, String.valueOf(x));
    }

    public void setInt(int parameterIndex, int x) throws SQLException {
        preparedStatement.setInt(parameterIndex, x);
        parametersValues.put(parameterIndex, String.valueOf(x));
    }

    public void setLong(int parameterIndex, long x) throws SQLException {
        preparedStatement.setLong(parameterIndex, x);
        parametersValues.put(parameterIndex, String.valueOf(x));
    }

    public void setFloat(int parameterIndex, float x) throws SQLException {
        preparedStatement.setFloat(parameterIndex, x);
        parametersValues.put(parameterIndex, String.valueOf(x));
    }

    public void setDouble(int parameterIndex, double x) throws SQLException {
        preparedStatement.setDouble(parameterIndex, x);
        parametersValues.put(parameterIndex, String.valueOf(x));
    }

    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        preparedStatement.setBigDecimal(parameterIndex, x);
        parametersValues.put(parameterIndex, x.toString());
    }

    public void setString(int parameterIndex, String x) throws SQLException {
        preparedStatement.setString(parameterIndex, x);
        parametersValues.put(parameterIndex, "\"" + x + "\"");
    }

    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        preparedStatement.setBytes(parameterIndex, x);
        StringBuilder value = new StringBuilder("(");
        for (int i = 0; i < x.length; i++) {
            if (i > 0) {
                value.append(",");
            }
            value.append(x[i]);
        }
        parametersValues.put(parameterIndex, value.toString());
    }

    public void setDate(int parameterIndex, Date x) throws SQLException {
        preparedStatement.setDate(parameterIndex, x);
    }

    public void setTime(int parameterIndex, Time x) throws SQLException {
        preparedStatement.setTime(parameterIndex, x);
    }

    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        preparedStatement.setTimestamp(parameterIndex, x);
    }

    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        preparedStatement.setAsciiStream(parameterIndex, x, length);
    }

    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        preparedStatement.setUnicodeStream(parameterIndex, x, length);
    }

    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        preparedStatement.setBinaryStream(parameterIndex, x, length);
    }

    public void clearParameters() throws SQLException {
        preparedStatement.clearParameters();
        parametersValues.clear();
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        if (x == null) {
            parametersValues.put(parameterIndex, "null");
        } else if (x instanceof String) {
            parametersValues.put(parameterIndex, "\"" + x + "\"");
        } else {
            parametersValues.put(parameterIndex, String.valueOf(x));
        }
        preparedStatement.setObject(parameterIndex, x, targetSqlType);
    }

    public void setObject(int parameterIndex, Object x) throws SQLException {
        if (x == null) {
            parametersValues.put(parameterIndex, "null");
        } else if (x instanceof String) {
            parametersValues.put(parameterIndex, "\"" + x + "\"");
        } else {
            parametersValues.put(parameterIndex, String.valueOf(x));
        }
        preparedStatement.setObject(parameterIndex, x);
    }

    public boolean execute() throws SQLException {
        return preparedStatement.execute();
    }

    public void addBatch() throws SQLException {
        preparedStatement.addBatch();
    }

    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        preparedStatement.setCharacterStream(parameterIndex, reader, length);
    }

    public void setRef(int parameterIndex, Ref x) throws SQLException {
        preparedStatement.setRef(parameterIndex, x);
    }

    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        preparedStatement.setBlob(parameterIndex, x);
    }

    public void setClob(int parameterIndex, Clob x) throws SQLException {
        preparedStatement.setClob(parameterIndex, x);
    }

    public void setArray(int parameterIndex, Array x) throws SQLException {
        preparedStatement.setArray(parameterIndex, x);
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return preparedStatement.getMetaData();
    }

    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        preparedStatement.setDate(parameterIndex, x, cal);
    }

    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        preparedStatement.setTime(parameterIndex, x, cal);
    }

    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        preparedStatement.setTimestamp(parameterIndex, x, cal);
    }

    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        preparedStatement.setNull(parameterIndex, sqlType, typeName);
        parametersValues.put(parameterIndex, "null");
    }

    public void setURL(int parameterIndex, URL x) throws SQLException {
        preparedStatement.setURL(parameterIndex, x);
        parametersValues.put(parameterIndex, "\"" + x.toString() + "\"");
    }

    public ParameterMetaData getParameterMetaData() throws SQLException {
        return preparedStatement.getParameterMetaData();
    }

    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        preparedStatement.setRowId(parameterIndex, x);
    }

    public void setNString(int parameterIndex, String value) throws SQLException {
        preparedStatement.setNString(parameterIndex, value);
        parametersValues.put(parameterIndex, "\"" + value + "\"");
    }

    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        preparedStatement.setNCharacterStream(parameterIndex, value, length);
    }

    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        preparedStatement.setNClob(parameterIndex, value);
    }

    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        preparedStatement.setClob(parameterIndex, reader, length);
    }

    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        preparedStatement.setBlob(parameterIndex, inputStream, length);
    }

    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        preparedStatement.setNClob(parameterIndex, reader, length);
    }

    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        preparedStatement.setSQLXML(parameterIndex, xmlObject);
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        if (x == null) {
            parametersValues.put(parameterIndex, "null");
        } else if (x instanceof String) {
            parametersValues.put(parameterIndex, "\"" + x + "\"");
        } else {
            parametersValues.put(parameterIndex, String.valueOf(x));
        }
        preparedStatement.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
    }

    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        preparedStatement.setAsciiStream(parameterIndex, x, length);
    }

    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        preparedStatement.setBinaryStream(parameterIndex, x, length);
    }

    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        preparedStatement.setCharacterStream(parameterIndex, reader, length);
    }

    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        preparedStatement.setAsciiStream(parameterIndex, x);
    }

    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        preparedStatement.setBinaryStream(parameterIndex, x);
    }

    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        preparedStatement.setCharacterStream(parameterIndex, reader);
    }

    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        preparedStatement.setNCharacterStream(parameterIndex, value);
    }

    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        preparedStatement.setClob(parameterIndex, reader);
    }

    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        preparedStatement.setBlob(parameterIndex, inputStream);
    }

    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        preparedStatement.setNClob(parameterIndex, reader);
    }

    public int executeUpdate(String sql) throws SQLException {
        return preparedStatement.executeUpdate(sql);
    }

    public void close() throws SQLException {
        preparedStatement.close();
    }

    public int getMaxFieldSize() throws SQLException {
        return preparedStatement.getMaxFieldSize();
    }

    public void setMaxFieldSize(int max) throws SQLException {
        preparedStatement.setMaxFieldSize(max);
    }

    public int getMaxRows() throws SQLException {
        return preparedStatement.getMaxRows();
    }

    public void setMaxRows(int max) throws SQLException {
        preparedStatement.setMaxRows(max);
    }

    public void setEscapeProcessing(boolean enable) throws SQLException {
        preparedStatement.setEscapeProcessing(enable);
    }

    public int getQueryTimeout() throws SQLException {
        return preparedStatement.getQueryTimeout();
    }

    public void setQueryTimeout(int seconds) throws SQLException {
        preparedStatement.setQueryTimeout(seconds);
    }

    public void cancel() throws SQLException {
        preparedStatement.cancel();
    }

    public SQLWarning getWarnings() throws SQLException {
        return preparedStatement.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        preparedStatement.clearWarnings();
    }

    public void setCursorName(String name) throws SQLException {
        preparedStatement.setCursorName(name);
    }

    public boolean execute(String sql) throws SQLException {
        return preparedStatement.execute(sql);
    }

    public ResultSet getResultSet() throws SQLException {
        return preparedStatement.getResultSet();
    }

    public int getUpdateCount() throws SQLException {
        return preparedStatement.getUpdateCount();
    }

    public boolean getMoreResults() throws SQLException {
        return preparedStatement.getMoreResults();
    }

    public void setFetchDirection(int direction) throws SQLException {
        preparedStatement.setFetchDirection(direction);
    }

    public int getFetchDirection() throws SQLException {
        return preparedStatement.getFetchDirection();
    }

    public void setFetchSize(int rows) throws SQLException {
        preparedStatement.setFetchSize(rows);
    }

    public int getFetchSize() throws SQLException {
        return preparedStatement.getFetchSize();
    }

    public int getResultSetConcurrency() throws SQLException {
        return preparedStatement.getResultSetConcurrency();
    }

    public int getResultSetType() throws SQLException {
        return preparedStatement.getResultSetType();
    }

    public void addBatch(String sql) throws SQLException {
        preparedStatement.addBatch(sql);
    }

    public void clearBatch() throws SQLException {
        preparedStatement.clearBatch();
    }

    public int[] executeBatch() throws SQLException {
        return preparedStatement.executeBatch();
    }

    public Connection getConnection() throws SQLException {
        return preparedStatement.getConnection();
    }

    public boolean getMoreResults(int current) throws SQLException {
        return preparedStatement.getMoreResults(current);
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        return preparedStatement.getGeneratedKeys();
    }

    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return preparedStatement.executeUpdate(sql, autoGeneratedKeys);
    }

    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return preparedStatement.executeUpdate(sql, columnIndexes);
    }

    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return preparedStatement.executeUpdate(sql, columnNames);
    }

    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return preparedStatement.execute(sql, autoGeneratedKeys);
    }

    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return preparedStatement.execute(sql, columnIndexes);
    }

    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return preparedStatement.execute(sql, columnNames);
    }

    public int getResultSetHoldability() throws SQLException {
        return preparedStatement.getResultSetHoldability();
    }

    public boolean isClosed() throws SQLException {
        return preparedStatement.isClosed();
    }

    public void setPoolable(boolean poolable) throws SQLException {
        preparedStatement.setPoolable(poolable);
    }

    public boolean isPoolable() throws SQLException {
        return preparedStatement.isPoolable();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return preparedStatement.unwrap(iface);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return preparedStatement.isWrapperFor(iface);
    }

    public void closeOnCompletion() throws SQLException {
        preparedStatement.closeOnCompletion();
    }

    public boolean isCloseOnCompletion() throws SQLException {
        return preparedStatement.isCloseOnCompletion();
    }
}
