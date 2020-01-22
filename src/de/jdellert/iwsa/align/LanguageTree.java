package de.jdellert.iwsa.align;

import de.jdellert.iwsa.util.io.ListReader;
import de.jdellert.iwsa.util.io.StringUtils;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.Map.Entry;

public class LanguageTree {
	Map<String, List<String>> paths;
	// virtual root node is marked "ROOT"
	public String root = "ROOT";
	public Map<String, String> parents;
	public Map<String, TreeSet<String>> children;

	// TODO: turn the annotation objects into maps, allowing key-value pairs as
	// annotations (e.g. branch length, confidence, cognate ID, geographical
	// position, ...)
	public Map<String, Set<String>> annotation;

	public LanguageTree() {
		paths = new TreeMap<String, List<String>>();
		parents = new TreeMap<String, String>();
		children = new TreeMap<String, TreeSet<String>>();
		children.put("ROOT", new TreeSet<String>());

		annotation = new TreeMap<String, Set<String>>();
	}

	public LanguageTree(String langPathFile) {
		paths = new TreeMap<String, List<String>>();
		parents = new TreeMap<String, String>();
		children = new TreeMap<String, TreeSet<String>>();
		children.put("ROOT", new TreeSet<String>());

		annotation = new TreeMap<String, Set<String>>();

		Map<String, List<String>> loadedPaths = loadLanguagePaths(langPathFile);
		for (String langID : loadedPaths.keySet()) {
			enlargeTreeByPath(loadedPaths.get(langID), langID);
		}
	}

	public LanguageTree(String langPathFile, String[] langs) {
		paths = new TreeMap<String, List<String>>();
		parents = new TreeMap<String, String>();
		children = new TreeMap<String, TreeSet<String>>();
		children.put("ROOT", new TreeSet<String>());

		annotation = new TreeMap<String, Set<String>>();

		Map<String, List<String>> loadedPaths = loadLanguagePaths(langPathFile);
		for (String langID : langs) {
			enlargeTreeByPath(loadedPaths.get(langID), langID);
		}
	}

	public LanguageTree copy() {
		LanguageTree copy = new LanguageTree();
		for (String key : paths.keySet()) {
			copy.paths.put(key, new LinkedList<>(paths.get(key)));
		}
		for (String key : parents.keySet()) {
			copy.parents.put(key, parents.get(key));
		}
		for (String key : children.keySet()) {
			copy.children.put(key, new TreeSet<String>(children.get(key)));
		}
		for (String key : annotation.keySet()) {
			copy.annotation.put(key, new TreeSet<String>(annotation.get(key)));
		}
		return copy;
	}

	public int getNumDescendants(String node) {
		int numDescendants = 1;
		if (children.get(node) != null) {
			for (String child : children.get(node)) {
				numDescendants += getNumDescendants(child);
			}
		}
		return numDescendants;
	}

	public void collectDescendants(String node, List<String> list) {
		list.add(node);
		if (children.get(node) != null) {
			for (String child : children.get(node)) {
				collectDescendants(child, list);
			}
		}
	}

	public void collectLeaves(String node, List<String> list) {
		if (children.get(node) != null) {
			for (String child : children.get(node)) {
				collectLeaves(child, list);
			}
		} else {
			list.add(node);
		}
	}

