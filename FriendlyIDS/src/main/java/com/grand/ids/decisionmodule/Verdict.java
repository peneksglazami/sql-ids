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
package com.grand.ids.decisionmodule;

import java.util.HashMap;
import java.util.Map;

/**
 * Результат выполнения оценки результирующего множества.
 *
 * @author Andrey Grigorov
 */
public class Verdict {

    public enum VerdictType {

        /**
         * Результат запроса признан допустимым.
         */
        NORMAL,

        /**
         * Результат выполнения запроса признан аномальным.
         */
        ANOMALITY,

        /**
         * При анализе результата выполнения запроса произошла ошибка,
         * не позволившая завершить начатый анализ.
         */
        ERROR,

        /**
         * В результате анализа решения об аномальности результата
         * выполненного запроса не принято.
         */
        NO_DECISION
    }

    private VerdictType verdict;
    private Map<String, String> properties = new HashMap<String, String>();

    public Verdict(VerdictType verdict) {
        this.verdict = verdict;
    }

    public VerdictType getVerdict() {
        return verdict;
    }

    public Verdict putProperty(String name, String value) {
        properties.put(name, value);
        return this;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
