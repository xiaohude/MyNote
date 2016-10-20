package com.xnote.activity;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.xnote.alarm.AlarmReceiver;
import com.xnote.alarm.WakeLockOpration;
import com.xnote.database.DateTimeUtil;
import com.xnote.database.DbInfo.NoteItems;
import com.xnote.log.MyLog;

/*
 * 一条便签的详细信息页面。
 */
public class NoteActivity extends Activity {
	private LinearLayout mLinearLayout_Header;
	private ImageButton ib_bgcolor;
	private TextView tv_note_title;
	private EditText et_content;
	// 存储便签的背景图片在R.java中的值
	private int mBackgroud_Color;
	// 用户创建或更新便签的日期/时间
	private String updateDate;
	private String updateTime;
	private int mYear;// 提醒时间的年份
	private int mMonth;// 提醒时间的月份
	private int mDay;// 提醒时间的日(dayOfMonth)
	private int mHour;// 提醒时间的小时
	private int mMinute;// 提醒时间的分钟
	private boolean hasSetAlartTime = false;// 用于标识用户是否设置Alarm
	// 用于判断是新建便签还是更新便签
	private String openType;
	// 数据库中原有的便签的内容
	private String oldContent;
	// 接受传递过来的Intent对象
	private Intent intent;
	// 被编辑的便签的ID
	private int _id;
	// 被编辑便签所在的文件夹的ID
	private int folderId;
	// 设置shortcut时使用该字段
	private final String ACTION_ADD_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";

