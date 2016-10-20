package com.xnote.log;

import android.util.Log;

// 简单的封装Log类,便于控制是否打印Log信息
public final class MyLog {
	// 控制是否打印Log信息
	private static boolean openLog = true;

	public static void d(String tag, String msg) {
		if (openLog) {
			Log.d(tag, msg);
		}
	}

	public static void w(String tag, String msg) {
		if (openLog) {
			Log.w(tag, msg);
		}
	}

	public static void e(String tag, String msg) {
		if (openLog) {
			Log.e(tag, msg);
		}
	}
}