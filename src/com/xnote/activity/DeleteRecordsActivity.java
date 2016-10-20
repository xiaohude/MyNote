package com.xnote.activity;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

import com.xnote.adapter.ListItemView;
import com.xnote.adapter.MyCursorAdapter;
import com.xnote.database.DbInfo.NoteItems;
import com.xnote.log.MyLog;

/*
 * 删除记录页面
 */
public class DeleteRecordsActivity extends Activity {
	private MyCursorAdapter mAdapter;
	private ListView mListView;
	private Button btnOK, btnCancel;

	// 用于收集被选中的item的id.以<position,id>形式存放
	private Map<Integer, Integer> mIds;
	private Cursor mCursor;

	private OnClickListener listener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.btnOK:
				int count = mCursor.getCount();
				if (count > 0) {// 有记录被选中
					for (int i = 0; i < count; i++) {
						String strTmp = String.valueOf(mIds.get(i));
						if (!TextUtils.isEmpty(strTmp)) {// 如果不为空,则可删除
							// 使用构建的Uri删除数据(如果是文件夹，则会删除文件夹下的所有数据)
							String id = String.valueOf(mIds.get(i));
							getContentResolver().delete(
									NoteItems.CONTENT_URI,
									" _id = ? or " + NoteItems.PARENT_FOLDER
											+ " = ? ", new String[] { id, id });
						}
					}
				}
				finish();
				break;
			case R.id.btnCancelDel:
				finish();
				break;
			default:
				break;
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 取消标题栏
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.listview_layout_del_or_move_records);

		btnOK = (Button) findViewById(R.id.btnOK);
		btnCancel = (Button) findViewById(R.id.btnCancelDel);
		mListView = (ListView) findViewById(R.id.listview);

		mIds = new HashMap<Integer, Integer>();

		// 判断删除主页的记录还是文件夹下的记录
		int folderId = getIntent().getIntExtra("folderId", -1);
		if (folderId == -1) {// 要删除主页上的记录,因此查询主页上的记录
			String selection = NoteItems.IS_FOLDER + " = '" + "yes" + "' or "
					+ NoteItems.PARENT_FOLDER + " = " + "-1";
			mCursor = getContentResolver().query(NoteItems.CONTENT_URI, null,
					selection, null, null);

		} else {// 删除文件夹下的记录，因为我们得到了文件夹的_id
			// 查询ID为folderId的文件夹下的记录
			String selection = NoteItems.PARENT_FOLDER + "  = ? ";
			String[] selectionArgs = new String[] { String.valueOf(folderId) };
			mCursor = getContentResolver().query(NoteItems.CONTENT_URI, null,
					selection, selectionArgs, null);
		}
		startManagingCursor(mCursor);
		mAdapter = new MyCursorAdapter(getApplicationContext(), mCursor, false);
		mListView.setAdapter(mAdapter);
		mListView.setItemsCanFocus(false);
		mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				ListItemView listItems = (ListItemView) view.getTag();
				listItems.cb_right.toggle();
				MyCursorAdapter.isSelected.put(position,
						listItems.cb_right.isChecked());
				mCursor.moveToPosition(position);
				int itemId = mCursor.getInt(mCursor
						.getColumnIndex(NoteItems._ID));
				if (MyCursorAdapter.isSelected.get(position)) {
					mIds.put(position, itemId);
					MyLog.d(MainActivity.TAG,
							"DeleteRecordsActivity==>被点击的记录的id : " + itemId
									+ "\t" + position);
				} else {
					mIds.remove(position);
				}
			}
		});
		btnOK.setOnClickListener(listener);
		btnCancel.setOnClickListener(listener);
	}
}