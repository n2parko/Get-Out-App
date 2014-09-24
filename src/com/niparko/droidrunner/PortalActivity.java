package com.niparko.droidrunner;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class PortalActivity extends TabActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mainportal);
		
		Intent i = getIntent();
		int currTab = i.getIntExtra("selectedTab", 0);
		

		final TabHost tabHost = getTabHost();
		TabHost.TabSpec spec;
		Intent intent;

		// Boiler plate code taken from lab write-up to create tab host

		// ****************************
		// Intent to launch the StartActivity
		intent = new Intent().setClass(this, StartActivity.class);

		// Initialize start tab and add it to the tabHost
		spec = tabHost.newTabSpec("Start").setIndicator("Start").setContent(intent);
		tabHost.addTab(spec);
		// ****************************

		// ****************************
		// Intent to launch the HistoryActivity
		intent = new Intent().setClass(this, HistoryActivity.class);

		// Initialize start tab and add it to the tabHost
		spec = tabHost.newTabSpec("History").setIndicator("History")
				.setContent(intent);
		tabHost.addTab(spec);
		// ****************************

		// ****************************
		// Intent to launch the HistoryActivity
		intent = new Intent().setClass(this, PreferenceAndSettingsActivity.class);

		// Initialize start tab and add it to the tabHost
		spec = tabHost.newTabSpec("Preference").setIndicator("Preference")
				.setContent(intent);
		tabHost.addTab(spec);
		
		tabHost.setCurrentTab(currTab);

	}
}
