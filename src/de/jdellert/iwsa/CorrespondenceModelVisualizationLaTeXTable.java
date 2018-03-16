package de.jdellert.iwsa;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.jdellert.iwsa.corrmodel.CorrespondenceModel;
import de.jdellert.iwsa.corrmodel.CorrespondenceModelStorage;
import de.jdellert.iwsa.sequence.PhoneticSymbolTable;
import de.jdellert.iwsa.sequence.ipa.IpaSymbolInformation;
import de.jdellert.iwsa.util.ranking.RankingEntry;

/**
 * A program to output the correspondences encoded in a correspondence file as a table
 * with lines visualizing all associations stronger than a PMI threshold given as a parameter.
 * 
 * The result is LaTeX code consisting of a header and a table
 * which can be pasted into any LaTeX document provided the tipa package is installed.
 *
 */

public class CorrespondenceModelVisualizationLaTeXTable {
	public static void main(String[] args)
	{
		if (args.length != 1 && args.length != 3) {
			System.err.println("Usage: CorrespondenceModelVisualizationLaTeXTable [globalCorrespondenceFile]");
			System.err.println("       CorrespondenceModelVisualizationLaTeXTable [localCorrespondenceFile] [langCode1] [langCode2]");
		}
		if (args.length == 1) {
			CorrespondenceModel globalCorrModel = null;
			try {
				globalCorrModel = CorrespondenceModelStorage
						.loadCorrespondenceModel(new ObjectInputStream(new FileInputStream(args[0])));

				PhoneticSymbolTable symbolTable = globalCorrModel.getSymbolTable();
				Set<String> definedSymbols = symbolTable.getDefinedSymbols();
				List<Integer> symbolIDOrder = new LinkedList<Integer>();
				for (String ipaSymbol : IpaSymbolInformation.getKnownSymbolsInOrder())
				{
					Integer id = symbolTable.toInt(ipaSymbol);
					if (id != null)
					{
						symbolIDOrder.add(id);
						definedSymbols.remove(ipaSymbol);
					}
				}
				for (String unknownSymbol : definedSymbols)
				{
					symbolIDOrder.add(symbolTable.toInt(unknownSymbol));
				}
				
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
				System.out.println("\\begin{longtable}{|l|l|l|}");
			    System.out.println("\\hline");
			    for (int symbolID1 : symbolIDOrder)
			    {
			    	String symbol1 = symbolTable.toSymbol(symbolID1); 	
			    	System.out.print("  \\ipa{" + IpaSymbolInformation.getTipaForSymbol(symbol1) + "}" + " & " + IpaSymbolInformation.getDescriptionForSymbol(symbol1) + " & ");
			    	
			    	List<RankingEntry<String>> neighborRanking = new ArrayList<RankingEntry<String>>();
			    	
				    for (int symbolID2 : symbolIDOrder)
				    {
				    	String symbol2 = symbolTable.toSymbol(symbolID2); 	
				    	
			    		if (symbolID1 != symbolID2)
			    		{
			    			neighborRanking.add(new RankingEntry<String>(symbol2, globalCorrModel.getScore(symbolID1, symbolID2)));
			    		}
				    }
				    Comparator<RankingEntry<String>> reversedComparator = Comparator.<RankingEntry<String>>naturalOrder().reversed();
			    	Collections.sort(neighborRanking, reversedComparator);
			    	
			    	System.out.print("\\ipa{");
			    	
			    	int i = 0;
			    	while (i < neighborRanking.size())
			    	{
			    		if (neighborRanking.get(i).value < 1.5)
			    		{
			    			break;
			    		}
			    		else
			    		{
			    			System.out.print(IpaSymbolInformation.getTipaForSymbol(neighborRanking.get(i).key) + "\\ ");
			    		}
			    		i++;
			    	}
			    	
			    	if (i > 0) System.out.print("\\color{gray}");
			    	
			    	while (i < neighborRanking.size())
			    	{
			    		if (neighborRanking.get(i).value < 0.8)
			    		{
			    			break;
			    		}
			    		else
			    		{
			    			System.out.print(IpaSymbolInformation.getTipaForSymbol(neighborRanking.get(i).key) + "\\ ");
			    		}
			    		i++;
			    	} 	
			    	System.out.println("}\\\\");
			    } 
			    System.out.println("\\hline");
			    System.out.println("\\end{longtable}");
			    System.out.println("");
				System.out.println("\\end{document}");
			} catch (FileNotFoundException e) {
				System.err.print(" file not found, need to infer global model first.\n");
			} catch (IOException e) {
				System.err.print(" format error, need to reinfer global model.\n");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
		if (args.length == 3) {
			//TODO: extract local correspondence model for a language pair
		}
	}
	
}