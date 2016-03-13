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
 * Корневой элемент описания объектов базы данных, для которых производится
 * мониторинг выполняемых запросов.
 *
 * @author Andrey Grigorov
 */
@XmlRootElement(name = "Schema")
@XmlAccessorType(XmlAccessType.NONE)
@XmlSeeAlso(value = {Table.class})
public class Schema {

    @XmlElementWrapper(name = "Tables", required = true)
    @XmlElement(name = "Table")
    private List<Table> tables = new ArrayList<Table>();

    public List<Table> getTables() {
        return tables;
    }

    public void setTables(List<Table> tables) {
        this.tables = tables;
    }
}