	/**
	 * Renames a tree node (costly because everything in this class is indexed
	 * by unique string IDs for each node).
	 * 
	 * @param oldName
	 * @param newName
	 * @return true if renaming was successful, false if it was impossible (new
	 *         name already existed, or old name did not / was "ROOT")
	 */
	public boolean renameNode(String oldName, String newName) {
		if (oldName.equals("ROOT") || parents.get(oldName) == null || parents.get(newName) != null)
			return false;

		// update paths
		if (paths.get(oldName) != null) {
			paths.put(newName, paths.remove(oldName));
		}
		for (List<String> path : paths.values()) {
			int oldNameIndex = path.indexOf(oldName);
			if (oldNameIndex != -1) {
				path.set(oldNameIndex, newName);
			}
		}

		// update annotations
		annotation.put(newName, annotation.remove(oldName));

		// update parent pointer (and the child Name in the parent's children
		// set)
		parents.put(newName, parents.remove(oldName));
		TreeSet<String> parentChildren = children.get(parents.get(newName));
		parentChildren.remove(oldName);
		parentChildren.add(newName);

		// update children (with their parent pointers)
		children.put(newName, children.remove(oldName));
		TreeSet<String> nodeChildren = children.get(newName);
		if (nodeChildren != null) {
			for (String nodeChild : nodeChildren) {
				parents.put(nodeChild, newName);
			}
		}

		return true;
	}

	public void reduceToLangs(String[] langs) {
		Map<String, List<String>> remainingPaths = new TreeMap<String, List<String>>();
		for (String lang : langs) {
			if (paths.get(lang) != null) {
				remainingPaths.put(lang, paths.get(lang));
			} else {
				System.err.println("WARNING: no path to lang " + lang + " found in tree!");
			}
		}

		paths = new TreeMap<String, List<String>>();
		parents = new TreeMap<String, String>();
		children = new TreeMap<String, TreeSet<String>>();
		children.put("ROOT", new TreeSet<String>());

		for (String lang : langs) {
			enlargeTreeByPath(remainingPaths.get(lang), lang);
		}
	}

	public String lowestCommonAncestor(String lang1, String lang2) {
		List<String> path1 = paths.get(lang1);
		if (path1 == null)
			return root;
		List<String> path2 = paths.get(lang2);
		for (int j = path2.size() - 1; j >= 0; j--) {
			String onPath2 = path2.get(j);
			if (path1.contains(onPath2))
				return onPath2;
		}
		return root;
	}

	public String lowestCommonAncestor(List<String> langs) {
		if (langs.size() == 1)
			return langs.get(0);
		Set<String> sharedNodes = new TreeSet<String>();
		List<String> path1 = paths.get(langs.get(0));
		if (path1 == null)
			return root;
		sharedNodes.addAll(path1);
		for (int i = 1; i < langs.size(); i++) {
			List<String> path = paths.get(langs.get(i));
			if (path == null)
				return root;
			sharedNodes.retainAll(sharedNodes);
		}
		if (sharedNodes.size() == 0)
			return root;
		for (int j = path1.size() - 1; j >= 0; j--) {
			String onPath = path1.get(j);
			if (sharedNodes.contains(onPath))
				return onPath;
		}
		return root;
	}

	private Map<String, List<String>> loadLanguagePaths(String langPathFile) {
		Map<String, List<String>> loadedPaths = new TreeMap<String, List<String>>();
		try {
			for (String[] line : ListReader.arrayFromTSV(langPathFile)) {
				String langID = line[0];
				List<String> path = new LinkedList<String>();
				for (int i = line.length - 1; i > 0; i--) {
					path.add(line[i]);
				}
				loadedPaths.put(langID, path);
			}
		} catch (FileNotFoundException e) {
			System.err.println("ERROR: file " + langPathFile + " not found! Returning empty tree ...");
		}
		return loadedPaths;
	}

	public void enlargeTreeByPath(List<String> path, String langID) {
		if (path == null) {
			System.err.println(
					"WARNING: no path found for language " + langID + ", it will not be part of the language tree.");
		}
		paths.put(langID, path);
		String currentNode = "ROOT";
		for (String pathElement : path) {
			TreeSet<String> currentChildren = children.get(currentNode);
			if (currentChildren == null) {
				currentChildren = new TreeSet<String>();
				children.put(currentNode, currentChildren);
			}
			if (!currentChildren.contains(pathElement)) {
				currentChildren.add(pathElement);
				parents.put(pathElement, currentNode);
			}
			currentNode = pathElement;
		}
		TreeSet<String> currentChildren = children.get(currentNode);
		if (currentChildren == null) {
			currentChildren = new TreeSet<String>();
			children.put(currentNode, currentChildren);
		}
		if (!currentChildren.contains(langID)) {
			currentChildren.add(langID);
			parents.put(langID, currentNode);
		}
	}

