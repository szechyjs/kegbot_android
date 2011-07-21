package com.goliathonline.android.kegbot.util;

import java.text.DecimalFormat;

public class UnitUtils {
	
	private static DecimalFormat df = new DecimalFormat("0.00");
	
	public static String mlToOz(String vol_ml)
	{
		double vol = Double.parseDouble(vol_ml);
		return df.format(vol * 0.0338140225589);
	}
	
	public static String cToF(String deg_c)
	{
		double deg = Double.parseDouble(deg_c);
		return df.format(deg * (9/5) + 32);
	}

}
