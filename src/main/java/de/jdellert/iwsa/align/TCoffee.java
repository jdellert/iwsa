package de.jdellert.iwsa.align;

import de.jdellert.iwsa.corrmodel.CorrespondenceModel;
import de.jdellert.iwsa.corrmodel.CorrespondenceModelStorage;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TCoffee {

    private static final int EMPTY_SYMBOL = PhoneticSymbolTable.EMPTY_ID;
    private List<PhoneticString> sequences;
    private Map<String, Integer> lang2ID;
    private ArrayList<String> languageOrder;
    private List<String> originalLanguages;
    private PhoneticSymbolTable symbols;
    private CorrespondenceModel corrModel;
    private GuideTree guideTree;
    private TCoffeeLibrary lib;

    private TCoffee(List<PhoneticString> sequences, List<String> languages,
                    CorrespondenceModel corrModel, GuideTree guideTree) {
        this.sequences = sequences;
        this.symbols = corrModel.getSymbolTable();
        this.corrModel = corrModel;
        this.originalLanguages = languages;

        this.lang2ID = new HashMap<>();
        languageOrder = new ArrayList<>();
        for (int i = 0; i < languages.size(); i++) {
            String language = languages.get(i);
            if (lang2ID.containsKey(language)) {
                language += i;
            }
            languageOrder.add(language);
            this.lang2ID.put(language, i);
        }

        this.lib = new TCoffeeLibrary();
        this.lib.extend();

        this.guideTree = guideTree;
        if (guideTree == null)
            createGuideTree(languageOrder);
    }

    public static MultipleAlignment align(List<PhoneticString> sequences, List<String> languages,
                                          CorrespondenceModel corrModel) {
        return align(sequences, languages, corrModel, null);
    }

    public static MultipleAlignment align(List<PhoneticString> sequences, List<String> languages,
                                          CorrespondenceModel corrModel, GuideTree guideTree) {
        return (new TCoffee(sequences, languages, corrModel, guideTree)).align();
    }
    
	public static MultipleAlignment alignAndReorder(List<PhoneticString> sequences, List<String> languages,
			CorrespondenceModel corrModel) {
		return alignAndReorder(sequences, languages, corrModel, null);
	}
	
	public static MultipleAlignment alignAndReorder(List<PhoneticString> sequences, List<String> languages,
			CorrespondenceModel corrModel, GuideTree guideTree) {
		TCoffee tc = new TCoffee(sequences, languages, corrModel, guideTree);
		MultipleAlignment msa = tc.align();
		msa.orderAndFill(tc.languageOrder);
		return msa;
	}

    private static double[][] transpose(double[][] M) {
        return transpose(M, false);
    }

    private static double[][] transpose(double[][] M, boolean inPlace) {
        int m = M.length;
        if (m < 1) return M;
        int n = M[0].length;
        double[][] T = (inPlace) ? M : new double[n][m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                T[j][i] = M[i][j];
            }
        }
        return T;
    }

    private static double[][] dotProduct(double[][] M1, double[][] M2) {
        int l = M1.length;
        int m = M2.length;
        int n = M2[0].length;

        double[][] P = new double[l][n];

        for (int i = 0; i < l; i++) {
            for (int j = 0; j < m; j++) {
                for (int k = 0; k < n; k++) {
                    P[i][k] += M1[i][j] * M2[j][k];
                }
            }
        }

        return P;
    }

    private static double[][] matrixAdditionWithScalar(double[][] M1, double[][] M2, double a) {
        return matrixAdditionWithScalar(M1, M2, a, false);
    }

    private static double[][] matrixAdditionWithScalar(double[][] M1, double[][] M2, double a, boolean inPlace) {
        int m = M1.length;
        int n = M1[0].length;

        double[][] A = (inPlace) ? M1 : new double[m][n];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                A[i][j] = M1[i][j] + a * M2[i][j];
            }
        }

        return A;
    }

    public static void main(String[] args) {
        //CorrespondenceModel corrModel = CorrespondenceModelStorage.readGlobalModelFromFile(
        //        "/src/test/resources/northeuralex-0.9/global-nw.corr");
        CorrespondenceModel corrModel = null;

        try {
            corrModel = CorrespondenceModelStorage.deserializeCorrespondenceModel(
                    new ObjectInputStream(new FileInputStream("/home/arne/HiWi/etinen-full/src/test/resources/northeuralex-0.9/global-nw.corr")));
        } catch (Exception e) {
            System.exit(1);
            e.printStackTrace();
        }

        PhoneticSymbolTable symTable = corrModel.getSymbolTable();

        List<PhoneticString> sequences = new ArrayList<>();
        /*
        sequences.add(new PhoneticString(symTable.encode(new String[]{"m", "ʊ", "l", "d", "ʊ"})));
        sequences.add(new PhoneticString(symTable.encode(new String[]{"m", "ɔ", "l", "l", "d", "ɛ"})));
        sequences.add(new PhoneticString(symTable.encode(new String[]{"m", "ʉ", "ɛ", "l", "t", "i", "ɛ"})));
        sequences.add(new PhoneticString(symTable.encode(new String[]{"f", "ø", "l", "d"})));
        List<String> languages = Arrays.asList("olo", "smj", "sma", "hun");
        */

        sequences.add(new PhoneticString(symTable.encode(new String[]{"t", "a", "n", "i", "m", "b", "u", "k", "a"})));
        sequences.add(new PhoneticString(symTable.encode(new String[]{"t", "a", "n", "i", "m", "u", "k", "a"})));
        sequences.add(new PhoneticString(symTable.encode(new String[]{"t", "a", "n", "i", "m", "u", "k", "a"})));
        List<String> languages = Arrays.asList("Tupinamba", "Omagua", "Kokama");

        //System.err.println(corrModel);

        MultipleAlignment msa = TCoffee.align(sequences, languages, corrModel);
        msa.orderAndFill(languages);
        System.err.println(msa.toString(languages));

        MultipleAlignment msa2 = TCoffee.align(sequences.subList(0, 2), languages.subList(0, 2), corrModel);
        msa2.orderAndFill(languages);
        System.err.println(msa2.toString(languages));

        PhoneticStringAlignment align = NeedlemanWunschAlgorithm.constructAlignment(
                sequences.get(0), sequences.get(1), corrModel, corrModel, corrModel, corrModel);
        System.err.println(align.toString(symTable));
    }

    private void createGuideTree(List<String> languages) {
        languages = new ArrayList<>(languages);
        int n = languages.size();
        Map<String, Set<String>> ancestorOf = new HashMap<>();
        this.guideTree = new GuideTree();

        Map<String, LinkedList<String>> paths = new HashMap<>();
        for (int i = 0; i < n; i++)
            paths.put(languages.get(i), new LinkedList<>());
        int node = 0;

        // https://de.wikipedia.org/wiki/Neighbor-Joining-Algorithmus

        // Use lib score matrix as distance estimation
        double[][] dist = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                dist[i][j] = 1.0 - lib.scores[i][j];

        for (int x = 0; x < n - 2; x++) {
            int m = n - x;

            double[] r = new double[m];
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < m; j++)
                    r[i] += dist[i][j];
                r[i] /= m - 2;
            }

            int minI = -1;
            int minJ = -1;
            double minVal = Double.MAX_VALUE;
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < m; j++) {
                    if (i != j) {
                        double m_ij = dist[i][j] - (r[i] + r[j]);
                        if (m_ij < minVal) {
                            minVal = m_ij;
                            minI = i;
                            minJ = j;
                        }
                    }
                }
            }

            String newNode = "proto" + node;
            String child1 = languages.remove(Math.max(minI, minJ));
            String child2 = languages.remove(Math.min(minI, minJ));
            languages.add(newNode);
            Set<String> descendants = new HashSet<>();
            if (ancestorOf.containsKey(child1))
                descendants.addAll(ancestorOf.remove(child1));
            else
                descendants.add(child1);
            if (ancestorOf.containsKey(child2))
                descendants.addAll(ancestorOf.remove(child2));
            else
                descendants.add(child2);
            ancestorOf.put(newNode, descendants);
            for (String descendant : descendants) {
                paths.get(descendant).push(newNode);
            }
            node++;

            double[][] newDist = new double[m - 1][m - 1];
            int skipI = 0;
            for (int i = 0; i < m; i++) {
                if (i == minI || i == minJ)
                    skipI++;
                else {
                    int skipJ = 0;
                    for (int j = 0; j < m; j++) {
                        if (j == minI || j == minJ)
                            skipJ++;
                        else {
                            newDist[i - skipI][j - skipJ] = dist[i][j];
                        }
                    }

                    newDist[m - 3][i - skipI] = (dist[minI][i - skipI]
                            + dist[minJ][i - skipI] - dist[minI][minJ]) / 2;
                    newDist[i - skipI][m - 3] = newDist[m - 3][i - skipI];
                }
            }
            dist = newDist;
        }

        for (String leaf : paths.keySet())
            guideTree.enlargeTreeByPath(paths.get(leaf), leaf);

        System.err.println(guideTree.getChildrenOf("ROOT"));
    }

    private MultipleAlignment align() {
        return align(guideTree.getRoot());
    }

    private MultipleAlignment align(String curNode) {
        Set<String> children = guideTree.getChildrenOf(curNode);
        if (children == null || children.isEmpty()) {
            int l = lang2ID.get(curNode);
            return new MultipleAlignment(new String[]{curNode},
                    new int[][]{sequences.get(l).segments}, symbols);
        } else {
            String[] childArray = children.toArray(new String[0]);
            MultipleAlignment msa1 = align(childArray[0]);
            if (childArray.length == 1)
                return msa1;
            MultipleAlignment msa2 = align(childArray[1]);
            return nwBlock(msa1, msa2);
        }
    }

    private MultipleAlignment nwBlock(MultipleAlignment msa1, MultipleAlignment msa2) {
        int n = msa1.stringLength();
        int m = msa2.stringLength();
        double[][] dp = new double[n + 1][m + 1];
        int[][] pointers = new int[n + 1][m + 1];
        for (int i = 1; i < n; i++)
            pointers[i][0] = 1;
        for (int j = 1; j < m; j++)
            pointers[0][j] = 2;

        for (int i = 1; i < n + 1; i++) {
            for (int j = 1; j < m + 1; j++) {
                double insert = dp[i - 1][j];
                double delete = dp[i][j - 1];
                double match = dp[i - 1][j - 1];
                for (int w1 = 0; w1 < msa1.nOfLanguages(); w1++) {
                    for (int w2 = 0; w2 < msa2.nOfLanguages(); w2++) {
                        if (!msa1.hasGapAt(w1, i - 1) && !msa2.hasGapAt(w2, j - 1)) {
                            match += lib.matrices[lang2ID.get(msa1.getLanguage(w1))]
                                    [lang2ID.get(msa2.getLanguage(w2))]
                                    [msa1.getOriginalPosition(w1, i - 1)]
                                    [msa2.getOriginalPosition(w2, j - 1)];
                        }
                    }
                }

                //System.out.println("MATCH: " + match + ", INSERT: " + insert + ", DELETE: " + delete);

                int argmax = argmax(match, insert, delete);
                dp[i][j] = (argmax == 0) ? match : ((argmax == 1) ? insert : delete);
                pointers[i][j] = argmax;
            }
        }

        int i = n;
        int j = m;
        int c1 = msa1.nOfLanguages();
        int c2 = msa2.nOfLanguages();
        int colHeight = c1 + c2;
        List<int[]> alCombined = new ArrayList<>();
        while (i > 0 || j > 0) {
            int p = pointers[i][j];
            int[] newCol = new int[colHeight];
            if (p == 0) {
                System.arraycopy(msa1.getColumn(i - 1), 0, newCol, 0, c1);
                System.arraycopy(msa2.getColumn(j - 1), 0, newCol, c1, c2);
                i--;
                j--;
            } else if (p == 1) {
                System.arraycopy(msa1.getColumn(i - 1), 0, newCol, 0, c1);
                Arrays.fill(newCol, c1, colHeight, EMPTY_SYMBOL);
                i--;
            } else {
                Arrays.fill(newCol, 0, c1, EMPTY_SYMBOL);
                System.arraycopy(msa2.getColumn(j - 1), 0, newCol, c1, c2);
                j--;
            }
            alCombined.add(newCol);
        }

        int syms = alCombined.size();
        int[][] newMsa = new int[colHeight][syms];
        for (int l = 0; l < colHeight; l++) {
            for (int s = 0; s < syms; s++) {
                newMsa[l][s] = alCombined.get(syms - 1 - s)[l];
            }
        }
        String[] newLangs = new String[colHeight];
        for (int l1 = 0; l1 < c1; l1++)
            newLangs[l1] = msa1.getLanguage(l1);
        for (int l2 = 0; l2 < c2; l2++)
            newLangs[c1 + l2] = msa2.getLanguage(l2);

        return new MultipleAlignment(newLangs, newMsa, symbols);
    }

    private int argmax(double x, double y, double z) {
        boolean xy = x >= y;
        boolean xz = x >= z;
        boolean yz = y >= z;
        if (xy && xz)
            return 0;
        if (!xy && yz)
            return 1;
        return 2;
    }

    private class TCoffeeLibrary {

        // [word1][word2][sym1][sym2]
        double[][][][] matrices;
        // [word1][word2]
        double[][] scores;

        TCoffeeLibrary() {
            matrices = new double[sequences.size()][sequences.size()][][];
            scores = new double[sequences.size()][sequences.size()];

            for (int i = 0; i < sequences.size(); i++) {
                for (int j = 0; j < sequences.size(); j++) {
                    // If (j, i) already in library:
                    if (j < i) {
                        matrices[i][j] = transpose(matrices[j][i]);
                        scores[i][j] = scores[j][i];
                    } else {
                        PhoneticString seq1 = sequences.get(i);
                        PhoneticString seq2 = sequences.get(j);
                        PhoneticStringAlignment align = NeedlemanWunschAlgorithm.constructAlignment(
                                seq1, seq2, corrModel, corrModel, corrModel, corrModel);
                        double[][] matrix = new double[seq1.getLength()][seq2.getLength()];
                        double hammingDist = 0;
                        int comparisons = 0;
                        int k = 0;
                        int l = 0;
                        if (align.getLength() == 0) {
                            System.out.println("WARNING: Alignment of length 0 between" + seq1.toUntokenizedString(symbols) + " and " + seq2.toUntokenizedString(symbols));
                        }
                        for (int a = 0; a < align.getLength(); a++) {
                            int inSym = align.getSymbol1IDAtPos(a);
                            int outSym = align.getSymbol2IDAtPos(a);
                            if (inSym == EMPTY_SYMBOL)
                                l++;
                            else if (outSym == EMPTY_SYMBOL)
                                k++;
                            else {
                                matrix[k][l] = 1;
                                comparisons++;
                                if (inSym != outSym)
                                    hammingDist++;
                                k++;
                                l++;
                            }
                        }
                        matrices[i][j] = matrix;
                        scores[i][j] = 1.0;
                        if (comparisons > 0) scores[i][j] -= (hammingDist / comparisons);
                    }
                }
            }

//            for (int i = 0; i < sequences.size(); i++) {
//                for (int j = 0; j < sequences.size(); j++) {
//                    PhoneticString seq1 = sequences.get(i);
//                    PhoneticString seq2 = sequences.get(j);
//                    System.out.println(seq1.toString() + ":" + seq2.toString());
//                    for (int k = 0; k < seq1.getLength(); k++) {
//                        for (int l = 0; l < seq2.getLength(); l++) {
//                            System.out.print(matrices[i][j][k][l] + "\t");
//                        }
//                        System.out.println();
//                    }
//                }
//            }
        }

        void extend() {
            int n = matrices.length;
            double[][][][] ext = new double[n][n][][];

            for (int w1 = 0; w1 < n; w1++) {
                for (int w2 = 0; w2 < n; w2++) {
                    int w1len = matrices[w1][w2].length;
                    int w2len = matrices[w1][w2][0].length;
                    double[][] dm = new double[w1len][w2len];
                    for (int w3 = 0; w3 < n; w3++) {
                        double[][] m1 = matrices[w1][w3];
                        double[][] m2 = matrices[w3][w2];
                        double s1 = scores[w1][w3];
                        double s2 = scores[w3][w2];
                        matrixAdditionWithScalar(dm, dotProduct(m1, m2), (s1 + s2), true);
                    }
                    ext[w1][w2] = dm;
                }
            }

            matrices = ext;
        }
    }
}
