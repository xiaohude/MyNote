package com.xnote.activity;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.xnote.activity.R;
import com.xnote.adapter.MyCursorAdapter;
import com.xnote.database.DateTimeUtil;
import com.xnote.database.DbInfo.NoteItems;
import com.xnote.log.MyLog;

/*
 * 显示某一文件夹下的所有便签
 */
public class FolderNotesActivity extends Activity implements
		OnGesturePerformedListener {
	private ImageButton imageButton;
	private TextView tvTitle;
	private ListView mListview;
	// 手势相关
	private GestureOverlayView mGestureOverlayView;
	private GestureLibrary mGestureLibrary;
	private String GestureName_Add = "add_Record";

	private MyCursorAdapter mAdapter;

	private Cursor mCursor;
	// 得到点击修改文件夹名称Menu以前的名称
	private String oldFolderName;
	// 添加快捷方式时使用
	private final String ACTION_ADD_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";

	// 菜单
	private static final int MENU_NEW_NOTE = Menu.FIRST;
	// 修改文件夹名称
	private static final int MENU_UPDATE_FOLDER = Menu.FIRST + 1;
	private static final int MENU_MOVE_OUTOF_FOLDER = Menu.FIRST + 2;
	private static final int MENU_DELETE = Menu.FIRST + 3;
	// 创建桌面快捷方式
	private static final int MENU_SEND_HOME = Menu.FIRST + 4;

	// 文件夹的ID
	private int _id;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 取消标题栏
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.index_page);

		// 读取GestureLibrary
		mGestureLibrary = GestureLibraries
				.fromRawResource(this, R.raw.gestures);
		if (mGestureLibrary.load()) {
			mGestureOverlayView = (GestureOverlayView) findViewById(R.id.gestureOverlayView);
			mGestureOverlayView.addOnGesturePerformedListener(this);
		}
		Intent i = getIntent();
		// 如果没有传递Intent对象,则返回主页(MainActivity)
		if (i.equals(null)) {
			startActivity(new Intent(FolderNotesActivity.this,
					MainActivity.class));
		}
		_id = i.getIntExtra(NoteItems._ID, -1);
		// 查询该文件夹记录.内容保存到Cursor对象中
		Uri tmpUri = ContentUris.withAppendedId(NoteItems.CONTENT_URI, _id);
		Cursor c2 = getContentResolver().query(tmpUri, null, null, null, null);
		c2.moveToFirst();
		oldFolderName = c2.getString(c2.getColumnIndex(NoteItems.CONTENT));
		c2.close();
		initViews();
		mListview.setOnItemClickListener(new OnItemClickListener() {
			// 点击文件夹下的便签执行该回调函数
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent();

				mCursor.moveToPosition(position);
				// 传递被选中记录的ID
				intent.putExtra(NoteItems._ID,
						mCursor.getInt(mCursor.getColumnIndex(NoteItems._ID)));
				// 传递当前文件夹的ID
				intent.putExtra("FolderId", _id);
				MyLog.d(MainActivity.TAG, "FolderNotesActivity==>进入id为: " + _id
						+ " 的文件夹");
				// 传递被编辑便签的内容
				intent.putExtra(NoteItems.CONTENT, mCursor.getString(mCursor
						.getColumnIndex(NoteItems.CONTENT)));
				// 编辑便签的方式
				intent.putExtra("Open_Type", "editFolderNote");
				// 跳转到NoteActivity
				intent.setClass(FolderNotesActivity.this, NoteActivity.class);
				startActivity(intent);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// 新建便签
		menu.add(Menu.NONE, MENU_NEW_NOTE, 1, R.string.new_note).setIcon(
				R.drawable.new_note);
		// 修改文件夹名称
		menu.add(Menu.NONE, MENU_UPDATE_FOLDER, 2, R.string.edit_folder_title)
				.setIcon(R.drawable.edit_folder_title);
		// 移出文件夹
		menu.add(Menu.NONE, MENU_MOVE_OUTOF_FOLDER, 3,
				R.string.move_out_of_folder).setIcon(
				R.drawable.move_out_of_folder);
		// 删除
		menu.add(Menu.NONE, MENU_DELETE, 4, R.string.delete).setIcon(
				R.drawable.delete);
		// 添加到桌面(shortcut)
		menu.add(Menu.NONE, MENU_SEND_HOME, 5, R.string.add_shortcut_to_home)
				.setIcon(R.drawable.add_shortcut_to_home);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_NEW_NOTE:
			newFolderNote();
			break;
		case MENU_UPDATE_FOLDER:
			updateFolderName();
			break;
		case MENU_MOVE_OUTOF_FOLDER:
			moveOutOfFolder();
			break;
		case MENU_DELETE:
			delete();
			break;
		case MENU_SEND_HOME:
			// 添加快捷方式
			Intent addShortCut = new Intent(ACTION_ADD_SHORTCUT);
			// 快捷方式的图标
			addShortCut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
					Intent.ShortcutIconResource.fromContext(this,
							R.drawable.shortcut_folder));
			// 快捷方式的名称是文件夹的名称.即oldFolderName变量的值
			addShortCut.putExtra(Intent.EXTRA_SHORTCUT_NAME, oldFolderName);
			addShortCut.putExtra("duplicate", false);// 不允许重复创建快捷方式
			// 设置点击快捷方式后执行的Intent对象
			Intent shortCutIntent = new Intent(FolderNotesActivity.this,
					FolderNotesActivity.class);
			// 传递被选中文件夹的ID
			shortCutIntent.putExtra(NoteItems._ID, _id);
			addShortCut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortCutIntent);
			sendBroadcast(addShortCut);
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	// 初始化组件
	private void initViews() {
		imageButton = (ImageButton) findViewById(R.id.imageButton);
		imageButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				newFolderNote();
			}
		});
		mListview = (ListView) findViewById(R.id.list);
		tvTitle = (TextView) findViewById(R.id.tvTitle);

		updateDisplay(oldFolderName);
	}

	// 新建便签函数
	private void newFolderNote() {
		Intent i = new Intent();
		// 传递打开NoteActivity的方式
		i.putExtra("Open_Type", "newFolderNote");
		// 传递文件夹ID
		i.putExtra("FolderId", _id);
		i.setClass(FolderNotesActivity.this, NoteActivity.class);
		startActivity(i);
	}

	// 修改文件夹名称
	private void updateFolderName() {
		Context mContext = FolderNotesActivity.this;
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setTitle(R.string.edit_folder_title);

		LayoutInflater inflater = (LayoutInflater) mContext
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		final View layout = inflater.inflate(R.layout.dialog_layout_new_folder,
				(ViewGroup) findViewById(R.id.dialog_layout_new_folder_root));
		builder.setView(layout);
		builder.setPositiveButton(R.string.Ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						EditText et_folder_name = (EditText) layout
								.findViewById(R.id.et_dialog_new_folder);
						String newFolderName = et_folder_name.getText()
								.toString();
						if ((!TextUtils.isEmpty(newFolderName))
								&& newFolderName != oldFolderName) {
							// 新文件夹名称不为空,且不等于原有的名称,则更新
							Uri tmpUri = ContentUris.withAppendedId(
									NoteItems.CONTENT_URI, _id);
							ContentValues values = new ContentValues();
							values.put(NoteItems.CONTENT, newFolderName);
							values.put(NoteItems.UPDATE_DATE,
									DateTimeUtil.getDate());
							values.put(NoteItems.UPDATE_TIME,
									DateTimeUtil.getTime());
							getContentResolver().update(tmpUri, values, null,
									null);
							oldFolderName = newFolderName;
							tvTitle.setText(newFolderName + "文件夹下");
						}
					}
				});
		builder.setNegativeButton(R.string.Cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// 点击取消按钮,撤销修改文件夹名称对话框
						dialog.dismiss();
					}
				});
		AlertDialog ad = builder.create();
		ad.show();
	}

	// 移出文件夹
	private void moveOutOfFolder() {
		Intent i = new Intent();
		i.setClass(FolderNotesActivity.this, MoveOutOfFolderActivity.class);
		// 传递文件夹的ID
		i.putExtra("folderId", _id);
		startActivity(i);
	}

	// 删除函数,选择删除文件夹下的便签
	private void delete() {
		Intent i = new Intent(getApplicationContext(),
				DeleteRecordsActivity.class);
		// 传递文件夹的ID
		i.putExtra("folderId", _id);
		startActivity(i);
	}

	// 负责更新数据
	private void updateDisplay(String folderName) {
		// 查询所属文件夹Id为_id的记录
		String selection = NoteItems.PARENT_FOLDER + "  = ? ";
		String[] selectionArgs = new String[] { String.valueOf(_id) };
		mCursor = getContentResolver().query(NoteItems.CONTENT_URI, null,
				selection, selectionArgs, null);
		startManagingCursor(mCursor);
		mAdapter = new MyCursorAdapter(this, mCursor, true);
		mListview.setAdapter(mAdapter);

		tvTitle.setText(folderName);
	}

	@Override
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
		ArrayList<Prediction> predictions = mGestureLibrary.recognize(gesture);
		for (Prediction pre : predictions) {
			MyLog.d(MainActivity.TAG,
					"FolderNotesActivity===>>onGesturePerformed-->手势匹配度: "
							+ pre.score);
			if (pre.name.equals(this.GestureName_Add) && pre.score > 2.0) {
				newFolderNote();
			}
		}
	}
	
	public void OnOpenMenu(View view) {
		openOptionsMenu();
	}
}