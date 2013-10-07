package com.t3hh4xx0r.nfcvault;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import android.util.Log;

import com.parse.ParseACL;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;

public class VaultedText implements Serializable {

	@Override
	public String toString() {
		return "VaultedText [dataStack=" + dataStack + ", dataValue="
				+ dataValue + ", dataTitle=" + dataTitle + ", dataType="
				+ dataType + ", parseId=" + parseId + "]";
	}

	public String getDataStack() {
		return dataStack;
	}

	public void setDataStack(String dataStack) {
		this.dataStack = dataStack;
	}

	public String getDataValue() {
		return dataValue;
	}

	public void setDataValue(String dataValue) {
		this.dataValue = dataValue;
	}

	public String getDataTitle() {
		return dataTitle;
	}

	public void setDataTitle(String dataTitle) {
		this.dataTitle = dataTitle;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	private static final long serialVersionUID = -187141204874302467L;

	public VaultedText(String stack, String value, String title, String type) {
		this.dataStack = stack;
		this.dataValue = value;
		this.dataTitle = title;
		this.dataType = type;
	}

	String dataStack;
	String dataValue;
	String dataTitle;
	String dataType;

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	public final static String TYPE_TEXT = "text";
	public final static String TYPE_IMAGE = "image";
	public final static String TYPE_FILE = "file";

	String parseId;

	public String getParseId() {
		return parseId;
	}

	public void setParseId(String parseId) {
		this.parseId = parseId;
	}

	public void update(VaultedText newPass) {
		setDataStack(newPass.getDataStack());
		setDataValue(newPass.getDataValue());
		setDataTitle(newPass.getDataTitle());
	}

	public ParseObject toParsePassword() {
		ParseObject o = new ParseObject("Password");
		if (dataValue.length() > 100) {
			ParseFile f = new ParseFile("data_Value", dataValue.getBytes());
			Log.d("THE SIZE", Integer.toString(dataValue.length()));
//			D/THE SIZE( 7944): 10215963

			o.put("data_value_file", f);
		} else {
			o.put("data_value", dataValue);
		}
		o.put("data_stack", dataStack);
		o.put("data_title", dataTitle);
		o.put("data_type", dataType);
		o.setObjectId(parseId);
		o.put("key_owner", ParseUser.getCurrentUser().getEmail());
		o.setACL(new ParseACL(ParseUser.getCurrentUser()));
		return o;
	}
}
