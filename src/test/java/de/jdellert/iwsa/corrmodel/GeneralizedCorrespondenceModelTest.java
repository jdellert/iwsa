package de.jdellert.iwsa.corrmodel;

import de.jdellert.iwsa.sequence.GeneralizedPhoneticSymbolTable;

public class GeneralizedCorrespondenceModelTest {
    static String[][] testPairs =
            {       {"æ", "a"},   {"æ", "ə"},   {"a", "i"},    {"p", "b"},    {"p", "f"},    {"p", "h"},    {"ħ", "h"},
                    {"aʊ", "ɑɪ"}, {"ʊu", "ʉː"}, {"aʊ", "oː"},  {"p", "p͡f"},  {"pʰ", "p͡f"}, {"t͡ʃ", "tʲ"}, {"t͡ʃ", "t"},
                    {"kn", "n"},  {"kn", "xn"}, {"kn", "kr"},  {"ᵐp", "p"},   {"ᵐp", "b"},   {"ᵐp", "f"},   {"ᵐp", "ⁿd"},
                    {"h₁", "h"},  {"h₁", "e"},  {"h₁", "a"},   {"h₂", "e"},   {"h₂", "a"},   {"h₁", "h₂"},  {"h₁", "h₃"},
                    {"H", "h"},   {"H", "h₁"},  {"H", "h₂"},   {"H", "a"},    {"H", "e"},    {"H", "-"},    {"h", "-"}
            };

    public static void main(String[] args) {
        try {
            GeneralizedCorrespondenceModel corrModel = new GeneralizedCorrespondenceModel();
            GeneralizedPhoneticSymbolTable symbolTable = (GeneralizedPhoneticSymbolTable) corrModel.getSymbolTable();
            symbolTable.declareCombinedSymbol("kn", "k", "n");
            symbolTable.declareCombinedSymbol("kr", "k", "r");
            symbolTable.declareCombinedSymbol("xn", "x", "n");
            symbolTable.declareCombinedSymbol("ᵐp", "m", "p");
            symbolTable.declareCombinedSymbol("ⁿd", "n", "d");
            symbolTable.declareMetasymbol("h₁", "h", "ʔ", "ç", "ə");
            symbolTable.declareMetasymbol("h₂", "ħ", "ʕ", "χ", "ɐ");
            symbolTable.declareMetasymbol("h₃", "ɣʷ", "ʁ", "ɵ");
            symbolTable.declareMetasymbol("H", "h₁", "h₂", "h₃");
            for (String[] testPair : testPairs) {
                System.err.println(testPair[0] + "\t" + testPair[1] + "\t" + corrModel.getScore(testPair[0], testPair[1]));
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
