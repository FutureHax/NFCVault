package com.t3hh4xx0r.nfcvault.activities;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;
import com.t3hh4xx0r.nfcvault.OfflineAccountManager;
import com.t3hh4xx0r.nfcvault.OfflineAccountManager.OfflineAccount;
import com.t3hh4xx0r.nfcvault.R;
import com.t3hh4xx0r.nfcvault.SettingsProvider;

import de.cketti.library.changelog.ChangeLog;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends Activity {

	/**
	 * The default email to populate the email field with.
	 */
	public static final String EXTRA_EMAIL = "com.example.android.authenticatordemo.extra.EMAIL";

	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */
	private UserLoginTask mAuthTask = null;

	// Values for email and password at the time of the login attempt.
	private String mEmail;
	private String mPassword;

	// UI references.
	private EditText mEmailView;
	private EditText mPasswordView;
	private View mLoginFormView;
	private View mLoginStatusView;
	private TextView mLoginStatusMessageView;
	private CheckBox rememberMe;
	private CheckBox offline;
	SettingsProvider settings;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		getActionBar().setTitle("Login");
		ChangeLog cl = new ChangeLog(this);
		// Uncomment to show always.
		// cl.dontuseSetLastVersion("0.0");
		if (cl.isFirstRun()) {
			cl.getLogDialog().show();
		}
		settings = new SettingsProvider(this);
		// Set up the login form.
		mEmail = getIntent().getStringExtra(EXTRA_EMAIL);
		if (settings.getRememberMe()) {
			mEmail = settings.getRememberMeEmail();
		}
		mEmailView = (EditText) findViewById(R.id.email);
		mEmailView.setText(mEmail);
		rememberMe = (CheckBox) findViewById(R.id.settings_remember_me);
		rememberMe.setChecked(settings.getRememberMe());
		rememberMe.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				settings.setRememberMe(isChecked);
			}
		});
		offline = (CheckBox) findViewById(R.id.settings_offline);
		offline.setChecked(settings.getOffline());
		offline.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				settings.setOffline(isChecked);
			}
		});
		mPasswordView = (EditText) findViewById(R.id.password);
		if (settings.getRememberMe() && mEmailView.getText().length() > 0) {
			mPasswordView.requestFocus();
		}
		mPasswordView
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView textView, int id,
							KeyEvent keyEvent) {
						if (id == R.id.login || id == EditorInfo.IME_NULL) {
							attemptLogin();
							return true;
						}
						return false;
					}
				});

		mLoginFormView = findViewById(R.id.login_form);
		mLoginStatusView = findViewById(R.id.login_status);
		mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

		findViewById(R.id.sign_in_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						attemptLogin();
					}
				});
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		if (mAuthTask != null) {
			return;
		}

		// Reset errors.
		mEmailView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		mEmail = mEmailView.getText().toString();
		if (settings.getRememberMe()) {
			settings.setRememberMeEmail(mEmail);
		} else {
			settings.setRememberMeEmail("");
		}
		mPassword = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password.
		if (TextUtils.isEmpty(mPassword)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
			cancel = true;
		} else if (mPassword.length() < 4) {
			mPasswordView.setError(getString(R.string.error_invalid_password));
			focusView = mPasswordView;
			cancel = true;
		}

		// Check for a valid email address.
		if (TextUtils.isEmpty(mEmail)) {
			mEmailView.setError(getString(R.string.error_field_required));
			focusView = mEmailView;
			cancel = true;
		} else if (!mEmail.contains("@")) {
			mEmailView.setError(getString(R.string.error_invalid_email));
			focusView = mEmailView;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
			showProgress(true);
			mAuthTask = new UserLoginTask();
			mAuthTask.execute((Void) null);
		}
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			mLoginStatusView.setVisibility(View.VISIBLE);
			mLoginStatusView.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginStatusView.setVisibility(show ? View.VISIBLE
									: View.GONE);
						}
					});

			mLoginFormView.setVisibility(View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginFormView.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

	/**
	 * Represents an asynchronous login/registration task used to authenticate
	 * the user.
	 */
	public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
		boolean attemptingOffline = false;

		@Override
		protected Boolean doInBackground(Void... params) {
			ParseUser.logInInBackground(mEmail, mPassword, new LogInCallback() {
				@Override
				public void done(ParseUser u, ParseException exception) {
					mAuthTask = null;
					showProgress(false);
					if (exception == null) {
						if (u != null) {
							// Login normally
							if (offline.isChecked()) {
								OfflineAccountManager acctMan = new OfflineAccountManager(
										LoginActivity.this);
								try {
									acctMan.saveAccount(u, mPassword);
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								} catch (NoSuchAlgorithmException e) {
									e.printStackTrace();
								}
							}
							Intent i = new Intent(LoginActivity.this,
									MainActivity.class);
							startActivity(i);
							finish();
						} else {
							// Needs to register
							ParseUser user = new ParseUser();
							user.setUsername(mEmail);
							user.setPassword(mPassword);
							user.setEmail(mEmail);
							user.signUpInBackground(new SignUpCallback() {
								@Override
								public void done(final ParseException e) {
									if (e != null) {
										LoginActivity.this
												.runOnUiThread(new Runnable() {
													@Override
													public void run() {
														Toast.makeText(
																LoginActivity.this,
																e.getMessage()
																		.toString(),
																Toast.LENGTH_LONG)
																.show();
														mPasswordView
																.setText("");
													}
												});
									} else {
										LoginActivity.this
												.runOnUiThread(new Runnable() {
													@Override
													public void run() {
														handleRegistered();
													}
												});
									}
								}
							});
						}
					} else {
						Boolean containsUnknownHostException = exception
								.getMessage().contains(
										"java.net.UnknownHostException");
						Boolean isInvalidCredentials = exception.getMessage()
								.contains("invalid login credentials");
						if (containsUnknownHostException) {
							attemptingOffline = true;
							try {
								attemptOfflineLogin(mEmail, mPassword);
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							} catch (NoSuchAlgorithmException e) {
								e.printStackTrace();
							}
						} else if (isInvalidCredentials) {
							// Toast.makeText(LoginActivity.this,
							// "Invalid credentials", Toast.LENGTH_LONG)
							// .show();
							// mPasswordView.setText("");
							ParseUser user = new ParseUser();
							user.setUsername(mEmail);
							user.setPassword(mPassword);
							user.setEmail(mEmail);
							user.signUpInBackground(new SignUpCallback() {
								@Override
								public void done(final ParseException e) {
									if (e != null) {
										LoginActivity.this
												.runOnUiThread(new Runnable() {
													@Override
													public void run() {
														Toast.makeText(
																LoginActivity.this,
																e.getMessage()
																		.toString(),
																Toast.LENGTH_LONG)
																.show();
														mPasswordView
																.setText("");
													}
												});
									} else {
										LoginActivity.this
												.runOnUiThread(new Runnable() {
													@Override
													public void run() {
														handleRegistered();
													}
												});
									}
								}
							});
						} else {
							Toast.makeText(LoginActivity.this,
									exception.getMessage(), Toast.LENGTH_LONG)
									.show();
						}
					}
				}
			});
			return true;
		}

		@Override
		protected void onCancelled() {
			mAuthTask = null;
			if (!attemptingOffline) {
				showProgress(false);
			}
		}
	}

	protected void attemptOfflineLogin(String email, String password)
			throws UnsupportedEncodingException, NoSuchAlgorithmException {
		OfflineAccountManager acctMan = new OfflineAccountManager(
				LoginActivity.this);
		OfflineAccount acct = acctMan.getOfflineAccountForEmail(email);
		if (acct != null) {
			byte[] bytesOfMessage = password.getBytes("UTF-8");
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(bytesOfMessage);
			String hashed = MainActivity.ByteArrayToHexString(digest);
			if (hashed.equals(acct.getHashedPass())) {
				Intent i = new Intent(LoginActivity.this, MainActivity.class);
				startActivity(i);
				finish();
			} else {
				Toast.makeText(this, "Offline Login failure", Toast.LENGTH_LONG)
						.show();
			}
		} else {
			AlertDialog.Builder b = new Builder(this);
			b.setTitle("Login Error")
					.setIcon(R.drawable.ic_launcher)
					.setMessage(
							"No internet connection is available and no account matching these credentials was found locally. To make an account available offline you must login at least one time with an internet connection.")
					.create().show();
		}
	}

	private void handleRegistered() {
		Toast.makeText(this, "Thank you for registering!", Toast.LENGTH_LONG)
				.show();
		Intent i = new Intent(LoginActivity.this, MainActivity.class);
		startActivity(i);
		finish();
	}
}
