package com.goliathonline.android.kegbot.util;

import java.text.DecimalFormat;

public class UnitUtils {
	
	private static DecimalFormat df2 = new DecimalFormat("0.00");
	private static DecimalFormat df0 = new DecimalFormat("0");
	
	public static String mlToOz(String vol_ml)
	{
		double vol = Double.parseDouble(vol_ml);
		return df2.format(vol * 0.0338140225589);
	}
	
	public static Double mlToOz(Double vol_ml)
	{
		return vol_ml * 0.0338140225589;
	}
	
	public static String mlToPint(String vol_ml)
	{
		double vol = Double.parseDouble(vol_ml);
		return df0.format((vol * 0.0338140225589) / 16);
	}
	
	public static Double mlToPint(Double vol_ml)
	{
		return (vol_ml * 0.0338140225589) / 16;
	}
	
	public static String cToF(String deg_c)
	{
		double deg = Double.parseDouble(deg_c);
		return df2.format(deg * (9/5) + 32);
	}
	
	public static Double cToF(Double deg_c)
	{
		return (deg_c * (9/5) + 32);
	}

}
