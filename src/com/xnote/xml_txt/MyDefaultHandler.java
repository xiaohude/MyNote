package com.xnote.xml_txt;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.text.TextUtils;

import com.xnote.activity.MainActivity;
import com.xnote.log.MyLog;

public class MyDefaultHandler extends DefaultHandler {
	// 用List存储读取到的记录
	private List<Note> mRecords;
	private Note mNote;
	// 节点标识,保存上一个节点的名称(localName)
	private String preTag = null;
	// 因为当字符串中包含\n时,characters()函数不能一次读取完毕,所以我们定义改变量来保存被分割的数据
	private String strElementData = "";

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		// 做一些初始化的操作
		mRecords = new ArrayList<Note>();
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (localName.equals("record")) {
			mNote = new Note();
		}
		if (!TextUtils.isEmpty(strElementData)) {
			strElementData = "";
		}
		preTag = localName;
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		MyLog.e(MainActivity.TAG, "^^^当节点中有换行符时,characters()函数不止执行一次^^^  "
				+ System.currentTimeMillis());
		if (mNote != null) {
			String strContent = new String(ch, start, length);
			if (preTag.equals("id")) {
				mNote.setId(Integer.parseInt(strContent));
			} else if (preTag.equals("content")) {
				// 考虑到用户换行的问题,我们先使用strElementData来保存内容,在endElement函数中
				// 调用mNote的setContent方法来保存数据
				strElementData += strContent;
				MyLog.d(MainActivity.TAG, "读取XML文件时==>characters()函数中的数据: "
						+ strContent);
			} else if (preTag.equals("update_date")) {
				mNote.setUpdate_date(strContent);
			} else if (preTag.equals("update_time")) {
				mNote.setUpdate_time(strContent);
			} else if (preTag.equals("alarm_time")) {
				mNote.setAlarm_time(strContent);
			} else if (preTag.equals("background_color")) {
				mNote.setBackground_color(Integer.parseInt(strContent));
			} else if (preTag.equals("is_folder")) {
				mNote.setIsfolder(strContent);
			} else if (preTag.equals("parent_folder")) {
				mNote.setParentfolder(Integer.parseInt(strContent));
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (!TextUtils.isEmpty(strElementData)) {
			MyLog.d(MainActivity.TAG, "从XML文件中读取到的完整的content节点的内容: "
					+ strElementData);
			MyLog.d(MainActivity.TAG, "从XML文件中读取到的完整的content节点的内容打印完毕!");
			// 保存Content到Note对象中
			mNote.setContent(strElementData);
		}
		if (localName.equals("record") && mNote != null) {
			mRecords.add(mNote);
			mNote = null;
		}
		preTag = null;
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
	}

	// 返回得到的所有记录
	public List<Note> getNotes() {
		return mRecords;
	}
}