	public List<String[]> getNonTiedTriplets() {
		// a list entry [l1, l2, l3] encodes the triplet {l1, l2 | l3}
		String[] langs = paths.keySet().toArray(new String[paths.keySet().size()]);
		List<String[]> triplets = new LinkedList<String[]>();
		for (int i = 0; i < langs.length; i++) {
			String l1 = langs[i];
			List<String> l1Path = paths.get(l1);
			for (int j = i + 1; j < langs.length; j++) {
				String l2 = langs[j];
				List<String> l2Path = paths.get(l2);
				for (int k = j + 1; k < langs.length; k++) {
					String l3 = langs[k];
					List<String> l3Path = paths.get(l3);
					/*
					 * System.err.println(l1 + "+" + l2 + "+" + l3);
					 * System.err.println("  l1Path: " + l1Path);
					 * System.err.println("  l2Path: " + l2Path);
					 * System.err.println("  l3Path: " + l3Path);
					 */
					int pathPos = 0;
					while (pathPos < l1Path.size() && l1Path.get(pathPos).equals(l2Path.get(pathPos))
							&& l1Path.get(pathPos).equals(l3Path.get(pathPos))) {
						pathPos++;
					}
					// System.err.println(" identical paths up to position: " +
					// pathPos);
					if (pathPos == l1Path.size()) {
						// path 1 exhausted => identical paths, a tie
						// System.err.println(" " + l1 + "+" + l2 + "+" + l3 +
						// ": path exhausted!");
						continue;
					}
					if (!l1Path.get(pathPos).equals(l2Path.get(pathPos))) {
						// l1 | l2
						if (!l1Path.get(pathPos).equals(l3Path.get(pathPos))) {
							// l1 | l3
							if (!l2Path.get(pathPos).equals(l3Path.get(pathPos))) {
								// l2 | l3 => a tie
								continue;
							} else {
								// {l2, l3 | l1}
								triplets.add(new String[] { l2, l3, l1 });
							}
						} else {
							// l1 = l3
							if (!l2Path.get(pathPos).equals(l3Path.get(pathPos))) {
								// {l1, l3 | l2}
								triplets.add(new String[] { l1, l3, l2 });
							} else {
								// l2 = l3
								System.err.println("WARNING: unexpected case during triplet computation!");
								continue;
							}
						}
					} else {
						// l1 = l2
						if (!l1Path.get(pathPos).equals(l3Path.get(pathPos))) {
							// l1 | l3
							if (!l2Path.get(pathPos).equals(l3Path.get(pathPos))) {
								// l2 | l3 => //{l1, l2 | l3}
								triplets.add(new String[] { l1, l2, l3 });
							} else {
								// l1 = l3
								System.err.println("WARNING: unexpected case during triplet computation!");
								continue;
							}
						}
					}
				}
			}
		}
		return triplets;
	}

	public Map<String, Double> getBranchLengthsFromAnnotations() {
		// if annotations encode branch lengths, store them and the old parent
		// relation
		Map<String, Double> branchLengths = new TreeMap<String, Double>();
		for (String lang : annotation.keySet()) {
			Set<String> annotations = annotation.get(lang);
			if (annotations.size() == 1) {
				try {
					Double branchLength = Double.parseDouble(annotations.iterator().next());
					branchLengths.put(lang, branchLength);
				} catch (NumberFormatException e) {
					// this is not really an exception, it only means that the
					// annotation is not a
					// branch length
				}
			}
		}

		return branchLengths;
	}

