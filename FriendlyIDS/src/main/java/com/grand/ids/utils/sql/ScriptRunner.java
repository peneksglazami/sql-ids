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
package com.grand.ids.utils.sql;

import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

/**
 * Исполнитель SQL-скриптов.
 *
 * @author Andrey Grigorov
 */
public class ScriptRunner {

    private final static String DEFAULT_DELIMITER = ";";
    private Connection connection;
    private boolean stopOnError;
    private String delimiter = DEFAULT_DELIMITER;
    private PrintWriter logWriter = new PrintWriter(System.out);
    private PrintWriter errorLogWriter = new PrintWriter(System.err);

    /**
     * Конструктор исполнителя SQL-скриптов.
     *
     * @param connection  соединение с БД, которое будет использоваться
     *                    при выполнении скрипта
     * @param stopOnError флаг необходимости прервать выполнение скрипта
     *                    в случае возникновения ошибки; если значение равно <code>true</code>, то
     *                    выполнение прервётся, если <code>false</code> - прервано не будет
     */
    public ScriptRunner(Connection connection, boolean stopOnError) {
        this.connection = connection;
        this.stopOnError = stopOnError;
    }

    /**
     * Установить регулярное выражение, определяющее разделитель между
     * отдельными командами в скрипте.
     *
     * @param delimiter регулярное выражение, определяющее разделитель
     */
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public void setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }

    public void setErrorLogWriter(PrintWriter errorLogWriter) {
        this.errorLogWriter = errorLogWriter;
    }

    public void setLogWriter(PrintWriter logWriter) {
        this.logWriter = logWriter;
    }

    /**
     * Выполнить SQL-скрипт.
     *
     * @param inputStream поток, содержащий скрипт
     * @throws SQLException
     */
    public void runScript(InputStream inputStream) throws SQLException {
        Scanner scanner = new Scanner(inputStream);
        StringBuilder script = new StringBuilder();
        while (scanner.hasNext()) {
            script.append(scanner.nextLine());
        }
        runScript(script.toString());
    }

    /**
     * Выполнить SQL-скрипт.
     *
     * @param script скрипт, в котором отдельные команды разделены специальным
     *               разделителем, регулярное выражение для поиска которого задано в поле
     *               delimiter
     * @throws SQLException
     */
    public void runScript(String script) throws SQLException {
        try {
            Statement statement = connection.createStatement();
            String[] commands = script.split(delimiter);
            for (String command : commands) {
                if (command.trim().length() > 0) {
                    try {
                        if (logWriter != null) {
                            // logWriter.println(command);
                        }
                        statement.execute(command);
                    } catch (SQLException ex) {
                        ex.fillInStackTrace();
                        if (errorLogWriter != null) {
                            errorLogWriter.println("Ошибка при выполнении:\n" + command.trim());
                            errorLogWriter.println(ex);
                        }
                        if (stopOnError) {
                            break;
                        }
                    }
                }
            }
            statement.close();
        } catch (SQLException ex) {
            ex.fillInStackTrace();
            throw ex;
        } finally {
            flush();
        }
    }

    private void flush() {
        if (logWriter != null) {
            logWriter.flush();
        }
        if (errorLogWriter != null) {
            errorLogWriter.flush();
        }
    }
}