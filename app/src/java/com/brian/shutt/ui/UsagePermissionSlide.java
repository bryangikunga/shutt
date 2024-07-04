package com.addie.timesapp.ui;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.addie.timesapp.R;

public class UsagePermissionSlide extends Fragment {

    private static final String ARG_LAYOUT_RES_ID = "layoutResId";
    private int layoutResId;
    private SharedPreferences preferences;
    private Context mContext;

    public static UsagePermissionSlide newInstance(int layoutResId) {
        UsagePermissionSlide usagePermissionSlide = new UsagePermissionSlide();

        Bundle args = new Bundle();
        args.putInt(ARG_LAYOUT_RES_ID, layoutResId);
        usagePermissionSlide.setArguments(args);

        return usagePermissionSlide;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null && getArguments().containsKey(ARG_LAYOUT_RES_ID)) {
            layoutResId = getArguments().getInt(ARG_LAYOUT_RES_ID);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mContext = getActivity();

        preferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        Button mPermissionButton = (Button) getView().findViewById(R.id.btn_usage_permission);

        mPermissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                grantPermissionClicked();
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
       ImageView mCheckImageView =(ImageView) getView().findViewById(R.id.iv_usage_permission_slide_check_state);

        if (hasUsageStatsPermission(mContext)){
                mCheckImageView.setImageResource(R.drawable.ic_check_green_24dp);
        }
        else{

            mCheckImageView.setImageResource(R.drawable.ic_clear_red_24dp);
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {


        return inflater.inflate(layoutResId, container, false);
    }


    /**
     * Handles the button click to launch activity or not
     */
    private void grantPermissionClicked() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && !hasUsageStatsPermission(mContext)) {
            requestUsageStatsPermission();
        } else {
            Toast.makeText(mContext, "Permission already granted!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Launches activity in settings to grant permission
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    void requestUsageStatsPermission() {
        Toast.makeText(mContext, R.string.usage_permission_instruction, Toast.LENGTH_LONG).show();
        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }

    /**
     * Checks if permission is granted or not
     * @param context
     * @return
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow("android:get_usage_stats",
                android.os.Process.myUid(), context.getPackageName());
        boolean granted = mode == AppOpsManager.MODE_ALLOWED;

        preferences.edit().putBoolean(getString(R.string.usage_permission_pref), granted).apply();

        return granted;
    }

}
