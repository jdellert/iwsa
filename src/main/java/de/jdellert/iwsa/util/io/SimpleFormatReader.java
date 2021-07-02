package de.jdellert.iwsa.util.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class SimpleFormatReader {
	public static List<String> listFromFile(String fileName) throws FileNotFoundException {
		ArrayList<String> list = new ArrayList<String>();
		Scanner in = new Scanner(new File(fileName));
		// first line: language IDs
		while (in.hasNextLine()) {
			list.add(in.nextLine());
		}
		in.close();
		return list;
	}

	public static List<String> listFromStream(InputStream stream) {
		ArrayList<String> list = new ArrayList<String>();
		Scanner in = new Scanner(stream);
		// first line: language IDs
		while (in.hasNextLine()) {
			list.add(in.nextLine());
		}
		in.close();
		return list;
	}

	public static List<String[]> arrayFromTSV(String fileName) throws FileNotFoundException {
		ArrayList<String[]> list = new ArrayList<String[]>();
		Scanner in = new Scanner(new File(fileName));
		while (in.hasNextLine()) {
			list.add(in.nextLine().split("\t"));
		}
		in.close();
		return list;
	}

	public static Map<String, String[]> mapFromTSV(String fileName, int uniqueKeyIndex, int minNumFields)
			throws FileNotFoundException {
		Scanner in = new Scanner(new File(fileName));
		return mapFromTSV(in, uniqueKeyIndex, minNumFields);
	}

	public static Map<String, String[]> mapFromTSV(InputStream stream, int uniqueKeyIndex, int minNumFields)
			throws FileNotFoundException {
		Scanner in = new Scanner(stream);
		return mapFromTSV(in, uniqueKeyIndex, minNumFields);
	}

	public static Map<String, String[]> mapFromTSV(Scanner in, int uniqueKeyIndex, int minNumFields) {
		HashMap<String, String[]> list = new HashMap<String, String[]>();
		while (in.hasNextLine()) {
			String[] tokens = in.nextLine().split("\t");
			if (tokens.length > uniqueKeyIndex && tokens.length >= minNumFields) {
				list.put(tokens[uniqueKeyIndex], tokens);
			} else {
				// ignore lines where there are fewer columns than the key index
			}
		}
		in.close();
		return list;
	}

	public static List<Map<String, String>> entriesFromCSV(String fileName) throws FileNotFoundException {
		List<Map<String, String>> entries = new ArrayList<Map<String, String>>();
		Scanner in = new Scanner(new File(fileName));
		String[] columnNames = in.nextLine().split(",");
		while (in.hasNextLine()) {
			String[] fields = in.nextLine().split(",");
			Map<String, String> entry = new HashMap<String, String>();
			for (int i = 0; i < columnNames.length; i++) {
				entry.put(columnNames[i], fields[i]);
			}
			entries.add(entry);
		}
		in.close();
		return entries;
	}
}
