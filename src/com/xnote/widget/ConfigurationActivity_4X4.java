package com.xnote.widget;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.xnote.activity.MainActivity;
import com.xnote.activity.R;
import com.xnote.database.DateTimeUtil;
import com.xnote.database.DbInfo.AppwidgetItems;
import com.xnote.log.MyLog;

/*
 * Widget的Configuration Activity,负责Widget的初始化
 */
public class ConfigurationActivity_4X4 extends Activity {

	private LinearLayout mLinearLayout_Header;
	private ImageButton ib_bgcolor;
	private TextView tv_widget_title;
	private EditText et_widget_content;
	// 存储便签的背景图片在R.java中的值
	private int mBackgroud_Color;
	// 新建Widget便签的日期、时间
	private String createDate;
	private String createTime;
	// 被插入便签的ID
	private int _id;
	// Widget的ID
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	// 更改背景颜色事件监听器
	private View.OnClickListener listener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			// 弹出一个自定义的Dialog,供用户选择便签的背景颜色
			AlertDialog.Builder builder = new AlertDialog.Builder(
					ConfigurationActivity_4X4.this);
			LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(R.layout.dialog_note_bg_color, null);
			final AlertDialog dialog = builder.create();
			dialog.setView(view, 0, 0, 0, 0);
			// 初始化布局文件中的ImageButton对象
			ImageButton pink = (ImageButton) view.findViewById(R.id.pink);
			ImageButton green = (ImageButton) view.findViewById(R.id.green);
			ImageButton yellow = (ImageButton) view.findViewById(R.id.yellow);
			ImageButton white = (ImageButton) view.findViewById(R.id.white);
			// 自定义ImageButton的点击事件监听器
			Button.OnClickListener listener = new Button.OnClickListener() {
				@Override
				public void onClick(View v) {
					switch (v.getId()) {
					case R.id.green:
						mBackgroud_Color = R.drawable.widget_big_green;
						et_widget_content
								.setBackgroundResource(R.drawable.item_light_green);
						// 设置标题栏的背景图片
						mLinearLayout_Header
								.setBackgroundResource(R.drawable.notes_header_green);
						MyLog.d(MainActivity.TAG,
								"ConfigurationActivity_4X4==>选择了绿色:"
										+ mBackgroud_Color);
						break;
					case R.id.pink:
						mBackgroud_Color = R.drawable.widget_big_red;
						et_widget_content
								.setBackgroundResource(R.drawable.item_light_pink);
						// 设置标题栏的背景图片
						mLinearLayout_Header
								.setBackgroundResource(R.drawable.notes_header_pink);
						MyLog.d(MainActivity.TAG,
								"ConfigurationActivity_4X4==>选择了粉红色:"
										+ mBackgroud_Color);
						break;
					case R.id.yellow:
						mBackgroud_Color = R.drawable.widget_big_yellow;
						et_widget_content
								.setBackgroundResource(R.drawable.item_light_yellow);
						// 设置标题栏的背景图片
						mLinearLayout_Header
								.setBackgroundResource(R.drawable.notes_header_yellow);
						MyLog.d(MainActivity.TAG,
								"ConfigurationActivity_4X4==>选择了黄色:"
										+ mBackgroud_Color);
						break;
					case R.id.white:
						mBackgroud_Color = R.drawable.widget_big_gray;
						et_widget_content
								.setBackgroundResource(R.drawable.item_light_white);
						// 设置标题栏的背景图片
						mLinearLayout_Header
								.setBackgroundResource(R.drawable.notes_header_gray);
						MyLog.d(MainActivity.TAG,
								"ConfigurationActivity_4X4==>选择了白色:"
										+ mBackgroud_Color);
						break;
					}
					// 结束对话框
					dialog.dismiss();
				}
			};
			// 注册点击事件监听器
			pink.setOnClickListener(listener);
			green.setOnClickListener(listener);
			yellow.setOnClickListener(listener);
			white.setOnClickListener(listener);
			dialog.show();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MyLog.d(MainActivity.TAG, "ConfigurationActivity_4X4==>***onCreate***");
		requestWindowFeature(Window.FEATURE_NO_TITLE);// 取消标题栏
		setContentView(R.layout.widget_note_layout);
		// 防止用户未完成配置就直接退出
		setResult(RESULT_CANCELED);
		// 取得Widget的ID
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
			mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		// If they gave us an intent without the widget id, just bail.
		if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
		}
		MyLog.d(MainActivity.TAG,
				"ConfigurationActivity_4X4==>onCreate-->AppWidget ID  : "
						+ mAppWidgetId);
		mLinearLayout_Header = (LinearLayout) findViewById(R.id.widget_detail_header);
		tv_widget_title = (TextView) findViewById(R.id.tv_widget_date_time);

		et_widget_content = (EditText) findViewById(R.id.et_content);
		// 使用默认的背景颜色
		et_widget_content.setBackgroundResource(R.drawable.item_light_blue);

		// ImageButton,点击改变便签背景颜色
		ib_bgcolor = (ImageButton) findViewById(R.id.imagebutton_bgcolor);
		// 设置便签背景颜色的按钮
		ib_bgcolor.setOnClickListener(listener);
		// 初始化新建便签的日期时间
		createDate = DateTimeUtil.getDate();
		createTime = DateTimeUtil.getTime();
		tv_widget_title.setText(createDate + "\t" + createTime.substring(0, 5));
	}

	@Override
	public void onBackPressed() {
		MyLog.d(MainActivity.TAG,
				"ConfigurationActivity_4X4==>***onBackPressed***");
		if (mBackgroud_Color == 0) {// 如果mBackgroud_Color==0,我们使用蓝色作为默认背景
			mBackgroud_Color = R.drawable.widget_big_blue;
		}
		// 取得便签的内容
		String newContent = et_widget_content.getText().toString();
		if (!TextUtils.isEmpty(newContent)) {
			// 用insert将记录插入数据库
			ContentValues values = new ContentValues();
			values.put(AppwidgetItems.CONTENT, newContent);
			values.put(AppwidgetItems.UPDATE_DATE, DateTimeUtil.getDate());
			values.put(AppwidgetItems.UPDATE_TIME, DateTimeUtil.getTime());
			values.put(AppwidgetItems.BACKGROUND_COLOR, mBackgroud_Color);
			_id = (int) ContentUris.parseId(getContentResolver().insert(
					AppwidgetItems.CONTENT_URI, values));
		}
		// 将新插入的记录的ID写到文本文件中
		SharedPreferences.Editor editor = this.getSharedPreferences(
				EditWidgetNoteActivity.SHAREDPREF, MODE_PRIVATE).edit();
		editor.putLong(EditWidgetNoteActivity.SHAREDPREF + mAppWidgetId, _id);
		editor.commit();
		// Push widget update to surface with newly set text
		AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(ConfigurationActivity_4X4.this);
		NoteWidget_4X4.updateAppwidget(ConfigurationActivity_4X4.this,
				appWidgetManager, mAppWidgetId);
		// Make sure we pass back the original appWidgetId
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setResult(RESULT_OK, resultValue);
		super.onBackPressed();
	}
}