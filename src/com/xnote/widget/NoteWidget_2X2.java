package com.xnote.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.widget.RemoteViews;

import com.xnote.activity.MainActivity;
import com.xnote.activity.R;
import com.xnote.database.DbInfo.AppwidgetItems;
import com.xnote.log.MyLog;

public class NoteWidget_2X2 extends AppWidgetProvider {

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		MyLog.d(MainActivity.TAG, "NoteWidget_2X2==>onUpdate()");
		int c = appWidgetIds.length;
		if (c > 0) {
			for (int i = 0; i < c; i++) {
				MyLog.d(MainActivity.TAG, "NoteWidget_2X2==>要更新的AppWidget的Id: "
						+ appWidgetIds[i]);
				NoteWidget_2X2.updateAppwidget(context, appWidgetManager,
						appWidgetIds[i]);
			}
		}
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		MyLog.d(MainActivity.TAG, "NoteWidget_2X2==>onDeleted()");
		// 移除Appwidget时，删除数据库中的记录
		int count = appWidgetIds.length;// 得到要移除的Appwidget的数量
		Uri deleUri = null;// 存放待删除的记录的Uri
		SharedPreferences sp = context.getSharedPreferences(
				EditWidgetNoteActivity.SHAREDPREF, Context.MODE_PRIVATE);
		long id = 0;// Appwidget中显示的内容在数据库中的_id
		for (int i = 0; i < count; i++) {
			id = sp.getLong(
					EditWidgetNoteActivity.SHAREDPREF + appWidgetIds[i], -1);
			if (id != -1) {
				MyLog.d(MainActivity.TAG,
						"NoteWidget_2X2==>onDelete()-->被删除记录的id : " + id);
				deleUri = ContentUris.withAppendedId(
						AppwidgetItems.CONTENT_URI, id);
				context.getContentResolver().delete(deleUri, null, null);
			}
		}
		super.onDeleted(context, appWidgetIds);
	}

	public static void updateAppwidget(Context context,
			AppWidgetManager appWidgetManager, int appWidgetId) {
		int mBackground = 0;
		String mContent = null;
		RemoteViews views = null;
		// 读取由ConfigurationActivity保存的ID
		SharedPreferences prefs = context.getSharedPreferences(
				EditWidgetNoteActivity.SHAREDPREF, Context.MODE_PRIVATE);
		int id = (int) prefs.getLong(EditWidgetNoteActivity.SHAREDPREF
				+ appWidgetId, -1);
		MyLog.d(MainActivity.TAG, "NoteWidget_2X2==>AppWidget中的记录在数据库中的id: "
				+ id);
		if (id != -1) {// 读取的id正常
			// 查询该记录的所有内容
			Cursor cursor = context.getContentResolver().query(
					ContentUris.withAppendedId(AppwidgetItems.CONTENT_URI, id),
					null, null, null, null);
			if (cursor.getCount() > 0) {
				cursor.moveToFirst();
				mBackground = cursor.getInt(cursor
						.getColumnIndex(AppwidgetItems.BACKGROUND_COLOR));
				mContent = cursor.getString(cursor
						.getColumnIndex(AppwidgetItems.CONTENT));
			}
			cursor.close();// 关闭Cursor对象

			// 用户点击widget会跳转至EditWidgetNoteActivity
			Intent widgetIntent = new Intent(context,
					EditWidgetNoteActivity.class);
			// 传递被点击的Appwidget的Id
			widgetIntent.putExtra("widget_id", appWidgetId);
			// 传递标识来判断是2X2还是4X4
			widgetIntent.putExtra("is4X4", false);
			// 第二个、第四个参数很重要
			// context: The Context in which this PendingIntent should start the
			// activity.

			// requestCode: Private request code for the sender (currently not
			// used).

			// intent: Intent of the activity to be launched.

			// flags: May be FLAG_ONE_SHOT, FLAG_NO_CREATE, FLAG_CANCEL_CURRENT,
			// FLAG_UPDATE_CURRENT, or any of the flags as supported by
			// Intent.fillIn() to control which unspecified parts
			// of the intent that can be supplied when the actual send happens.

			PendingIntent pendingIntent = PendingIntent.getActivity(context,
					appWidgetId, widgetIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);

			views = new RemoteViews(context.getPackageName(),
					R.layout.widget_2x2_layout);
			MyLog.d(MainActivity.TAG,
					"NoteWidget_2X2==>context.getPackageName(): "
							+ context.getPackageName());
			// 设置widget的内容及其单击事件
			views.setTextViewText(R.id.widget_2x2_textView, mContent);

			views.setImageViewResource(R.id.widget_2x2_imageView, mBackground);
			MyLog.d(MainActivity.TAG, "NoteWidget_2X2==>要显示的内容 ： " + mContent);
			views.setOnClickPendingIntent(R.id.widget_2x2_imageView,
					pendingIntent);
			if (views != null) {
				MyLog.d(MainActivity.TAG, "NoteWidget_2X2==>RemoteViews不为空！");
				// Push update for this widget to the home screen
				appWidgetManager.updateAppWidget(appWidgetId, views);// 执行更新
			}
		}
	}
}