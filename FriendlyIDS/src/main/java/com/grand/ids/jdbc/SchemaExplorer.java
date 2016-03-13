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

import com.grand.ids.model.Column;
import com.grand.ids.model.Schema;
import com.grand.ids.model.Table;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class explores the database and constructs the schema, which used by IDS.
 *
 * @author Andrey Grigorov
 */
public class SchemaExplorer {

    private Connection connection;

    public SchemaExplorer(Connection connection) {
        if (connection == null) {
            throw new RuntimeException("The connection object is null.");
        }
        this.connection = connection;
    }

    /**
     * Extract from the database definition of the elements.
     *
     * @return schema
     */
    public Schema extractSchema() {
        Schema schema = new Schema();
        schema.setTables(extractTables());
        return schema;
    }

    private List<Table> extractTables() {
        List<Table> tables = new ArrayList<Table>();
        try {
            ResultSet resultSet = connection.getMetaData().getTables(null, null, "", new String[]{"TABLE", "VIEW"});
            while (resultSet.next()) {
                Table table = new Table();
                String tableName = resultSet.getString("TABLE_NAME");
                table.setName(tableName);
                table.setColumns(extactColumns(tableName));
                table.setPrimaryKeys(extractPrimaryKeys(tableName));
                tables.add(table);
            }
            resultSet.close();
        } catch (SQLException ex) {
        }

        return tables;
    }

    private List<String> extractPrimaryKeys(String table) {
        List<String> primaryKeys = new ArrayList<String>();
        try {
            ResultSet resultSet = connection.getMetaData().getPrimaryKeys(null, null, table);
            while (resultSet.next()) {
                primaryKeys.add(resultSet.getString("COLUMN_NAME"));
            }
            resultSet.close();
        } catch (SQLException ex) {
        }
        return primaryKeys;
    }

    private List<Column> extactColumns(String table) {
        List<Column> columns = new ArrayList<Column>();
        try {
            ResultSet resultSet = connection.getMetaData().getColumns(null, null, table, "");
            while (resultSet.next()) {
                Column column = new Column();
                column.setName(resultSet.getString("COLUMN_NAME"));
                column.setType(resultSet.getInt("DATA_TYPE"));
                column.setTypeName(resultSet.getString("TYPE_NAME"));
                column.setSize(resultSet.getInt("COLUMN_SIZE"));
                columns.add(column);
            }
            resultSet.close();
        } catch (SQLException ex) {
        }
        return columns;
    }
}