	public Map<String, Double> getTimeDepthsFromAnnotatedBranchLengths() {
		Map<String, Double> timeDepths = new TreeMap<String, Double>();

		Map<String, Double> branchLengths = getBranchLengthsFromAnnotations();
		double rootDepth = 1.0;
		for (String node : branchLengths.keySet()) {
			Double totalDepth = rootDepth;
			String parent = node;
			while (!parent.equals("ROOT")) {
				Double branchLength = branchLengths.get(parent);
				if (branchLength == null) {
					totalDepth = null;
					break;
				} else {
					totalDepth -= branchLength;
					parent = parents.get(parent);
				}
			}
			if (totalDepth != null) {
				timeDepths.put(node, totalDepth);
			}
		}

		timeDepths.put("ROOT", 1.0);

		return timeDepths;
	}

	public void removeNonBranchingNodes() {
		Map<String, Double> branchLengths = getBranchLengthsFromAnnotations();

		Map<String, String> oldParentsRelation = new TreeMap<String, String>();
		for (Entry<String, String> entry : parents.entrySet()) {
			oldParentsRelation.put(entry.getKey(), entry.getValue());
		}

		removeNonBranchingNodes(root);

		// recompute branch length annotations
		TreeSet<String> reducedNodes = new TreeSet<String>();
		for (String lang : getLangsAndProtoLangs()) {
			reducedNodes.add(lang);
		}
		for (String lang : reducedNodes) {
			Double summedBranchLength = sumBranchLengths(lang, branchLengths, oldParentsRelation, reducedNodes);
			if (summedBranchLength != null) {
				TreeSet<String> newAnnotations = new TreeSet<String>();
				newAnnotations.add(String.format("%.4f", summedBranchLength).replaceAll(",", "."));
				annotation.put(lang, newAnnotations);
			}
		}
	}

	private void removeNonBranchingNodes(String node) {
		TreeSet<String> nodeChildren = children.get(node);
		// System.err.println("removeNonBranchingNodes(" + node + "), children:
		// " +
		// nodeChildren + ", parent: " + parents.get(node));
		// base case: node is leaf node
		if (nodeChildren == null || nodeChildren.size() == 0)
			return;
		// look for non-branching nodes (only one child)
		if (nodeChildren.size() == 1) {
			String nodeChild = nodeChildren.iterator().next();
			TreeSet<String> nodeGrandChildren = children.get(nodeChild);
			// child is leaf node => replace non-branching node by child
			if (nodeGrandChildren == null || nodeGrandChildren.size() == 0) {
				paths.get(nodeChild).remove(node);
				parents.put(nodeChild, parents.get(node));
				TreeSet<String> siblings = children.get(parents.get(node));
				siblings.remove(node);
				siblings.add(nodeChild);
				parents.remove(node); // TODO: check whether this breaks
										// anything
				children.remove(node); // TODO: check whether this breaks
										// anything
				return;
			}
			// otherwise, remove the single child and append grandchildren
			// directly
			else {
				List<String> nodeNames = new LinkedList<String>();
				collectLangNames(node, nodeNames);
				for (String nodeName : nodeNames) {
					List<String> path = paths.get(nodeName);
					if (path != null)
						path.remove(nodeChild);
				}
				parents.remove(nodeChild); // TODO: check whether this breaks
											// anything
				children.remove(nodeChild); // TODO: check whether this breaks
											// anything
				for (String grandChild : nodeGrandChildren) {
					parents.put(grandChild, node);
				}
				children.put(node, nodeGrandChildren);
				// repeat process on same node (in case it is still
				// non-branching)
				nodeChildren = new TreeSet<String>();
				nodeChildren.add(node);
			}
		}
		// recursive case: pre-order traversal (needs to be cached because of
		// concurrent
		// modification)
		for (String nodeChild : new TreeSet<String>(nodeChildren)) {
			removeNonBranchingNodes(nodeChild);
		}
	}

	private Double sumBranchLengths(String lang, Map<String, Double> branchLengths,
			Map<String, String> oldParentsRelation, TreeSet<String> reducedNodes) {
		Double sum = branchLengths.get(lang);
		if (branchLengths.get(lang) != null) {
			String parent = oldParentsRelation.get(lang);
			while (!reducedNodes.contains(parent)) {
				sum += branchLengths.get(parent);
				parent = oldParentsRelation.get(parent);
			}
		}
		return sum;
	}

