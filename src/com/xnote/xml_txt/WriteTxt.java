package com.xnote.xml_txt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;

import com.xnote.activity.MainActivity;
import com.xnote.database.DateTimeUtil;
import com.xnote.database.DbInfo.NoteItems;
import com.xnote.log.MyLog;

/*
 * 输出TXT文本菜单项对应的类,
 * 该类实现了对数据库中所有文件夹和便签内容的输出(写入TXT文本文件)
 */
public class WriteTxt {
	private Context context;
	private FileWriter nwriter;
	private String BASE_DIR = "/sdcard";
	private String MID_DIR = "XNote";
	private String FILE_NAME = "notes_backup.txt";

	public WriteTxt(Context context) {
		this.context = context;
	}

	// 输出TXT文本函数
	public boolean writeTxt() throws IOException {
		// 查询条件，查询文件夹记录和主页便签记录
		String selection = NoteItems.IS_FOLDER + " = '" + "yes" + "' or "
				+ NoteItems.PARENT_FOLDER + " = " + "-1";

		Cursor mCursor = context.getContentResolver().query(
				NoteItems.CONTENT_URI, null, selection, null, null);
		int totalCount = mCursor.getCount();
		mCursor.close();
		// 判断SDCard的状态是否可用
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			if (totalCount > 0) {// 有数据,则写文件
				// 注意目录位置的定义。File.separator
				File dir = new File(BASE_DIR + File.separator + MID_DIR);
				if (!dir.exists()) {
					dir.mkdir();
					MyLog.d(MainActivity.TAG, "WriteTxt==>" + dir
							+ " is created successfully!");
				} else {
					MyLog.d(MainActivity.TAG, "WriteTxt==>" + dir
							+ " has been created");
				}
				String path = BASE_DIR + File.separator + MID_DIR
						+ File.separator + FILE_NAME;

				File file = new File(path);
				if (!file.exists()) {
					file.createNewFile();
				}
				// 以覆盖的方式写文件
				nwriter = new FileWriter(file, false);

				Cursor folderCursor = context.getContentResolver().query(
						NoteItems.CONTENT_URI, null,
						NoteItems.IS_FOLDER + "  = ? ", new String[] { "yes" },
						null);
				// 文件夹数量
				int countOfFolders = folderCursor.getCount();
				// 总的便签数量(只要不是文件夹就查询)
				Cursor allNotes = context.getContentResolver().query(
						NoteItems.CONTENT_URI, null,
						NoteItems.IS_FOLDER + "  = ? ", new String[] { "no" },
						null);
				int countOfAllNotes = allNotes.getCount();
				allNotes.close();
				nwriter.write("XNote" + "\t" + DateTimeUtil.getDate() + "\t"
						+ DateTimeUtil.getTime() + "\n");
				nwriter.write("共输出" + countOfFolders + "个文件夹和"
						+ countOfAllNotes + "条便签" + "\n");
				// 先输出所有文件夹的内容
				if (countOfFolders > 0) {
					// 循环输出所有文件夹及其包含的便签的内容
					folderCursor.moveToFirst();
					for (int i = 0; i < countOfFolders; i++) {
						// 写文件夹名称
						String name = folderCursor.getString(folderCursor
								.getColumnIndex(NoteItems.CONTENT));
						writeFolderName("文件夹名称：", name);
						// 写文件夹下的便签内容
						// 文件夹ID
						int _id = folderCursor.getInt(folderCursor
								.getColumnIndex(NoteItems._ID));
						// 保存有某文件夹下所有便签数据的Cursor对象
						Cursor notesInOneFolder = context.getContentResolver()
								.query(NoteItems.CONTENT_URI, null,
										NoteItems.PARENT_FOLDER + "  = ? ",
										new String[] { String.valueOf(_id) },
										null);
						// 某文件夹下便签的数量
						int noteCountInOneFolder = notesInOneFolder.getCount();
						if (noteCountInOneFolder > 0) {
							notesInOneFolder.moveToFirst();
							for (int j = 0; j < noteCountInOneFolder; j++) {
								// 用户最后更新便签的日期(date)
								String date = notesInOneFolder
										.getString(notesInOneFolder
												.getColumnIndex(NoteItems.UPDATE_DATE));
								// 用户最后更新便签的时间(time)
								String time = notesInOneFolder
										.getString(notesInOneFolder
												.getColumnIndex(NoteItems.UPDATE_TIME));
								// 用户最后更新的便签的内容
								String content = notesInOneFolder
										.getString(notesInOneFolder
												.getColumnIndex(NoteItems.CONTENT));
								// 写便签内容
								writeNoteInfo(date, time, content);
								// 指向该文件夹下的下一条便签
								notesInOneFolder.moveToNext();
							}
						}
						notesInOneFolder.close();
						// 指向下一个文件夹
						folderCursor.moveToNext();
					}
				}
				folderCursor.close();
				// 文件夹处理完以后,写顶级便签的内容
				writeFolderName("", "XNote");
				nwriter.write("\n" + "\n");
				// Cursor对象保存顶级便签的内容
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
						// 用户最后更新便签的日期(date)
						String date = rootNotes.getString(rootNotes
								.getColumnIndex(NoteItems.UPDATE_DATE));
						// 用户最后更新便签的时间(time)
						String time = rootNotes.getString(rootNotes
								.getColumnIndex(NoteItems.UPDATE_TIME));
						// 用户最后更新的便签的内容
						String content = rootNotes.getString(rootNotes
								.getColumnIndex(NoteItems.CONTENT));
						// 写便签内容
						writeNoteInfo(date, time, content);
						// 指向该下一条便签
						rootNotes.moveToNext();
					}
				}
				rootNotes.close();
				nwriter.flush();
				nwriter.close();
			} else {
				return false;
			}
			return true;
		}
		return false;
	}

	// 写文件夹名称
	private void writeFolderName(String prefix, String name) throws IOException {
		nwriter.write("............" + "\n");
		nwriter.write(" " + prefix + " " + name + "\n");
		nwriter.write("............" + "\n");
		nwriter.write(" " + "\n");
	}

	// 写入便签的日期时间及内容
	private void writeNoteInfo(String date, String time, String content)
			throws IOException {
		nwriter.write(date + "\t" + time + "\n");
		nwriter.write(content + "\n" + "\n");
	}
}