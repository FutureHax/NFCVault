package com.t3hh4xx0r.nfcvault;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.parse.ParseUser;
import com.t3hh4xx0r.nfcvault.activities.MainActivity;

public class OfflineAccountManager {
	Context c;

	public OfflineAccountManager(Context c) {
		this.c = c;
	}

	public void saveAccount(ParseUser u, String pass)
			throws UnsupportedEncodingException, NoSuchAlgorithmException {
		byte[] bytesOfMessage = pass.getBytes("UTF-8");
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] digest = md.digest(bytesOfMessage);

		Editor e = PreferenceManager.getDefaultSharedPreferences(c).edit();
		HashSet<String> acct = new HashSet<String>();
		acct.add("user_" + u.getEmail());
		acct.add("pass_" + MainActivity.ByteArrayToHexString(digest));
		e.putStringSet(u.getEmail(), acct);
		e.commit();
	}

	public OfflineAccount getOfflineAccountForEmail(String email) {
		OfflineAccount res = new OfflineAccount();
		Set<String> resSet = PreferenceManager.getDefaultSharedPreferences(c)
				.getStringSet(email, null);
		for (String line : resSet) {
			if (line.startsWith("user_")) {
				res.setUsername(line.replace("user_", ""));
			} else if (line.startsWith("pass_")) {
				res.setHashedPass(line.replace("pass_", ""));
			}
		}
		return res;
	}

	public class OfflineAccount {
		String username, hashedPass;

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getHashedPass() {
			return hashedPass;
		}

		public void setHashedPass(String hashedPass) {
			this.hashedPass = hashedPass;
		}
	}

}
