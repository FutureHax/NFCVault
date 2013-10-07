package com.t3hh4xx0r.nfcvault.activities;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import com.t3hh4xx0r.nfcvault.R;
import com.t3hh4xx0r.nfcvault.SettingsProvider;
import com.t3hh4xx0r.nfcvault.VaultedText;
import com.t3hh4xx0r.nfcvault.encryption.Encryption;

public class AddPasswordFileActivity extends Activity {

	private NfcAdapter mAdapter;
	private PendingIntent mPendingIntent;

	boolean isListeningforInitialTagScan = true;

	String secrectID;
	String encryptedPass;
	String dataType;

	EditText title;
	AutoCompleteTextView stackName;
	boolean isEditing = false;
	VaultedText editable;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_password_file);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		mAdapter = NfcAdapter.getDefaultAdapter(this);
		mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		stackName = (AutoCompleteTextView) findViewById(R.id.stack);
		stackName.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, getIntent()
						.getStringArrayListExtra("stacks")));
		dataType = getIntent().getStringExtra("type");
		title = (EditText) findViewById(R.id.title);
		isEditing = getIntent().hasExtra("password");
		if (isEditing) {
			editable = (VaultedText) getIntent().getSerializableExtra(
					"password");
			stackName.setText(editable.getDataStack());
			title.setText(editable.getDataTitle());
		}
		View save = findViewById(R.id.save);
		save.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				saveAndFinish();
			}
		});

	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			super.onBackPressed();
		}
		return super.onMenuItemSelected(featureId, item);
	}

	protected void saveAndFinish() {
		if (editable == null) {
			if (MainActivity.TMP_FILE_AS_STRING != null) {
				try {
					encryptedPass = Encryption.encryptString(MainActivity.TMP_FILE_AS_STRING,
							secrectID);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			encryptedPass = editable.getDataValue();
		}
		finish();
	}

	private void resolveIntent(Intent intent) {
		if (intent == null || !intent.hasExtra(NfcAdapter.EXTRA_ID)) {
			return;
		}
		try {
			byte[] tagId = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
			String key = MainActivity.ByteArrayToHexString(tagId);
			if (!new SettingsProvider(this).isCorrectKey(key)) {
				warnWrongKey();
				return;
			}
			secrectID = key;
			View scanView = findViewById(R.id.scan);
			final View detailsView = findViewById(R.id.details);
			Animation outAnim = AnimationUtils.loadAnimation(this,
					android.R.anim.fade_out);
			Animation inAnim = AnimationUtils.loadAnimation(this,
					android.R.anim.fade_in);
			outAnim.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
					detailsView.setVisibility(View.VISIBLE);
				}

				@Override
				public void onAnimationRepeat(Animation animation) {
				}

				@Override
				public void onAnimationEnd(Animation animation) {
				}
			});
			scanView.setAnimation(outAnim);
			detailsView.setAnimation(inAnim);
			scanView.setVisibility(View.GONE);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void warnWrongKey() {
		AlertDialog.Builder b = new Builder(this);
		b.setTitle("Master Password Mismatch");
		b.setMessage("When you originally setup your master password, the app hashed and stored that value. The hash of the key you just scanned does not match the stored hash. Confirm you are using the correct key and try again.");
		b.create().show();
	}

	@Override
	public void finish() {
		Intent i = new Intent();
		Bundle b = new Bundle();
		if (encryptedPass != null) {
			VaultedText p = new VaultedText(stackName.getText().toString(),
					"", title.getText().toString(),
					dataType);
			if (editable != null) {
				p.setParseId(editable.getParseId());
				b.putSerializable("oldPassword", editable);
			}
			b.putSerializable("password", p);
		}
		i.putExtras(b);
		setResult(RESULT_OK, i);
		super.finish();
	}

	@Override
	public void onNewIntent(Intent intent) {
		setIntent(intent);
		resolveIntent(intent);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mAdapter != null) {
			mAdapter.disableForegroundDispatch(this);
		}
		overridePendingTransition(android.R.anim.fade_in,
				android.R.anim.fade_out);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mAdapter != null) {
			mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
		}
	}

}
