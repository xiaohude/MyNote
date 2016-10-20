package com.xnote.database;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.xnote.activity.MainActivity;
import com.xnote.database.DbInfo.AppwidgetItems;
import com.xnote.database.DbInfo.NoteItems;
import com.xnote.log.MyLog;

public class DbInfoProvider extends ContentProvider {
	// 数据库名
	private static final String DATABASE_NAME = "note.db";
	// 数据库版本号
	private static final int DATABASE_VERSION = 1;
	// 数据库中表名
	private static final String TABLE_NOTEITEMS = "noteitems";
	private static final String TABLE_APPWIDGETITEMS = "appwidgetitems";
	private DatabaseHelper mOpenHelper;
	private static UriMatcher mUriMatcher;

	// Uri指定到NoteItems表
	private static final int NOTEITEMS = 1;
	// Uri指定到NoteItems表中的一条数据
	private static final int NOTEITEMS_ITEM = 2;
	private static final int APPWIDGETITEMS = 3;
	private static final int APPWIDGETITEMS_ITEM = 4;
	// 查询表(NoteItems)时要查询的列
	private static HashMap<String, String> mProjectionMap_NoteItems;
	private static HashMap<String, String> mProjectionMap_AppwidgetItems;
	// 静态初始化UriMatcher对象,以及所要使用的ProjectionMap
	static {
		mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		// NoteItems表
		mUriMatcher.addURI(DbInfo.AUTHORITY, "noteitems", NOTEITEMS);
		mUriMatcher.addURI(DbInfo.AUTHORITY, "noteitems/#", NOTEITEMS_ITEM);
		// AppwidgetItems表
		mUriMatcher.addURI(DbInfo.AUTHORITY, "appwidgetitems", APPWIDGETITEMS);
		mUriMatcher.addURI(DbInfo.AUTHORITY, "appwidgetitems/#",
				APPWIDGETITEMS_ITEM);
		// 初始化查询列
		mProjectionMap_NoteItems = new HashMap<String, String>();
		mProjectionMap_NoteItems.put(NoteItems._ID, NoteItems._ID);
		mProjectionMap_NoteItems.put(NoteItems.CONTENT, NoteItems.CONTENT);
		mProjectionMap_NoteItems.put(NoteItems.UPDATE_DATE,
				NoteItems.UPDATE_DATE);
		mProjectionMap_NoteItems.put(NoteItems.UPDATE_TIME,
				NoteItems.UPDATE_TIME);
		mProjectionMap_NoteItems
				.put(NoteItems.ALARM_TIME, NoteItems.ALARM_TIME);
		mProjectionMap_NoteItems.put(NoteItems.BACKGROUND_COLOR,
				NoteItems.BACKGROUND_COLOR);
		mProjectionMap_NoteItems.put(NoteItems.IS_FOLDER, NoteItems.IS_FOLDER);
		mProjectionMap_NoteItems.put(NoteItems.PARENT_FOLDER,
				NoteItems.PARENT_FOLDER);

		mProjectionMap_AppwidgetItems = new HashMap<String, String>();
		mProjectionMap_AppwidgetItems.put(AppwidgetItems._ID,
				AppwidgetItems._ID);
		mProjectionMap_AppwidgetItems.put(AppwidgetItems.CONTENT,
				AppwidgetItems.CONTENT);
		mProjectionMap_AppwidgetItems.put(AppwidgetItems.UPDATE_DATE,
				AppwidgetItems.UPDATE_DATE);
		mProjectionMap_AppwidgetItems.put(AppwidgetItems.UPDATE_TIME,
				AppwidgetItems.UPDATE_TIME);
		mProjectionMap_AppwidgetItems.put(AppwidgetItems.BACKGROUND_COLOR,
				AppwidgetItems.BACKGROUND_COLOR);
	}

