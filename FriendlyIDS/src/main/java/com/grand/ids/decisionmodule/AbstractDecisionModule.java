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

import com.grand.ids.audit.AuditModule;
import com.grand.ids.audit.EventType;
import com.grand.ids.jdbc.IDSMode;
import com.grand.ids.jdbc.knowledgemodule.KnowledgeModule;
import com.grand.ids.jdbc.knowledgemodule.KnowledgeModule.UpdateResult;
import com.grand.ids.model.Graph;
import edu.jhuapl.idmef.Alert;
import edu.jhuapl.idmef.Analyzer;
import edu.jhuapl.idmef.Classification;
import edu.jhuapl.idmef.CreateTime;
import edu.jhuapl.idmef.IDMEF_Message;
import edu.jhuapl.idmef.IDMEF_Node;
import edu.jhuapl.idmef.IDMEF_Process;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.apache.log4j.Logger;

/**
 * Абстрактный модуль принятия решений.
 *
 * @author Andrey Grigorov
 */
public abstract class AbstractDecisionModule implements DecisionModule {

    private final static Logger LOG = Logger.getLogger(AbstractDecisionModule.class);
    private final Set<IDMEFListener> listeners = new CopyOnWriteArraySet<IDMEFListener>();
    private KnowledgeModule knowledgeModule;
    private AuditModule auditModule;
    private IDSMode mode;

    public AbstractDecisionModule(KnowledgeModule knowledgeModule, AuditModule auditModule, IDSMode mode) {
        this.knowledgeModule = knowledgeModule;
        this.auditModule = auditModule;
        this.mode = mode;
    }

    public AnalysisResult analyze(Event event) {

        if (mode == IDSMode.INTRUSION_DETECTING_WITHOUT_LEARNING
                || mode == IDSMode.INTRUSION_DETECTING_WITH_LEARNING) {
            Verdict verdict = analyzeEvent(event);
            EventType eventType;
            if (verdict.getVerdict() == Verdict.VerdictType.NORMAL) {
                // результат признан нормальным
                eventType = EventType.NORMAL;
                if (mode == IDSMode.INTRUSION_DETECTING_WITH_LEARNING) {
                    // выполняем обновление базы знаний
                    try {
                        knowledgeModule.updateKnowledge(event.getResultSet(), event.getUserId(), event.getSqlQuery());
                    } catch (Exception ex) {
                        LOG.error("Ошибка при обновлении базы знаний", ex);
                    }
                }
            } else if (verdict.getVerdict() == Verdict.VerdictType.ANOMALITY) {
                eventType = EventType.ANOMALY;
            } else if (verdict.getVerdict() == Verdict.VerdictType.ERROR) {
                eventType = EventType.ERROR;
            } else {
                eventType = EventType.NO_DECISION;
            }

            auditModule.logEvent(event.getUserId(), event.getSqlQuery(), event.getDate(), eventType, mode, verdict.getProperties());

            if (eventType == EventType.ANOMALY) {
                notifyListeners(event, verdict);
            }

            return new AnalysisResult(false);
        } else if (mode == IDSMode.LEARNING) {
            try {
                UpdateResult updateResult = knowledgeModule.updateKnowledge(event.getResultSet(), event.getUserId(), event.getSqlQuery());
                auditModule.logEvent(event.getUserId(), event.getSqlQuery(), event.getDate(), EventType.TRAINING_QUERY, mode, updateResult.getProperties());
            } catch (Exception ex) {
                LOG.error("Ошибка при обновлении базы знаний", ex);
                Map<String, String> properties = new HashMap<String, String>();
                properties.put("description", "Ошибка при обновлении базы знаний");
                auditModule.logEvent(event.getUserId(), event.getSqlQuery(), event.getDate(), EventType.ERROR, mode, properties);
            }

            return new AnalysisResult(false);
        } else if (mode == IDSMode.NO_OBSERVATION) {
            auditModule.logEvent(event.getUserId(), event.getSqlQuery(), event.getDate(), EventType.NO_OBSERVATION, mode, Collections.EMPTY_MAP);
        }
        return new AnalysisResult(false);
    }

    private Verdict analyzeEvent(Event event) {
        List<Verdict> verdicts = new ArrayList<Verdict>();
        try {
            List<Graph> relationGraphs = knowledgeModule.getRelationGraphs(event.getResultSet(), event.getUserId(), event.getSqlQuery());
            if (relationGraphs.isEmpty()) {
                LOG.warn("Невозможно построить граф отношений для записей, выбранных в результате выполнения запроса. Запрос: " + event.getSqlQuery());
                return new Verdict(Verdict.VerdictType.ERROR).putProperty("description", "Невозможно построить граф отношений для записей, выбранных в результате выполнения запроса.");
            }

            for (Graph relationGraph : relationGraphs) {
                verdicts.add(analyzeGraph(relationGraph));
            }

            for (Verdict verdict : verdicts) {
                if (verdict.getVerdict() == Verdict.VerdictType.ANOMALITY) {
                    return verdict;
                }
            }

            return verdicts.get(0);
        } catch (Exception ex) {
            LOG.error("При выполнении анализа результата выполнения запроса произошла ошибка. Запрос: " + event.getSqlQuery(), ex);
            return new Verdict(Verdict.VerdictType.ERROR).putProperty("description", "При выполнении анализа результата выполнения запроса произошла ошибка. " + ex.getMessage());
        }
    }

    public void setMode(IDSMode mode) {
        this.mode = mode;
    }

    public AuditModule getAuditModule() {
        return auditModule;
    }

    public void setAuditModule(AuditModule auditModule) {
        this.auditModule = auditModule;
    }

    public void addIDMEFListener(IDMEFListener listener) {
        listeners.add(listener);
    }

    public void removeIDMEFListener(IDMEFListener listener) {
        listeners.remove(listener);
    }

    /**
     * Выполнить анализ графа взаимосвязей записей в результате выполнения
     * запроса.
     *
     * @param graph Граф взаимосвязей записей в результате выполнения запроса.
     * @return Вердикт. Должен иметь тип или NORMAL, или ANOMALITY.
     * FIXME: возможно стоит переработать структуру Verdict
     */
    protected abstract Verdict analyzeGraph(Graph graph);

    private void notifyListeners(Event event, Verdict verdict) {
        IDMEF_Message message = createIDMEFMessage(event, verdict);

        for (IDMEFListener listener : listeners) {
            listener.onEvent(message);
        }
    }

    private IDMEF_Message createIDMEFMessage(Event event, Verdict verdict) {

        IDMEF_Node node = new IDMEF_Node();
        try {
            node.setLocation(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException ex) {
        }

        IDMEF_Process process = new IDMEF_Process();
        process.setName("java");
        Analyzer analyzer = new Analyzer(node,
                process,
                "FiendlyIDS",
                null,
                null,
                null,
                null,
                null,
                null);
        CreateTime createTime = new CreateTime();
        createTime.setIdmefDate(event.getDate());
        createTime.setNtpstamp(event.getDate());
        Classification classification = new Classification();

        StringBuilder message = new StringBuilder();
        message.append("ANOMALY DETECTED\n")
                .append("userId: ")
                .append(event.getUserId().getUserId())
                .append("\nsqlQuery: ")
                .append(event.getSqlQuery());
        for (Map.Entry<String, String> entry : verdict.getProperties().entrySet()) {
            message.append("\n")
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue());
        }
        classification.setName(message.toString());

        Alert alert = new Alert();
        alert.setAnalyzer(analyzer);
        alert.setCreateTime(createTime);
        alert.setClassifications(new Classification[]{classification});
        return alert;
    }
}
