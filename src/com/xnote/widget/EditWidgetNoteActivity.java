package com.xnote.widget;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
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

public class EditWidgetNoteActivity extends Activity {
	// 用SharedPreference存放AppWidget显示的便签的_id
	public static final String SHAREDPREF = "widget_note_id";
	private LinearLayout mLinearLayout_Header;
	private ImageButton ib_bgcolor;
	private TextView tv_widget_title;
	private EditText et_content;
	// 存储便签的背景图片在R.java中的值
	private int mBackgroud_Color;
	// 更新2X2还是4X4
	private boolean is4X4 = false;
	// 最后一次更新Widget便签的日期、时间
	private String updateDate;
	private String updateTime;
	// 被插入便签的ID
	private int _id;
	// widget的id
	private int mAppWidgetId;
	// 更改背景颜色事件监听器
	private View.OnClickListener listener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			// 弹出一个自定义的Dialog,供用户选择便签的背景颜色
			AlertDialog.Builder builder = new AlertDialog.Builder(
					EditWidgetNoteActivity.this);
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
						mBackgroud_Color = R.drawable.widget_small_green;
						et_content
								.setBackgroundResource(R.drawable.item_light_green);
						// 设置标题栏的背景图片
						mLinearLayout_Header
								.setBackgroundResource(R.drawable.notes_header_green);
						MyLog.d(MainActivity.TAG,
								"EditWidgetNoteActivity==>选择了绿色:"
										+ mBackgroud_Color);
						break;
					case R.id.pink:
						mBackgroud_Color = R.drawable.widget_small_red;
						et_content
								.setBackgroundResource(R.drawable.item_light_pink);
						// 设置标题栏的背景图片
						mLinearLayout_Header
								.setBackgroundResource(R.drawable.notes_header_pink);
						MyLog.d(MainActivity.TAG,
								"EditWidgetNoteActivity==>选择了粉红色:"
										+ mBackgroud_Color);
						break;
					case R.id.yellow:
						mBackgroud_Color = R.drawable.widget_small_yellow;
						et_content
								.setBackgroundResource(R.drawable.item_light_yellow);
						// 设置标题栏的背景图片
						mLinearLayout_Header
								.setBackgroundResource(R.drawable.notes_header_yellow);
						MyLog.d(MainActivity.TAG,
								"EditWidgetNoteActivity==>选择了黄色:"
										+ mBackgroud_Color);
						break;
					case R.id.white:
						mBackgroud_Color = R.drawable.widget_small_gray;
						et_content
								.setBackgroundResource(R.drawable.item_light_white);
						// 设置标题栏的背景图片
						mLinearLayout_Header
								.setBackgroundResource(R.drawable.notes_header_gray);
						MyLog.d(MainActivity.TAG,
								"EditWidgetNoteActivity==>选择了白色:"
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
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.widget_note_layout);
		Intent intent = getIntent();
		// 取得Appwidget的Id
		mAppWidgetId = intent.getIntExtra("widget_id", -1);
		// 是否更新4X4类型的Appwidget
		is4X4 = intent.getBooleanExtra("is4X4", false);
		MyLog.d(MainActivity.TAG, "EditWidgetNoteActivity==>点击的Appwidget的id ： "
				+ mAppWidgetId);
		if (mAppWidgetId == -1) {// 没有正常获得ID
			finish();
		}
		SharedPreferences prefs = this.getSharedPreferences(
				EditWidgetNoteActivity.SHAREDPREF, Context.MODE_WORLD_READABLE);
		// 保存方式：prefix+AppwidgetId
		_id = (int) prefs.getLong(EditWidgetNoteActivity.SHAREDPREF
				+ mAppWidgetId, -1);
		MyLog.d(MainActivity.TAG,
				"EditWidgetNoteActivity==>从SharedPreferences中读到的AppWidget的id: "
						+ _id);
		mLinearLayout_Header = (LinearLayout) findViewById(R.id.widget_detail_header);
		et_content = (EditText) findViewById(R.id.et_content);
		// ImageButton,点击改变便签背景颜色
		ib_bgcolor = (ImageButton) findViewById(R.id.imagebutton_bgcolor);
		tv_widget_title = (TextView) findViewById(R.id.tv_widget_date_time);
		if (_id != -1) {// 正常得到_id,编辑主页或文件夹下的便签
			// 根据便签的ID查询该便签的详细内容
			Cursor c = getContentResolver()
					.query(ContentUris.withAppendedId(
							AppwidgetItems.CONTENT_URI, _id), null, null, null,
							null);
			c.moveToFirst();
			// 最后更新便签的日期时间及其内容
			String content = c.getString(c
					.getColumnIndex(AppwidgetItems.CONTENT));
			et_content.setText(content);
			// 根据数据库中的值设定背景颜色(该值是桌面widget背景图片的值)
			mBackgroud_Color = c.getInt(c
					.getColumnIndex(AppwidgetItems.BACKGROUND_COLOR));
			updateDate = c.getString(c
					.getColumnIndex(AppwidgetItems.UPDATE_DATE));
			updateTime = c.getString(c
					.getColumnIndex(AppwidgetItems.UPDATE_TIME));
			// 以2011-09-09 9:10的样式显示日期时间
			tv_widget_title.setText(updateDate + "\t"
					+ updateTime.substring(0, 5));
			c.close();
		}
		et_content
				.setBackgroundResource(getContentBackground(mBackgroud_Color));
		mLinearLayout_Header
				.setBackgroundResource(getHeaderBackground(mBackgroud_Color));
		// 设置便签背景颜色的按钮
		ib_bgcolor.setOnClickListener(listener);
	}

	// 判断Header的背景图片
	private int getHeaderBackground(int resId) {
		switch (resId) {
		case R.drawable.widget_small_blue:
			return R.drawable.notes_header_blue;
		case R.drawable.widget_small_green:
			return R.drawable.notes_header_green;
		case R.drawable.widget_small_red:
			return R.drawable.notes_header_pink;
		case R.drawable.widget_small_gray:
			return R.drawable.notes_header_gray;
		case R.drawable.widget_small_yellow:
			return R.drawable.notes_header_yellow;
		default:
			break;
		}
		return R.drawable.notes_header_blue;
	}

	// 判断EditText的背景
	private int getContentBackground(int resId) {
		switch (resId) {
		case R.drawable.widget_small_blue:
			return R.drawable.item_light_blue;
		case R.drawable.widget_small_green:
			return R.drawable.item_light_green;
		case R.drawable.widget_small_red:
			return R.drawable.item_light_pink;
		case R.drawable.widget_small_gray:
			return R.drawable.item_light_white;
		case R.drawable.widget_small_yellow:
			return R.drawable.item_light_yellow;
		default:
			break;
		}
		return R.drawable.item_light_blue;
	}

	@Override
	public void onBackPressed() {
		// 取得便签的内容
		String newContent = et_content.getText().toString();
		if (!TextUtils.isEmpty(newContent)) {
			// 用update更新数据库中的记录
			ContentValues values = new ContentValues();
			values.put(AppwidgetItems.CONTENT, newContent);
			values.put(AppwidgetItems.UPDATE_DATE, DateTimeUtil.getDate());
			values.put(AppwidgetItems.UPDATE_TIME, DateTimeUtil.getTime());
			values.put(AppwidgetItems.BACKGROUND_COLOR, mBackgroud_Color);
			getContentResolver()
					.update(ContentUris.withAppendedId(
							AppwidgetItems.CONTENT_URI, _id), values, null,
							null);
			// Push widget update to surface with newly set text
			AppWidgetManager appWidgetManager = AppWidgetManager
					.getInstance(getApplicationContext());
			if (is4X4) {
				NoteWidget_4X4.updateAppwidget(getApplicationContext(),
						appWidgetManager, mAppWidgetId);
			} else {
				NoteWidget_2X2.updateAppwidget(getApplicationContext(),
						appWidgetManager, mAppWidgetId);
			}
			super.onBackPressed();
		}
	}
}