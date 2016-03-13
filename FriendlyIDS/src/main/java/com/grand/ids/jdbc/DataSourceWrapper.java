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

import com.grand.ids.decisionmodule.DecisionModule;
import com.grand.ids.model.Schema;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * Обёртка над источником данных.
 *
 * @author Andrey Grigorov
 */
public class DataSourceWrapper implements DataSource, IDSDataSource {

    private DataSource dataSource;
    private DecisionModule decisionModule;
    private ContextProvider contextProvider;

    public DataSourceWrapper(DataSource dataSource, DecisionModule decisionModule,
                             ContextProvider contextProvider) {
        if (dataSource == null) {
            throw new NullPointerException("Data source object should be not null.");
        }
        if (decisionModule == null) {
            throw new NullPointerException("Decision module object should be not null.");
        }
        if (contextProvider == null) {
            throw new NullPointerException("Context provider object should be not null.");
        }
        this.dataSource = dataSource;
        this.decisionModule = decisionModule;
        this.contextProvider = contextProvider;
    }

    public Connection getConnection() throws SQLException {
        return new ConnectionWrapper(dataSource.getConnection(), decisionModule, contextProvider);
    }

    public Connection getConnection(String username, String password) throws SQLException {
        return new ConnectionWrapper(dataSource.getConnection(username, password), decisionModule, contextProvider);
    }

    public PrintWriter getLogWriter() throws SQLException {
        return dataSource.getLogWriter();
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        dataSource.setLogWriter(out);
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        dataSource.setLoginTimeout(seconds);
    }

    public int getLoginTimeout() throws SQLException {
        return dataSource.getLoginTimeout();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return dataSource.unwrap(iface);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return dataSource.isWrapperFor(iface);
    }

    public void setMode(IDSMode mode) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public IDSMode getMode() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setSchema(Schema schema) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setContextProvider(ContextProvider contextProvider) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
