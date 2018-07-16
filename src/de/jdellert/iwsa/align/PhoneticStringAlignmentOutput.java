package de.jdellert.iwsa.align;

import de.jdellert.iwsa.corrmodel.CorrespondenceModel;
import de.jdellert.iwsa.infomodel.InformationModel;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;
import de.jdellert.iwsa.util.io.Formatting;

public class PhoneticStringAlignmentOutput {
	public static String lineToString(PhoneticStringAlignment alignment, PhoneticSymbolTable table, int index) {
		StringBuilder stringLine = new StringBuilder();

		for (int pos = 0; pos < alignment.getLength(); pos++) {
			if (index == 1) {
				int symb1 = alignment.str1.segments[pos];
				stringLine.append(table.toSymbol(symb1) + " ");
			}
			if (index == 2) {
				int symb2 = alignment.str2.segments[pos];
				stringLine.append(table.toSymbol(symb2) + " ");
			}
		}

		return stringLine.toString();
	}

	public static String needlemanWunschtoString(PhoneticStringAlignment alignment, PhoneticSymbolTable table,
			CorrespondenceModel gloCorrModel, CorrespondenceModel locCorrModel, CorrespondenceModel selfSimModel1,
			CorrespondenceModel selfSimModel2) {
		StringBuilder selfSim1Line = new StringBuilder();
		StringBuilder string1Line = new StringBuilder();
		StringBuilder corrLine = new StringBuilder();
		StringBuilder string2Line = new StringBuilder();
		StringBuilder selfSim2Line = new StringBuilder();

		double selfSimScoreSum1 = 0.0;
		double selfSimScoreSum2 = 0.0;
		double corrScoreSum = 0.0;

		int l1 = 0;
		int l2 = 0;

		for (int pos = 0; pos < alignment.getLength(); pos++) {
			int symb1 = alignment.str1.segments[pos];
			int symb2 = alignment.str2.segments[pos];

			if (symb1 != 1)
				l1++;
			if (symb2 != 1)
				l2++;

			string1Line.append(table.toSymbol(symb1) + "\t");
			string2Line.append(table.toSymbol(symb2) + "\t");

			double selfSimScore1 = selfSimModel1.getScore(symb1, symb1);
			double selfSimScore2 = selfSimModel2.getScore(symb2, symb2);
			double corrScore = getCorrespondenceScore(gloCorrModel, locCorrModel, symb1, symb2);

			selfSimScoreSum1 += selfSimScore1;
			selfSimScoreSum2 += selfSimScore2;
			corrScoreSum += corrScore;

			selfSim1Line.append(Formatting.str3f(selfSimScore1) + "\t");
			selfSim2Line.append(Formatting.str3f(selfSimScore2) + "\t");
			corrLine.append(Formatting.str3f(corrScore) + "\t");
		}

		selfSim1Line.append(Formatting.str3f(selfSimScoreSum1));
		selfSim2Line.append(Formatting.str3f(selfSimScoreSum2));
		corrLine.append(Formatting.str3f(corrScoreSum));

		selfSimScoreSum1 /= l1;
		selfSimScoreSum2 /= l2;
		corrScoreSum /= alignment.getLength();

		selfSim1Line.append("\t" + Formatting.str3f(selfSimScoreSum1));
		selfSim2Line.append("\t" + Formatting.str3f(selfSimScoreSum2));
		corrLine.append("\t" + Formatting.str3f(corrScoreSum));

		double normalizedDistanceScore = 1 - (2 * corrScoreSum) / (selfSimScoreSum1 + selfSimScoreSum2);
		corrLine.append("\t" + Formatting.str3f(normalizedDistanceScore));

		selfSim1Line.append("\n");
		string1Line.append("\n");
		corrLine.append("\n");
		string2Line.append("\n");
		selfSim2Line.append("\n");

		return selfSim1Line.toString() + string1Line.toString() + corrLine.toString() + string2Line.toString()
				+ selfSim2Line.toString();
	}

