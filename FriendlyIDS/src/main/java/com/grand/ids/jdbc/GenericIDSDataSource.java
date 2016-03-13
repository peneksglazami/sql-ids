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

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * Класс, представляющий собой источник данных, осуществляющий механизм
 * экранирования JDBC соединения с БД.
 * Переданный в качестве значения параметра конструктора класса экземпляр
 * соединения с БД помещается в "оболочку" - объект-обёртку, который помимо
 * реализации базового интерфейса Connection из JDBC API осуществляет
 * оценку аномальности выполняемых запросов.
 *
 * @author Andrey Grigorov
 */
public class GenericIDSDataSource implements IDSDataSource, DataSource {

    /**
     * Имя класса JDBC-драйвера
     */
    private String driverClassName;
    /**
     * URL соединения с базой данных
     */
    private String url;
    /**
     * Имя пользователя
     */
    private String username;
    /**
     * Пароль пользователя
     */
    private String password;
    /**
     * Поток, в который должны записываться служебные сообщения о работе,
     * состоянии и других характеристиках источника данных. Если значение
     * равно null, то считается что журналирование отключено.
     */
    private PrintWriter logWriter;
    /**
     * Модуль принятия решений
     */
    protected DecisionModule decisionModule;
    /**
     * Провайдер контекста приложения
     */
    protected ContextProvider contextProvider;

    public GenericIDSDataSource() {
    }

    public void setDecisionModule(DecisionModule decisionModule) {
        this.decisionModule = decisionModule;
    }

    public DecisionModule getDecisionModule() {
        return decisionModule;
    }

    @Override
    public void setContextProvider(ContextProvider contextProvider) {
        this.contextProvider = contextProvider;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        if (driverClassName != null) {
            this.driverClassName = driverClassName.trim();
        }
        try {
            Class.forName(this.driverClassName);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Невозможно загрузить класс JDBC-драйвера [" + this.driverClassName + "]", ex);
        }
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Получение соединения с базой данных для установленных имени пользователя
     * и пароля
     *
     * @return соединение с базой данных
     */
    public Connection getConnection() throws SQLException {
        Connection connection = getConnectionFromDriverManager(url, username, password);
        return getWrappedConnection(connection);
    }

    /**
     * Получение соединения с базой данных для указанных пользователя и пароля
     *
     * @param username имя пользователя
     * @param password пароль
     * @return соединение с базой данных
     * @throws SQLException
     */
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = getConnectionFromDriverManager(url, username, password);
        return getWrappedConnection(connection);
    }

    public PrintWriter getLogWriter() throws SQLException {
        return logWriter;
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        this.logWriter = out;
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Получить максимальное количество секунд, которые могут быть потрачены на
     * выполнение установки соединения с базой данных.
     *
     * @return возвращается значение равное 0, что согласно спецификации
     * интерфейса DataSource означает, что используется принятое по умолчанию
     * в системе время ожидания.
     * @throws SQLException
     */
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private Connection getConnectionFromDriverManager(String url, String userName, String password) throws SQLException {
        return DriverManager.getConnection(url, userName, password);
    }

    private Connection getWrappedConnection(Connection connection) {
        return new ConnectionWrapper(connection, decisionModule, contextProvider);
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
