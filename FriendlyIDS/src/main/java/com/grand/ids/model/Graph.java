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

import java.util.ArrayList;
import java.util.List;

/**
 * Класс, предназначенный для хранения графа и использования при вычислении
 * модульности графа.
 *
 * @author Andrey Grigorov
 */
public class Graph {

    public static class Link {

        public int node;
        public double weight;

        public Link(int node, double weight) {
            this.node = node;
            this.weight = weight;
        }
    }

    private int nodeNumber;
    private Node[] nodes;
    private List<Link>[] nodeLinks;
    private double totalWeight;

    public Graph(int nodeNumber) {
        this.nodeNumber = nodeNumber;
        totalWeight = 0;
        nodes = new Node[nodeNumber];
        nodeLinks = new ArrayList[nodeNumber];
        for (int i = 0; i < this.nodeNumber; i++) {
            nodes[i] = new Node(i);
            nodeLinks[i] = new ArrayList<Link>();
        }
    }

    public void addEdge(int node1, int node2, double weight) {
        nodes[node1].linkNumber++;
        nodes[node1].linksWeight += weight;
        nodeLinks[node1].add(new Link(node2, weight));
        if (node1 != node2) {
            nodes[node2].linkNumber++;
            nodes[node2].linksWeight += weight;
            nodeLinks[node2].add(new Link(node1, weight));
        }
        totalWeight += weight;
    }

    public int getNodeNumber() {
        return nodeNumber;
    }

    public double selfloopsWeight(int node) {
        for (int i = 0, size = nodeLinks[node].size(); i < size; i++) {
            if (nodeLinks[node].get(i).node == node) {
                return nodeLinks[node].get(i).weight;
            }
        }
        return 0.0;
    }

    public double weightedDegree(int node) {
        return nodes[node].linksWeight;
    }

    public double getTotalWeight() {
        return totalWeight;
    }

    public List<Link> getNeighbors(int node) {
        return nodeLinks[node];
    }
}
