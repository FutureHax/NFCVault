package com.t3hh4xx0r.nfcvault.activities;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fima.cardsui.objects.Card;
import com.fima.cardsui.objects.Card.OnCardSwiped;
import com.fima.cardsui.objects.CardStack;
import com.fima.cardsui.views.CardUI;
import com.ipaulpro.afilechooser.utils.FileUtils;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseQuery.CachePolicy;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.t3hh4xx0r.nfcvault.LaunchPopup;
import com.t3hh4xx0r.nfcvault.LaunchPopupManager;
import com.t3hh4xx0r.nfcvault.R;
import com.t3hh4xx0r.nfcvault.SettingsProvider;
import com.t3hh4xx0r.nfcvault.VaultedText;
import com.t3hh4xx0r.nfcvault.VaultedTextCard;
import com.t3hh4xx0r.nfcvault.VaultedTextCard.OnViewButtonLister;
import com.t3hh4xx0r.nfcvault.encryption.Encryption;

import de.cketti.library.changelog.ChangeLog;

public class MainActivity extends FragmentActivity {
	CardUI mCardView;
	ArrayList<CardStack> mCardStacks;
	ArrayList<VaultedTextCard> mCards;
	ArrayList<String> stackTitles;

	private NfcAdapter mAdapter;
	private PendingIntent mPendingIntent;

	boolean isListeningforInitialTagScan = false;
	boolean isListeningforDecryptTagScan = false;
	VaultedTextCard cardFromDecrypt;

	String COLOR = "#33b6ea";

	ProgressBar pBar;

	AlertDialog setupDialog;
	AlertDialog typeDialog;