	public String[] getLangsAndProtoLangs() {
		List<String> langs = new LinkedList<String>();
		collectLangNames("ROOT", langs);
		return langs.toArray(new String[langs.size()]);
	}

	public String[] getLangsAndProtoLangsExceptRoot() {
		List<String> langs = new LinkedList<String>();
		collectLangNames("ROOT", langs);
		langs.remove("ROOT");
		return langs.toArray(new String[langs.size()]);
	}

	private void collectLangNames(String langName, List<String> langs) {
		langs.add(langName);
		Set<String> daughters = children.get(langName);
		if (daughters == null || daughters.size() == 0)
			return;
		for (String daughter : daughters) {
			collectLangNames(daughter, langs);
		}
	}

	public String toNewickString() {
		StringBuilder str = new StringBuilder();
		toNewickString(root, str);
		str.append(";");
		return str.toString();
	}

	public String toNewickStringWithBranchLengths() {
		StringBuilder str = new StringBuilder();
		toNewickStringWithBranchLengths(root, str);
		str.append(";");
		return str.toString();
	}

	public void toNewickString(String node, StringBuilder str) {
		TreeSet<String> nodeChildren = children.get(node);
		if (nodeChildren == null || nodeChildren.size() == 0) {
			str.append(node.replaceAll(" ", "_"));
			Set<String> nodeAnnotation = annotation.get(node);
			if (nodeAnnotation != null)
				str.append("#" + StringUtils.join("/", nodeAnnotation));
		} else {
			str.append("(");
			for (String nodeChild : nodeChildren) {
				toNewickString(nodeChild, str);
				str.append(",");
			}
			str.deleteCharAt(str.length() - 1);
			str.append(")");
			str.append(node.replaceAll(" ", "_"));
			Set<String> nodeAnnotation = annotation.get(node);
			// System.err.println(node.replaceAll("_", " ") + ".annotation = " +
			// nodeAnnotation);
			if (nodeAnnotation != null)
				str.append("#" + StringUtils.join("/", nodeAnnotation));
		}
	}

	public void toNewickStringWithBranchLengths(String node, StringBuilder str) {
		TreeSet<String> nodeChildren = children.get(node);
		if (nodeChildren == null || nodeChildren.size() == 0) {
			str.append(node.replaceAll(" ", "_"));
			Set<String> nodeAnnotation = annotation.get(node);
			if (nodeAnnotation != null)
				str.append(":" + StringUtils.join("/", nodeAnnotation));
		} else {
			str.append("(");
			for (String nodeChild : nodeChildren) {
				toNewickStringWithBranchLengths(nodeChild, str);
				str.append(",");
			}
			str.deleteCharAt(str.length() - 1);
			str.append(")");
			str.append(node.replaceAll(" ", "_"));
			Set<String> nodeAnnotation = annotation.get(node);
			if (nodeAnnotation != null)
				str.append(":" + StringUtils.join("/", nodeAnnotation));
		}
	}

