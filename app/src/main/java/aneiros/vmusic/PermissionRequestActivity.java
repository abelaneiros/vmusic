/*
 * Copyright (C) 2015-2023 Adrian Ulrich <adrian@blinkenlights.ch>
 * Copyright (C) 2026 Abel ANEIROS <abelaneiros@protonmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>. 
 */

package aneiros.vmusic;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.os.Build;
import android.os.Bundle;

import java.util.Arrays;
import java.util.ArrayList;

public class PermissionRequestActivity extends Activity {

	/**
	 * The intent to start after acquiring the required permissions
	 */
	private Intent mCallbackIntent;
	private ArrayList<String[]> mSteps;
	private int mStep;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mCallbackIntent = getIntent().getExtras().getParcelable("callbackIntent");

		mSteps = new ArrayList<>();
		mSteps.add(getNeededPermissions());
		for (String permission : getOptionalPermissions()) {
			mSteps.add(new String[] { permission });
		}

		if (savedInstanceState != null) {
			mStep = savedInstanceState.getInt("step");
			return;
		}

		requestPermissions(mSteps.get(0), 0);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("step", mStep);
	}

	/**
	 * Called by Activity after the user interacted with the permission request
	 * Requests the next step if the required permissions were granted, exits otherwise.
	 * Will launch the main activity once all steps were requested
	 *
	 * @param requestCode The code set by requestPermissions
	 * @param permissions Names of the permissions we got granted or denied
	 * @param grantResults Results of the permission requests
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (requestCode == 0) {
			ArrayList<String> neededPerms = new ArrayList<>(Arrays.asList(getNeededPermissions()));
			int grantedPermissions = 0;

			for (int i = 0; i < permissions.length; i++) {
				if (!neededPerms.contains(permissions[i]))
					continue;
				if (grantResults[i] == PackageManager.PERMISSION_GRANTED)
					grantedPermissions++;
			}

			if (grantedPermissions != neededPerms.size()) {
				finish();
				return;
			}
		}

		mStep = requestCode + 1;
		if (mStep < mSteps.size()) {
			requestPermissions(mSteps.get(mStep), mStep);
			return;
		}

		// set as finished before (possibly) killing ourselfs
		finish();

		SharedPrefHelper.getSettings(this).edit().putBoolean(PrefKeys.PENDING_FULL_SCAN, true).commit();
		if (mCallbackIntent != null) {
			// start the old intent but ensure to make it a new task & clear any old attached activites
			mCallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(mCallbackIntent);
		}
		// Hack: We *kill* ourselfs (while launching the main activity) to get startet
		// in a new process: This works around a bug/feature in 6.0 that would cause us
		// to get 'partial read' permissions (eg: reading from the content provider works
		// but reading from /sdcard doesn't)
		android.os.Process.killProcess(android.os.Process.myPid());
	}

	/**
	 * Injects a warning that we are missing read permissions into the activity layout
	 *
	 * @param activity Reference to LibraryActivity
	 * @param intent The intent starting the parent activity
	 */
	public static void showWarning(final LibraryActivity activity, final Intent intent) {
		LayoutInflater inflater = LayoutInflater.from(activity);
		View view = inflater.inflate(R.layout.permission_request, null, false);
		((TextView)view.findViewById(R.id.permission_request_title)).setText(getWarningText(activity));

		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PermissionRequestActivity.requestPermissions(activity, intent);
			}
		});

		ViewGroup parent = (ViewGroup)activity.findViewById(R.id.content); // main layout of library_content
		parent.addView(view, -1);
	}

	private static String getWarningText(Context context) {
		int[] lines;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			lines = new int[] { R.string.permission_request_audio, R.string.permission_request_images, R.string.permission_request_notifications };
		} else {
			lines = new int[] { R.string.permission_request_storage };
		}

		StringBuilder text = new StringBuilder(context.getString(R.string.permission_request_summary));
		for (int line : lines) {
			text.append("\n\n\u2022 ").append(context.getString(line));
		}
		return text.toString();
	}

	/**
	 * Launches a permission request dialog if needed
	 *
	 * @param activity The activitys context to use for the permission check
	 * @return boolean true if we showed a permission request dialog
	 */ 
	public static boolean requestPermissions(Activity activity, Intent callbackIntent) {
		boolean havePermissions = havePermissions(activity);

		if (havePermissions == false) {
			Intent intent = new Intent(activity, PermissionRequestActivity.class);
			intent.putExtra("callbackIntent", callbackIntent);
			activity.startActivity(intent);
		}

		return !havePermissions;
	}

	/**
	 * Checks if all required permissions have been granted
	 *
	 * @param context The context to use
	 * @return boolean true if all permissions have been granded
	 */
	public static boolean havePermissions(Context context) {
		for (String permission : getNeededPermissions()) {
			if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
				return false;
			}
		}
		return true;
	}

	private static String[] getNeededPermissions() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			return new String[] { Manifest.permission.READ_MEDIA_AUDIO };
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			return new String[] { Manifest.permission.READ_EXTERNAL_STORAGE };
		}
		return new String[] { Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE };
	}

	private static String[] getOptionalPermissions() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			return new String[] { Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.POST_NOTIFICATIONS };
		}
		return new String[]{};
	}

}
