package de.jdellert.iwsa;

/**
 * A program to output the correspondences encoded in a correspondence file as a graph,
 * with lines visualizing all associations stronger than a positive PMI threshold given as a parameter,
 * and line thickness corresponding to the strength of the association.
 * <p>
 * The result is a DOT file which can be layouted with any executable GraphViz tool,
 * such as "dot" and "neato". Some non-standard symbols will be replaced to create valid DOT labels.
 * <p>
 * With a language-pair correspondence file and two additional language codes as arguments,
 * the program extracts the correspondences for that language pair from the second file
 * and visualizes the difference in symbol pair association strength as a drift graph,
 * where green arrows express the situation where the lange-specific correspondence is stronger
 * than globally. The drift graph only includes those symbols for which non-zero PMI scores exist.
 */

public class CorrespondenceModelVisualizationGraphviz {
    //TODO
}
