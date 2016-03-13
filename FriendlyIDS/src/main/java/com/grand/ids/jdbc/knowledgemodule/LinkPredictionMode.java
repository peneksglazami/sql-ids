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

/**
 * Способ предсказания существования рёбер.
 *
 * @author Andrey Grigorov
 */
public enum LinkPredictionMode {

    /**
     * Предсказание не выполняется.
     */
    NONE,

    /**
     * Предсказание на основе метода Adamic-Adar.
     * Adamic L.A., Adar E. Friends and neighbors on the web, Social Networks, ISSN 0378-8733, 25(3): 211-230.
     */
    ADAMIC_ADAR,

    /**
     * Предсказание на основе
     * <a href="http://en.wikipedia.org/wiki/Jaccard_index">коэффициента Джаккара</a>.
     */
    JACCARD
}
