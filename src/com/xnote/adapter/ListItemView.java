package com.xnote.adapter;

import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

/*
 * ListView中每一行中包含的组件
 * 这个类把ListView中的每一行都包装成一个ListItemView对象
 */
public final class ListItemView {
	public LinearLayout linearlayout;
	public TextView tv_left;
	public TextView tv_right;
	public CheckBox cb_right;
}