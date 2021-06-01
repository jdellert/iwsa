package de.jdellert.iwsa.align;

import java.util.*;

public class GuideTree {
    // Reduced version of LanguageTree

    protected Map<String, List<String>> paths;
    // virtual root node is marked "ROOT"
    protected String root = "ROOT";
    protected Map<String, String> parents;
    protected Map<String, TreeSet<String>> children;

    public GuideTree() {
        paths = new TreeMap<>();
        parents = new TreeMap<>();
        children = new TreeMap<>();
        children.put(root, new TreeSet<>());
    }

    public void enlargeTreeByPath(List<String> path, String langID) {
        if (path == null) {
            System.err.println(
                    "WARNING: no path found for language " + langID + ", it will not be part of the language tree.");
        }
        else {
            paths.put(langID, path);
            String currentNode = root;
            for (String pathElement : path) {
                TreeSet<String> currentChildren = children.computeIfAbsent(currentNode, k -> new TreeSet<>());
                if (!currentChildren.contains(pathElement)) {
                    currentChildren.add(pathElement);
                    parents.put(pathElement, currentNode);
                }
                currentNode = pathElement;
            }
            TreeSet<String> currentChildren = children.computeIfAbsent(currentNode, k -> new TreeSet<>());
            if (!currentChildren.contains(langID)) {
                currentChildren.add(langID);
                parents.put(langID, currentNode);
            }
        }
    }

    public String getRoot() {
        return root;
    }

    public Set<String> getChildrenOf(String node) {
        return children.computeIfAbsent(node, x -> new TreeSet<>());
    }
}
