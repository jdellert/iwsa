package de.jdellert.iwsa;

import java.io.IOException;

import de.jdellert.iwsa.data.CLDFImport;
import de.jdellert.iwsa.data.LexicalDatabase;
import de.jdellert.iwsa.infomodel.InformationModel;
import de.jdellert.iwsa.infomodel.InformationModelInference;
import de.jdellert.iwsa.sequence.PhoneticString;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;
import de.jdellert.iwsa.sequence.ipa.IpaSymbolInformation;
import de.jdellert.iwsa.util.io.Formatting;

public class InformationWeightOutputLaTeX {
	public static void main(String[] args) {
		try {
			CLDFImport.VERBOSE = false;
			LexicalDatabase database = CLDFImport.loadDatabase(args[0], true);
			PhoneticSymbolTable symbolTable = database.getSymbolTable();

			String langCode = args[1];
			int langID = database.getIDForLanguageCode(langCode);
			if (langID == -1) {
				System.err.println("ERROR: language code " + langCode + " not represented in database!");
				System.exit(1);
			}

			InformationModel infoModel = InformationModelInference.inferInformationModelForLanguage(langID, database,
					symbolTable);
			
			System.out.println("\\documentclass{article}");
			System.out.println("");
			System.out.println("\\usepackage{xcolor}");
			System.out.println("\\usepackage{longtable}");
			System.out.println("\\usepackage{tipa}");
			System.out.println("\\expandafter\\def\\csname ver@xunicode.sty\\endcsname{}");
			System.out.println("\\let\\ipa\\textipa");
			System.out.println("");
			System.out.println("\\begin{document}");
			System.out.println("");
			System.out.println("\\begin{longtable}{|l|l|}");
		    System.out.println("\\hline");
			
			for (int formID : database.getFormIDsForLanguage(langID)) {
				PhoneticString form = database.getForm(formID);
				int[] s = form.segments;
				StringBuilder formString = new StringBuilder();

				
				formString.append("\\ipa{");
				for (int i = 0; i < s.length; i++)
				{
					formString.append(IpaSymbolInformation.getTipaForSymbol(symbolTable.toSymbol(s[i])));
				}
				formString.append("}");
				
				StringBuilder infoString = new StringBuilder();
				for (int i = 0; i < s.length; i++)
				{
					double infoVal = infoModel.informationContent(s, i);
					
					infoString.append("{\\color[gray]{" + (1 - (infoVal / 6.0)) + "}\\ipa{");
					infoString.append(IpaSymbolInformation.getTipaForSymbol(symbolTable.toSymbol(s[i])));
					infoString.append("} }");
				}
				System.out.println(formString + " & " + infoString + "\\\\");
				System.out.println();
			}
			
		    System.out.println("\\hline");
		    System.out.println("\\end{longtable}");
		    System.out.println("");
			System.out.println("\\end{document}");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
