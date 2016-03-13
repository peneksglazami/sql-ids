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
package com.grand.ids.decisionmodule.density;

import com.grand.ids.audit.AuditModule;
import com.grand.ids.decisionmodule.AbstractDecisionModule;
import com.grand.ids.decisionmodule.Verdict;
import com.grand.ids.jdbc.IDSMode;
import com.grand.ids.jdbc.knowledgemodule.KnowledgeModule;
import com.grand.ids.model.Graph;

/**
 * Модуль принятия решений, оценивающий граф с точки зрения плотности рёбер.
 *
 * @author Andrey Grigorov
 */
public class DensityDecisionModule extends AbstractDecisionModule {

    private double acceptedDensity;

    public DensityDecisionModule(KnowledgeModule knowledgeModule, AuditModule auditModule, IDSMode mode, double acceptedDensity) {
        super(knowledgeModule, auditModule, mode);
        this.acceptedDensity = acceptedDensity;
    }

    public Verdict analyzeGraph(Graph graph) {
        double density;
        int nodeNumber = graph.getNodeNumber();
        if (nodeNumber == 0) {
            density = 1.0;
        } else {
            density = graph.getTotalWeight() / ((nodeNumber * (nodeNumber - 1) / 2.0 + nodeNumber));
        }
        if (density >= acceptedDensity) {
            return new Verdict(Verdict.VerdictType.NORMAL)
                    .putProperty("density", String.valueOf(density))
                    .putProperty("nodeNumber", String.valueOf(nodeNumber))
                    .putProperty("totalWeight", String.valueOf(graph.getTotalWeight()));
        } else {
            return new Verdict(Verdict.VerdictType.ANOMALITY)
                    .putProperty("density", Double.valueOf(density).toString())
                    .putProperty("nodeNumber", String.valueOf(nodeNumber))
                    .putProperty("totalWeight", String.valueOf(graph.getTotalWeight()));
        }
    }
}
