package com.xnote.database;

import android.net.Uri;
import android.provider.BaseColumns;

// 描述数据库、表的信息
public final class DbInfo {

	public static final String AUTHORITY = "com.xnote.provider.DbInfo";

	// This class cannot be instantiated
	private DbInfo() {
	}

	// 数据库表：NoteItems
	public static final class NoteItems implements BaseColumns {
		// This class cannot be instantiated
		private NoteItems() {
		}

		// The content:// style URL for this table
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/noteitems");
		// The MIME type of CONTENT_URI providing a directory of NoteItems.
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.xnote.noteitem";

		// The MIME type of a CONTENT_URI sub-directory of a single NoteItem.
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.xnote.noteitem";

		// 便签内容(text类型)
		public static final String CONTENT = "content";
		// 最后更新的日期(date类型),由于ContentValues的原因，我们把Date类型的数据转为String类型
		public static final String UPDATE_DATE = "update_date";
		// 最后更新的时间(time类型)，由于ContentValues的原因，我们把Time类型的数据转为String类型
		public static final String UPDATE_TIME = "update_time";
		// 闹钟提醒时间(datetime类型:YYYY-MM-DD
		// HH:MM:SS)由于ContentValues的原因，我们把类型转为String类型
		public static final String ALARM_TIME = "alarm_time";
		// 便签的背景颜色(integer类型,直接存储资源文件的id)
		public static final String BACKGROUND_COLOR = "background_color";
		// 标识是否为文件夹(text类型,yes和no来区分)
		public static final String IS_FOLDER = "is_folder";
		// 如果是文件夹下的便签,本字段存储其所属文件夹(Integer类型(存储被标记为文件夹的记录的_id字段的值))
		// 如果是顶级便签，该字段用-1代替
		public static final String PARENT_FOLDER = "parent_folder";
		// 默认的排序方式。
		public static final String DEFAULT_SORT_ORDER = IS_FOLDER + "  DESC , "
				+ UPDATE_DATE + " DESC ," + UPDATE_TIME + " DESC";
	}

	// 用于存放Appwidget记录的表
	public static final class AppwidgetItems implements BaseColumns {
		// This class cannot be instantiated
		private AppwidgetItems() {
		}

		// The content:// style URL for this table
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/appwidgetitems");
		// The MIME type of CONTENT_URI providing a directory of AppwidgetItems.
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.xnote.appwidgetitem";

		// The MIME type of a CONTENT_URI sub-directory of a single NoteItem.
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.xnote.appwidgetitem";

		// 便签内容(text类型)
		public static final String CONTENT = "content";
		// 最后更新的日期(date类型),由于ContentValues的原因，我们把Date类型的数据转为String类型
		public static final String UPDATE_DATE = "update_date";
		// 最后更新的时间(time类型)，由于ContentValues的原因，我们把Time类型的数据转为String类型
		public static final String UPDATE_TIME = "update_time";
		// 便签的背景颜色(integer类型)
		public static final String BACKGROUND_COLOR = "background_color";
	}
}