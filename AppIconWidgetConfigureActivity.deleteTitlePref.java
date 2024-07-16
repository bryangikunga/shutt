com.addie.timesapp.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.RemoteViews;

import com.addie.timesapp.R;
import com.addie.timesapp.service.LaunchAppFromWidgetService;
import com.addie.timesapp.utils.Utils;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link AppIconWidgetConfigureActivity AppIconWidgetConfigureActivity}
 */
public class AppIconWidget extends AppWidgetProvider {

    private static final String TARGET_PACKAGE_KEY = "target_package";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        String packageName = AppIconWidgetConfigureActivity.loadPackageNameForWidget(context, appWidgetId);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.layout_widget_icon);
        try {
            Drawable iconDrawable = context.getPackageManager().getApplicationIcon(packageName);
            views.setImageViewBitmap(R.id.app_widget_icon, Utils.getBitmapFromDrawable(iconDrawable));

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

//        TODO: FIX!! Causing crash. Fixed, still might cause crash
        if (!packageName.equals(context.getString(R.string.appwidget_text))) {
            // Launches the selected app
            PackageManager packageManager = context.getPackageManager();

//            Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
            Intent launchIntent = new Intent(context, LaunchAppFromWidgetService.class);
            launchIntent.putExtra(TARGET_PACKAGE_KEY,packageName);
            launchIntent.setAction(Long.toString(System.currentTimeMillis()));

//                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            Log.d("WIDGET", "Package being applied in intent is :" + packageName);
            PendingIntent appPendingIntent = PendingIntent.getService(context, 0, launchIntent, 0);
            views.setOnClickPendingIntent(R.id.app_widget_icon, appPendingIntent);
        }


        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            AppIconWidgetConfigureActivity.deleteTitlePref(context, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}