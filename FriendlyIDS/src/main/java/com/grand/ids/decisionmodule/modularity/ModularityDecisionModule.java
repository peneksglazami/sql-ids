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
package com.grand.ids.decisionmodule.modularity;

import com.grand.ids.audit.AuditModule;
import com.grand.ids.decisionmodule.AbstractDecisionModule;
import com.grand.ids.decisionmodule.Verdict;
import com.grand.ids.jdbc.IDSMode;
import com.grand.ids.jdbc.knowledgemodule.KnowledgeModule;
import com.grand.ids.model.Graph;
import com.grand.ids.model.Graph.Link;

/**
 * Модуль принятия решений на основе алгоритма кластеризации графа и вычисления
 * значения модульности полученного разбиения на кластеры.
 * Если вычисленное значение модульности графа будет превышать заданное
 * максимально допустимое значение, то запрос, который породил данный граф,
 * будет считаться аномальным. Если полученное значение модульности не превышает
 * заданное максимально допустимое значение, то запрос, породивший
 * рассматриваемый граф, будет считаться нормальным.
 * Модуль производит оценку взвешенного графа, однако перед вычислением
 * модульности производится преобразование этого графа в невзвешенный граф по
 * следующему правилу: если вес ребра между двумя вершинами i и j не меньше
 * заданного порогового значения, то в создаваемый невзвешенный граф между
 * вершинами i и j добавляется ребро, в противном случае - между вершинами
 * i и j ребра нет.
 *
 * @author Andrey Grigorov
 */
public class ModularityDecisionModule extends AbstractDecisionModule {

    /**
     * Допустимый уровень модульности
     */
    private double acceptedModularity;
    private double edgeWeightThreshold;

    public ModularityDecisionModule(KnowledgeModule knowledgeModule, AuditModule auditModule, IDSMode mode, double acceptedModularity, double edgeWeightThreshold) {
        super(knowledgeModule, auditModule, mode);
        this.acceptedModularity = acceptedModularity;
        this.edgeWeightThreshold = edgeWeightThreshold;
    }

    /**
     * Преобразование взвешенного графа в незвешенный
     *
     * @param graph взвешенный граф
     * @return невзвешенный граф
     */
    private Graph simplifyGraph(final Graph graph) {
        Graph res = new Graph(graph.getNodeNumber());
        for (int i = 0; i < graph.getNodeNumber(); i++) {
            for (Link link : graph.getNeighbors(i)) {
                if (link.weight >= edgeWeightThreshold) {
                    res.addEdge(i, link.node, 1);
                }
            }
        }
        return res;
    }

    public Verdict analyzeGraph(Graph graph) {
        ModularityCalculator modularityCalculator = new ModularityCalculator();
        Graph unweightedGraph = simplifyGraph(graph);
        double modularity = modularityCalculator.getModularity(unweightedGraph);
        Verdict verdict;
        if (modularity <= acceptedModularity) {
            // модульность графа не больше допустимого значения, значит запрос, породивший этот граф, нормальный
            verdict = new Verdict(Verdict.VerdictType.NORMAL).putProperty("modularity", String.valueOf(modularity));
        } else {
            // модульность графа больше допустимого значения, значит запрос, породивший этот граф, аномальный
            verdict = new Verdict(Verdict.VerdictType.ANOMALITY).putProperty("modularity", String.valueOf(modularity));
        }
        return verdict;
    }
}