	// 菜单
	private static final int MENU_DELETE = Menu.FIRST;
	private static final int MENU_REMIND = Menu.FIRST + 1;
	private static final int MENU_SEND_HOME = Menu.FIRST + 2;
	private static final int MENU_SHARE = Menu.FIRST + 3;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 取消标题栏
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.note_detail);
		// 得到有前一个Activity传递过来的Intent对象
		intent = getIntent();
		// 如果没有传递Intent对象,则返回主页(MainActivity)
		if (intent.equals(null)) {
			startActivity(new Intent(NoteActivity.this, MainActivity.class));
		}
		// 取得Open_Type的值,判断是新建便签还是更新便签
		openType = intent.getStringExtra("Open_Type");
		MyLog.d(MainActivity.TAG, "NoteActivity==>" + String.valueOf(openType));
		// 被编辑的便签的ID
		_id = intent.getIntExtra(NoteItems._ID, -1);
		MyLog.d(MainActivity.TAG, "NoteActivity==>被编辑的便签的id:" + _id);
		// 得到文件夹的ID(如果从文件夹页面内新建或编辑便签则要求传递文件夹的ID)
		folderId = intent.getIntExtra("FolderId", -1);
		MyLog.d(MainActivity.TAG, "NoteActivity==>要操作的文件夹的 id :" + folderId);
		// 在AlarmReceiver中定义
		if (intent.getIntExtra("alarm", -1) == 3080905) {
			// 显示提醒
			noteAlarm(_id);
		}
		initViews();
	}

	@Override
	protected void onResume() {
		// 恢复Keyguard
		KeyguardManager km = (KeyguardManager) this
				.getSystemService(Context.KEYGUARD_SERVICE);
		KeyguardLock kl = km.newKeyguardLock(MainActivity.TAG);
		kl.reenableKeyguard();
		super.onResume();
	}

	@Override
	protected void onPause() {
		// 释放 wakelock
		WakeLockOpration.release();
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// 删除
		menu.add(Menu.NONE, MENU_DELETE, 1, R.string.delete).setIcon(
				R.drawable.delete);
		// 设置闹铃
		menu.add(Menu.NONE, MENU_REMIND, 2, R.string.alarm_time).setIcon(
				R.drawable.alarm_time);
		// 添加到桌面
		menu.add(Menu.NONE, MENU_SEND_HOME, 3, R.string.add_shortcut_to_home)
				.setIcon(R.drawable.add_shortcut_to_home);
		// 修改文件夹
		menu.add(Menu.NONE, MENU_SHARE, 4, R.string.share_sms_or_email)
				.setIcon(R.drawable.share);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_DELETE:
			deleteNote();
			break;
		case MENU_REMIND:
			setAlarm();
			break;
		case MENU_SEND_HOME:
			addShortCut();
			break;
		case MENU_SHARE:
			shareNote();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	// 初始化组件
	private void initViews() {
		mLinearLayout_Header = (LinearLayout) findViewById(R.id.note_detail_header);
		// ImageButton,点击改变便签背景颜色
		ib_bgcolor = (ImageButton) findViewById(R.id.imagebutton_bgcolor);
		tv_note_title = (TextView) findViewById(R.id.tv_note_date_time);
		et_content = (EditText) findViewById(R.id.et_content);
		if (_id != -1) {// 正常得到_id,编辑主页或文件夹下的便签
			// 根据便签的ID查询该便签的详细内容
			Cursor c = getContentResolver().query(
					ContentUris.withAppendedId(NoteItems.CONTENT_URI, _id),
					null, null, null, null);
			c.moveToFirst();
			// 最后更新便签的日期时间及其内容
			oldContent = c.getString(c.getColumnIndex(NoteItems.CONTENT));
			updateDate = c.getString(c.getColumnIndex(NoteItems.UPDATE_DATE));
			updateTime = c.getString(c.getColumnIndex(NoteItems.UPDATE_TIME));

			// 根据数据库中的值设定背景颜色
			mBackgroud_Color = c.getInt(c
					.getColumnIndex(NoteItems.BACKGROUND_COLOR));
			c.close();
		}
		// 判断打开方式
		if (openType.equals("newNote")) {// 新建"顶级便签",即没有放在文件夹内的便签
			// 初始化新建便签的日期时间
			updateDate = DateTimeUtil.getDate();
			updateTime = DateTimeUtil.getTime();
			// 使用默认的背景颜色
			et_content.setBackgroundResource(R.drawable.item_light_blue);
		} else if (openType.equals("editNote")) {// 编辑顶级便签(不在文件夹内的便签)
			et_content.setText(oldContent);
			// 根据数据库中的值设定背景颜色
			if (mBackgroud_Color != 0) {
				et_content.setBackgroundResource(mBackgroud_Color);
				mLinearLayout_Header
						.setBackgroundResource(headerBackground(mBackgroud_Color));
			}
		} else if (openType.equals("newFolderNote")) {// 在某文件夹下新建便签
			// 初始化新建便签的日期时间
			updateDate = DateTimeUtil.getDate();
			updateTime = DateTimeUtil.getTime();
			// 使用默认的背景颜色
			et_content.setBackgroundResource(R.drawable.item_light_blue);
		} else if (openType.equals("editFolderNote")) {// 编辑某文件夹下的便签
			et_content.setText(oldContent);
			if (mBackgroud_Color != 0) {
				et_content.setBackgroundResource(mBackgroud_Color);
				mLinearLayout_Header
						.setBackgroundResource(headerBackground(mBackgroud_Color));
			}
		}
		// 设置便签背景颜色的按钮
		ib_bgcolor.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// 弹出一个自定义的Dialog,供用户选择便签的背景颜色
				AlertDialog.Builder builder = new AlertDialog.Builder(
						NoteActivity.this);
				LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
				View view = inflater.inflate(R.layout.dialog_note_bg_color,
						null);
				final AlertDialog dialog = builder.create();
				dialog.setView(view, 0, 0, 0, 0);
				// 初始化布局文件中的ImageButton对象
				ImageButton pink = (ImageButton) view.findViewById(R.id.pink);
				ImageButton green = (ImageButton) view.findViewById(R.id.green);
				ImageButton yellow = (ImageButton) view
						.findViewById(R.id.yellow);
				ImageButton white = (ImageButton) view.findViewById(R.id.white);
				// 自定义ImageButton的点击事件监听器
				Button.OnClickListener listener = new Button.OnClickListener() {
					@Override
					public void onClick(View v) {
						MyLog.d(MainActivity.TAG, "NoteActivity==>选择背景颜色...");
						switch (v.getId()) {
						case R.id.green:
							mBackgroud_Color = R.drawable.item_light_green;
							et_content.setBackgroundResource(mBackgroud_Color);
							// 设置标题栏的背景图片
							mLinearLayout_Header
									.setBackgroundResource(R.drawable.notes_header_green);
							MyLog.d(MainActivity.TAG, "NoteActivity==>选择了绿色:"
									+ mBackgroud_Color);
							break;
						case R.id.pink:
							mBackgroud_Color = R.drawable.item_light_pink;
							et_content.setBackgroundResource(mBackgroud_Color);
							// 设置标题栏的背景图片
							mLinearLayout_Header
									.setBackgroundResource(R.drawable.notes_header_pink);
							MyLog.d(MainActivity.TAG, "NoteActivity==>选择了粉红色:"
									+ mBackgroud_Color);
							break;
						case R.id.yellow:
							mBackgroud_Color = R.drawable.item_light_yellow;
							et_content.setBackgroundResource(mBackgroud_Color);
							// 设置标题栏的背景图片
							mLinearLayout_Header
									.setBackgroundResource(R.drawable.notes_header_yellow);
							MyLog.d(MainActivity.TAG, "NoteActivity==>选择了黄色:"
									+ mBackgroud_Color);
							break;
						case R.id.white:
							mBackgroud_Color = R.drawable.item_light_white;
							et_content.setBackgroundResource(mBackgroud_Color);
							// 设置标题栏的背景图片
							mLinearLayout_Header
									.setBackgroundResource(R.drawable.notes_header_gray);
							MyLog.d(MainActivity.TAG, "NoteActivity==>选择了白色:"
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
		});
		// 以2011-09-09 9:10的样式显示日期时间
		tv_note_title.setText(updateDate + "\t" + updateTime.substring(0, 5));
	}

	// 判断Header的背景图片
	private int headerBackground(int resId) {
		switch (resId) {
		case R.drawable.item_light_blue:
			return R.drawable.notes_header_blue;
		case R.drawable.item_light_green:
			return R.drawable.notes_header_green;
		case R.drawable.item_light_pink:
			return R.drawable.notes_header_pink;
		case R.drawable.item_light_white:
			return R.drawable.notes_header_gray;
		case R.drawable.item_light_yellow:
			return R.drawable.notes_header_yellow;
		default:
			break;
		}
		return R.drawable.notes_bg_blue;
	}

	// 设置便签的提醒时间
	private void setAlarm() {
		MyLog.d(MainActivity.TAG, "NoteActivity==>Set Alarm");
		// 获得AlarmManager
		final AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		final Calendar c = Calendar.getInstance();
		mYear = c.get(Calendar.YEAR);
		mMonth = c.get(Calendar.MONTH);
		mDay = c.get(Calendar.DAY_OF_MONTH);
		mHour = c.get(Calendar.HOUR_OF_DAY);
		mMinute = c.get(Calendar.MINUTE);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.alarm_time);
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.set_alarm, null);
		builder.setView(view);
		// 点击设置闹钟日期
		final Button btnAlarmDate = (Button) view
				.findViewById(R.id.btnAlarmDate);
		btnAlarmDate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				DatePickerDialog dpd = new DatePickerDialog(NoteActivity.this,
						new OnDateSetListener() {
							@Override
							public void onDateSet(DatePicker view, int year,
									int monthOfYear, int dayOfMonth) {
								mYear = year;
								mMonth = monthOfYear;
								mDay = dayOfMonth;
								String alarmDate = mYear + "-" + mMonth + "-"
										+ mDay;
								btnAlarmDate.setText(alarmDate);
								MyLog.d(MainActivity.TAG,
										"NoteActivity==>设置的闹钟日期: " + alarmDate);
							}
						}, mYear, mMonth, mDay);
				dpd.show();
			}
		});
		// 点击设置闹钟时间
		final Button btnAlarmTime = (Button) view
				.findViewById(R.id.btnAlarmTime);
		btnAlarmTime.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TimePickerDialog tpd = new TimePickerDialog(NoteActivity.this,
						new OnTimeSetListener() {
							@Override
							public void onTimeSet(TimePicker view,
									int hourOfDay, int minute) {
								mHour = hourOfDay;
								mMinute = minute;
								String alarmTime = hourOfDay + ":" + minute;
								btnAlarmTime.setText(alarmTime);
								MyLog.d(MainActivity.TAG,
										"NoteActivity==>设置的闹钟时间" + alarmTime);
							}
						}, mHour, mMinute, true);
				tpd.show();
			}
		});
		builder.setPositiveButton(R.string.Ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// 检测时间是否合理,如:不能早于现在
						if (checkAlarmTime(am)) {
							hasSetAlartTime = true;
							dialog.dismiss();
							Toast.makeText(getApplicationContext(), "设置提醒时间成功",
									Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(getApplicationContext(), "设置提醒时间失败",
									Toast.LENGTH_SHORT).show();
						}
					}
				});
		builder.setNegativeButton(R.string.Cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						hasSetAlartTime = false;
						dialog.dismiss();
					}
				});
		builder.create().show();
	}

	private boolean checkAlarmTime(AlarmManager am) {
		MyLog.d(MainActivity.TAG, "NoteActivity==>checkAlarmTime()");
		Calendar alarmCalendar = Calendar.getInstance();
		alarmCalendar.set(mYear, mMonth, mDay, mHour, mMinute, 0);
		// 使用传递过来的intent,因为它包含了打开NoteActivity所需的一切参数
		Intent i = new Intent();
		i.setClass(NoteActivity.this, AlarmReceiver.class);
		i.putExtra("Open_Type", openType);
		i.putExtra(NoteItems._ID, _id);
		i.putExtra("FolderId", folderId);
		PendingIntent pi = PendingIntent.getBroadcast(NoteActivity.this, _id,
				i, PendingIntent.FLAG_UPDATE_CURRENT);
		if (!alarmCalendar.before(Calendar.getInstance())) {// 判断时间设置是否合理
			am.set(AlarmManager.RTC_WAKEUP, alarmCalendar.getTimeInMillis(), pi);
			return true;
		}
		return false;
	}

	// 到了用户设定的提醒时间后,调用该函数弹出Dialog提醒用户
	private void noteAlarm(long noteId) {
		MyLog.d(MainActivity.TAG, "NoteActivity==>闹钟时间到达,要显示的记录的 id: " + noteId);
		// 传递note的id，在Dialog中显示出来
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("提醒");
		// 根据便签的ID查询该便签的详细内容
		Cursor c = getContentResolver().query(
				ContentUris.withAppendedId(NoteItems.CONTENT_URI, noteId),
				null, null, null, null);
		c.moveToFirst();
		// 最后更新便签 的日期时间及其内容
		String content = c.getString(c.getColumnIndex(NoteItems.CONTENT));
		MyLog.d(MainActivity.TAG, "NoteActivity==>闹钟显示时显示的内容: " + content);
		c.close();
		// dialog中显示便签的内容
		builder.setMessage(content);
		builder.setPositiveButton(R.string.Ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						MyLog.d(MainActivity.TAG,
								"NoteActivity==>release wakelock");
						WakeLockOpration.release();
						dialog.dismiss();
					}
				});
		builder.setNegativeButton(R.string.Cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						MyLog.d(MainActivity.TAG,
								"NoteActivity==>release wakelock");
						WakeLockOpration.release();
						NoteActivity.this.finish();
					}
				});
		builder.create().show();
	}

	@Override
	public void onBackPressed() {
		MyLog.d(MainActivity.TAG,
				"NoteActivity==>onBackPressed()-->用户选择的背景颜色 : "
						+ mBackgroud_Color);
		if (mBackgroud_Color == 0) {// 如果mBackgroud_Color==0,我们使用蓝色作为默认背景
			mBackgroud_Color = R.drawable.item_light_blue;
		}
		// 得到EditText中当前的内容
		String content = et_content.getText().toString();
		// 判断是更新还是新建便签
		if (openType.equals("newNote")) {
			// 创建主页上的便签(顶级便签)
			if (!TextUtils.isEmpty(content)) {
				ContentValues values = new ContentValues();
				values.put(NoteItems.CONTENT, content);
				values.put(NoteItems.UPDATE_DATE, DateTimeUtil.getDate());
				values.put(NoteItems.UPDATE_TIME, DateTimeUtil.getTime());
				values.put(NoteItems.BACKGROUND_COLOR, mBackgroud_Color);
				values.put(NoteItems.IS_FOLDER, "no");
				values.put(NoteItems.PARENT_FOLDER, -1);
				getContentResolver().insert(NoteItems.CONTENT_URI, values);
			}
		} else if (openType.equals("newFolderNote")) {
			// 创建文件夹下的便签
			if (!TextUtils.isEmpty(content)) {
				ContentValues values = new ContentValues();
				values.put(NoteItems.CONTENT, content);
				values.put(NoteItems.UPDATE_DATE, DateTimeUtil.getDate());
				values.put(NoteItems.UPDATE_TIME, DateTimeUtil.getTime());
				values.put(NoteItems.BACKGROUND_COLOR, mBackgroud_Color);
				values.put(NoteItems.IS_FOLDER, "no");
				values.put(NoteItems.PARENT_FOLDER, folderId);
				getContentResolver().insert(NoteItems.CONTENT_URI, values);
			}
		} else if (openType.equals("editNote")) {
			// 编辑主页上的便签
			if (!TextUtils.isEmpty(content)) {
				// 内容不为空,更新记录
				ContentValues values = new ContentValues();
				values.put(NoteItems.CONTENT, content);
				values.put(NoteItems.UPDATE_DATE, DateTimeUtil.getDate());
				values.put(NoteItems.UPDATE_TIME, DateTimeUtil.getTime());
				if (hasSetAlartTime) {// 如果用户设置了Alarm,则更新
					values.put(NoteItems.ALARM_TIME, mYear + "-" + mMonth + "-"
							+ mDay + " " + mHour + ":" + mMinute);
					MyLog.d(MainActivity.TAG, "NoteActivity==>提醒时间:" + mYear
							+ "-" + mMonth + "-" + mDay + " " + mHour + ":"
							+ mMinute);
				}
				values.put(NoteItems.BACKGROUND_COLOR, mBackgroud_Color);
				MyLog.d(MainActivity.TAG, "NoteActivity==>用户最终使用的背景颜色: "
						+ mBackgroud_Color);
				getContentResolver().update(
						ContentUris.withAppendedId(NoteItems.CONTENT_URI, _id),
						values, null, null);
			}
		} else if (openType.equals("editFolderNote")) {
			// 更新文件夹下的便签
			if (!TextUtils.isEmpty(content)) {
				// 更新记录
				ContentValues values = new ContentValues();
				values.put(NoteItems.CONTENT, content);
				values.put(NoteItems.UPDATE_DATE, DateTimeUtil.getDate());
				values.put(NoteItems.UPDATE_TIME, DateTimeUtil.getTime());
				if (hasSetAlartTime) {
					values.put(NoteItems.ALARM_TIME, mYear + "-" + mMonth + "-"
							+ mDay + " " + mHour + ":" + mMinute);
					MyLog.d(MainActivity.TAG, "提醒时间:" + mYear + "-" + mMonth
							+ "-" + mDay + " " + mHour + ":" + mMinute);
				}
				values.put(NoteItems.BACKGROUND_COLOR, mBackgroud_Color);
				values.put(NoteItems.IS_FOLDER, "no");
				values.put(NoteItems.PARENT_FOLDER, folderId);
				getContentResolver().update(
						ContentUris.withAppendedId(NoteItems.CONTENT_URI, _id),
						values, null, null);
				MyLog.d(MainActivity.TAG, "NoteActivity==>编辑文件夹下的记录时,文件夹的id : "
						+ folderId);
			}
		}
		if (!TextUtils.isEmpty(content)) {
			oldContent = content;
		}
		super.onBackPressed();
	}

	// 添加桌面快捷方式
	private void addShortCut() {
		Intent addShortCut = new Intent(ACTION_ADD_SHORTCUT);
		// 设置快捷方式的图标
		addShortCut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
				Intent.ShortcutIconResource.fromContext(this, R.drawable.icon));
		// 快捷方式的名称使用文件夹的名称.即oldFolderName变量的值
		// 不考虑文件夹名称太长的情况
		addShortCut.putExtra(Intent.EXTRA_SHORTCUT_NAME, oldContent);
		addShortCut.putExtra("duplicate", false);// 不允许重复创建快捷方式
		// 设置点击快捷方式后执行的Intent对象
		Intent shortCutIntent = new Intent(NoteActivity.this,
				NoteActivity.class);
		// 传递便签的ID
		shortCutIntent.putExtra(NoteItems._ID, _id);
		if (openType.equals("editNote")) {
			// 编辑主页上的便签
			shortCutIntent.putExtra("Open_Type", "editNote");
		} else if (openType.equals("editFolderNote")) {
			// 更新文件夹下的便签
			shortCutIntent.putExtra("Open_Type", "editFolderNote");
			// 传递文件夹ID
			shortCutIntent.putExtra("FolderId", folderId);
		}
		addShortCut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortCutIntent);
		// 发送广播,添加快捷方式
		sendBroadcast(addShortCut);
	}

	// 删除便签
	private void deleteNote() {
		Context mContext = NoteActivity.this;
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setTitle(R.string.delete_note);
		builder.setPositiveButton(R.string.Ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// 构造Uri
						Uri deleUri = ContentUris.withAppendedId(
								NoteItems.CONTENT_URI, _id);
						getContentResolver().delete(deleUri, null, null);
						MyLog.d(MainActivity.TAG,
								"NoteActivity==>deleteNote() via ContentResolver");
						// 返回上一级
						Intent intent = new Intent();
						if (openType.equals("editNote")) {
							// 显示主页
							intent.setClass(NoteActivity.this,
									MainActivity.class);
						} else if (openType.equals("editFolderNote")) {
							// 显示便签所属文件夹下页面
							intent.putExtra(NoteItems._ID, folderId);
							intent.setClass(NoteActivity.this,
									FolderNotesActivity.class);
						}
						startActivity(intent);
					}
				});
		builder.setNegativeButton(R.string.Cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// 点击取消按钮,撤销删除便签对话框
						dialog.dismiss();
					}
				});
		AlertDialog ad = builder.create();
		ad.show();
	}

	// 用短信或或邮件分享便签内容
	private void shareNote() {
		final CharSequence[] items = {
				getResources().getString(R.string.share_with_sms),
				getResources().getString(R.string.share_with_email) };

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				// 当前便签的内容
				String strContent = et_content.getText().toString();
				switch (item) {
				case 0:
					// 使用短信分享
					Uri smsToUri = Uri.parse("smsto:");// 联系人地址
					Intent mIntent = new Intent(
							android.content.Intent.ACTION_SENDTO, smsToUri);
					// 短信的内容
					mIntent.putExtra("sms_body", strContent);// 短信的内容
					startActivity(mIntent);
					Toast.makeText(NoteActivity.this,
							"启动" + items[item] + "程序中...", Toast.LENGTH_SHORT)
							.show();
					break;
				case 1:
					// 使用邮件分享
					Intent emailIntent = new Intent(
							android.content.Intent.ACTION_SEND);
					// 设置文本格式
					emailIntent.setType("text/plain");
					// 设置对方邮件地址
					emailIntent
							.putExtra(android.content.Intent.EXTRA_EMAIL, "");
					// 设置标题内容
					emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
							"通过XNote分享信息");
					// 设置邮件文本内容
					emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,
							strContent);
					startActivity(Intent.createChooser(emailIntent,
							"Choose Email Client"));
					Toast.makeText(NoteActivity.this,
							"启动" + items[item] + "程序中...", Toast.LENGTH_SHORT)
							.show();
					break;
				default:
					break;
				}
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
}