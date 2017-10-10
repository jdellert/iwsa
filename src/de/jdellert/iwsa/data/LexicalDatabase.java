package de.jdellert.iwsa.data;

import java.util.ArrayList;
import java.util.Map;

import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

/**
 * The object representing a lexicostatistical database in memory.
 * 
 * @author jdellert
 *
 */
public class LexicalDatabase {
	private String[] langCodes;
	private Map<String,Integer> langCodeToID;
	
	private String[] conceptNames;
	private Map<String,Integer> conceptNameToID;
	
	//core of the database: phonetic forms with symbol table for representation
	private ArrayList<PhoneticString> forms;
	private PhoneticSymbolTable symbolTable;
	
	//indexes into the forms
	
	
	//cognate set ID => {set of form IDs}
	private int[][] cognateSets;
	//form ID => cognate set ID
	private int[] cognateSetForForm;
	

	
	
}