	// This class helps open, create, and upgrade the database file.
	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// 创建表noteitems
			String sql_noteitems = "CREATE TABLE " + TABLE_NOTEITEMS + " ("
					+ NoteItems._ID + " INTEGER PRIMARY KEY AUTOINCREMENT ,"
					+ NoteItems.CONTENT + " TEXT," + NoteItems.UPDATE_DATE
					+ " TEXT," + NoteItems.UPDATE_TIME + " TEXT,"
					+ NoteItems.ALARM_TIME + " TEXT,"
					+ NoteItems.BACKGROUND_COLOR + " INTEGER,"
					+ NoteItems.IS_FOLDER + " TEXT," + NoteItems.PARENT_FOLDER
					+ " INTEGER" + ");";
			MyLog.w(MainActivity.TAG, "创建表 " + TABLE_NOTEITEMS + " 的SQL语句："
					+ sql_noteitems);
			// 创建表appwidgetitems
			String sql_appwidgetitems = "CREATE TABLE " + TABLE_APPWIDGETITEMS
					+ " (" + AppwidgetItems._ID
					+ " INTEGER PRIMARY KEY AUTOINCREMENT ,"
					+ AppwidgetItems.CONTENT + " TEXT,"
					+ AppwidgetItems.UPDATE_DATE + " TEXT,"
					+ AppwidgetItems.UPDATE_TIME + " TEXT,"
					+ NoteItems.BACKGROUND_COLOR + " INTEGER" + ");";
			MyLog.w(MainActivity.TAG, "创建表 " + TABLE_APPWIDGETITEMS
					+ " 的SQL语句：" + sql_appwidgetitems);
			db.execSQL(sql_noteitems);
			db.execSQL(sql_appwidgetitems);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			MyLog.w(MainActivity.TAG, "Upgrading database from version "
					+ oldVersion + " to " + newVersion
					+ ", which will destroy all old data");
			// 删除原有的表
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTEITEMS);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_APPWIDGETITEMS);
			// 重新创建表
			onCreate(db);
		}
	}

	@Override
	public boolean onCreate() {
		mOpenHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// 只能通过NoteItems表中的CONTENT_URI来插入数据
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		String table;
		long id;
		Uri tmpUri;
		switch (mUriMatcher.match(uri)) {
		case NOTEITEMS:// Uri匹配到NoteItems表
			table = TABLE_NOTEITEMS;
			id = db.insert(table, NoteItems.CONTENT, values);
			if (id > 0) {
				tmpUri = ContentUris.withAppendedId(NoteItems.CONTENT_URI, id);
				getContext().getContentResolver().notifyChange(tmpUri, null);
				MyLog.d(MainActivity.TAG, "ContentProvider==>insert()");
				return tmpUri;
			}
			break;
		case APPWIDGETITEMS:
			table = TABLE_APPWIDGETITEMS;
			id = db.insert(table, AppwidgetItems.CONTENT, values);
			if (id > 0) {
				tmpUri = ContentUris.withAppendedId(AppwidgetItems.CONTENT_URI,
						id);
				getContext().getContentResolver().notifyChange(tmpUri, null);
				MyLog.d(MainActivity.TAG, "ContentProvider==>insert()");
				return tmpUri;
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		// 记录被删除记录条数
		int count = 0;
		String tmpId;
		switch (mUriMatcher.match(uri)) {
		case NOTEITEMS:
			count = db.delete(TABLE_NOTEITEMS, selection, selectionArgs);
			break;
		case NOTEITEMS_ITEM:
			tmpId = uri.getPathSegments().get(1);
			count = db.delete(TABLE_NOTEITEMS, NoteItems._ID
					+ "="
					+ tmpId
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
							+ ')' : ""), selectionArgs);
			break;

		case APPWIDGETITEMS:
			count = db.delete(TABLE_APPWIDGETITEMS, selection, selectionArgs);
			break;
		case APPWIDGETITEMS_ITEM:
			tmpId = uri.getPathSegments().get(1);
			count = db.delete(TABLE_APPWIDGETITEMS, AppwidgetItems._ID
					+ "="
					+ tmpId
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
							+ ')' : ""), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		MyLog.d(MainActivity.TAG, "ContentProvider==>delete()");
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		// 记录被修改记录条数
		int count = 0;
		String tmpId;
		switch (mUriMatcher.match(uri)) {
		case NOTEITEMS:
			count = db
					.update(TABLE_NOTEITEMS, values, selection, selectionArgs);
			break;
		case NOTEITEMS_ITEM:
			tmpId = uri.getPathSegments().get(1);
			count = db.update(TABLE_NOTEITEMS, values, NoteItems._ID
					+ "="
					+ tmpId
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
							+ ')' : ""), selectionArgs);
			break;

		case APPWIDGETITEMS:
			count = db.update(TABLE_APPWIDGETITEMS, values, selection,
					selectionArgs);
			break;
		case APPWIDGETITEMS_ITEM:
			tmpId = uri.getPathSegments().get(1);
			count = db.update(TABLE_APPWIDGETITEMS, values, AppwidgetItems._ID
					+ "="
					+ tmpId
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
							+ ')' : ""), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		MyLog.d(MainActivity.TAG, "ContentProvider==>update()");
		return count;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// SQL查询构造器
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		// If no sort order is specified use the default
		String orderBy = "";
		switch (mUriMatcher.match(uri)) {
		case NOTEITEMS_ITEM:// 注意：没有使用break，我们让它使用后面一条case语句的break
			// 组建SQL的where
			queryBuilder.appendWhere(NoteItems._ID + " = "
					+ uri.getPathSegments().get(1));
		case NOTEITEMS:
			// 设置待查询的表
			queryBuilder.setTables(TABLE_NOTEITEMS);
			// 设置要查询的列
			queryBuilder.setProjectionMap(mProjectionMap_NoteItems);
			// 设置排序
			if (TextUtils.isEmpty(sortOrder)) {
				orderBy = NoteItems.DEFAULT_SORT_ORDER;
			} else {
				orderBy = sortOrder;
			}
			break;

		case APPWIDGETITEMS_ITEM:// 注意：没有使用break，我们让它使用后面一条case语句的break
			// 组建SQL的where
			queryBuilder.appendWhere(AppwidgetItems._ID + " = "
					+ uri.getPathSegments().get(1));
		case APPWIDGETITEMS:
			// 设置待查询的表
			queryBuilder.setTables(TABLE_APPWIDGETITEMS);
			// 设置要查询的列
			queryBuilder.setProjectionMap(mProjectionMap_AppwidgetItems);
			// 设置排序
			if (!TextUtils.isEmpty(sortOrder)) {
				orderBy = sortOrder;
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = queryBuilder.query(db, projection, selection, selectionArgs,
				null, null, orderBy);
		// Tell the cursor what uri to watch, so it knows when its source data
		// changes
		c.setNotificationUri(getContext().getContentResolver(), uri);
		MyLog.d(MainActivity.TAG, "ContentProvider==>query()");
		return c;
	}

	@Override
	public String getType(Uri uri) {
		switch (mUriMatcher.match(uri)) {
		case NOTEITEMS:
			return NoteItems.CONTENT_TYPE;
		case NOTEITEMS_ITEM:
			return NoteItems.CONTENT_ITEM_TYPE;
		case APPWIDGETITEMS:
			return AppwidgetItems.CONTENT_TYPE;
		case APPWIDGETITEMS_ITEM:
			return AppwidgetItems.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}
}