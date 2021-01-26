/*******************************************************************************
 * This file is part of MOTHER 3 VF for Android (2017, JumpmanFR)
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * <p>
 * Contributors:
 * Paul Kratt - main MultiPatch application for macOS
 * byuu - UPS patcher
 * xperia64 - port to Android support
 * JumpmanFR - adaptation for MOTHER3VF
 ******************************************************************************/
package fr.mother3vf.mother3vf;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.nio.charset.Charset;

public class MainActivity extends Activity implements View.OnClickListener {

    public static native int upsPatchRom(String romPath, String patchPath, String outputFile, int jignoreChecksum);
    public static native String getKey();

    public static final String ROM_FORMATS = ".*\\.(gba|agb|bin|jgc|rom)";

    private static final String CURRENT_FOLDER = "CURRENT_FOLDER";
    private static final String ROM_FILE = "ROM_FILE";
    private static final String DOC_FILE = "DOC_FILE";

    public static final int DIALOG_PERMISSIONS = 101;
    public static final int DIALOG_OVERWRITE = 102;
    public static final int DIALOG_ERROR_PERMISSIONS = 103;
    public static final int DIALOG_BROWSE_PATCH = 104;
    public static final int DIALOG_CONFIRM_PATCH = 105;
    public static final int DIALOG_PATCH_DONE = 106;
    public static final int DIALOG_PATCH_SUCCESS = 107;

    private static final int PERMISSION_REQUEST = 178;
    private static final int NUM_PERMISSIONS = 2;

    private DFragment progressDialog;

    private String currentFolder = "";
    private String romFile = "";
    private String docFile = "";