	public static LanguageTree fromNewickString(String nwkString) {
		LanguageTree tree = new LanguageTree();
		// stack-based parsing, creating and adding nodes on the fly
		List<List<String>> stack = new LinkedList<List<String>>();
		stack.add(new LinkedList<String>());

		int unnamedNodeID = 0;

		char[] nwk = nwkString.toCharArray();
		boolean parsingNodeName = false;
		boolean parsingBranchLength = false;
		boolean parsingAnnotation = false;
		boolean closingLabel = false;
		StringBuilder currentNodeName = new StringBuilder();
		StringBuilder currentAnnotation = new StringBuilder();
		for (int i = 0; i < nwk.length; i++) {
			// TODO: do not treat branch length as annotation (correct inference
			// of branch
			// lengths during simplification!)
			if (parsingBranchLength) {
				if (!(nwk[i] == ',' || nwk[i] == ')' || nwk[i] == '(' || nwk[i] == ';'))
					continue;
				parsingBranchLength = false;
				parsingNodeName = true;
			}
			if (parsingAnnotation) {
				if (!(nwk[i] == ',' || nwk[i] == ')' || nwk[i] == '(' || nwk[i] == ';')) {
					currentAnnotation.append(nwk[i]);
					continue;
				} else {
					parsingAnnotation = false;
					parsingNodeName = true;
				}
			}
			if (parsingNodeName) {
				if ((nwk[i] == ',' || nwk[i] == ')' || nwk[i] == ';' || nwk[i] == '(')) {
					String completeNodeName = currentNodeName.toString();
					if (completeNodeName.length() == 0) {
						completeNodeName = "unnamedNode" + (unnamedNodeID++);
					}
					String completeAnnotation = currentAnnotation.toString();
					currentNodeName = new StringBuilder();
					currentAnnotation = new StringBuilder();
					parsingNodeName = false;

					// System.err.println("completed node: " +
					// completeNodeName);

					if (closingLabel) {
						TreeSet<String> children = new TreeSet<String>();
						for (String child : stack.remove(0)) {
							tree.parents.put(child, completeNodeName);
							children.add(child);
						}
						tree.children.put(completeNodeName, children);
						if (completeAnnotation.length() > 0)
							tree.annotation.put(completeNodeName,
									new TreeSet<String>(Arrays.asList(completeAnnotation.split("/"))));
						closingLabel = false;
					}

					switch (nwk[i]) {
					case ',': {
						stack.get(0).add(completeNodeName);
						if (completeAnnotation.length() > 0)
							tree.annotation.put(completeNodeName,
									new TreeSet<String>(Arrays.asList(completeAnnotation.split("/"))));
						break;
					}
					case ')': {
						closingLabel = true;
						stack.get(0).add(completeNodeName);
						if (completeAnnotation.length() > 0)
							tree.annotation.put(completeNodeName,
									new TreeSet<String>(Arrays.asList(completeAnnotation.split("/"))));
						break;
					}
					// ; lowest stack layer contains nodes to be connected by
					// root
					case ';': {
						stack.get(0).add(completeNodeName);
						if (completeAnnotation.length() > 0)
							tree.annotation.put(completeNodeName,
									new TreeSet<String>(Arrays.asList(completeAnnotation.split("/"))));
						break;
					}
					case '(': {
						stack.add(0, new LinkedList<String>());
						break;
					}
					}
					/*
					 * System.err.println("Stack: "); for (List<String>
					 * stackLayer : stack) { System.err.println(stackLayer); }
					 */
				} else {
					if (nwk[i] == ':') {
						parsingAnnotation = true;
						// parsingBranchLength = true;
						continue;
					} else if (nwk[i] == '#') {
						parsingAnnotation = true;
						continue;
					} else {
						currentNodeName.append(nwk[i]);
					}
				}
			} else if (nwk[i] == '(') {
				stack.add(0, new LinkedList<String>());
			} else if (nwk[i] == ':') {
				// empty node label
				parsingAnnotation = true;
			} else {
				parsingNodeName = true;
				currentNodeName.append(nwk[i]);
			}
		}
		TreeSet<String> roots = new TreeSet<String>();
		if (stack.get(0).size() == 1) {
			roots.add(stack.get(0).get(0));
			tree.root = stack.get(0).get(0);
		}

		for (String root : roots) {
			addPaths(tree, root, new LinkedList<String>());
		}

		return tree;
	}

