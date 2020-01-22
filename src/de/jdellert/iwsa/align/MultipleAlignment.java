package de.jdellert.iwsa.align;

import de.jdellert.iwsa.corrmodel.CorrespondenceModel;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

import java.util.*;
import java.util.stream.Collectors;

public class MultipleAlignment {

    private static final int EMPTY_ID = PhoneticSymbolTable.EMPTY_ID;
    public static final String UNKNOWN_SYMBOL = PhoneticSymbolTable.UNKNOWN_SYMBOL;

    private String[] langs;
    private int[][] msa;
    private boolean hasUnattested;

    private PhoneticSymbolTable symbolTable;

    public MultipleAlignment(String[] langs, int[][] msa, PhoneticSymbolTable symbolTable) {
        this.langs = langs;
        this.msa = msa;
        this.hasUnattested = false;
        this.symbolTable = symbolTable;
    }

    public MultipleAlignment(PhoneticString root, List<PhoneticString> other, List<String> languages,
                             PhoneticSymbolTable symbols, CorrespondenceModel corrModel) {
        langs = languages.toArray(new String[0]);
        hasUnattested = false;
        symbolTable = symbols;
        List<List<Integer>> algn = new ArrayList<>();

        List<Integer> rootSyms = Arrays.stream(root.segments).boxed().collect(Collectors.toList());
        algn.add(rootSyms);

        for (PhoneticString otherString : other) {
            PhoneticStringAlignment alignment = NeedlemanWunschAlgorithm.constructAlignment(
                    root, otherString, corrModel, corrModel, corrModel, corrModel);
//            System.out.println(alignment.getLength() + " " + alignment.toString(symbols));
            List<Integer> syms = new ArrayList<>();
            int a = 0;
            for (int j = 0; j < alignment.getLength(); j++) {
                int symPair = alignment.getSymbolPairIDAtPos(j, symbols);
                int inSym = symPair / symbols.getSize();
                int outSym = symPair % symbols.getSize();
                if (a >= rootSyms.size() || inSym != rootSyms.get(a)) {
                    if (inSym == EMPTY_ID) {
                        for (List<Integer> otherSyms : algn)
                            otherSyms.add(a, EMPTY_ID);
                        syms.add(outSym);
                    } else {
                        syms.add(EMPTY_ID);
                        j--;
                    }
                } else {
                    syms.add(outSym);
                }
                a++;
            }
            while (syms.size() < rootSyms.size())
                syms.add(EMPTY_ID);
            algn.add(syms);
        }

        msa = new int[algn.size()][rootSyms.size()];
        for (int i = 0; i < algn.size(); i++) {
            List<Integer> alignment = algn.get(i);
            for (int j = 0; j < alignment.size(); j++) {
                msa[i][j] = alignment.get(j);
            }
        }
    }

    public void orderAndFill(List<String> languages) {
        Map<String, Integer> id2lang = new HashMap<>();
        for (int i = 0; i < langs.length; i++)
            id2lang.put(langs[i], i);

        String[] newLangs = new String[languages.size()];
        int[][] newMsa = new int[languages.size()][];
        for (int i = 0; i < languages.size(); i++) {
            String lang = languages.get(i);
            newLangs[i] = lang;
            if (id2lang.containsKey(lang))
                newMsa[i] = msa[id2lang.get(lang)];
            else {
                int[] empty = new int[msa[0].length];
                Arrays.fill(empty, -1);
                newMsa[i] = empty;
                hasUnattested = true;
            }
        }

        langs = newLangs;
        msa = newMsa;
    }

    private String decode(int sym) {
        if (sym < 0)
            return UNKNOWN_SYMBOL;
        else
            return symbolTable.toSymbol(sym);
    }

    public int nOfLanguages() {
        return langs.length;
    }

    public int stringLength() {
        return msa[0].length;
    }

    public boolean hasUnattestedForms() {
        return hasUnattested;
    }

    public Iterator<String[]> getAlignmentsByForm() {
        return new AlignmentRowIterator();
    }

    public Iterator<String[]> getAlignmentsByForm(boolean returnUnattested) {
        return new AlignmentRowIterator(returnUnattested);
    }

    public Iterator<String[]> getAlignmentsBySegment() {
        return new AlignmentColumnIterator();
    }

    public String getLanguage(int i) {
        return (i >= 0 && i < langs.length) ? langs[i] : "???";
    }

    public int getSymbolID(int word, int position) {
        return msa[word][position];
    }

    public String getSymbol(int word, int position) {
        return decode(msa[word][position]);
    }

    public boolean hasGapAt(int word, int position) {
        return msa[word][position] == EMPTY_ID;
    }

    public int getOriginalPosition(int word, int position) {
        if (position == 0)
            return 0;
        int i = -1;
        int j = 0;
        while (j <= position) {
            if (msa[word][j] != EMPTY_ID)
                i++;
            j++;
        }
        return i;
    }

    public int[] getColumn(int c) {
        int[] col = new int[langs.length];
        for (int i = 0; i < langs.length; i++)
            col[i] = msa[i][c];
        return col;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean includeUnattested) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < msa.length; i++) {
            if (includeUnattested || msa[i][0] >= 0) {
                for (int j = 0; j < msa[0].length; j++) {
                    sb.append(decode(msa[i][j])).append(' ');
                }
                sb.setCharAt(sb.length() - 1, '\n');
            }
        }
        return sb.toString();
    }

    private class AlignmentRowIterator implements Iterator<String[]> {

        private int i;
        private boolean unattested;

        public AlignmentRowIterator() {
            this(true);
        }

        public AlignmentRowIterator(boolean returnUnattested) {
            unattested = returnUnattested;
            i = 0;
//            if (!unattested)
//                skip();
        }

        @Override
        public boolean hasNext() {
            return i < msa.length;
        }

        @Override
        public String[] next() {
            while (!unattested && msa[i][0] < 0)
                skip();
            String[] algn = new String[msa[i].length];
            for (int j = 0; j < algn.length; j++) {
                algn[j] = decode(msa[i][j]);
            }
            i++;
//            if (!unattested)
//                skip();
            return algn;
        }

        private void skip() {
            while (hasNext() && msa[i][0] < 0)
                i++;
        }
    }

    private class AlignmentColumnIterator implements Iterator<String[]> {

        private int j;

        public AlignmentColumnIterator() {
            j = 0;
        }

        @Override
        public boolean hasNext() {
            return j < msa[0].length;
        }

        @Override
        public String[] next() {
            String[] algn = new String[msa.length];
            for (int i = 0; i < algn.length; i++) {
                algn[i] = decode(msa[i][j]);
            }
            j++;
            return algn;
        }
    }
}
