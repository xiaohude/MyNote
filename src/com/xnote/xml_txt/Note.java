package com.xnote.xml_txt;

/*
 * 从XML文件读数据时,以该类的对象进行存储
 */
public class Note {
	private int id;
	private String content;
	private String update_date;
	private String update_time;
	private String alarm_time;
	private int background_color;
	private String isfolder;
	private int parentfolder;

	public Note() {
		super();
	}

	public Note(int id, String con, String update_date, String update_time,
			String alarm_time, int background_color, String isfolder,
			int parentfolder) {
		this.id = id;
		this.content = con;
		this.update_date = update_date;
		this.update_time = update_time;
		this.alarm_time = alarm_time;
		this.background_color = background_color;
		this.isfolder = isfolder;
		this.parentfolder = parentfolder;
	}

	public String getUpdate_date() {
		return update_date;
	}

	public void setUpdate_date(String update_date) {
		this.update_date = update_date;
	}

	public String getUpdate_time() {
		return update_time;
	}

	public void setUpdate_time(String update_time) {
		this.update_time = update_time;
	}

	public String getAlarm_time() {
		return alarm_time;
	}

	public void setAlarm_time(String alarm_time) {
		this.alarm_time = alarm_time;
	}

	public int getBackground_color() {
		return background_color;
	}

	public void setBackground_color(int background_color) {
		this.background_color = background_color;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getIsfolder() {
		return isfolder;
	}

	public void setIsfolder(String isfolder) {
		this.isfolder = isfolder;
	}

	public int getParentfolder() {
		return parentfolder;
	}

	public void setParentfolder(int parentfolder) {
		this.parentfolder = parentfolder;
	}
}