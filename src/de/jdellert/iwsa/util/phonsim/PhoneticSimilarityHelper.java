package de.jdellert.iwsa.util.phonsim;

import de.jdellert.iwsa.align.NeedlemanWunschAlgorithm;
import de.jdellert.iwsa.align.PhoneticStringAlignment;
import de.jdellert.iwsa.corrmodel.CorrespondenceModel;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.tokenize.IPATokenizer;
import de.tuebingen.sfs.cldfjava.data.CLDFForm;

import java.util.Arrays;
import java.util.List;

public class PhoneticSimilarityHelper {

	IPATokenizer ipaTokenizer;
	CorrespondenceModel corrModel;

	public PhoneticSimilarityHelper(IPATokenizer ipaTokenizer, CorrespondenceModel corrModel) {
		this.ipaTokenizer = ipaTokenizer;
		this.corrModel = corrModel;
	}

	public double similarity(CLDFForm lang1Form, CLDFForm lang2Form) {
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

	public PhoneticString extractSegments(CLDFForm form) {
		if (form == null)
			return new PhoneticString(new int[0]);
		if (ipaTokenizer == null) {
			return new PhoneticString(corrModel.getSymbolTable().encode(form.getSegments()));
		} else
			return new PhoneticString(corrModel.getSymbolTable().encode(ipaTokenizer.tokenizeIPA(form.getForm())));
	}

	public CorrespondenceModel getCorrModel() {
		return corrModel;
	}

	public void setCorrModel(CorrespondenceModel corrModel) {
		this.corrModel = corrModel;
	}

}
