package de.jdellert.iwsa.bootstrap;

import java.util.ArrayList;
import java.util.List;

import de.jdellert.iwsa.data.LexicalDatabase;
import de.jdellert.iwsa.sequence.PhoneticString;

/**
 * This class implements a wrapper around a lexical database which acts just as the original database,
 * except that concept lookup (by ID, not by name!) is redirected through a table which implements a bootstrap sample.
 * 
 * @author jdellert
 *
 */
public class LexicalDatabaseConceptBootstrapSample extends LexicalDatabase {
	LexicalDatabase origDatabase;
	int[] conceptIDToSampledID;
	
	public LexicalDatabaseConceptBootstrapSample(LexicalDatabase origDatabase) {
		super(origDatabase.getSymbolTable(), origDatabase.langCodes, origDatabase.conceptNames);
		this.origDatabase = origDatabase;
		
		//resample concepts with replacement, maintaining the sample-specific mapping in conceptIDToSampledID
		int numConcepts = origDatabase.getNumConcepts();
		conceptIDToSampledID = new int[numConcepts];
		for (int i = 0; i < numConcepts; i++) {
			conceptIDToSampledID[i] = (int) (Math.random() * numConcepts);
		}
	}
	
	//blocked methods
	public int addForm(String langCode, String conceptName, PhoneticString form) {
		System.err.println("ERROR: attempting to add a form to a bootstrap sample of a database, this should not happen!");
		return -1;
	}
	
	public void addCognateSet(List<Integer> cognateSetFormIDs) {
		System.err.println("ERROR: attempting to add a cognate set to a bootstrap sample of a database, this should not happen!");
	}
	
	public void addAnnotation(int formID, String field, String value) {
		System.err.println("ERROR: attempting to add an annotation to a bootstrap sample of a database, this should not happen!");
	}
	
	//modified methods: redirect concept lookup via the sample ID table
	
	public List<Integer> getFormIDsForLanguageAndConcept(int langID, int conceptID) {
		return origDatabase.langAndConceptToForms.get(langID).get(conceptIDToSampledID[conceptID]);
	}

	public String getConceptName(int conceptID) {
		return origDatabase.getConceptName(conceptIDToSampledID[conceptID]);
	}
	
	//methods which retrieve concepts by ID and need to partially rely on the wrapper methods
	//(these would not actually be necessary, but code is repeated here for clarity)

	public List<Integer> getFormIDsForConcept(int conceptID) {
		List<Integer> result = new ArrayList<Integer>();
		for (int langID = 0; langID < langCodes.length; langID++) {
			result.addAll(getFormIDsForLanguageAndConcept(langID, conceptID));
		}
		return result;
	}
	
	public List<List<Integer>> getFormIDsForConceptPerLanguage(int conceptID) {
		List<List<Integer>> result = new ArrayList<List<Integer>>(langCodes.length);
		for (int langID = 0; langID < langCodes.length; langID++) {
			result.add(getFormIDsForLanguageAndConcept(langID, conceptID));
		}
		return result;
	}
	
	//methods which can directly be handed on to the wrapped database
	
	public List<Integer> getFormIDsForLanguage(int langID) {
		return origDatabase.getFormIDsForLanguage(langID);
	}
	
	public PhoneticString getForm(int formID) {
		return origDatabase.getForm(formID);
	}
	
	public List<Integer> lookupFormIDs(String langCode, String conceptName) {
		return origDatabase.lookupFormIDs(langCode, conceptName);
	}
	
	public String getConceptNameForForm(int formID) {
		return origDatabase.getConceptNameForForm(formID);
	}
	
	public String getLanguageCodeForForm(int formID) {
		return origDatabase.getLanguageCodeForForm(formID);
	}
	
	public int getNumForms() {
		return origDatabase.getNumForms();
	}
	
	public int getIDForLanguageCode(String langCode) {
		return origDatabase.getIDForLanguageCode(langCode);
	}
	
	public PhoneticString getRandomForm() {
		return origDatabase.getRandomForm();
	}
	
	public PhoneticString getRandomFormForLanguage(int langID) {
		return origDatabase.getRandomFormForLanguage(langID);
	}
	
	public PhoneticString getFormForLangConceptOrth(String lang, String concept, String orth) {
		return origDatabase.getFormForLangConceptOrth(lang, concept, orth);
	}
	
	public int getCognateSetID(int formID) {
		return origDatabase.getCognateSetID(formID);
	}
	
	//all other (minor) methods can be inherited because they operate only on symbolTable, langCodes, or conceptNames,
	//i.e. the objects references to which had to be copied in order to be able to inherit from LexicalDatabase
	
}
