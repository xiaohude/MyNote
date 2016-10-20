package com.xnote.xml_txt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.util.Xml;

import com.xnote.activity.MainActivity;
import com.xnote.database.DbInfo.NoteItems;
import com.xnote.log.MyLog;

/*
 * 以XML格式备份数据
 */
public class WriteXml {
	private Context context;
	private FileWriter nwriter;
	private String BASE_DIR = "/sdcard";
	private String MID_DIR = "XNote";
	private String FILE_NAME = "notes_backup.xml";

	public WriteXml(Context context) {
		this.context = context;
	}

	public boolean writeXml() throws Exception {
		// 判断SDCard的状态是否可用
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			// 注意目录位置的定义。File.separator
			File dir = new File(BASE_DIR + File.separator + MID_DIR);
			if (!dir.exists()) {
				dir.mkdir();
				MyLog.d(MainActivity.TAG, "WriteXml==>" + dir
						+ " is created successfully!");
			} else {
				MyLog.d(MainActivity.TAG, "WriteXml==>" + dir
						+ " has been created");
			}
			String path = BASE_DIR + File.separator + MID_DIR + File.separator
					+ FILE_NAME;

			File file = new File(path);
			if (file.exists()) {
				if (file.delete()) {// delete the old .xml file
					file.createNewFile();
				}
			}
			// 以覆盖的方式写文件
			nwriter = new FileWriter(file, false);
			if (!writeNote(nwriter)) {
				return false;
			}
			return true;
		} else {
			MyLog.d(MainActivity.TAG, "WriteXml==>can not access SDCard");
			return false;
		}
	}

	// 把Cursor中的数据输出到XML文件中
	private boolean writeNote(FileWriter nwriter) throws Exception {
		MyLog.d(MainActivity.TAG, "WriteXml==>begin to write XML...");
		XmlSerializer serializer = Xml.newSerializer();
		serializer.setOutput(nwriter);
		// <?xml version=”1.0″ encoding=”UTF-8″ standalone=”yes”?>
		serializer.startDocument("UTF-8", true);
		// <records>
		serializer.startTag(null, "records");
		Cursor cursorAllRecords = context.getContentResolver().query(
				NoteItems.CONTENT_URI, null, null, null, null);
		int itemCount = cursorAllRecords.getCount();
		cursorAllRecords.close();
		if (itemCount > 0) {
			// 如果有文件夹,则写文件夹以及该文件夹下的便签
			Cursor folderCursor = context.getContentResolver().query(
					NoteItems.CONTENT_URI, null,
					NoteItems.IS_FOLDER + "  = ? ", new String[] { "yes" },
					null);
			// 文件夹数量
			int countOfFolders = folderCursor.getCount();
			if (countOfFolders > 0) {// 有文件夹
				// 循环输出所有文件夹及其包含的便签的内容
				folderCursor.moveToFirst();
				for (int i = 0; i < countOfFolders; i++) {
					int id = writeRecord(serializer, folderCursor);// 向XML写一条文件夹的记录
					// 根据返回的id,查询保存在该文件夹下所有的便签
					Cursor notesInOneFolder = context.getContentResolver()
							.query(NoteItems.CONTENT_URI, null,
									NoteItems.PARENT_FOLDER + "  = ? ",
									new String[] { String.valueOf(id) }, null);
					// 文件夹下便签的数量
					int noteCountInOneFolder = notesInOneFolder.getCount();
					if (noteCountInOneFolder > 0) {// 判断文件夹下是否有便签
						notesInOneFolder.moveToFirst();
						for (int j = 0; j < noteCountInOneFolder; j++) {
							writeRecord(serializer, notesInOneFolder);
							// 指向该文件夹下的下一条便签
							notesInOneFolder.moveToNext();
						}
					}
					notesInOneFolder.close();
					// 指向下一条
					folderCursor.moveToNext();
				}
			}
			folderCursor.close();
			// 如果有顶级便签,即主页便签,则写记录
			// 查询顶级便签的内容
			Cursor rootNotes = context.getContentResolver().query(
					NoteItems.CONTENT_URI,
					null,
					NoteItems.IS_FOLDER + " = '" + "no" + "' and "
							+ NoteItems.PARENT_FOLDER + " = " + "-1", null,
					null);
			// 顶级便签的数量
			int countOfRootNotes = rootNotes.getCount();
			if (countOfRootNotes > 0) {
				rootNotes.moveToFirst();
				for (int i = 0; i < countOfRootNotes; i++) {
					writeRecord(serializer, rootNotes);
					// 指向该文件夹下的下一条便签
					rootNotes.moveToNext();
				}
			}
			rootNotes.close();
			// </records>
			serializer.endTag(null, "records");
			serializer.endDocument();
			nwriter.flush();
			nwriter.close();
			return true;
		} else {
			nwriter.close();
			return false;
		}
	}

	// 根据传递的Cursor对象写xml文件.使用该函数时一定要注意Cursor对象的position
	// 返回记录的id
	private int writeRecord(XmlSerializer serializer, Cursor cursor)
			throws IOException {
		// 记录的_id字段
		int id = cursor.getInt(cursor.getColumnIndex(NoteItems._ID));
		// 记录的content字段
		String content = cursor.getString(cursor
				.getColumnIndex(NoteItems.CONTENT));
		// 记录的update_date字段
		String update_date = cursor.getString(cursor
				.getColumnIndex(NoteItems.UPDATE_DATE));
		// 记录的update_time字段
		String update_time = cursor.getString(cursor
				.getColumnIndex(NoteItems.UPDATE_TIME));
		// 记录的alarm_time字段
		String alarm_time = cursor.getString(cursor
				.getColumnIndex(NoteItems.ALARM_TIME));
		// 记录的background_color字段
		int background_color = cursor.getInt(cursor
				.getColumnIndex(NoteItems.BACKGROUND_COLOR));
		// 记录的is_folder字段
		String is_folder = cursor.getString(cursor
				.getColumnIndex(NoteItems.IS_FOLDER));
		// 记录的parent_folder字段
		int parent_folder = cursor.getInt(cursor
				.getColumnIndex(NoteItems.PARENT_FOLDER));

		// <record>
		serializer.startTag(null, "record");
		// <id>...</id>
		serializer.startTag(null, "id");
		serializer.text(String.valueOf(id));
		serializer.endTag(null, "id");
		// <content>...</content>
		serializer.startTag(null, "content");
		serializer.text(content);
		serializer.endTag(null, "content");
		// <update_date>...</update_date>
		serializer.startTag(null, "update_date");
		if (update_date != null)
			serializer.text(update_date);
		serializer.endTag(null, "update_date");
		// <update_time>...</update_time>
		serializer.startTag(null, "update_time");
		if (update_time != null)
			serializer.text(update_time);
		serializer.endTag(null, "update_time");
		// <alarm_time>...</alarm_time>
		serializer.startTag(null, "alarm_time");
		if (alarm_time != null)
			serializer.text(alarm_time.toString());
		serializer.endTag(null, "alarm_time");
		// <background_color>...</background_color>
		serializer.startTag(null, "background_color");
		serializer.text(String.valueOf(background_color));
		serializer.endTag(null, "background_color");
		// <is_folder>...</is_folder>
		serializer.startTag(null, "is_folder");
		if (is_folder != null)
			serializer.text(is_folder);
		serializer.endTag(null, "is_folder");
		// <parent_folder>...</parent_folder>
		serializer.startTag(null, "parent_folder");
		serializer.text(String.valueOf(parent_folder));
		serializer.endTag(null, "parent_folder");
		// </record>
		serializer.endTag(null, "record");
		return id;
	}
}