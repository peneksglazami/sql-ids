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

import com.grand.ids.model.Graph;
import com.grand.ids.model.Graph.Link;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Класс, выполняющий расчёт модульности взвешенного графа.
 * Используется алгоритм расчёта, описанный в статье
 * <a href="http://arxiv.org/PS_cache/arxiv/pdf/0803/0803.0476v2.pdf">
 * Blondel V.D., Guillaume J., Lambiotte R., Lefebvre E. Fast unfolding
 * of communities in large networks // Journal of Statistical Mechanics:
 * Theory and Experiment, 1742-5468, P10008 (12 pp.)</a>.
 *
 * @author Andrey Grigorov
 */
public class ModularityCalculator {

    private static class PartitionManager {

        private int size;
        private int[] n2c;
        private double[] in, tot;
        private Graph graph;

        public PartitionManager(Graph graph) {
            this.graph = graph;
            size = graph.getNodeNumber();
            n2c = new int[size];
            in = new double[size];
            tot = new double[size];

            for (int i = 0; i < size; i++) {
                n2c[i] = i;
                in[i] = graph.selfloopsWeight(i);
                tot[i] = graph.weightedDegree(i);
            }
        }

        public double modularity() {
            double q = 0;
            double m2 = 2 * graph.getTotalWeight();
            for (int i = 0; i < size; i++) {
                if (tot[i] > 0) {
                    q += in[i] / m2 - (tot[i] / m2) * (tot[i] / m2);
                }
            }
            return q;
        }

        private double modularity_gain(int node, int comm, double dnodecomm) {
            return dnodecomm - tot[comm] * graph.weightedDegree(node) / (2 * graph.getTotalWeight());
        }

        private Map<Integer, Double> neighborCommunities(int node) {
            Map<Integer, Double> res = new HashMap<Integer, Double>();
            List<Link> neighbor = graph.getNeighbors(node);
            res.put(n2c[node], 0.0);
            for (int i = 0, len = neighbor.size(); i < len; i++) {
                if (neighbor.get(i).node != node) {
                    int neighborNode = neighbor.get(i).node;
                    int communityNumber = n2c[neighborNode];
                    double linkWeight = neighbor.get(i).weight;
                    if (neighborNode != node) {
                        Double curWeight = res.get(communityNumber);
                        if (curWeight != null) {
                            res.put(communityNumber, curWeight + linkWeight);
                        } else {
                            res.put(communityNumber, linkWeight);
                        }
                    }
                }
            }
            return res;
        }

        private void remove(int node, int comm, double dnodecom) {
            tot[comm] -= graph.weightedDegree(node);
            in[comm] -= 2 * dnodecom + graph.selfloopsWeight(node);
            n2c[node] = -1;
        }

        private void insert(int node, int comm, double dnodecom) {
            tot[comm] += graph.weightedDegree(node);
            in[comm] += 2 * dnodecom + graph.selfloopsWeight(node);
            n2c[node] = comm;
        }

        public double improve() {

            double new_mod = modularity();
            double cur_mod;

            do {
                cur_mod = new_mod;
                for (int node = 0; node < size; node++) {
                    int node_comm = n2c[node];
                    Map<Integer, Double> ncomm = neighborCommunities(node);

                    remove(node, node_comm, ncomm.get(node_comm));

                    int best_comm = node_comm;
                    double best_linksWeight = 0;
                    double best_increase = 0;
                    List<Entry<Integer, Double>> ncommList = new ArrayList<Entry<Integer, Double>>(ncomm.entrySet());

                    for (int i = 0, len = ncommList.size(); i < len; i++) {
                        double increase = modularity_gain(node, ncommList.get(i).getKey(), ncommList.get(i).getValue());
                        if (increase > best_increase) {
                            best_comm = ncommList.get(i).getKey();
                            best_linksWeight = ncommList.get(i).getValue();
                            best_increase = increase;
                        }
                    }

                    insert(node, best_comm, best_linksWeight);

                    new_mod = modularity();
                }
            } while (new_mod > cur_mod);

            return cur_mod;
        }

        private void updateGraph() {
            int renumber[] = new int[size];
            for (int i = 0; i < size; i++) {
                renumber[i] = -1;
            }
            for (int i = 0; i < size; i++) {
                renumber[n2c[i]]++;
            }
            int newSize = 0;
            for (int i = 0; i < size; i++) {
                if (renumber[i] != -1) {
                    renumber[i] = newSize++;
                }
            }

            List<Integer>[] comm_nodes = new ArrayList[newSize];
            for (int i = 0; i < newSize; i++) {
                comm_nodes[i] = new ArrayList<Integer>();
            }
            for (int i = 0; i < size; i++) {
                comm_nodes[renumber[n2c[i]]].add(i);
            }

            Graph newGraph = new Graph(newSize);

            for (int i = 0; i < newSize; i++) {
                double w[] = new double[newSize];
                for (int j = 0, len = comm_nodes[i].size(); j < len; j++) {
                    List<Link> neighborLinks = graph.getNeighbors(comm_nodes[i].get(j));
                    for (int g = 0, linksNumber = neighborLinks.size(); g < linksNumber; g++) {
                        Link link = neighborLinks.get(g);
                        if (renumber[n2c[link.node]] >= i) {
                            w[renumber[n2c[link.node]]] += link.weight;
                        }
                    }
                }
                for (int j = i; j < newSize; j++) {
                    if (w[j] > 0) {
                        newGraph.addEdge(i, j, w[j]);
                    }
                }
            }
            size = newSize;
            graph = newGraph;
            n2c = new int[size];
            in = new double[size];
            tot = new double[size];

            for (int i = 0; i < size; i++) {
                n2c[i] = i;
                in[i] = graph.selfloopsWeight(i);
                tot[i] = graph.weightedDegree(i);
            }
        }
    }

    public double getModularity(Graph graph) {

        PartitionManager partitionManager = new PartitionManager(graph);
        double modularity, newModularity = partitionManager.modularity();
        do {
            modularity = newModularity;
            newModularity = partitionManager.improve();
            partitionManager.updateGraph();
        } while (newModularity > modularity);
        return modularity;
    }
}
