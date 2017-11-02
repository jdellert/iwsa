package de.jdellert.iwsa.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

/**
 * The object representing a lexicostatistical database in memory.
 * 
 * @author jdellert
 *
 */
public class LexicalDatabase {
	// core of the database: phonetic forms with symbol table for representation
	private PhoneticSymbolTable symbolTable;

	// form ID => preprocessed phonetic string for efficient alignment
	private ArrayList<PhoneticString> forms;

	// form annotations (used e.g. for orthography, loanword status)
	private Map<String, List<String>> annotations;

	// model of main filters: languages and concepts
	private String[] langCodes;
	private Map<String, Integer> langCodeToID;
	private String[] conceptNames;
	private Map<String, Integer> conceptNameToID;

	// indexes into the forms by the two filters
	// form ID => lang ID
	private ArrayList<Integer> formToLang;
	// concept ID => lang ID
	private ArrayList<Integer> formToConcept;
	// lang ID => (concept ID => {set of form IDs})
	private List<List<List<Integer>>> langAndConceptToForms;

	// cognacy model (allows cross-semantic cognate classes)
	// cognate set ID => {set of form IDs}
	private ArrayList<List<Integer>> cognateSets;
	// form ID => cognate set ID
	private ArrayList<Integer> cognateSetForForm;

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

	public int getNumLanguages() {
		return langCodes.length;
	}

	public String getLanguageCode(int langID) {
		return langCodes[langID];
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
}