    class PatchingTaskReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshPatchingDialog();
        }
    }

    private PatchingTaskReceiver patchingTaskReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.main_view_title);

        try {
            System.loadLibrary("upspatcher");
        } catch (UnsatisfiedLinkError e) {
            Log.e("Bad:", "Cannot grab upspatcher!");
        }

        findViewById(R.id.romButton).setOnClickListener(this);
        findViewById(R.id.applyPatch).setOnClickListener(this);
        findViewById(R.id.website).setOnClickListener(this);
        findViewById(R.id.opendoc).setOnClickListener(this);
        findViewById(R.id.about).setOnClickListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions();
        }

        if (savedInstanceState != null) {
            romFile = savedInstanceState.getString(ROM_FILE);
            currentFolder = savedInstanceState.getString(CURRENT_FOLDER);
            docFile = savedInstanceState.getString(DOC_FILE);
            updateViews();
        }
        patchingTaskReceiver = new PatchingTaskReceiver();
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshPatchingDialog();
        IntentFilter filter = new IntentFilter(PatchingTask.REFRESH_DIALOG);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(patchingTaskReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(patchingTaskReceiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString(ROM_FILE, romFile);
        state.putString(CURRENT_FOLDER, currentFolder);
        state.putString(DOC_FILE, docFile);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        //if (DFragment.TAG_PROGRESS.equals(fragment.getTag())) {
            progressDialog = (DFragment) fragment;
        //}
    }

    public void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                DialogFragment dFragment = new DFragment();
                Bundle args = new Bundle();
                args.putInt(DFragment.TITLE, R.string.permissions_title);
                args.putString(DFragment.MESSAGE, getResources().getString(R.string.permissions));
                args.putInt(DFragment.ID, DIALOG_PERMISSIONS);
                args.putInt(DFragment.ICON, android.R.drawable.ic_dialog_alert);
                args.putInt(DFragment.BUTTONS, 2);
                dFragment.setArguments(args);
                dFragment.setCancelable(false);
                dFragment.show(getFragmentManager(), "");

            } else {
                // No explanation needed, we can request the permission.
                actuallyRequestPermissions();

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void actuallyRequestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                boolean good = true;
                if (permissions.length != NUM_PERMISSIONS || grantResults.length != NUM_PERMISSIONS) {
                    good = false;
                }

                for (int i = 0; i < grantResults.length && good; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        good = false;
                    }
                }
                if (!good) {
                    showFatalPermissionsError();
                } else {
                    /*if (!Environment.getExternalStorageDirectory().canRead()) {
                        // Buggy emulator? Try restarting the app
                        AlarmManager alm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
                        alm.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, PendingIntent.getActivity(this, 237462, new Intent(this, MainActivity.class), Intent.FLAG_ACTIVITY_NEW_TASK));
                        System.exit(0);
                    }*/
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void patchCheck() {
        // Just try to interpret the following if statement. I dare you.
        if (new File(romFile + ".original").exists()) {
            DialogFragment dFragment = new DFragment();
            Bundle args = new Bundle();
            args.putInt(DFragment.TITLE, R.string.warning);
            args.putInt(DFragment.ID, DIALOG_OVERWRITE);
            args.putInt(DFragment.ICON, android.R.drawable.ic_dialog_alert);
            args.putString(DFragment.MESSAGE, getResources().getString(R.string.already_exists));
            args.putInt(DFragment.BUTTONS, 2);
            dFragment.setArguments(args);
            dFragment.show(getFragmentManager(), "");
        } else {
            patch();
        }

    }

    public void patch() {
        patch(null);
    }

    public void patch(String patchFile) {
        final CheckBox backupCheckbox = (CheckBox) findViewById(R.id.backupCheckbox);
        Intent i = new Intent(this, PatchingTask.class);
        i.setAction(PatchingTask.ACTION_PATCH);
        i.putExtra(PatchingTask.ROM_FILE, romFile);
        if (patchFile != null) {
            i.putExtra(PatchingTask.PATCH_FILE, patchFile);
        }
        i.putExtra(PatchingTask.BACKUP, backupCheckbox.isChecked());
        //i.putExtra(PatchingTask.RECEIVER, receiver);
        startService(i);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            currentFolder = extras.getString((FileBrowserActivity.FOLDER));
            if (extras != null) {
                switch (requestCode) {
                    case FileBrowserActivity.BROWSE_FOR_ROM:
                        romFile = extras.getString(FileBrowserActivity.FILE);
                        break;
                    case FileBrowserActivity.BROWSE_FOR_PATCH:
                        patch(extras.getString(FileBrowserActivity.FILE));
                        break;
                }
            }
            updateViews();
        } else if (resultCode == RESULT_CANCELED && requestCode == FileBrowserActivity.BROWSE_FOR_PATCH) {
            showPatchingCanceled();
        }
    }

    private void updateViews() {
        TextView romText = (TextView) findViewById(R.id.romText);
        boolean hasRom = !"".equals(romFile);
        findViewById(R.id.applyPatch).setEnabled(hasRom);
        findViewById(R.id.applyPatch).setFocusable(hasRom);
        boolean hasDoc = !"".equals(docFile);
        findViewById(R.id.opendoc).setVisibility(hasDoc ? View.VISIBLE : View.INVISIBLE);
        findViewById(R.id.website).setVisibility(hasDoc ? View.INVISIBLE : View.VISIBLE);
        if (romFile.lastIndexOf("/") > -1) {
            romText.setText(romFile.substring(romFile.lastIndexOf("/") + 1));
        }
        romText.postInvalidate();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.romButton:
                Intent intent = new Intent(this, FileBrowserActivity.class);
                intent.putExtra(FileBrowserActivity.SHOW_UPS, false);
                intent.putExtra(FileBrowserActivity.TARGET_TYPE, ROM_FORMATS);
                intent.putExtra(FileBrowserActivity.TARGET_ICON, "\uD83C\uDFAE");
                if (!"".equals(currentFolder)) {
                    intent.putExtra(FileBrowserActivity.FOLDER, currentFolder);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivityForResult(intent, FileBrowserActivity.BROWSE_FOR_ROM);
                break;
            case R.id.applyPatch:
                String lowerName = romFile.toLowerCase();
                if (lowerName.endsWith(".zip") || lowerName.endsWith(".7z") || lowerName.endsWith(".rar")) {
                    DialogFragment dFragment = new DFragment();
                    Bundle args = new Bundle();
                    args.putInt(DFragment.ID, DIALOG_CONFIRM_PATCH);
                    args.putInt(DFragment.ICON, android.R.drawable.ic_dialog_alert);
                    args.putInt(DFragment.TITLE, R.string.warning);
                    args.putString(DFragment.MESSAGE, getResources().getString(lowerName.endsWith(".zip") ? R.string.zipped_rom_ask : R.string.bad_compression_format));
                    args.putInt(DFragment.BUTTONS, 2);
                    dFragment.setArguments(args);
                    dFragment.show(getFragmentManager(), "");
                } else {
                    patchCheck();
                }
                break;
            case R.id.website:
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("http://mother3vf.free.fr"));
                startActivity(i);
                break;
            case R.id.opendoc:
                Intent docIntent = new Intent(this, DocActivity.class);
                docIntent.putExtra(DocActivity.DOC_FILE, docFile);
                docIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(docIntent);
                break;
            case R.id.about:
                DialogFragment dFragment = new DFragment();
                Bundle args = new Bundle();
                args.putString(DFragment.MESSAGE, getResources().getString(R.string.about));
                args.putInt(DFragment.BUTTONS, 1);
                dFragment.setArguments(args);
                dFragment.show(getFragmentManager(), "");
                break;
        }
    }

    protected void onDialogResponse(int dialogType, boolean response) {
        switch(dialogType) {
            case DIALOG_CONFIRM_PATCH:
                if (response) {
                    patchCheck();
                } else {
                    showPatchingCanceled();
                }
                break;
            case DIALOG_OVERWRITE:
                if (response) {
                    new File(romFile + ".original").delete();
                    patch();
                } else {
                    showPatchingCanceled();
                }
                break;
            case DIALOG_PERMISSIONS:
                if (response) {
                    actuallyRequestPermissions();
                } else {
                    showFatalPermissionsError();
                }
                break;
            case DIALOG_ERROR_PERMISSIONS:
                finish();
                break;
            case DIALOG_BROWSE_PATCH:
                if (response) {
                    Intent intent = new Intent(this, FileBrowserActivity.class);
                    intent.putExtra(FileBrowserActivity.SHOW_UPS, true);
                    intent.putExtra(FileBrowserActivity.TARGET_TYPE, "mother3vf.*\\.ups");
                    intent.putExtra(FileBrowserActivity.TARGET_ICON, "\uD83C\uDDEB\uD83C\uDDF7");
                    if (!"".equals(currentFolder)) {
                        intent.putExtra(FileBrowserActivity.FOLDER, currentFolder);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivityForResult(intent, FileBrowserActivity.BROWSE_FOR_PATCH);
                } else {
                    showPatchingCanceled();
                }
                break;
            case DIALOG_PATCH_DONE:
                PatchingDialogModel.getInstance().reset();
                break;
            case DIALOG_PATCH_SUCCESS:
                PatchingDialogModel.getInstance().reset();
                if (!"".equals(docFile)) {
                    Intent docIntent = new Intent(this, DocActivity.class);
                    docIntent.putExtra(DocActivity.DOC_FILE, docFile);
                    docIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(docIntent);
                }
                break;
        }
    }

    private void showFatalPermissionsError() {
        DialogFragment dFragment = new DFragment();
        Bundle args = new Bundle();
        args.putString(DFragment.MESSAGE, getResources().getString(R.string.permissions_error));
        args.putInt(DFragment.ID, DIALOG_ERROR_PERMISSIONS);
        args.putInt(DFragment.ICON, android.R.drawable.ic_dialog_alert);
        args.putInt(DFragment.BUTTONS, 1);
        dFragment.setArguments(args);
        dFragment.setCancelable(false);
        dFragment.show(getFragmentManager(), "");
    }

    private void showPatchingCanceled() {
        PatchingDialogModel.getInstance().reset();
        DialogFragment dFragment = new DFragment();
        Bundle args = new Bundle();
        args.putString(DFragment.MESSAGE, getResources().getString(R.string.nopatch));
        args.putInt(DFragment.BUTTONS, 1);
        dFragment.setArguments(args);
        dFragment.show(getFragmentManager(), "");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        findViewById(R.id.romButton).setOnClickListener(null);
        findViewById(R.id.applyPatch).setOnClickListener(null);
        findViewById(R.id.website).setOnClickListener(null);
        findViewById(R.id.opendoc).setOnClickListener(null);
        findViewById(R.id.about).setOnClickListener(null);
    }


    private void refreshPatchingDialog() {
        String message = PatchingDialogModel.getInstance().getMessage(); //resultData.getString(PatchingTask.RESPONSE);
        switch (PatchingDialogModel.getInstance().getResultCode() /*resultCode*/) {
            case PatchingDialogModel.STEP_DONE:
            case PatchingDialogModel.STEP_SUCCESS:
                if (progressDialog != null) {
                    progressDialog.dismissAllowingStateLoss();
                }
                ((Vibrator)getSystemService(VIBRATOR_SERVICE)).vibrate(40);
                progressDialog = new DFragment();
                Bundle alertArgs = new Bundle();
                if (PatchingDialogModel.getInstance().getResultCode() == PatchingDialogModel.STEP_SUCCESS) {
                    alertArgs.putInt(DFragment.ID, DIALOG_PATCH_SUCCESS);
                } else {
                    alertArgs.putInt(DFragment.ID, DIALOG_PATCH_DONE);
                }
                alertArgs.putInt(DFragment.TITLE, R.string.app_name_dialogs);
                alertArgs.putString(DFragment.MESSAGE, message);
                alertArgs.putInt(DFragment.BUTTONS, 1);
                progressDialog.setArguments(alertArgs);
                progressDialog.setCancelable(false);
                //alertFragment.show(getFragmentManager(), "");
                docFile = PatchingDialogModel.getInstance().getDocFile();
                updateViews();
                getFragmentManager().beginTransaction().add(progressDialog, "").commitAllowingStateLoss();
                PatchingDialogModel.getInstance().reset();
                break;
            case PatchingDialogModel.STEP_RUNNING:
                if (progressDialog == null || progressDialog.isDismissed()) {
                    progressDialog = new DFragment();
                    Bundle progressArgs = new Bundle();
                    progressArgs.putInt(DFragment.TITLE, R.string.wait);
                    progressArgs.putString(DFragment.MESSAGE, message);
                    progressArgs.putBoolean(DFragment.PROGRESS, true);
                    progressDialog.setArguments(progressArgs);
                    progressDialog.setCancelable(false);
                    //progressDialog.show(getFragmentManager(), DFragment.TAG_PROGRESS);
                    getFragmentManager().beginTransaction().add(progressDialog, DFragment.TAG_PROGRESS).commitAllowingStateLoss();
                } else {
                    progressDialog.updateMessage(message);
                }
                PatchingDialogModel.getInstance().reset();
                break;
            case PatchingDialogModel.STEP_BROWSE:
                if (progressDialog != null) {
                    progressDialog.dismissAllowingStateLoss();
                }
                progressDialog = new DFragment();
                Bundle args = new Bundle();
                args.putInt(DFragment.TITLE, R.string.warning);
                args.putString(DFragment.MESSAGE, message);
                args.putInt(DFragment.ID, DIALOG_BROWSE_PATCH);
                args.putInt(DFragment.ICON, android.R.drawable.ic_dialog_alert);
                args.putInt(DFragment.BUTTONS, 2);
                progressDialog.setArguments(args);
                //dFragment.show(getFragmentManager(), "");
                getFragmentManager().beginTransaction().add(progressDialog, "").commitAllowingStateLoss();
                PatchingDialogModel.getInstance().reset();
                docFile = ""; // Le fichier a probablement été supprimé s'il était conservé temporairement dans le dossier de l'app
                updateViews();
                break;
        }

    }
}
