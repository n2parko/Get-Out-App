package com.niparko.droidrunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class DroidRunnerActivity extends Activity {
	/** Called when the activity is first created. */

	// Constants
	private static final int NO_GENDER = -1;
	private static final int NO_CLASS = 0;
	private int classSpinnerSavedInput = NO_CLASS;
	private static final String SAVED = "SAVED";
	private static final String namePref = "namePref";
	private static final String phonePref = "phonePref";
	private static final String emailPref = "emailPref";
	private static final String majorPref = "majorPref";
	private static final String genderPref = "genderPref";
	private static final String userInputPrefName = "userInput";
	private static final String birthdayPref = "birthdayPref";
	private static final String classPref = "classPref";
	private static final String profileURIPref = "profilePref";

	private SharedPreferences userInput;

	// Dialog reference codes
	private static final int BIRTHDAY_PICKER = 0;
	private static final int PHOTO_PICKER = 1;
	private static final int CAMERA_CHOSEN_ITEM = 0;

	// Constants used for spinner
	int year, month, day;

	// Intent codes for onActivityForResults callback
	private static final int CAMERA_CHOSEN_INTENT_CODE = 1000;
	private static final int FILE_CHOSEN_INTENT_CODE = 2000;
	private static final int CROP_INTENT_CODE = 3000;

	// Used for photo storage and cropping
	private File pathProfileFile;
	private static final String pathProfileAddOn = "profileImg.png";
	private Uri imageCaptureURI;

	// Views for the UI
	private EditText nameEditText;
	private EditText emailEditText;
	private EditText phoneEditText;
	private EditText majorEditText;
	private TextView birthdayView;
	private RadioGroup genderRadioGroup;
	private ImageView profileImageView;
	private Spinner classSpinner;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		pathProfileFile = Environment.getExternalStorageDirectory();

		// Initialize the spinner, code sampled from
		// http://developer.android.com/resources/tutorials/views/hello-spinner.html
		classSpinner = (Spinner) findViewById(R.id.spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
				R.array.classSpinner, android.R.layout.simple_spinner_dropdown_item);
		adapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		classSpinner.setAdapter(adapter);

		classSpinner
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

					public void onItemSelected(AdapterView<?> parent, View view, int pos,
							long id) {
						((TextView) parent.getChildAt(NO_CLASS)).setTextColor(Color.WHITE);
						classSpinnerSavedInput = pos;
					}

					public void onNothingSelected(AdapterView<?> arg0) {
						classSpinnerSavedInput = NO_CLASS;
					}

				});

		// Get saved user input
		userInput = getSharedPreferences(userInputPrefName, MODE_PRIVATE);

		birthdayView = (TextView) findViewById(R.id.birthdayDisplay);
		nameEditText = (EditText) findViewById(R.id.name_field);
		emailEditText = (EditText) findViewById(R.id.email_field);
		phoneEditText = (EditText) findViewById(R.id.phone_field);
		majorEditText = (EditText) findViewById(R.id.major_field);
		genderRadioGroup = (RadioGroup) findViewById(R.id.gender_radio_group);
		profileImageView = (ImageView) findViewById(R.id.profilePicImageView);

		loadUserInput();

		// Set up clicklistener for button taken in part from Lee's Beginning
		// Android
		Button reusableButton = (Button) findViewById(R.id.save);
		Button clear_button = (Button) findViewById(R.id.clear);
		OnClickListener save_listener = new OnClickListener() {
			public void onClick(View v) {
				Log.d(SAVED, "SAVE BUTTON HIT");
				// Save the profile data
				saveProfile();

				// Create and display a toast
				Toast saveMessage = Toast.makeText(getApplicationContext(),
						"Profile Saved", Toast.LENGTH_SHORT);
				saveMessage.setGravity(Gravity.CENTER, 0, 0);
				saveMessage.show();
			}
		};

		OnClickListener clear_listener = new OnClickListener() {
			public void onClick(View v) {
				clearContents();
			}
		};

		reusableButton.setOnClickListener(save_listener);
		clear_button.setOnClickListener(clear_listener);

		// Set up the click callback for the birthday selector
		// Code sampled from Lee, 150-151

		Calendar today = Calendar.getInstance();
		year = today.get(Calendar.YEAR);
		month = today.get(Calendar.MONTH);
		day = today.get(Calendar.DAY_OF_MONTH);

		reusableButton = (Button) findViewById(R.id.birthdayButton);
		reusableButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				showDialog(BIRTHDAY_PICKER);
			}

		});

		// Set the click callback for the profile picture button
		reusableButton = (Button) findViewById(R.id.changePhotoButton);
		reusableButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showDialog(PHOTO_PICKER);
			}
		});

	}

	// Callback for startActivityForResult, sampled from Campbell lab write-up
	@Override
	protected void onActivityResult(int intentCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) {
			return;
		}
		else if (intentCode == CAMERA_CHOSEN_INTENT_CODE) {
			cropSelection();
		}
		else if (intentCode == FILE_CHOSEN_INTENT_CODE) {
			imageCaptureURI = data.getData();
			cropSelection();
		}
		else if (intentCode == CROP_INTENT_CODE) {
			Bundle extras = data.getExtras();
			if (extras != null) {
				Bitmap photo = extras.getParcelable("data");
				profileImageView.setImageBitmap(photo);
			}
			// Delete the file
			File f = new File(imageCaptureURI.getPath());
			if (f.exists()) {
				f.delete();
			}

		}
	}

	// Method for cropping a photo, code sampled from
	// http://www.londatiga.net/featured-articles/how-to-select-and-crop-image-on-android/
	private void cropSelection() {

		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setType("image/*");

		List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent,
				0);

		int size = list.size();

		if (size == 0) {
			Toast.makeText(this, "Cannot find the image", Toast.LENGTH_SHORT).show();
			return;
		}
		else {
			intent.setData(imageCaptureURI);
			intent.putExtra("outputX", 100);
			intent.putExtra("outputY", 100);
			intent.putExtra("aspectX", 1);
			intent.putExtra("aspectY", 1);
			intent.putExtra("scale", true);
			intent.putExtra("return-data", true);

			Intent i = new Intent(intent);
			ResolveInfo res = list.get(0);

			i.setComponent(new ComponentName(res.activityInfo.packageName,
					res.activityInfo.name));
			startActivityForResult(i, CROP_INTENT_CODE);
		}
	}

	// Method to save the profile
	public void saveProfile() {

		// Save the text in the name textbox taken in part from Lee's Beginning
		// Android
		String nameInput = nameEditText.getText().toString();
		String emailInput = emailEditText.getText().toString();
		String phoneInput = phoneEditText.getText().toString();
		String majorInput = majorEditText.getText().toString();
		String birthdayInput = birthdayView.getText().toString();
		int checkedRadioId = genderRadioGroup.getCheckedRadioButtonId();
		int classSpinnerInput = classSpinnerSavedInput;

		SharedPreferences.Editor userInputSaver = userInput.edit();

		userInputSaver.putInt(classPref, classSpinnerInput);
		userInputSaver.putString(namePref, nameInput);
		userInputSaver.putString(phonePref, phoneInput);
		userInputSaver.putString(majorPref, majorInput);
		userInputSaver.putString(emailPref, emailInput);
		userInputSaver.putInt(genderPref, checkedRadioId);
		userInputSaver.putString(birthdayPref, birthdayInput);
		userInputSaver.putInt(classPref, classSpinnerInput);

		userInputSaver.commit();

		savePhoto();

	}

	// Method to save the photo, sampled from
	// http://www.londatiga.net/featured-articles/how-to-select-and-crop-image-on-android/
	public boolean savePhoto() {
		// Build a bitmap of the profile picture
		profileImageView.buildDrawingCache();
		Bitmap savedProfilePic = Bitmap.createBitmap(profileImageView
				.getDrawingCache());
		boolean res = false;

		if (savedProfilePic == null) {
			Log.v("savePhoto", "savedPicNull");
		}

		// store the profile picture in internal memory
		File file = new File(pathProfileFile, pathProfileAddOn);
		try {
			file.createNewFile();
			Log.v("savePhoto", file.getPath());
			if (!file.exists()) {
				Log.v("SavePhoto", "FileDOESN'tExists");
			}
			FileOutputStream fOut = new FileOutputStream(file);
			res = savedProfilePic.compress(Bitmap.CompressFormat.PNG, 100, fOut);
			fOut.flush();
			fOut.close();
			res = true;
		}
		catch (IOException ice) {
			Log.v("EXCEPTION", ice.toString());
		}
		profileImageView.destroyDrawingCache();

		return res;

	}

	// Clear contents of the fields
	public void clearContents() {
		// Clear all fields
		majorEditText.setText("");
		nameEditText.setText("");
		phoneEditText.setText("");
		emailEditText.setText("");
		genderRadioGroup.clearCheck();

		SharedPreferences.Editor userInputSaver = userInput.edit();

		// delete from shared preferences
		userInputSaver.putString(namePref, "");
		userInputSaver.putString(phonePref, "");
		userInputSaver.putString(majorPref, "");
		userInputSaver.putString(emailPref, "");
		userInputSaver.putInt(classPref, NO_CLASS);
		userInputSaver.putInt(genderPref, NO_GENDER);
		userInputSaver.putString(profileURIPref, "");

		userInputSaver.commit();

	}

	// Load the input from savedPreferences
	public void loadUserInput() {

		// Display user inputs in the appropriate text boxes
		String nameInput = userInput.getString(namePref, "");
		if (nameInput.length() > 0) {
			nameEditText.setText(nameInput);
		}

		int classInput = userInput.getInt(classPref, NO_CLASS);
		classSpinner.setSelection(classInput);

		String emailInput = userInput.getString(emailPref, "");
		if (emailInput.length() > 0) {
			emailEditText.setText(emailInput);
		}

		String phoneInput = userInput.getString(phonePref, "");
		if (phoneInput.length() > 0) {
			phoneEditText.setText(phoneInput);
		}

		String majorInput = userInput.getString(majorPref, "");
		if (majorInput.length() > 0) {
			majorEditText.setText(majorInput);
		}

		int checkedRadioId = userInput.getInt(genderPref, NO_GENDER);
		if (checkedRadioId != NO_GENDER) {
			genderRadioGroup.check(checkedRadioId);
		}

		String birthdayInput = userInput.getString(birthdayPref, "Birthday");
		birthdayView.setText(birthdayInput);

		// Text sampled from Android-er Blog "Load bitmap file from SD Card"
		File file = new File(pathProfileFile, pathProfileAddOn);
		try {
			Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
			if (bitmap != null) {
				profileImageView.setImageBitmap(bitmap);
			}
			else {
				Log.v("LoadUserInput", file.getPath() + " is Null");
			}

		}
		catch (Exception e) {
			Log.v("loadUserInput", e.toString());
		}

	}

	// Create the dialog, sampled from Lee's Beginning Android
	protected Dialog onCreateDialog(int id) {
		Log.v("showDialog", Integer.toString(id));
		switch (id) {
		case BIRTHDAY_PICKER:
			return new DatePickerDialog(this, mDateSetListener, year, month, day);
		case PHOTO_PICKER:
			return new AlertDialog.Builder(this)
					.setTitle("Find your profile photo")
					.setItems(R.array.photoPickerOptions,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int item) {
									if (item == CAMERA_CHOSEN_ITEM) {
										Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
										imageCaptureURI = Uri.fromFile(new File(Environment
												.getExternalStorageDirectory(), "tmp_profile_photo"
												+ String.valueOf(System.currentTimeMillis()) + ".jpg"));
										intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
												imageCaptureURI);

										try {
											intent.putExtra("return-data", true);
											startActivityForResult(intent, CAMERA_CHOSEN_INTENT_CODE);
										}
										catch (ActivityNotFoundException e) {
											e.printStackTrace();
										}
									}
									else {
										Intent intent = new Intent();
										intent.setType("image/*").setAction(
												Intent.ACTION_GET_CONTENT);

										startActivityForResult(
												Intent.createChooser(intent, "Complete action using"),
												FILE_CHOSEN_INTENT_CODE);
									}
								}
							}).create();
		}
		return null;
	}

	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			TextView birthday = (TextView) findViewById(R.id.birthdayDisplay);
			String birthdayInput = (monthOfYear + 1) + "/" + dayOfMonth + "/" + year;
			birthday.setText(birthdayInput);
			Toast.makeText(getBaseContext(), birthdayInput, Toast.LENGTH_SHORT)
					.show();
		}

	};
}
