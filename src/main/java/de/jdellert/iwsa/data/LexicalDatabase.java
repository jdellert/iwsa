package de.jdellert.iwsa.data;

import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The object representing a lexicostatistical database in memory.
 *
 * @author jdellert
 */
public class LexicalDatabase {
    // model of main filters: languages and concepts
    public String[] langCodes;
    public String[] conceptNames;
    // lang ID => (concept ID => {set of form IDs})
    public List<List<List<Integer>>> langAndConceptToForms;
    // core of the database: phonetic forms with symbol table for representation
    protected PhoneticSymbolTable symbolTable;
    // form ID => preprocessed phonetic string for efficient alignment
    protected ArrayList<PhoneticString> forms;
    // form annotations (used e.g. for orthography, loanword status)
    protected Map<String, List<String>> annotations;
    protected Map<String, Integer> langCodeToID;
    protected Map<String, Integer> conceptNameToID;
    // indexes into the forms by the two filters
    // form ID => lang ID
    protected ArrayList<Integer> formToLang;
    // concept ID => lang ID
    protected ArrayList<Integer> formToConcept;
    // cognacy model (allows cross-semantic cognate classes)
    // cognate set ID => {set of form IDs}
    protected ArrayList<List<Integer>> cognateSets;
    // form ID => cognate set ID
    protected ArrayList<Integer> cognateSetForForm;

    public LexicalDatabase(PhoneticSymbolTable symbolTable, String[] langs, String[] concepts) {
        this(symbolTable, langs, concepts, langs.length * concepts.length);
    }

    public LexicalDatabase(PhoneticSymbolTable symbolTable, String[] langs, String[] concepts, int numForms) {
        this.symbolTable = symbolTable;
        this.forms = new ArrayList<PhoneticString>(numForms);

        this.annotations = new TreeMap<String, List<String>>();

        this.langCodes = langs;
        this.langCodeToID = new TreeMap<String, Integer>();
        for (int langID = 0; langID < this.langCodes.length; langID++) {
            this.langCodeToID.put(this.langCodes[langID], langID);
        }

        this.conceptNames = concepts;
        this.conceptNameToID = new TreeMap<String, Integer>();
        for (int conceptID = 0; conceptID < this.conceptNames.length; conceptID++) {
            this.conceptNameToID.put(this.conceptNames[conceptID], conceptID);
        }

        this.formToLang = new ArrayList<Integer>(numForms);
        this.formToConcept = new ArrayList<Integer>(numForms);
        this.langAndConceptToForms = new ArrayList<List<List<Integer>>>(this.langCodes.length);
        for (int langID = 0; langID < this.langCodes.length; langID++) {
            List<List<Integer>> conceptToForms = new ArrayList<List<Integer>>();
            for (int conceptID = 0; conceptID < this.conceptNames.length; conceptID++) {
                conceptToForms.add(new ArrayList<Integer>());
            }
            this.langAndConceptToForms.add(conceptToForms);
        }

        this.cognateSets = new ArrayList<List<Integer>>();
        this.cognateSetForForm = new ArrayList<Integer>(numForms);
        for (int i = 0; i < numForms; i++) {
            cognateSetForForm.add(0);
        }
    }

    public PhoneticSymbolTable getSymbolTable() {
        return symbolTable;
    }

    public int addForm(String langCode, String conceptName, PhoneticString form) {
        forms.add(form);

        int formID = forms.size() - 1;
        int langID = langCodeToID.get(langCode);
        int conceptID = conceptNameToID.get(conceptName);

        formToLang.add(langID);
        formToConcept.add(conceptID);
        langAndConceptToForms.get(langID).get(conceptID).add(formID);

        return formID;
    }

    public List<Integer> getFormIDsForLanguage(int langID) {
        List<Integer> result = new ArrayList<Integer>();
        for (List<Integer> formList : langAndConceptToForms.get(langID)) {
            result.addAll(formList);
        }
        return result;
    }

    public List<Integer> getFormIDsForConcept(int conceptID) {
        List<Integer> result = new ArrayList<Integer>();
        for (int langID = 0; langID < langCodes.length; langID++) {
            result.addAll(getFormIDsForLanguageAndConcept(langID, conceptID));
        }
        return result;
    }

