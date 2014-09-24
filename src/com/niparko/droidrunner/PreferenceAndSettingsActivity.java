package com.niparko.droidrunner;

//Images from Everaldo Coelho and SoftIcons

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class PreferenceAndSettingsActivity extends PreferenceActivity {

	private Context currContext;
	private static final int ABOUT_DROIDRUNNER_DIALOG = 0;
	private static final int VERSION_DIALOG = 1;

	// showDialog callback to create a dialog (code sampled from Lee's Beginning
	// Android p. 35)
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case ABOUT_DROIDRUNNER_DIALOG:
			return new AlertDialog.Builder(this)
					.setTitle("About DroidRunner")
					.setMessage(
							"This application was created by Kevin Niparko '12.  It is his "
									+ "first Android application.  Yup, this is where it all began.")
					.setIcon(R.drawable.clickrun)
					.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
						}
					}).create();
		case VERSION_DIALOG:
			return new AlertDialog.Builder(this).setTitle("Version")
					.setMessage(R.string.thisVersion).setIcon(R.drawable.clickrun)

					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
						}
					}).create();
		}
		return null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		currContext = this;

		// Create preference click variables and add preferences to activity
		Preference pref;
		OnPreferenceClickListener preferenceListener;
		addPreferencesFromResource(R.xml.settings);

		// Set click listener for the User Profile
		pref = findPreference(getString(R.string.prefKeyProfile));
		preferenceListener = new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(currContext, DroidRunnerActivity.class));
				return true;
			}
		};

		pref.setOnPreferenceClickListener(preferenceListener);

		// Set click listener for the homepage preference
		pref = findPreference(getString(R.string.prefKeyHomepage));
		preferenceListener = new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(android.content.Intent.ACTION_VIEW,
						Uri.parse(getString(R.string.projectURL))));
				return true;
			}
		};

		pref.setOnPreferenceClickListener(preferenceListener);

		// Set click listener for the AboutDroidRunner preference
		pref = findPreference(getString(R.string.prefKeyAbout));
		preferenceListener = new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				showDialog(ABOUT_DROIDRUNNER_DIALOG);
				return true;
			}
		};
		pref.setOnPreferenceClickListener(preferenceListener);

		// Set click listener for the Version preference
		pref = findPreference(getString(R.string.prefKeyVersion));
		preferenceListener = new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				showDialog(VERSION_DIALOG);
				return true;
			}
		};
		pref.setOnPreferenceClickListener(preferenceListener);

	}
}
