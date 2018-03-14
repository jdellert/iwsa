package de.jdellert.iwsa.util.io;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class Formatting {
	static NumberFormat threeFractionDigitsFormat;
	static NumberFormat sixFractionDigitsFormat;
	static NumberFormat twelveFractionDigitsFormat;
	
	static {
		threeFractionDigitsFormat = DecimalFormat.getInstance(Locale.ENGLISH);
		threeFractionDigitsFormat.setMinimumIntegerDigits(1);
		threeFractionDigitsFormat.setMinimumFractionDigits(3);
		threeFractionDigitsFormat.setMaximumFractionDigits(3);
		
		sixFractionDigitsFormat = DecimalFormat.getInstance(Locale.ENGLISH);
		sixFractionDigitsFormat.setMinimumIntegerDigits(1);
		sixFractionDigitsFormat.setMinimumFractionDigits(6);
		sixFractionDigitsFormat.setMaximumFractionDigits(6);
		
		twelveFractionDigitsFormat = DecimalFormat.getInstance(Locale.ENGLISH);
		twelveFractionDigitsFormat.setMinimumIntegerDigits(1);
		twelveFractionDigitsFormat.setMinimumFractionDigits(12);
		twelveFractionDigitsFormat.setMaximumFractionDigits(12);
	}
	
	/**
	 * Coerces a double into a length-5 string format for output.
	 * @return
	 */
	public static String str3f(double num)
	{
		return threeFractionDigitsFormat.format(num);
	}
	
	/**
	 * Coerces a double into a length-8 string format for output.
	 * @return
	 */
	public static String str6f(double num)
	{
		return sixFractionDigitsFormat.format(num);
	}
	
	public static String str12f(double num) {
		return twelveFractionDigitsFormat.format(num);
	}
	
	public static String intLPad(int num, int reqLength)
	{
		String result = num + "";
		while (result.length() < reqLength)
		{
			result += " ";
		}
		return result;
	}
	
	public static String intRPad(int num, int reqLength)
	{
		String result = num + "";
		while (result.length() < reqLength)
		{
			result = " " + result;
		}
		return result;
	}


}