    public List<Integer> getFormIDsForLanguageAndConcept(int langID, int conceptID) {
        return langAndConceptToForms.get(langID).get(conceptID);
    }

    public List<List<Integer>> getFormIDsForConceptPerLanguage(int conceptID) {
        List<List<Integer>> result = new ArrayList<List<Integer>>(langCodes.length);
        for (int langID = 0; langID < langCodes.length; langID++) {
            result.add(getFormIDsForLanguageAndConcept(langID, conceptID));
        }
        return result;
    }

    public PhoneticString getForm(int formID) {
        return forms.get(formID);
    }

    public List<Integer> lookupFormIDs(String langCode, String conceptName) {
        int langID = langCodeToID.get(langCode);
        int conceptID = conceptNameToID.get(conceptName);
        return getFormIDsForLanguageAndConcept(langID, conceptID);
    }

    public int getNumConcepts() {
        return conceptNames.length;
    }

    public String getConceptName(int conceptID) {
        return conceptNames[conceptID];
    }

    public String getConceptNameForForm(int formID) {
        return conceptNames[formToConcept.get(formID)];
    }

    public int getNumLanguages() {
        return langCodes.length;
    }

    public String getLanguageCode(int langID) {
        return langCodes[langID];
    }

    public String[] getLanguageCodes() {
        return Arrays.copyOf(langCodes, langCodes.length);
    }

    public String getLanguageCodeForForm(int formID) {
        return langCodes[formToLang.get(formID)];
    }

    public int getNumForms() {
        return forms.size();
    }

    /**
     * Gets the internal language ID for a given language code.
     *
     * @param langCode
     * @return the internal ID of that language, or -1 if language code is not in
     * database.
     */
    public int getIDForLanguageCode(String langCode) {
        Integer langID = langCodeToID.get(langCode);
        if (langID == null)
            langID = -1;
        return langID;
    }

    public String getAnnotation(String field, int formID) {
        List<String> annotationsPerForm = annotations.get(field);
        if (annotationsPerForm == null)
            return "?";
        return annotationsPerForm.get(formID);
    }

    public void addAnnotation(int formID, String field, String value) {
        List<String> annotationsPerForm = annotations.get(field);
        if (annotationsPerForm == null) {
            annotationsPerForm = new ArrayList<String>(formToLang.size());
            annotations.put(field, annotationsPerForm);
        }
        String unknown = "?";
        while (annotationsPerForm.size() <= formID) {
            annotationsPerForm.add(unknown);
        }
        annotationsPerForm.set(formID, value);
    }

    public PhoneticString getRandomForm() {
        return forms.get((int) (Math.random() * forms.size()));
    }

    public PhoneticString getRandomFormForLanguage(int langID) {
        List<List<Integer>> conceptToFormsForLang = langAndConceptToForms.get(langID);
        List<Integer> formsForRandomConcept = conceptToFormsForLang
                .get((int) (Math.random() * conceptToFormsForLang.size()));
        while (formsForRandomConcept.size() == 0) {
            formsForRandomConcept = conceptToFormsForLang.get((int) (Math.random() * conceptToFormsForLang.size()));
        }
        return forms.get(formsForRandomConcept.get((int) (Math.random() * formsForRandomConcept.size())));
    }

    public PhoneticString getFormForLangConceptOrth(String lang, String concept, String orth) {
        int langID = getIDForLanguageCode(lang);
        if (langID == -1)
            return null;
        Integer conceptID = conceptNameToID.get(concept);
        if (conceptID == null)
            return null;
        for (int formID : getFormIDsForLanguageAndConcept(langID, conceptID)) {
            String formOrth = getAnnotation("Word_Form", formID);
            if (formOrth.equals(orth))
                return getForm(formID);
        }
        return null;
    }

    public int getCognateSetID(int formID) {
        return cognateSetForForm.get(formID);
    }

    public void addCognateSet(List<Integer> cognateSetFormIDs) {
        int cognateSetID = cognateSets.size() + 1;
        for (int formID : cognateSetFormIDs) {
            cognateSetForForm.set(formID, cognateSetID);
        }
        cognateSets.add(cognateSetFormIDs);
    }
}
