package de.jdellert.iwsa.align;

import de.jdellert.iwsa.corrmodel.CorrespondenceModel;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

import java.util.*;
import java.util.stream.Collectors;

public class MultipleAlignment {

    public static final String UNKNOWN_SYMBOL = PhoneticSymbolTable.UNKNOWN_SYMBOL;
    private static final int EMPTY_ID = PhoneticSymbolTable.EMPTY_ID;
    private String[] langs;
    private List<Integer> forms;
    private int[][] msa;
    private boolean hasUnattested;

    private PhoneticSymbolTable symbolTable;

    public MultipleAlignment() {
        this.forms = new ArrayList<>();
    }

    public MultipleAlignment(PhoneticSymbolTable symbolTable) {
        this.forms = new ArrayList<>();
        this.symbolTable = symbolTable;
    }

    public MultipleAlignment(String[] langs, int[][] msa, PhoneticSymbolTable symbolTable) {
        this.langs = langs;
        this.msa = msa;
        this.hasUnattested = false;
        this.symbolTable = symbolTable;
    }

    public MultipleAlignment(String[] langs, int[][] msa, List<Integer> forms, PhoneticSymbolTable symbolTable) {
        this.langs = langs;
        this.msa = msa;
        this.forms = forms;
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

    public void fixLangs(Map<Integer, String> formIdsToLangs) {
        List<String> seenLangs = new ArrayList<>();
        for (int i = 0; i < forms.size(); i++) {
            int formId = forms.get(i);
            String lang = formIdsToLangs.get(formId);
            if (seenLangs.contains(lang)) lang += "_" + i;
            seenLangs.add(lang);
        }
        langs = seenLangs.toArray(new String[0]);
    }

    public MultipleAlignment getAlignmentForLangs(List<String> languages) {
        int[][] tmpAlignment = new int[msa.length][msa[0].length];
        String[] tmpLangs = new String[msa.length];
        List<Integer> newForms = new ArrayList<>(msa.length);
        int i = 0; // counting variable for current alignment...
        int idx = 0; // ...and for the new alignment
        for (String lang : langs) {
            if (languages.contains(lang)) {
                tmpAlignment[idx] = msa[i];
                tmpLangs[idx] = lang;
                newForms.add(forms.get(i));
                idx++;
            }
            i++;
        }
        int[][] alignment = Arrays.copyOfRange(tmpAlignment, 0, idx);
        String[] newLangs = Arrays.copyOfRange(tmpLangs, 0, idx);
        return new MultipleAlignment(newLangs, alignment, newForms, symbolTable);
    }

    public void orderAndFill(List<String> languages) {
        //TODO: refactor
        Map<String, Integer> lang2idx = new HashMap<>();
        for (int i = 0; i < langs.length; i++)
            lang2idx.put(langs[i], i);

        String[] newLangs = new String[languages.size()];
        int[][] newMsa = new int[languages.size()][];
        Integer[] newForms = (forms == null || forms.isEmpty()) ? null : new Integer[languages.size()];
        for (int i = 0; i < languages.size(); i++) {
            String lang = languages.get(i);
            newLangs[i] = lang;
            if (lang2idx.containsKey(lang)) {
            	int idx = lang2idx.get(lang);
                newMsa[i] = msa[idx];
            	if (newForms != null) {
            		newForms[i] = forms.get(idx);
            	}
            } else {
                int[] empty = new int[msa[0].length];
                Arrays.fill(empty, -1);
                newMsa[i] = empty;
            	if (newForms != null) {
            		newForms[i] = null;
            	}
                hasUnattested = true;
            }
        }

        langs = newLangs;
        msa = newMsa;
        if (newForms != null) {
        	forms = Arrays.asList(newForms);
        }
    }

    public void setLangs(List<String> languages) {
        this.langs = languages.toArray(new String[0]);
    }

    public void setSymbolTable(PhoneticSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public List<Integer> getFormIds() {
        return this.forms;
    }

    public void setFormIds(List<Integer> formIds) {
        this.forms = formIds;
    }

    public List<Integer> getFormIds(List<String> languages) {
        //TODO: refactor
        Map<String, Integer> lang2idx = new HashMap<>();
        for (int l = 0; l < languages.size(); l++)
            lang2idx.put(languages.get(l), l);
        Integer[] filteredFormIds = new Integer[languages.size()];
        for (int i = 0; i < forms.size(); i++) {
            Integer l = lang2idx.get(langs[i]);
            if (l != null)
                filteredFormIds[l] = forms.get(i);
        }
        return Arrays.asList(filteredFormIds);
    }

    public void addEntry(int formId, String[] form, String lang) {
        int lastInd=forms.size();
        int[][] newMsa = new int[lastInd+1][];
        for(int row=0; row<lastInd+1; row++) {
            if(row==lastInd) {
                newMsa[lastInd] = symbolTable.encode(form);
            } else {
                newMsa[row]=msa[row];
            }
        }
        forms.add(formId);
        List<String> langsList = new ArrayList<>(Arrays.asList(langs));
        langsList.add(lang);
        langs=langsList.toArray(String[]::new);
        msa=newMsa;
    }

    public void replaceEntry(int formId, String[] form) {
        int replaceInd=forms.indexOf(formId);
        int[][] newMsa = new int[msa.length][];
        for(int row=0; row<msa.length; row++) {
            if(row==replaceInd) {
                newMsa[row] = symbolTable.encode(form);
            } else {
                newMsa[row]=msa[row];
            }
        }
        msa=newMsa;
    }

    public void removeEntry(int formId) {
        int toRemove=forms.indexOf(formId);
        List<String> langsList = new ArrayList<>(Arrays.asList(langs));
        List<int[]> msaList=new ArrayList<>(Arrays.asList(msa));
        forms.remove(toRemove);
        langsList.remove(toRemove);
        msaList.remove(toRemove);
        langs=langsList.toArray(String[]::new);
        msa=msaList.toArray(int[][]::new);
    }

    public String[] getEntry(int idx) {
        int[] segments = msa[idx];
        String[] decodedSegments = new String[segments.length];
        for (int i = 0; i < segments.length; i++) {
            decodedSegments[i] = decode(segments[i]);
        }
        return decodedSegments;
    }

    public String[] getEntryByLanguage(String lang) {
        for (int i = 0; i < langs.length; i++) {
            if (langs[i].equals(lang)) {
                return getEntry(i);
            }
        }
        return null;
    }

    public void setAlignments(List<String[]> alignments) {
        msa = new int[alignments.size()][];
        for (int i = 0; i < alignments.size(); i++) {
            msa[i] = symbolTable.encode(alignments.get(i));
        }
    }

    public void changeAlignment(Set<Integer> rowIndices, String[][] newAlignments) {
        if (newAlignments[0].length > msa[0].length) {
            int[][] newMsa = new int[newAlignments.length][newAlignments[0].length];
            for (Integer rowInd : rowIndices) {
                newMsa[rowInd] = symbolTable.encode(newAlignments[rowInd]);
            }
            msa = newMsa;
        } else {
            for (Integer rowInd : rowIndices) {
                msa[rowInd] = symbolTable.encode(newAlignments[rowInd]);
            }
        }
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

    public int size() {
        if (msa == null)
            return 0;
        else
            return msa.length;
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

    public Iterator<String[]> getAlignmentsBySegment(List<String> languages) {
        return new AlignmentColumnIterator(languages);
    }

    public String getLanguage(int i) {
        return (i >= 0 && i < langs.length) ? langs[i] : "???";
    }

    public String[] getLanguages() {
        return langs;
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

    public boolean containsLanguage(String language) {
        if (langs == null) return false;
        for (String l : langs) {
            if (l.equals(language)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAllLanguages(Collection<String> languages) {
        for (String language : languages) {
            if (!containsLanguage(language)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(List<String> languages) {
        return toString(languages, false);
    }

    public String toString(boolean includeUnattested) {
        return toString(null, includeUnattested);
    }

    public String toString(List<String> languages, boolean includeUnattested) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < msa.length; i++) {
            if (includeUnattested || msa[i][0] >= 0) {
                for (int j = 0; j < msa[0].length; j++) {
                    sb.append(decode(msa[i][j])).append(' ');
                }
                if (languages != null)
                    sb.append("(").append(languages.get(i)).append(")\n");
                else
                    sb.setCharAt(sb.length() - 1, '\n');
            }
        }
        return sb.toString();
    }

    public boolean isEmpty() {
        return this.forms.isEmpty();
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
            if (msa == null) return false;
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
        //TODO: refactor
        private int j;
        private Map<String, Integer> lang2idx;

        public AlignmentColumnIterator() {
            this(Arrays.asList(langs));
        }

        public AlignmentColumnIterator(List<String> languages) {
            j = 0;
            lang2idx = new HashMap<>();
            for (int l = 0; l < languages.size(); l++) {
                lang2idx.put(languages.get(l), l);
            }
        }

        @Override
        public boolean hasNext() {
            return j < msa[0].length;
        }

        @Override
        public String[] next() {
            String[] algn = new String[lang2idx.size()];
            // String[] algn = new String[msa.length];
            Arrays.fill(algn, PhoneticSymbolTable.UNKNOWN_SYMBOL);
            for (int i = 0; i < msa.length; i++) {
                Integer l = lang2idx.get(langs[i]);
                if (l != null)
                    algn[l] = decode(msa[i][j]);
                // algn[i] = decode(msa[i][j]);
            }
            j++;
            return algn;
        }
    }
}