	public static String TMP_FILE_AS_STRING;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.password_list);
		mAdapter = NfcAdapter.getDefaultAdapter(this);
		resolveIntent(getIntent());
		mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		pBar = (ProgressBar) findViewById(R.id.progressBar1);
		mCardView = (CardUI) findViewById(R.id.cardsview);
		mCardView.setSwipeable(true);
		stackTitles = new ArrayList<String>();
		mCardStacks = new ArrayList<CardStack>();
		mCards = new ArrayList<VaultedTextCard>();

		setupPasswordCards();

		managePopups();
	}

	public CardStack getStackForTitle(String title) {
		for (CardStack stack : mCardStacks) {
			if (stack.getTitle().equals(title)) {
				return stack;
			}
		}
		CardStack stack = new CardStack();
		stack.setColor(COLOR);
		stack.setTitle(title);
		mCardStacks.add(stack);

		return stack;
	}

	private void setupPasswordStacks() {
		for (VaultedTextCard card : mCards) {
			getStackForTitle(card.getPassword().getDataStack()).add(card);
		}

		for (CardStack stack : mCardStacks) {
			mCardView.addStack(stack);
		}
		mCardView.refresh();
		hideProgress(this);
	}

	private void setupPasswordCards() {
		mCardView.clearCards();
		mCardStacks.clear();
		mCards.clear();
		mCardView.refresh();
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Password");
		query.setCachePolicy(CachePolicy.NETWORK_ELSE_CACHE);
		query.whereEqualTo("key_owner", ParseUser.getCurrentUser().getEmail());
		query.findInBackground(new FindCallback<ParseObject>() {
			@Override
			public void done(List<ParseObject> results, ParseException e) {
				if (e == null) {
					for (final ParseObject o : results) {
						VaultedText p = new VaultedText(o
								.getString("data_stack"), o
								.getString("data_value"), o
								.getString("data_title"), o
								.getString("data_type"));

						p.setParseId(o.getObjectId());
						if (p.getDataType() == null) {
							p.setDataType(VaultedText.TYPE_TEXT);
							p.toParsePassword().saveInBackground();
						}
						final VaultedTextCard card = new VaultedTextCard(p,
								viewButtonListener);
						card.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								if (isOnline(v.getContext())) {
									Intent i = new Intent(v.getContext(),
											AddPasswordActivity.class);
									Bundle b = new Bundle();
									b.putSerializable("password",
											card.getPassword());
									b.putStringArrayList("stacks",
											getStackNames());
									i.putExtras(b);
									startActivityForResult(i, 1);
								} else {
									Toast.makeText(
											v.getContext(),
											"Offline editing not currently supported",
											Toast.LENGTH_LONG).show();
								}
							}
						});

						card.setOnCardSwipedListener(new OnCardSwiped() {
							@Override
							public void onCardSwiped(Card cardRes,
									final View layout) {

								handleCardSwipe(layout, card, o);
							}
						});
						mCards.add(card);
					}
					setupPasswordStacks();
				} else {
					e.printStackTrace();
				}
			}
		});
	}

	private void handleCardSwipe(View layout, final VaultedTextCard card,
			final ParseObject o) {
		if (isOnline(this)) {
			AlertDialog.Builder b = new Builder(layout.getContext());
			b.setTitle("Delete This Data?");
			b.setMessage("This will delete the data from your local device and from the cloud.\n\nAre you sure you want to continue?");
			b.setPositiveButton("Yes", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					o.deleteInBackground(new DeleteCallback() {
						@Override
						public void done(ParseException arg0) {
							CardStack stack = getStackForTitle(card
									.getPassword().getDataStack());
							if (stack.getCards().size() == 0) {
							}
						}
					});
				}
			});
			b.setNegativeButton("No", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					getStackForTitle(card.getPassword().getDataStack()).add(
							card);
					mCardView.refresh();
				}
			});
			b.create().show();
		} else {
			Toast.makeText(
					this,
					"Deletion is only supported while online. Please check your conection and try again.",
					Toast.LENGTH_LONG).show();
		}
	}

	private void hideProgress(Context c) {
		mCardView.setAnimation(AnimationUtils.loadAnimation(c,
				android.R.anim.fade_in));
		mCardView.setVisibility(View.VISIBLE);

		pBar.setAnimation(AnimationUtils.loadAnimation(c,
				android.R.anim.fade_out));
		pBar.setVisibility(View.GONE);
	}

	private void showProgress(Context c) {
		mCardView.setAnimation(AnimationUtils.loadAnimation(c,
				android.R.anim.fade_out));
		mCardView.setVisibility(View.GONE);

		pBar.setAnimation(AnimationUtils.loadAnimation(c,
				android.R.anim.fade_in));
		pBar.setVisibility(View.VISIBLE);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == R.id.action_add_key) {
			if (new SettingsProvider(this).getHashedKey().isEmpty()) {
				requestKeyValidation(false, null);
			} else {
				notifyKeySet();
			}
		} else if (item.getItemId() == R.id.action_help) {
			requestHelp();
		} else if (item.getItemId() == R.id.action_add_new) {
			showTypePopup();
		} else if (item.getItemId() == R.id.action_contact) {
			final Intent emailIntent = new Intent(
					android.content.Intent.ACTION_SEND);
			emailIntent.setType("plain/text");
			emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
					new String[] { "r2doesinc@gmail.com" });
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
					"NFCVault Feedback");
			startActivity(Intent.createChooser(emailIntent, "Send via..."));
		}
		return super.onMenuItemSelected(featureId, item);
	}

	private void showTypePopup() {
		AlertDialog.Builder b = new Builder(this);
		b.setTitle("Add New...");
		b.setView(getNewDialogView());
		typeDialog = b.create();
		typeDialog.show();

	}

	private View getNewDialogView() {
		LayoutInflater inf = LayoutInflater.from(this);
		View root = inf.inflate(R.layout.type_dialog_popup, null, false);
		View text = root.findViewById(R.id.text_holder);
		text.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				typeDialog.dismiss();
				Intent i = new Intent(v.getContext(), AddPasswordActivity.class);
				Bundle b = new Bundle();
				b.putStringArrayList("stacks", getStackNames());
				i.putExtras(b);
				startActivityForResult(i, 0);
			}
		});
		View images = root.findViewById(R.id.image_holder);
		images.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				typeDialog.dismiss();

				Intent i = new Intent(
						Intent.ACTION_PICK,
						android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				startActivityForResult(i, 2);
			}
		});
		View file = root.findViewById(R.id.file_holder);
		file.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				typeDialog.dismiss();
				Intent target = FileUtils.createGetContentIntent();
				Intent intent = Intent.createChooser(target, "Select With...");
				try {
					startActivityForResult(intent, 3);
				} catch (ActivityNotFoundException e) {
					e.printStackTrace();
				}
			}
		});
		return root;
	}

	private ArrayList<String> getStackNames() {
		ArrayList<String> results = new ArrayList<String>();
		for (int i = 0; i < mCardStacks.size(); i++) {
			results.add(mCardStacks.get(i).getTitle());
		}
		return results;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 0) {
			if (data != null && data.hasExtra("password")) {
				VaultedText result = (VaultedText) data
						.getSerializableExtra("password");
				if (result.getDataValue().equals("")) {
					result.setDataValue(TMP_FILE_AS_STRING);
					TMP_FILE_AS_STRING = null;
				}
				VaultedTextCard pCard = new VaultedTextCard(result,
						viewButtonListener);
				getStackForTitle(pCard.getPassword().getDataStack()).add(pCard);
				mCardView.refresh();
				showProgress(this);
				ParseObject o = result.toParsePassword();
				Log.d("THE PARSE OBJECT", o.toString());
				o.saveInBackground(new SaveCallback() {
					@Override
					public void done(ParseException arg0) {
						setupPasswordCards();
						hideProgress(MainActivity.this);
					}
				});
			}
		} else if (requestCode == 1) {
			if (data != null && data.hasExtra("password")) {
				final VaultedText result = (VaultedText) data
						.getSerializableExtra("password");
				final VaultedText old = (VaultedText) data
						.getSerializableExtra("oldPassword");
				final VaultedTextCard pCard = new VaultedTextCard(result,
						viewButtonListener);
				ParseQuery<ParseObject> query = ParseQuery
						.getQuery("VaultedText");
				query.getInBackground(pCard.getPassword().getParseId(),
						new GetCallback<ParseObject>() {
							@Override
							public void done(ParseObject foundPassObject,
									ParseException arg1) {
								if (foundPassObject != null) {
									VaultedText foundPass = new VaultedText(
											foundPassObject
													.getString("data_stack"),
											foundPassObject
													.getString("data_value"),
											foundPassObject
													.getString("data_title"),
											VaultedText.TYPE_TEXT);
									foundPass.setParseId(foundPassObject
											.getObjectId());
									foundPass.update(result);
									foundPass.toParsePassword()
											.saveInBackground(
													new SaveCallback() {
														@Override
														public void done(
																ParseException arg0) {
															for (VaultedTextCard card : mCards) {
																if (card.getPassword()
																		.getParseId()
																		.equals(old
																				.getParseId())) {
																	mCards.remove(card);
																	mCards.add(pCard);
																	break;
																}
															}
															setupPasswordCards();
															hideProgress(MainActivity.this);
														}
													});
								} else {
									hideProgress(MainActivity.this);
								}
							}
						});
			}
		} else if (requestCode == 2 && resultCode == RESULT_OK && null != data) {
			Uri selectedImage = data.getData();
			String[] filePathColumn = { MediaStore.Images.Media.DATA };

			Cursor cursor = getContentResolver().query(selectedImage,
					filePathColumn, null, null, null);
			cursor.moveToFirst();

			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			String picturePath = cursor.getString(columnIndex);
			cursor.close();
			File imageF = new File(picturePath);
			try {
				String toBeStored = readFileToString(imageF);
				Log.d("GOT A PICTURE AS A STRING TO ENCRYPT", toBeStored);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (requestCode == 3 && resultCode == RESULT_OK) {
			if (data != null) {
				final Uri uri = data.getData();
				try {
					String path = FileUtils.getPath(this, uri);
					File file = new File(path);
					String toBeStored = readFileToString(file);
					Log.d("GOT A FILE AS A STRING TO ENCRYPT", toBeStored);
					Intent i = new Intent(this, AddPasswordFileActivity.class);
					Bundle b = new Bundle();
					b.putString("type", VaultedText.TYPE_FILE);
					b.putStringArrayList("stacks", getStackNames());
					TMP_FILE_AS_STRING = toBeStored;
					i.putExtras(b);
					startActivityForResult(i, 0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public String readFileToString(File file) throws IOException {
		// Open file
		RandomAccessFile f = new RandomAccessFile(file, "r");
		try {
			// Get and check length
			long longlength = f.length();
			int length = (int) longlength;
			if (length != longlength)
				throw new IOException("File size >= 2 GB");
			// Read file and return data
			byte[] data = new byte[length];
			f.readFully(data);
			return Base64.encodeToString(data, Base64.DEFAULT);
		} finally {
			f.close();
		}
	}

	private void notifyKeySet() {
		AlertDialog.Builder b = new Builder(this);
		b.setTitle("Master VaultedText Already Set");
		b.setMessage("You have already set a master password. If you overwrite this, any password you have encrypted with your current master password will be reset. Are you sure you want to continue?");
		b.setPositiveButton("Yes", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				requestConfirmation();
			}
		});
		b.create().show();
	}

	protected void requestConfirmation() {
		AlertDialog.Builder b = new Builder(this);
		b.setTitle("Master VaultedText Already Set");
		b.setMessage("Again, if you overwrite this, any password you have encrypted with your current master password will be reset. Are you sure you want to continue?");
		b.setPositiveButton("Yes", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				requestKeyValidation(false, null);
			}
		});
		b.create().show();
	}

	private void requestHelp() {
		AlertDialog.Builder b = new Builder(this);
		b.setTitle("How Does It Work?");
		b.setMessage("NFCVault uses the UID of the tag you scan as the encryption key. This app and the cloud storage never store your key, and all of the encryption and decryption is done locally. No unencrypted data is ever transmitted off of the device. This means if you have lost it and did not write down the value, there is no way to retrieve your passwords. We will provide you with the string value of your key. You MUST write this down in a safe place!");
		b.setPositiveButton("View Changelog", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ChangeLog cl = new ChangeLog(MainActivity.this);
				cl.getFullLogDialog().show();
			}
		});
		b.create().show();
	}

	private void requestKeyValidation(boolean forDecryptView,
			VaultedTextCard card) {
		isListeningforInitialTagScan = !forDecryptView;
		isListeningforDecryptTagScan = forDecryptView;
		if (card != null) {
			cardFromDecrypt = card;
		}
		AlertDialog.Builder b = new Builder(this);
		b.setTitle("Master VaultedText");
		if (forDecryptView) {
			final EditText keyView = new EditText(this);
			keyView.setHint("Or enter you master password here");
			b.setMessage("Please scan the tag you setup as your master password. We will then decrypt and show you your password.");
			b.setView(keyView);
			b.setPositiveButton("Decrypt", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					isListeningforInitialTagScan = false;
					isListeningforDecryptTagScan = false;
					try {
						handleDecryptTagInput(keyView.getText().toString());
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					} catch (NoSuchAlgorithmException e) {
						e.printStackTrace();
					}
					dialog.dismiss();
				}
			});
		} else {
			b.setMessage("Please scan the tag you plan to use as your master key. We will store the md5 of this value to ensure you are using the correct key when encrypting/decrypting your passwords.");
		}
		setupDialog = b.create();
		setupDialog.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				isListeningforInitialTagScan = false;
				isListeningforDecryptTagScan = false;
			}
		});
		setupDialog.show();
	}

	private void warnWrongKey() {
		AlertDialog.Builder b = new Builder(this);
		b.setTitle("Master VaultedText Mismatch");
		b.setMessage("When you originally setup your master password, the app hashed and stored that value. The hash of the key you just scanned does not match the stored hash. Confirm you are using the correct key and try again.");
		b.create().show();
	}

	private void warnNoKey() {
		AlertDialog.Builder b = new Builder(this);
		b.setTitle("Master VaultedText No Set");
		b.setMessage("You need to setup a tag as your master password before we continue.");
		b.create().show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
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

	private void resolveIntent(Intent intent) {
		if (!isListeningforInitialTagScan && !isListeningforDecryptTagScan) {
			return;
		} else {
			if (isListeningforInitialTagScan) {
				handleInitialTagScan(intent);
			} else {
				try {
					handleDecryptTagScan(intent);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void handleDecryptTagScan(Intent intent)
			throws UnsupportedEncodingException, NoSuchAlgorithmException {
		byte[] tagId = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
		String key = ByteArrayToHexString(tagId);
		handleDecryptTagInput(key);
	}

	private void handleDecryptTagInput(String key)
			throws UnsupportedEncodingException, NoSuchAlgorithmException {
		if (new SettingsProvider(this).isCorrectKey(key)) {
			if (cardFromDecrypt != null) {
				String decrypted = Encryption.decryptString(cardFromDecrypt
						.getPassword().getDataValue(), key);
				cardFromDecrypt.desc.setText(decrypted);
				cardFromDecrypt.setShowingTrueData(!cardFromDecrypt
						.isShowingTrueData());
			}
		} else {
			if (new SettingsProvider(this).getHashedKey().trim().isEmpty()) {
				warnNoKey();
			} else {
				warnWrongKey();
			}
		}
		setupDialog.dismiss();
		cardFromDecrypt = null;
	}

	private void handleInitialTagScan(Intent intent) {
		try {
			TextView keyView = new TextView(this);
			LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT);
			keyView.setGravity(Gravity.CENTER);
			keyView.setLayoutParams(lp);
			keyView.setTextSize(35);
			keyView.setTypeface(null, Typeface.BOLD_ITALIC);
			keyView.setTextColor(getResources().getColor(
					android.R.color.holo_red_light));
			setupDialog.dismiss();
			byte[] tagId = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
			String key = ByteArrayToHexString(tagId);
			keyView.setText(key);
			AlertDialog.Builder b = new Builder(this);
			b.setCancelable(false);
			b.setTitle("Master VaultedText");
			b.setMessage("This is your master password. This is the only time it will ever be shown. Be SURE you write this down in case your tag is lost.");
			b.setView(keyView);
			b.setPositiveButton("Done", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			new SettingsProvider(this).setHashedKey(key);
			b.create().show();
		} catch (Exception e) {
			e.printStackTrace();
			AlertDialog.Builder b2 = new Builder(this);
			b2.setTitle("Error");
			b2.setMessage("A problem occured hashing your key. We were unable to continue, please try again.");
			b2.create().show();
		}
	}

	public static String ByteArrayToHexString(byte[] inarray) {
		int i, j, in;
		String[] hex = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A",
				"B", "C", "D", "E", "F" };
		String out = "";

		for (j = 0; j < inarray.length; ++j) {
			in = (int) inarray[j] & 0xff;
			i = (in >> 4) & 0x0f;
			out += hex[i];
			i = in & 0x0f;
			out += hex[i];
		}
		return out;
	}

	OnViewButtonLister viewButtonListener = new OnViewButtonLister() {
		@Override
		public void onViewButtonClicked(VaultedTextCard card) {
			if (!card.isShowingTrueData()) {
				if (new SettingsProvider(card.getContext()).getHashedKey()
						.trim().isEmpty()) {
					warnNoKey();
				} else {
					requestKeyValidation(true, card);
				}
			} else {
				card.setShowingTrueData(!card.isShowingTrueData());
			}
		}
	};

	private void managePopups() {
		LaunchPopup rateMe = setupRateMePopup();

		ArrayList<LaunchPopup> popups = new ArrayList<LaunchPopup>();
		popups.add(rateMe);
		LaunchPopupManager popupManager = new LaunchPopupManager(popups, this);
		// LaunchPopupManager.resetData(this);
		popupManager.start();
	}

	private int[] getRatePopupTimes() {
		ArrayList<Integer> res = new ArrayList<Integer>();
		for (int i = 1; i < 50; i++) {
			if ((i % 2) == 0) {
				res.add(i);
			}
		}
		return convertIntegers(res);
	}

	public static int[] convertIntegers(List<Integer> integers) {
		int[] ret = new int[integers.size()];
		Iterator<Integer> iterator = integers.iterator();
		for (int i = 0; i < ret.length; i++) {
			ret[i] = iterator.next().intValue();
		}
		return ret;
	}

	private LaunchPopup setupRateMePopup() {
		final LaunchPopup rate = new LaunchPopup();
		rate.setNeedsPlayStore(true);
		rate.setLaunchesTimes(getRatePopupTimes());
		rate.setTitle("Rate NFCVault?");
		rate.setMessage("If you like this app, please take a moment to comment and rate it on Google Play. Thank you!");
		rate.setPosButton("Rate it", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("market://details?id=" + getPackageName())));
				rate.getManagerCallback().onPopupFinished(rate);
			}
		});
		return rate;
	}

	public static boolean isOnline(Context c) {
		ConnectivityManager cm = (ConnectivityManager) c
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		return cm.getActiveNetworkInfo() != null
				&& cm.getActiveNetworkInfo().isConnected();
	}

}
