package com.xnote.activity;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.xnote.adapter.ListItemView;
import com.xnote.adapter.MyCursorAdapter;
import com.xnote.database.DbInfo.NoteItems;
import com.xnote.log.MyLog;

public class MoveOutOfFolderActivity extends Activity {
	private MyCursorAdapter mAdapter;
	private ListView mListview;
	private Button btnOK, btnCancel;

	// 数组,用于收集被选中的item的id
	private Map<Integer, Integer> mIds = new HashMap<Integer, Integer>();
	private Cursor mCursor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 取消标题栏
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.listview_layout_del_or_move_records);

		btnOK = (Button) findViewById(R.id.btnOK);
		btnCancel = (Button) findViewById(R.id.btnCancelDel);
		mListview = (ListView) findViewById(R.id.listview);

		// 查询文件夹下的便签
		Intent intent = getIntent();
		int folderId = intent.getIntExtra("folderId", -1);
		MyLog.d(MainActivity.TAG, "MoveOutOfFolderActivity==>被操作的文件夹的ID :　"
				+ folderId);
		String selection = NoteItems.PARENT_FOLDER + "  = ? ";
		String[] selectionArgs = new String[] { String.valueOf(folderId) };
		mCursor = getContentResolver().query(NoteItems.CONTENT_URI, null,
				selection, selectionArgs, null);
		startManagingCursor(mCursor);
		mAdapter = new MyCursorAdapter(getApplicationContext(), mCursor, false);
		mListview.setAdapter(mAdapter);
		mListview.setItemsCanFocus(false);
		mListview.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		mListview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				ListItemView listItems = (ListItemView) view.getTag();
				// 改变CheckBox的状态
				listItems.cb_right.toggle();
				MyCursorAdapter.isSelected.put(position,
						listItems.cb_right.isChecked());
				mCursor.moveToPosition(position);

				if (MyCursorAdapter.isSelected.get(position)) {
					// 获取对应位置上的记录的ID
					int itemId = mCursor.getInt(mCursor
							.getColumnIndex(NoteItems._ID));
					mIds.put(position, itemId);
					MyLog.d(MainActivity.TAG,
							"MoveOutOfFolderActivity==>被点击的记录的id : " + itemId
									+ "\t" + position);
				} else {
					mIds.remove(position);
				}
			}
		});
		btnOK.setOnClickListener(listener);
		btnCancel.setOnClickListener(listener);
	}

	private OnClickListener listener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.btnOK:
				// 更新记录
				moveOutOfFolder();
				break;
			case R.id.btnCancelDel:
				finish();
				break;
			default:
				break;
			}
		}
	};

	private void moveOutOfFolder() {
		// 先判断是否选择了记录
		final int noteCount = mIds.size();
		if (noteCount > 0) {// 选择了要移出文件夹的便签
			MyLog.d(MainActivity.TAG, "MoveOutOfFolderActivity==>被选中的便签数量:"
					+ noteCount);
			// 文件夹下的记录数
			int count = mCursor.getCount();
			for (int i = 0; i < count; i++) {
				String strTmp = String.valueOf(mIds.get(i));
				if (!(strTmp == "null")) {// 如果不为空,则可更新
					// 得到被选择的记录的ID
					int noteId = mIds.get(i);
					Uri tmpUri = ContentUris.withAppendedId(
							NoteItems.CONTENT_URI, noteId);
					Cursor oneNote = getContentResolver().query(tmpUri, null,
							null, null, null);
					startManagingCursor(oneNote);
					oneNote.moveToFirst();
					// 更新记录,即设置该记录的ParentFolder值为-1
					ContentValues values = new ContentValues();
					values.put(NoteItems.PARENT_FOLDER, -1);
					getContentResolver().update(tmpUri, values, null, null);
					MyLog.d(MainActivity.TAG,
							"MoveOutOfFolderActivity==>最后选择的要移出文件夹的记录的id : "
									+ mIds.get(i));
				}
			}
			finish();
		} else {// 用户未曾选择任何便签
			Toast.makeText(getApplicationContext(), "您未选择要移出文件夹的便签!",
					Toast.LENGTH_LONG).show();
		}
	}
}