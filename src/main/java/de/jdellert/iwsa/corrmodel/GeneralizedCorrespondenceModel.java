package de.jdellert.iwsa.corrmodel;

import de.jdellert.iwsa.sequence.GeneralizedPhoneticSymbolTable;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;

public class GeneralizedCorrespondenceModel extends CorrespondenceModel {

    public GeneralizedCorrespondenceModel() {
        //TODO: wrap around a PmiScoreModel, emulate interface of CorrespondenceModel, cache lookup results
        //TODO: put the GeneralizedPhoneticSymbolTable into this as the PhoneticSymbolTable
        super(new GeneralizedPhoneticSymbolTable());
    }
}
