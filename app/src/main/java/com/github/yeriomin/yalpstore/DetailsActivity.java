package com.github.yeriomin.yalpstore;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.yeriomin.yalpstore.model.App;

public class DetailsActivity extends YalpStoreActivity {

    static public final int PERMISSIONS_REQUEST_CODE = 828;

    static private final String INTENT_PACKAGE_NAME = "INTENT_PACKAGE_NAME";

    protected DownloadOrInstallFragment downloadOrInstallManager;
    private IgnoreOptionFragment ignoreOptionManager;

    static public void start(Context context, String packageName) {
        Intent intent = new Intent(context, DetailsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(DetailsActivity.INTENT_PACKAGE_NAME, packageName);
        context.startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        if (null != ignoreOptionManager) {
            ignoreOptionManager.setMenu(menu);
            ignoreOptionManager.draw();
        }
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_ignore) {
            ignoreOptionManager.toggleBlackWhiteList();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        final String packageName = getIntentPackageName(intent);
        if (TextUtils.isEmpty(packageName)) {
            Log.e(this.getClass().getName(), "No package name provided");
            finishActivity(0);
            return;
        }
        ignoreOptionManager = new IgnoreOptionFragment(this, new App());

        if (null != DetailsDependentActivity.app) {
            drawDetails(DetailsDependentActivity.app);
        }
        Log.i(this.getClass().getName(), "Getting info about " + packageName);

        DetailsTask task = getDetailsTask(packageName);
        task.setTaskClone(getDetailsTask(packageName));
        task.execute();
    }

    private DetailsTask getDetailsTask(String packageName) {
        DetailsTask task = new DetailsTask() {

            @Override
            protected void onPostExecute(Throwable e) {
                super.onPostExecute(e);
                if (this.app != null) {
                    DetailsDependentActivity.app = this.app;
                    drawDetails(this.app);
                }
            }
        };
        task.setPackageName(packageName);
        task.setContext(this);
        task.setProgressIndicator(findViewById(R.id.progress));
        return task;
    }

    @Override
    protected void onPause() {
        if (null != downloadOrInstallManager) {
            downloadOrInstallManager.unregisterReceivers();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (null != downloadOrInstallManager) {
            downloadOrInstallManager.unregisterReceivers();
            downloadOrInstallManager.registerReceivers();
            downloadOrInstallManager.draw();
        }
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onNewIntent(getIntent());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE
            && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            downloadOrInstallManager.downloadOrInstall();
        }
    }

    private String getIntentPackageName(Intent intent) {
        if (intent.hasExtra(INTENT_PACKAGE_NAME)) {
            return intent.getStringExtra(INTENT_PACKAGE_NAME);
        } else if (intent.getScheme() != null
            && (intent.getScheme().equals("market")
                || intent.getScheme().equals("http")
                || intent.getScheme().equals("https")
        )) {
            return intent.getData().getQueryParameter("id");
        }
        return null;
    }

    private void drawDetails(App app) {
        setTitle(app.getDisplayName());
        setContentView(R.layout.details_activity_layout);
        new GeneralDetailsFragment(this, app).draw();
        new ScreenshotFragment(this, app).draw();
        new ReviewFragment(this, app).draw();
        new AppListsFragment(this, app).draw();
        new BackToPlayStoreFragment(this, app).draw();
        ignoreOptionManager.setApp(app);
        ignoreOptionManager.draw();
        if (null != downloadOrInstallManager) {
            downloadOrInstallManager.unregisterReceivers();
        }
        downloadOrInstallManager = new DownloadOrInstallFragment(this, app);
        downloadOrInstallManager.registerReceivers();
        downloadOrInstallManager.draw();
    }
}