	public static LanguageTree fromNewickFile(String nwkFile) throws FileNotFoundException {
		// one tree per line, will be connected by virtual ROOT node
		// important assumption: unique leaf names!
		List<String> nwkStrings = ListReader.listFromFile(nwkFile);

		int unnamedNodeID = 0;

		LanguageTree tree = new LanguageTree();
		// stack-based parsing, creating and adding nodes on the fly
		List<List<String>> stack = new LinkedList<List<String>>();
		stack.add(new LinkedList<String>());
		for (String nwkString : nwkStrings) {
			char[] nwk = nwkString.toCharArray();
			boolean parsingNodeName = false;
			boolean parsingBranchLength = false;
			boolean parsingAnnotation = false;
			boolean closingLabel = false;
			StringBuilder currentNodeName = new StringBuilder();
			StringBuilder currentAnnotation = new StringBuilder();
			for (int i = 0; i < nwk.length; i++) {
				// DEBUGGING INFORMATION (UNCOMMENT TO DUMP PARSER STATE AFTER
				// EACH CHARACTER)
				// System.err.print("next char " + i + "\'" + nwk[i] +
				// "\'\tstate ");
				// System.err.print((parsingNodeName ? '+' : '-') + "name,");
				// System.err.print((parsingBranchLength ? '+' : '-') +
				// "blen,");
				// System.err.print((parsingAnnotation ? '+' : '-') + "anno,");
				// System.err.print((closingLabel ? '+' : '-') + "clol");
				// System.err.println("\tcurrentNodeName=\"" +
				// currentNodeName.toString() +
				// "\"\tcurrentAnnotation=\"" + currentAnnotation.toString() +
				// "\"");

				// TODO: do not treat branch length as annotation (correct
				// inference of branch
				// lengths during simplification!)
				if (parsingBranchLength) {
					if (!(nwk[i] == ',' || nwk[i] == ')' || nwk[i] == '(' || nwk[i] == ';'))
						continue;
					parsingBranchLength = false;
					parsingNodeName = true;
				}
				if (parsingAnnotation) {
					if (!(nwk[i] == ',' || nwk[i] == ')' || nwk[i] == '(' || nwk[i] == ';')) {
						currentAnnotation.append(nwk[i]);
						continue;
					} else {
						parsingAnnotation = false;
						parsingNodeName = true;
					}
				}
				if (parsingNodeName) {
					if ((nwk[i] == ',' || nwk[i] == ')' || nwk[i] == ';' || nwk[i] == '(')) {
						String completeNodeName = currentNodeName.toString();
						if (completeNodeName.length() == 0) {
							completeNodeName = "unnamedNode" + (unnamedNodeID++);
						}
						String completeAnnotation = currentAnnotation.toString();
						currentNodeName = new StringBuilder();
						currentAnnotation = new StringBuilder();
						parsingNodeName = false;

						// System.err.println("completed node: " +
						// completeNodeName);

						if (closingLabel) {
							TreeSet<String> children = new TreeSet<String>();
							for (String child : stack.remove(0)) {
								tree.parents.put(child, completeNodeName);
								children.add(child);
							}
							tree.children.put(completeNodeName, children);
							if (completeAnnotation.length() > 0)
								tree.annotation.put(completeNodeName,
										new TreeSet<String>(Arrays.asList(completeAnnotation.split("/"))));
							closingLabel = false;
						}

						switch (nwk[i]) {
						case ',': {
							stack.get(0).add(completeNodeName);
							if (completeAnnotation.length() > 0)
								tree.annotation.put(completeNodeName,
										new TreeSet<String>(Arrays.asList(completeAnnotation.split("/"))));
							break;
						}
						case ')': {
							closingLabel = true;
							stack.get(0).add(completeNodeName);
							if (completeAnnotation.length() > 0)
								tree.annotation.put(completeNodeName,
										new TreeSet<String>(Arrays.asList(completeAnnotation.split("/"))));
							break;
						}
						// ; lowest stack layer contains nodes to be connected
						// by root
						case ';': {
							stack.get(0).add(completeNodeName);
							if (completeAnnotation.length() > 0)
								tree.annotation.put(completeNodeName,
										new TreeSet<String>(Arrays.asList(completeAnnotation.split("/"))));
							break;
						}
						case '(': {
							stack.add(0, new LinkedList<String>());
							break;
						}
						}
						/*
						 * System.err.println("Stack: "); for (List<String>
						 * stackLayer : stack) { System.err.println(stackLayer);
						 * }
						 */
					} else {
						if (nwk[i] == ':') {
							parsingAnnotation = true;
							// parsingBranchLength = true;
							continue;
						} else if (nwk[i] == '#') {
							parsingAnnotation = true;
							continue;
						} else {
							currentNodeName.append(nwk[i]);
						}
					}
				} else if (nwk[i] == '(') {
					stack.add(0, new LinkedList<String>());
				} else if (nwk[i] == ':') {
					// empty node label
					parsingAnnotation = true;
				} else {
					parsingNodeName = true;
					currentNodeName.append(nwk[i]);
				}
			}
		}
		TreeSet<String> roots = new TreeSet<String>();
		if (stack.get(0).size() == 1) {
			if (stack.get(0).get(0).equals("ROOT")) {
				roots.addAll(tree.children.get("ROOT"));
			} else {
				roots.add(stack.get(0).get(0));
			}
			tree.root = stack.get(0).get(0);
		} else {
			for (String lang : stack.get(0)) {
				roots.add(lang);
				tree.parents.put(lang, "ROOT");
			}
			tree.children.put("ROOT", roots);
		}

		for (String root : roots) {
			addPaths(tree, root, new LinkedList<String>());
		}
		for (String node : tree.paths.keySet()) {
			// System.err.println(node + ": " + tree.paths.get(node));
		}

		return tree;
	}

