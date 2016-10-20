package com.xnote.alarm;

import com.xnote.activity.MainActivity;

import android.content.Context;
import android.os.PowerManager;

// 当到了用户设置的闹钟时间时,如果屏幕关闭,我们可以通过使用WakeLock和KeyguardLock来解锁屏幕并弹出我们自己的Dialog
public final class WakeLockOpration {
	private static PowerManager.WakeLock wakeLock;

	// 获得wakelock
	public static void acquire(Context context) {
		if (wakeLock != null) {
			wakeLock.release();
		}
		PowerManager pm = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
				| PowerManager.ACQUIRE_CAUSES_WAKEUP
				| PowerManager.ON_AFTER_RELEASE, MainActivity.TAG);
		wakeLock.acquire();
	}

	// 释放wakelock
	public static void release() {
		if (wakeLock != null)
			wakeLock.release();
		wakeLock = null;
	}
}