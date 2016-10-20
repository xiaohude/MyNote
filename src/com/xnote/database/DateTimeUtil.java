package com.xnote.database;

import java.sql.Date;
import java.sql.Time;
import java.util.Calendar;

public final class DateTimeUtil {
	// 日期操作
	public static String getDate() {
		Calendar c = Calendar.getInstance();
		int theYear = c.get(Calendar.YEAR) - 1900;
		int theMonth = c.get(Calendar.MONTH);
		int theDay = c.get(Calendar.DAY_OF_MONTH);
		return (new Date(theYear, theMonth, theDay)).toString();
	}

	// 时间操作
	public static String getTime() {
		Calendar c = Calendar.getInstance();
		int theHour = c.get(Calendar.HOUR_OF_DAY);
		int theMinute = c.get(Calendar.MINUTE);
		int theSecond = c.get(Calendar.SECOND);
		return (new Time(theHour, theMinute, theSecond)).toString();
	}
}