	public static void addPaths(LanguageTree tree, String node, List<String> pathToNode) {
		// System.err.println("addPaths(tree," + node + "," + pathToNode + ")");
		tree.paths.put(node, new LinkedList<String>(pathToNode));
		if (tree.children.get(node) == null || tree.children.get(node).size() == 0) {
			// tree.paths.put(node, new LinkedList<String>(pathToNode));
		} else {
			List<String> extendedPath = new LinkedList<String>(pathToNode);
			extendedPath.add(node);
			for (String child : tree.children.get(node)) {
				addPaths(tree, child, extendedPath);
			}
		}
	}

	public List<String> pathToRoot(String lang) {
		LinkedList<String> path = new LinkedList<String>();
		while (parents.get(lang) != null) {
			lang = parents.get(lang);
			path.add(lang);
		}
		return path;
	}

	public static void main(String[] args) {
		try {
			LanguageTree tree = LanguageTree.fromNewickFile(args[0]);
			// System.err.println(tree.annotation);
			System.err.println(tree.toNewickStringWithBranchLengths());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public Map<Integer, Integer> createIntParentTable(Map<String, Integer> langToInt) {
		int nextID = langToInt.values().stream().mapToInt(v -> v).max().orElseThrow(NoSuchElementException::new) + 1;
		Map<Integer, Integer> parentTableInt = new TreeMap<Integer, Integer>();
		for (String child : parents.keySet()) {
			String parent = parents.get(child);
			Integer childInt = langToInt.get(child);
			if (childInt == null) {
				childInt = nextID++;
				langToInt.put(child, childInt);
			}
			Integer parentInt = langToInt.get(parent);
			if (parentInt == null) {
				parentInt = nextID++;
				langToInt.put(parent, parentInt);
			}
			parentTableInt.put(childInt, parentInt);
		}
		return parentTableInt;
	}

	public static Map<Integer, Set<Integer>> convertToIntChildrenTable(Map<Integer, Integer> intParentTable) {
		Map<Integer, Set<Integer>> intChildrenTable = new TreeMap<Integer, Set<Integer>>();
		for (Integer child : intParentTable.keySet()) {
			int parentID = intParentTable.get(child);
			Set<Integer> intChildrenForParent = intChildrenTable.get(parentID);
			if (intChildrenForParent == null) {
				intChildrenForParent = new TreeSet<Integer>();
				intChildrenTable.put(parentID, intChildrenForParent);
			}
			intChildrenForParent.add(child);
		}
		return intChildrenTable;
	}
}
