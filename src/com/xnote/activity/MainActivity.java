package com.xnote.activity;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.xnote.adapter.MyCursorAdapter;
import com.xnote.database.DateTimeUtil;
import com.xnote.database.DbInfo.NoteItems;
import com.xnote.log.MyLog;
import com.xnote.xml_txt.RestoreDataFromXml;
import com.xnote.xml_txt.WriteTxt;
import com.xnote.xml_txt.WriteXml;

/*
 * 显示所有的文件夹和没有父文件夹的便签
 */
public class MainActivity extends Activity implements
		OnGesturePerformedListener {
	/** Called when the activity is first created. */
	private static final String SETTINGS = "user_configurations";
	// 手势相关
	private GestureOverlayView mGestureOverlayView;
	private GestureLibrary mGestureLibrary;
	private String GestureName_Add = "add_Record";

	private ImageButton imageButton;
	private ListView mListview;

	private MyCursorAdapter mAdapter;
	private Cursor mCursor;
	// 菜单
	private static final int MENU_NEW_NOTE = Menu.FIRST;
	private static final int MENU_NEW_FOLDER = Menu.FIRST + 1;
	private static final int MENU_MOVE_TO_FOLDER = Menu.FIRST + 2;
	private static final int MENU_DELETE = Menu.FIRST + 3;
	private static final int MENU_EXPORT_TO_TEXT = Menu.FIRST + 4;
	private static final int MENU_BACKUP_DATA = Menu.FIRST + 5;
	private static final int MENU_RESTORE_DATA_FROM_SDCARD = Menu.FIRST + 6;
	private static final int MENU_SET_PASSWORD = Menu.FIRST + 7;
	private static final int MENU_ABOUT = Menu.FIRST + 8;

	public static final String TAG = "Note";

	// 如果用户输入的密码错误,使用该变量记录错误次数
	private int count = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 取消标题栏
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.index_page);
		// 判断有无密码,如果有,检测输入的密码是否正确
		this.inputPsd();

		// 加载手势库文件
		mGestureLibrary = GestureLibraries
				.fromRawResource(this, R.raw.gestures);
		if (mGestureLibrary.load()) {
			mGestureOverlayView = (GestureOverlayView) findViewById(R.id.gestureOverlayView);
			mGestureOverlayView.addOnGesturePerformedListener(this);
		}

		mListview = (ListView) findViewById(R.id.list);
		// 更新ListView数据
		this.updateDisplay();
		mListview.setOnItemClickListener(new OnItemClickListener() {
			// 点击文件夹或者便签执行该回调函数
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent();
				// mCursor在updateDisplay函数中进行初始化
				mCursor.moveToPosition(position);
				MyLog.d(TAG, "MainActivity==>被点击的记录的Position : " + position);
				// 传递被选中记录的ID
				intent.putExtra(NoteItems._ID,
						mCursor.getInt(mCursor.getColumnIndex(NoteItems._ID)));
				// 取得此记录的IS_FOLDER字段的值,用以判断选中文件夹还是便签
				String is_Folder = mCursor.getString(mCursor
						.getColumnIndex(NoteItems.IS_FOLDER));
				if (is_Folder.equals("no")) {
					// 不是文件夹
					// 跳转到详细内容页面
					// 传递此记录的CONTENT字段的值
					intent.putExtra(NoteItems.CONTENT, mCursor
							.getString(mCursor
									.getColumnIndex(NoteItems.CONTENT)));
					// 告诉NoteActivity打开它是为了编辑便签
					intent.putExtra("Open_Type", "editNote");
					intent.setClass(MainActivity.this, NoteActivity.class);
				} else if (is_Folder.equals("yes")) {
					// 是文件夹
					// 跳转到FileNotesActivity,显示选中的文件夹下所有的便签
					intent.setClass(MainActivity.this,
							FolderNotesActivity.class);
				}
				startActivity(intent);
			}
		});
		// 调用该函数,执行一些初始化的操作
		initViews();
	}

	// 创建菜单
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// 新建便签
		menu.add(Menu.NONE, MENU_NEW_NOTE, 1, R.string.new_note).setIcon(
				R.drawable.new_note);
		// 新建文件夹
		menu.add(Menu.NONE, MENU_NEW_FOLDER, 2, R.string.new_folder).setIcon(
				R.drawable.new_folder);
		// 移进文件夹
		menu.add(Menu.NONE, MENU_MOVE_TO_FOLDER, 3, R.string.move_to_folder)
				.setIcon(R.drawable.move_to_folder);
		// 删除
		menu.add(Menu.NONE, MENU_DELETE, 4, R.string.delete).setIcon(
				R.drawable.delete);
		// 输出TXT文本
		menu.add(Menu.NONE, MENU_EXPORT_TO_TEXT, 5, R.string.export_to_text)
				.setIcon(R.drawable.export_to_text);
		// 备份数据
		menu.add(Menu.NONE, MENU_BACKUP_DATA, 6, R.string.backup_data);
		// 从SD卡还原
		menu.add(Menu.NONE, MENU_RESTORE_DATA_FROM_SDCARD, 7,
				R.string.restore_data);
		// 设置密码
		menu.add(Menu.NONE, MENU_SET_PASSWORD, 8, R.string.set_password);
		// 关于
		menu.add(Menu.NONE, MENU_ABOUT, 9, R.string.about);
		return super.onCreateOptionsMenu(menu);
	}

	// 菜单选中事件处理函数
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_NEW_NOTE:
			newNote();
			break;
		case MENU_NEW_FOLDER:
			newFolder();
			break;
		case MENU_MOVE_TO_FOLDER:
			moveToFolder();
			break;
		case MENU_DELETE:
			delete();
			break;
		case MENU_EXPORT_TO_TEXT:
			this.exportToTxt();
			break;
		case MENU_BACKUP_DATA:
			this.backupData();
			break;
		case MENU_RESTORE_DATA_FROM_SDCARD:
			this.restoreDataFromSDCard();
			break;
		case MENU_SET_PASSWORD:
			psdDialog();
			break;
		case MENU_ABOUT:
			// 不做了
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	// 新建便签函数
	private void newNote() {
		Intent i = new Intent();
		i.putExtra("Open_Type", "newNote");
		i.setClass(MainActivity.this, NoteActivity.class);
		startActivity(i);
	}

	// 新建文件夹函数
	private void newFolder() {
		Context mContext = MainActivity.this;
		// 使用AlertDialog来处理新建文件夹的动作
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setTitle(R.string.new_folder);
		builder.setIcon(null);
		// 自定义AlertDialog的布局
		LayoutInflater inflater = (LayoutInflater) mContext
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		final View layout = inflater.inflate(R.layout.dialog_layout_new_folder,
				(ViewGroup) findViewById(R.id.dialog_layout_new_folder_root));
		builder.setView(layout);
		// 设置一个类似确定的按钮
		builder.setPositiveButton(R.string.Ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// 实例化AlertDialog中的EditText对象
						EditText et_folder_name = (EditText) layout
								.findViewById(R.id.et_dialog_new_folder);
						// 取得EditText对象的值
						String newFolderName = et_folder_name.getText()
								.toString();
						// 判断文件夹名称是否为空
						if (!TextUtils.isEmpty(newFolderName)) {
							// 名称符合条件则插入数据库
							ContentValues values = new ContentValues();
							values.put(NoteItems.CONTENT, newFolderName);
							values.put(NoteItems.UPDATE_DATE,
									DateTimeUtil.getDate());
							values.put(NoteItems.UPDATE_TIME,
									DateTimeUtil.getTime());
							values.put(NoteItems.IS_FOLDER, "yes");
							values.put(NoteItems.PARENT_FOLDER, -1);
							getContentResolver().insert(NoteItems.CONTENT_URI,
									values);
							// 更新ListView的数据源
							mAdapter.notifyDataSetChanged();
						}
					}
				});
		// 设置一个类似取消的按钮
		builder.setNegativeButton(R.string.Cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// 点击取消按钮,撤销新建文件夹对话框
						dialog.dismiss();
					}
				});
		AlertDialog ad = builder.create();
		ad.show();
	}

	// 移进文件夹函数
	private void moveToFolder() {
		Intent intent = new Intent();
		intent.setClass(MainActivity.this, MoveToFolderActivity.class);
		startActivity(intent);
	}

	// 删除函数
	private void delete() {
		Intent i = new Intent(getApplicationContext(),
				DeleteRecordsActivity.class);
		startActivity(i);
	}

	// 输出TXT文本函数
	private void exportToTxt() {
		// 通过WriteTxt类实现TXT文本输出
		WriteTxt wt = new WriteTxt(this);
		try {
			// 如果写入失败,则用Toast提醒用户
			if (!wt.writeTxt()) {
				Toast.makeText(this, R.string.exportTXTFailed,
						Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, R.string.exportTXTSuc, Toast.LENGTH_SHORT)
						.show();
			}
		} catch (IOException e) {
			// 有待改进,以改善UE
			MyLog.d(TAG,
					"MainActivity==>exportToTxt get Exception : "
							+ e.getMessage());
			e.printStackTrace();
		}
	}

	// 备份数据函数(备份到XML文件中)
	private void backupData() {
		WriteXml wx = new WriteXml(this);
		try {
			// 如果写入失败,则用Toast提醒用户
			if (!wx.writeXml()) {
				Toast.makeText(this, R.string.backupDataFailed,
						Toast.LENGTH_SHORT).show();
				MyLog.d(TAG, "MainActivity==>backup to SDCard failed");
			} else {
				Toast.makeText(this, R.string.backupDataSuc, Toast.LENGTH_SHORT)
						.show();
				MyLog.d(TAG, "MainActivity==>backup to SDCard successfully");
			}
		} catch (Exception e) {
			// 有待改进,以改善UE
			MyLog.d(TAG,
					"MainActivity==>backupData get Exception : "
							+ e.getMessage());
			e.printStackTrace();
		}
	}

	// 从SD卡恢复数据函数
	private void restoreDataFromSDCard() {
		RestoreDataFromXml rsd = new RestoreDataFromXml(getContentResolver());
		try {
			rsd.restoreData();
			mAdapter.notifyDataSetChanged();
			Toast.makeText(this, R.string.restoreDataSuc, Toast.LENGTH_SHORT)
					.show();
		} catch (Exception e) {
			Toast.makeText(this, R.string.restoreDataFailed, Toast.LENGTH_SHORT)
					.show();
			MyLog.d(TAG,
					"MainActivity==>restoreDataFromSDCard Failed Exception : "
							+ e.getMessage());
			e.printStackTrace();
		}
	}

	// 设置密码、修改密码或清除密码对话框
	private void psdDialog() {
		// 创建SharedPreferences对象,使用它保存用户的密码
		SharedPreferences settings = getSharedPreferences(SETTINGS,
				MODE_PRIVATE);
		String psd = settings.getString("psd", "");
		if (psd.length() > 0) {
			// 有密码,点击设置密码后应该弹出有清除密码和修改密码两个选项的Dialog
			final CharSequence[] items = {
					getResources().getString(R.string.change_password),
					getResources().getString(R.string.clear_password) };
			// 使用AlertDialog来实现
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			// 设置AlertDialog要显示的item
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					switch (item) {
					case 0:
						// 选中修改密码,调用设置密码的函数
						setPassword(R.string.change_password);
						break;
					case 1:
						// 选中清除密码
						SharedPreferences settings = getSharedPreferences(
								SETTINGS, MODE_PRIVATE);
						Editor editor = settings.edit();
						// 清空SharedPreferences中的密码
						editor.putString("psd", "");
						editor.commit();
						Toast.makeText(
								MainActivity.this,
								getResources().getString(
										R.string.change_password)
										+ "成功!", Toast.LENGTH_SHORT).show();
						break;
					default:
						break;
					}
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
		} else {
			// 没有密码,直接弹出设置密码对话框
			this.setPassword(R.string.set_password);
		}
	}

	// 设置密码函数,传递字符串在R.java中的int值
	private void setPassword(int resId) {
		final int name = resId;
		Context mContext = MainActivity.this;
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		// 自定义AlertDialog的布局方式
		LayoutInflater inflater = (LayoutInflater) mContext
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		final View layout = inflater.inflate(
				R.layout.dialog_layout_set_password,
				(ViewGroup) findViewById(R.id.dialog_layout_set_password_root));
		builder.setView(layout);
		builder.setTitle(name);
		builder.setPositiveButton(R.string.Ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						EditText et_psw1 = (EditText) layout
								.findViewById(R.id.et_password);
						String psw1 = et_psw1.getText().toString();
						EditText et_psw2 = (EditText) layout
								.findViewById(R.id.et_confirm_Password);
						String psw2 = et_psw2.getText().toString();
						// 判断密码是否一致,且不为空
						if (!TextUtils.isEmpty(psw1) && (psw1.equals(psw2))) {
							// 密码一致,写入SharedPreference
							SharedPreferences settings = getSharedPreferences(
									SETTINGS, MODE_PRIVATE);
							Editor editor = settings.edit();
							editor.putString("psd", psw1);
							editor.commit();
							switch (name) {
							case R.string.change_password:
								Toast.makeText(
										MainActivity.this,
										getResources().getString(
												R.string.change_password_succ),
										Toast.LENGTH_SHORT).show();
								break;
							case R.string.set_password:
								Toast.makeText(
										MainActivity.this,
										getResources().getString(
												R.string.set_password_succ),
										Toast.LENGTH_SHORT).show();
								break;
							default:
								break;
							}
						} else {
							switch (name) {
							case R.string.change_password:
								Toast.makeText(
										MainActivity.this,
										getResources()
												.getString(
														R.string.change_password_failed),
										Toast.LENGTH_SHORT).show();
								break;
							case R.string.set_password:
								Toast.makeText(
										MainActivity.this,
										getResources().getString(
												R.string.set_password_failde),
										Toast.LENGTH_SHORT).show();
								break;
							default:
								break;
							}
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							setPassword(name);
						}
					}
				});
		builder.setNegativeButton(R.string.Cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// 点击取消按钮,撤销设置密码对话框
						dialog.dismiss();
					}
				});
		AlertDialog ad = builder.create();
		ad.show();
	}

	// 如果有密码,程序运行时先让用户输入密码
	private void inputPsd() {
		// 用于统计输入密码的次数
		count++;
		SharedPreferences settings = getSharedPreferences(SETTINGS,
				MODE_PRIVATE);
		final String psd = settings.getString("psd", "");
		// 判断是否有密码
		if (psd.length() > 0) {
			// 有密码
			Context mContext = MainActivity.this;
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setTitle("输入密码");
			LayoutInflater inflater = (LayoutInflater) mContext
					.getSystemService(LAYOUT_INFLATER_SERVICE);
			final View layout = inflater.inflate(R.layout.dialog_input_psd,
					(ViewGroup) findViewById(R.id.dialog_input_psd_root));
			builder.setView(layout);
			builder.setPositiveButton(R.string.Ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							EditText et_psd = (EditText) layout
									.findViewById(R.id.et_input_password);
							String psd_inputted = et_psd.getText().toString();
							if (!psd.equals(psd_inputted)) {
								Toast.makeText(MainActivity.this, "密码不正确!",
										Toast.LENGTH_SHORT).show();
								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								// 只允许填错密码3次
								if (count < 3) {
									inputPsd();
								} else {
									MainActivity.this.finish();
								}
							}
						}
					});
			builder.setNegativeButton(R.string.Cancel,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							MainActivity.this.finish();// 结束程序
						}
					});
			// 设置对话框不可取消.可以修正用户设置了密码,在弹出输入密码对话框时点返回键取消了输入密码对话框的BUG
			builder.setCancelable(false);
			AlertDialog ad = builder.create();
			ad.show();
		}
	}

	// 负责更新ListView中的数据
	private void updateDisplay() {
		// 查询条件，查询所有文件夹记录及显示在主页的便签记录
		String selection = NoteItems.IS_FOLDER + " = '" + "yes" + "' or "
				+ NoteItems.PARENT_FOLDER + " = " + "-1";

		mCursor = getContentResolver().query(NoteItems.CONTENT_URI, null,
				selection, null, null);
		// This method allows the activity to take care of managing the given
		// Cursor's lifecycle for you based on the activity's lifecycle.
		startManagingCursor(mCursor);
		mAdapter = new MyCursorAdapter(this, mCursor, true);
		mListview.setAdapter(mAdapter);
		MyLog.d(TAG, "MainActivity==>Update Display finished...");
	}

	// 初始化组件
	private void initViews() {
		imageButton = (ImageButton) findViewById(R.id.imageButton);
		imageButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				newNote();
			}
		});
	}
	
	public void OnOpenMenu(View view) {
		openOptionsMenu();
	}

	@Override
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
		ArrayList<Prediction> predictions = mGestureLibrary.recognize(gesture);
		for (Prediction pre : predictions) {
			MyLog.d(TAG, "MainActivity===>>onGesturePerformed-->手势相似度: "
					+ pre.score);
			if (pre.score > 2.0 && pre.name.equals(this.GestureName_Add)) {// 认为手势合理
				newFolder();// 创建文件夹
			}
		}
	}
}