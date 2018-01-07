package de.jdellert.iwsa.infomodel;

import de.jdellert.iwsa.data.LexicalDatabase;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

public class InformationModelInference {
	public static InformationModel[] inferInformationModels(LexicalDatabase database, PhoneticSymbolTable symbolTable) {
		InformationModel[] infoModels = new InformationModel[database.getNumLanguages()];
		for (int langID = 0; langID < database.getNumLanguages(); langID++) {
			infoModels[langID] = inferInformationModelForLanguage(langID, database, symbolTable);
		}
		return infoModels;
	}

	public static InformationModel inferInformationModelForLanguage(int langID, LexicalDatabase database,
			PhoneticSymbolTable symbolTable) {
		InformationModel model = new InformationModel(symbolTable);
		for (int formID : database.getFormIDsForLanguage(langID)) {
			PhoneticString form = database.getForm(formID);
			if (form.getLength() == 0) continue;
			int k = form.getLength() - 1;
			model.addTrigramObservation(0, 0, form.segments[0]);
			model.addTrigramObservation(form.segments[k], 0, 0);
			if (k == 0) {
				model.addTrigramObservation(0, form.segments[0], 0);
			} else {
				model.addTrigramObservation(0, form.segments[0], form.segments[1]);
				model.addTrigramObservation(form.segments[k - 1], form.segments[k], 0);
			}
			for (int i = 0; i < k - 1; i++) {
				model.addTrigramObservation(form.segments[i], form.segments[i + 1], form.segments[i + 2]);
			}
		}
		return model;
	}
}
