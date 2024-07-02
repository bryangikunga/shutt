*
 * MIT License
 *
 * Copyright (c) 2018 aSoft
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.addie.timesapp.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AlertDialog;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.addie.timesapp.R;
import com.addie.timesapp.service.AppTimeDialogService;
import com.addie.timesapp.utils.Utils;

import java.util.List;

import timber.log.Timber;

/**
 * Displays dialog on top of the foreground running activity
 * Transparent activity so only dialog is visible
 */

public class DialogActivity extends Activity {

    private static final String TARGET_PACKAGE_KEY = "target_package";
    private static final String TIME_KEY = "time";
    private static final String DISPLAY_1_MIN = "display_1_min";

    private static final String APP_COLOR_KEY = "app_color";
    private static final String TEXT_COLOR_KEY = "text_color";
    private static final String CALLING_CLASS_KEY = "calling_class";


    private SharedPreferences preferences;
    private boolean hasUsageAccess;
    private String mPackageName;
    private int mAppColor;
    private int mTextColor;
    private String mAppName;
    private Bitmap mAppIcon;
    private boolean mDisplay1Min;
    private String mCallingClass;

    private TimeDialog mTimeDialog;
    private AlertDialog mStopAppDialog;
    private PrefTimeDialog mPrefDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);

        initVariables();

        // For updating to Q, need display over other apps
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.Q && !preferences.contains(getString(R.string.pref_overlay_permission_update))){
            displayOverlayPermissionScreen();
        }

        fetchAppData();

        Timber.d("Calling activity %s", getCallingActivity());

        switch (mCallingClass) {
            case "AppTimeDialogService":
                displayStopAppDialog();
                break;
            case "SettingsFragment":
                displayPrefTimeDialog();

                break;
            default:
                displayTimeDialog();
                break;
        }
    }

    private void displayOverlayPermissionScreen(){

        Intent intent = new Intent(DialogActivity.this,IntroActivity.class);
        intent.putExtra(getString(R.string.intro_activity_mode),getString(R.string.intro_activity_mode_overlay_update));
        startActivity(intent);
        finish();
    }
    /**
     * Initialises values of variables to be used from starting intent and preferencemanager
     */
    private void initVariables() {

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        hasUsageAccess = preferences.getBoolean(getString(R.string.usage_permission_pref), false);
        mPackageName = getIntent().getStringExtra(TARGET_PACKAGE_KEY);
        mAppColor = getIntent().getIntExtra(APP_COLOR_KEY, getResources().getColor(R.color.black));
        mTextColor = getIntent().getIntExtra(TEXT_COLOR_KEY, getResources().getColor(R.color.white));
        mDisplay1Min = getIntent().getBooleanExtra(DISPLAY_1_MIN, true);
        mCallingClass = getIntent().getStringExtra(CALLING_CLASS_KEY);
        if (mCallingClass == null) {
            mCallingClass = "";
        }
    }

    /**
     * A variation of TimeDialog to be displayed from Preferences to be able to set a default time
     */
    private void displayPrefTimeDialog() {
        mPrefDialog = new PrefTimeDialog(this);
        mPrefDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        mPrefDialog.setCancelable(false);

        mPrefDialog.show();
    }

    /**
     * Displays a TimeDialog to select time to be set for app usage
     */
    private void displayTimeDialog() {


        mTimeDialog = new TimeDialog(DialogActivity.this, mPackageName, mAppColor, mTextColor);
        mTimeDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;

        mTimeDialog.show();

        Timber.d("#%06X", (0xFFFFFF & mAppColor));


        mTimeDialog.getWindow().getDecorView().setBackgroundColor(mAppColor);
        mTimeDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                Window window = ((AlertDialog) dialog).getWindow();
                window.getDecorView().setBackgroundColor(mAppColor);
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();

        // Get locked and sleep state of device
        KeyguardManager myKM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        boolean isPhoneLocked = myKM.inKeyguardRestrictedInputMode();

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean isSceenAwake = (Build.VERSION.SDK_INT < 20 ? powerManager.isScreenOn() : powerManager.isInteractive());




        //Do not dismiss dialog if activity is paused due to being locked
        // This is required as app may be in use when time expires, hence a dialog is to be shown when screen is turned on
        if (!isPhoneLocked && isSceenAwake) {

            // Prevents dialog activity from leaking dialog window when activity is paused
            switch (mCallingClass) {
                case "AppTimeDialogService":
                    mStopAppDialog.dismiss();
                    break;
                case "SettingsFragment":
                    mPrefDialog.dismiss();

                    break;
                default:
                    mTimeDialog.dismiss();
                    break;
            }


            // Remove activity from Recents screen
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    List<ActivityManager.AppTask> tasks = am.getAppTasks();
                    if (tasks != null && tasks.size() > 0) {
                        tasks.get(0).setExcludeFromRecents(true);
                    }
                }
            }
        }
    }

    /**
     * Displays the alertDialog for user notifying that time has passed
     */
    private void displayStopAppDialog() {

        String colorHex = String.format("#%06X", (0xFFFFFF & mTextColor));
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AnimatedDialog);
        String title = mAppName;
        builder.setTitle(Html.fromHtml("<font color='" + colorHex + "'>" + title + "</font>"))
                .setCancelable(false)
                .setPositiveButton(Html.fromHtml("<font color='" + colorHex + "'>Stop</font>")
                        , new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();

                                // Goes to home screen
                                Intent intent = new Intent(Intent.ACTION_MAIN);
                                intent.addCategory(Intent.CATEGORY_HOME);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        }).setIcon(new BitmapDrawable(getResources(), mAppIcon))
        ;

        // Boolean flag for whether the +1 Min option is to be shown or not
        if (mDisplay1Min) {

            builder.setNegativeButton(Html.fromHtml("<font color='" + colorHex + "'>+1 Min</font>")
                    , new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                            grantOneMinuteExtra();
                        }
                    });

        }

        builder.setMessage(Html.fromHtml("<font color='" + colorHex + "'>Time's up!</font>"));

        mStopAppDialog = builder.show();

        // Only this seems to work, Passing the int directly to setTextColor seems to be missing some properties
        int parsedTextColor = Color.parseColor(String.format("#%06X", (0xFFFFFF & mTextColor)));

        Button nbutton = mStopAppDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        if (nbutton != null) {

            nbutton.setTextColor(parsedTextColor);
        }

        Button pbutton = mStopAppDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        pbutton.setTextColor(parsedTextColor);


        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        mStopAppDialog.getWindow().setLayout((6 * width) / 7, WindowManager.LayoutParams.WRAP_CONTENT);

        mStopAppDialog.getWindow().getDecorView().setBackgroundColor(mAppColor);
        mStopAppDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                Window window = ((AlertDialog) dialog).getWindow();
                window.getDecorView().setBackgroundColor(mAppColor);
            }
        });

    }


    /**
     * Called when +1 Minute is selected on the dialog
     */
    private void grantOneMinuteExtra() {

        // Start service selected time for app to be stopped
        int time = 60000;
        Intent timeServiceIntent = new Intent(this, AppTimeDialogService.class);
        timeServiceIntent.putExtra(TIME_KEY, time);
        timeServiceIntent.putExtra(TARGET_PACKAGE_KEY, mPackageName);
        timeServiceIntent.putExtra(APP_COLOR_KEY, mAppColor);
        timeServiceIntent.putExtra(TEXT_COLOR_KEY, mTextColor);
        this.startService(timeServiceIntent);

        finish();

    }

    /**
     * Fetches app data
     */
    private void fetchAppData() {
        ApplicationInfo appInfo;
        PackageManager pm = getPackageManager();

        try {
            Drawable drawableIcon = pm.getApplicationIcon(mPackageName);
            mAppIcon = Utils.getBitmapFromDrawable(drawableIcon);
            appInfo = pm.getApplicationInfo(mPackageName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            appInfo = null;
            mAppIcon = null;
        }
        mAppName = (String) (appInfo != null ? pm.getApplicationLabel(appInfo) : "(unknown)");

    }

}