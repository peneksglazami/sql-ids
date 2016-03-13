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
package com.grand.ids.model;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Таблица или представление в базе данных.
 *
 * @author Andrey Grigorov
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlSeeAlso({Column.class})
public class Table {

    @XmlAttribute(required = true)
    private String name;
    @XmlElementWrapper(name = "Columns", required = true)
    @XmlElement(name = "Column")
    private List<Column> columns = new ArrayList<Column>();
    @XmlElementWrapper(name = "PrimaryKeys")
    @XmlElement(name = "PrimaryKey")
    private List<String> primaryKeys = new ArrayList<String>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public List<String> getPrimaryKeys() {
        return primaryKeys;
    }

    public void setPrimaryKeys(List<String> primaryKeys) {
        this.primaryKeys = primaryKeys;
    }

    public List<Column> getNonkeyColumns() {
        List<Column> res = new ArrayList<Column>();
        for (Column column : columns) {
            if (!primaryKeys.contains(column.getName()))
                res.add(column);
        }
        return res;
    }
}