	public static String iwsaToString(PhoneticStringAlignment alignment, PhoneticSymbolTable table,
			CorrespondenceModel gloCorrModel, CorrespondenceModel locCorrModel, CorrespondenceModel selfSimModel1,
			CorrespondenceModel selfSimModel2, InformationModel infoModel1, InformationModel infoModel2) {
		StringBuilder informationModel1Line = new StringBuilder();
		StringBuilder selfSim1Line = new StringBuilder();
		StringBuilder string1Line = new StringBuilder();
		StringBuilder corrLine = new StringBuilder();
		StringBuilder string2Line = new StringBuilder();
		StringBuilder selfSim2Line = new StringBuilder();
		StringBuilder informationModel2Line = new StringBuilder();

		double selfSimScoreSum1 = 0.0;
		double selfSimScoreSum2 = 0.0;
		double corrScoreSum = 0.0;

		int pos1 = -1;
		int pos2 = -1;

		PhoneticString str1Reduced = alignment.str1.copyWithoutGaps();
		PhoneticString str2Reduced = alignment.str2.copyWithoutGaps();

		int l1 = 0;
		int l2 = 0;

		for (int pos = 0; pos < alignment.getLength(); pos++) {
			int symb1 = alignment.str1.segments[pos];
			int symb2 = alignment.str2.segments[pos];

			if (symb1 > 1) {
				pos1++;
				l1++;
			}
			if (symb2 > 1) {
				pos2++;
				l2++;
			}

			string1Line.append(table.toSymbol(symb1) + "\t");
			string2Line.append(table.toSymbol(symb2) + "\t");

			double infoScore1 = 0.0;
			if (symb1 > 1)
				infoScore1 = InformationWeightedSequenceAlignment.getInfoScore(str1Reduced, pos1, infoModel1);
			double infoScore2 = 0.0;
			if (symb2 > 1)
				infoScore2 = InformationWeightedSequenceAlignment.getInfoScore(str2Reduced, pos2, infoModel2);

			double selfSimScore1 = getCorrespondenceScore(gloCorrModel, selfSimModel1, symb1, symb1);
			double selfSimScore2 = getCorrespondenceScore(gloCorrModel, selfSimModel2, symb2, symb2);
			double corrScore = getCorrespondenceScore(gloCorrModel, locCorrModel, symb1, symb2);

			if (symb1 == 1) {
				corrScoreSum += corrScore * InformationWeightedSequenceAlignment.getMeanInfoScore(str2Reduced,
						str2Reduced, pos2, pos2, infoModel2, infoModel2);
				selfSimScoreSum2 += selfSimScore2 * InformationWeightedSequenceAlignment.getMeanInfoScore(str2Reduced,
						str2Reduced, pos2, pos2, infoModel2, infoModel2);
			} else if (symb2 == 1) {
				corrScoreSum += corrScore * InformationWeightedSequenceAlignment.getMeanInfoScore(str1Reduced,
						str1Reduced, pos1, pos1, infoModel1, infoModel1);
				selfSimScoreSum1 += selfSimScore1 * InformationWeightedSequenceAlignment.getMeanInfoScore(str1Reduced,
						str1Reduced, pos1, pos1, infoModel1, infoModel1);
			} else {
				corrScoreSum += corrScore * InformationWeightedSequenceAlignment.getMeanInfoScore(str1Reduced,
						str2Reduced, pos1, pos2, infoModel1, infoModel2);
				selfSimScoreSum1 += selfSimScore1 * InformationWeightedSequenceAlignment.getMeanInfoScore(str1Reduced,
						str1Reduced, pos1, pos1, infoModel1, infoModel1);
				selfSimScoreSum2 += selfSimScore2 * InformationWeightedSequenceAlignment.getMeanInfoScore(str2Reduced,
						str2Reduced, pos2, pos2, infoModel2, infoModel2);
			}

			informationModel1Line.append(Formatting.str3f(infoScore1) + "\t");
			informationModel2Line.append(Formatting.str3f(infoScore2) + "\t");
			selfSim1Line.append(Formatting.str3f(selfSimScore1) + "\t");
			selfSim2Line.append(Formatting.str3f(selfSimScore2) + "\t");
			corrLine.append(Formatting.str3f(corrScore) + "\t");

		}

		selfSim1Line.append(Formatting.str3f(selfSimScoreSum1) + "\t");
		selfSim2Line.append(Formatting.str3f(selfSimScoreSum2) + "\t");
		corrLine.append(Formatting.str3f(corrScoreSum) + "\t");

		selfSimScoreSum1 /= l1;
		selfSimScoreSum2 /= l2;
		corrScoreSum /= alignment.getLength();

		selfSim1Line.append(Formatting.str3f(selfSimScoreSum1));
		selfSim2Line.append(Formatting.str3f(selfSimScoreSum2));
		corrLine.append(Formatting.str3f(corrScoreSum));

		double normalizedDistanceScore = 1 - (2 * corrScoreSum) / (selfSimScoreSum1 + selfSimScoreSum2);
		corrLine.append("\t" + Formatting.str3f(normalizedDistanceScore));

		informationModel1Line.append("\n");
		selfSim1Line.append("\n");
		string1Line.append("\n");
		corrLine.append("\n");
		string2Line.append("\n");
		selfSim2Line.append("\n");
		informationModel2Line.append("\n");

		return informationModel1Line.toString() + selfSim1Line.toString() + string1Line.toString() + corrLine.toString()
				+ string2Line.toString() + selfSim2Line.toString() + informationModel2Line.toString();
	}

	public static double getCorrespondenceScore(CorrespondenceModel gloCorrModel, CorrespondenceModel locCorrModel,
			int ci, int cj) {
		Double score = locCorrModel.getScoreOrNull(ci, cj);
		if (score == null) {
			score = gloCorrModel.getScoreOrNull(ci, cj);
		}
		if (score == null) {
			score = 0.0;
		}
		return score;
	}
}
