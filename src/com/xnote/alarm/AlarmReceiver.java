package com.xnote.alarm;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.xnote.activity.MainActivity;
import com.xnote.activity.NoteActivity;
import com.xnote.database.DbInfo.NoteItems;
import com.xnote.log.MyLog;

public class AlarmReceiver extends BroadcastReceiver {
	private int _id;
	private String openType;
	private int folderId;

	@Override
	public void onReceive(Context context, Intent intent) {
		MyLog.d(MainActivity.TAG, "AlarmReceiver==>acquire wakelock");
		WakeLockOpration.acquire(context);
		KeyguardManager km = (KeyguardManager) context
				.getSystemService(Context.KEYGUARD_SERVICE);
		KeyguardLock kl = km.newKeyguardLock(MainActivity.TAG);
		kl.disableKeyguard();
		// 取得Open_Type的值,判断是新建便签还是更新便签
		openType = intent.getStringExtra("Open_Type");
		// 被编辑的便签的ID
		_id = intent.getIntExtra(NoteItems._ID, -1);
		MyLog.d(MainActivity.TAG, "AlarmReceiver==>要提醒的记录的id: " + _id);
		// 得到文件夹的ID(如果从文件夹页面内新建或编辑便签则要求传递文件夹的ID)
		folderId = intent.getIntExtra("FolderId", -1);

		Intent i = new Intent();
		i.setClass(context, NoteActivity.class);
		i.putExtra("Open_Type", openType);
		i.putExtra(NoteItems._ID, _id);
		i.putExtra("FolderId", folderId);
		i.putExtra("alarm", 3080905);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(i);
	}
}