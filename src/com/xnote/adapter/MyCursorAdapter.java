package com.xnote.adapter;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.xnote.activity.MainActivity;
import com.xnote.activity.R;
import com.xnote.database.DbInfo.NoteItems;
import com.xnote.log.MyLog;

/*
 * 自定义Adapter,在移动、删除、显示记录时使用
 */
public class MyCursorAdapter extends CursorAdapter {
	// This class is used to instantiate layout XML file into its corresponding
	// View objects.
	private LayoutInflater mListContainer; // 视图容器
	// 存储CheckBox的状态,即是否被选中
	public static Map<Integer, Boolean> isSelected;
	// 删除记录、移动记录还是显示记录
	private boolean isShowingRecords = true;
	// 自定义视图
	private ListItemView listItemView = null;

	// 构造器
	public MyCursorAdapter(Context context, Cursor c, boolean isShowingRecords) {
		super(context, c);
		this.isShowingRecords = isShowingRecords;
		mListContainer = LayoutInflater.from(context); // 创建视图容器并设置上下文
		isSelected = new HashMap<Integer, Boolean>();
		// 初始化多选框的状态
		int count = c.getCount();
		for (int i = 0; i < count; i++) {
			isSelected.put(i, false);
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View convertView = null;
		listItemView = new ListItemView();
		if (!isShowingRecords) {// 删除或移动记录
			// 获取listview中每个item的布局文件的视图
			convertView = mListContainer.inflate(
					R.layout.listview_del_or_move_item_layout, null);
			// 获取控件对象
			listItemView.tv_left = (TextView) convertView
					.findViewById(R.id.tv_left);
			listItemView.cb_right = (CheckBox) convertView
					.findViewById(R.id.cb_right);
		} else { // 显示记录
			// 获取listview中每个item的布局文件的视图
			convertView = mListContainer.inflate(R.layout.listview_item_layout,
					null);
			// 获取控件对象
			listItemView.tv_left = (TextView) convertView
					.findViewById(R.id.tv_left);
			listItemView.tv_right = (TextView) convertView
					.findViewById(R.id.tv_right);
		}
		// 初始化ListView中每一行布局中的LinearLayout
		listItemView.linearlayout = (LinearLayout) convertView
				.findViewById(R.id.listview_linearlayout);
		// 设置控件集到convertView
		convertView.setTag(listItemView);
		return convertView;
	}

	// view newView函数的返回值
	// Cursor cursor记录的位置有系统管理，使用的时候将它当作只含有一个记录的对象。用户只需要直接用。
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		listItemView = (ListItemView) view.getTag();
		int position = cursor.getPosition();
		// 取出字段的值,判断该记录是否为文件夹
		String is_Folder = cursor.getString(cursor
				.getColumnIndex(NoteItems.IS_FOLDER));
		if (is_Folder.equals("no")) {
			// 不是文件夹
			int bg_color = cursor.getInt(cursor
					.getColumnIndex(NoteItems.BACKGROUND_COLOR));
			MyLog.d(MainActivity.TAG, "MyCursorAdapter==>数据库中存储的记录的背景颜色: "
					+ bg_color);
			// 因为我们在数据库中存储资源文件的ID,所以在这儿直接使用数据库中该字段的值
			listItemView.linearlayout.setBackgroundResource(bg_color);
		} else if (is_Folder.equals("yes")) {
			// 是文件夹,直接设置它的背景图片
			listItemView.linearlayout
					.setBackgroundResource(R.drawable.folder_background);
		}
		// 设置标题(或内容)
		String content = cursor.getString(cursor
				.getColumnIndex(NoteItems.CONTENT));
		// 如果内容太长或出现了换行符,则采用如下方式显示
		int count = content.indexOf("\n");
		MyLog.d(MainActivity.TAG, "MyCursorAdapter==>第一个换行符的位置:" + count);
		if (count > -1 && count < 17) {
			listItemView.tv_left.setText(content.substring(0, count) + "...");
		} else if (content.length() > 17) {
			listItemView.tv_left.setText(content.substring(0, 17) + "...");
		} else {
			listItemView.tv_left.setText(content);
		}
		if (!isShowingRecords) { // 移动或删除记录,使用CheckBox来供用户选择要操作的记录
			listItemView.cb_right.setChecked(isSelected.get(position));
		} else {// 显示记录,使用TextView来显示记录的最后更新时间
			// 显示创建(最后更新)记录的日期时间
			listItemView.tv_right.setText(cursor.getString(cursor
					.getColumnIndex(NoteItems.UPDATE_DATE))
					+ "\t"
					+ cursor.getString(
							cursor.getColumnIndex(NoteItems.UPDATE_TIME))
							.substring(0, 5));
		}
	}
}