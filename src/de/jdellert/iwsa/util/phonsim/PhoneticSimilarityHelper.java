package de.jdellert.iwsa.util.phonsim;

import de.jdellert.iwsa.align.NeedlemanWunschAlgorithm;
import de.jdellert.iwsa.align.PhoneticStringAlignment;
import de.jdellert.iwsa.corrmodel.CorrespondenceModel;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.tokenize.IPATokenizer;
import de.tuebingen.sfs.eie.core.IndexedObjectStore;

import java.util.Arrays;
import java.util.List;

public class PhoneticSimilarityHelper {

	IPATokenizer ipaTokenizer;
	CorrespondenceModel corrModel;
	IndexedObjectStore objectStore;

	public PhoneticSimilarityHelper(IPATokenizer ipaTokenizer, CorrespondenceModel corrModel, IndexedObjectStore objectStore) {
		this.ipaTokenizer = ipaTokenizer;
		this.corrModel = corrModel;
		this.objectStore = objectStore;
	}

	public double similarity(int lang1Form, int lang2Form) {
		return similarity(extractSegments(lang1Form), extractSegments(lang2Form));
	}

	public double similarity(PhoneticString form1, PhoneticString form2) {
		double sim = 1.0;
		if (!form1.equals(form2)) {
			PhoneticStringAlignment align = NeedlemanWunschAlgorithm.constructAlignment(form1, form2, corrModel,
					corrModel, corrModel, corrModel);
			sim = 1 / (1 + Math.exp(-align.alignmentScore));
		}
		return sim;
	}

	public PhoneticString extractSegments(int form) {
		if (form == -1)
			return new PhoneticString(new int[0]);
		if (ipaTokenizer == null) {
			return new PhoneticString(corrModel.getSymbolTable().encode(objectStore.getSegmentsForForm(form)));
		} else
			return new PhoneticString(corrModel.getSymbolTable().encode(ipaTokenizer.tokenizeIPA(objectStore.getFormForFormId(form))));
	}

	public CorrespondenceModel getCorrModel() {
		return corrModel;
	}

	public void setCorrModel(CorrespondenceModel corrModel) {
		this.corrModel = corrModel;
	}

}
