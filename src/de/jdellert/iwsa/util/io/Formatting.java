package de.jdellert.iwsa.util.io;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class Formatting {
	static NumberFormat threeFactionDigitsFormat;
	
	static {
		threeFactionDigitsFormat = DecimalFormat.getInstance(Locale.ENGLISH);
		threeFactionDigitsFormat.setMinimumIntegerDigits(1);
		threeFactionDigitsFormat.setMinimumFractionDigits(3);
		threeFactionDigitsFormat.setMaximumFractionDigits(3);
	}
	
	/**
	 * Coerces a double into a length-5 string format for output.
	 * @return
	 */
	public static String str3f(double num)
	{
		return threeFactionDigitsFormat.format(num);
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
