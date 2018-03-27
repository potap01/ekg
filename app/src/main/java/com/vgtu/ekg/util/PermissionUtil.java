package com.vgtu.ekg.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;


/**
 * Utility class that wraps access to the runtime permissions API in M and provides basic helper
 * methods.
 */
public abstract class PermissionUtil {

	/**
	 * Check that all given permissions have been granted by verifying that each entry in the
	 * given array is of the value {@link PackageManager#PERMISSION_GRANTED}.
	 *
	 * @see Activity#onRequestPermissionsResult(int, String[], int[])
	 */
	public static boolean verifyPermissions(int[] grantResults) {
		// At least one result must be checked.
		if (grantResults == null || grantResults.length < 1) {
			return false;
		}

		// Verify that each required permission has been granted, otherwise return false.
		for (int result : grantResults) {
			if (result != PackageManager.PERMISSION_GRANTED) {
				return false;
			}
		}
		return true;
	}

	public static boolean hasWriteExtStoragePermission(Context context) {
		return hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
	}

	private static boolean hasPermission(Context context, String permission) {
		return ActivityCompat
				.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
	}
}