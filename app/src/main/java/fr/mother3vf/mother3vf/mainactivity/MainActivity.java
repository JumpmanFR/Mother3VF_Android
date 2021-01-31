package fr.mother3vf.mother3vf.mainactivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import fr.mother3vf.mother3vf.doc.DocActivity;
import fr.mother3vf.mother3vf.R;
import fr.mother3vf.mother3vf.browser.FileBrowserActivity;
import fr.mother3vf.mother3vf.databinding.ActivityMainBinding;
import fr.mother3vf.mother3vf.patching.PatchingResultReceiver;
import fr.mother3vf.mother3vf.patching.PatchingTask;

/*******************************************************************************
 * This file is part of MOTHER 3 VF for Android (2017, JumpmanFR)
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * <p>
 * Developed by JumpmanFR
 * Inspired from Paul Kratt’s MultiPatch app for macOS
 ******************************************************************************/
public class MainActivity extends FragmentActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, PatchingResultReceiver.Receiver {

    public static final String ROM_FORMATS = ".*\\.(gba|agb|bin|jgc|rom)";
    public static final String ROM_FORMATS_EXTENDED = ".*\\.(gba|agb|bin|jgc|rom|rom\\.original\\d+)";

    private static final String SAVED_CURRENT_FOLDER = "CURRENT_FOLDER";
    private static final String SAVED_ROM_FILE = "ROM_FILE";
    private static final String SAVED_PATCH_FILE = "PATCH_FILE";
    private static final String SAVED_DOC_FILE = "DOC_FILE";
    private static final String SAVED_DO_BACKUP_ROM = "DO_BACKUP_ROM";
    private static final String SAVED_DIALOG_MODEL = "DIALOG_MODEL";
    private static final String SAVED_PATCHING_RECEIVER = "SAVED_PATCHING_RECEIVER";

    public static final int DIALOG_PERMISSIONS = 101;
    public static final int DIALOG_OVERWRITE = 102;
    public static final int DIALOG_ERROR_PERMISSIONS = 103;
    public static final int DIALOG_BROWSE_PATCH = 104;
    public static final int DIALOG_CONFIRM_PATCH = 105;
    public static final int DIALOG_PATCH_FAILED = 106;
    public static final int DIALOG_PATCH_SUCCESS = 107;

    private static final int PERMISSION_REQUEST = 178;
    private static final int NUM_PERMISSIONS = 2;

    private ActivityMainBinding views;

    private DFragment progressDialog;

    private String currentFolder = "";
    private String romFile = "";
    private String patchFile = "";
    private String docFile = "";
    private boolean doBackupRom = false;
    private PatchingDialogModel dialogModel;
    private PatchingResultReceiver patchingResultReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        views = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(views.getRoot());

        setActionBar(views.toolbar);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.main_view_title);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setIcon(R.drawable.ic_actionbar);
        }

        views.romButton.setOnClickListener(this);
        views.applyPatch.setOnClickListener(this);
        views.website.setOnClickListener(this);
        views.openDoc.setOnClickListener(this);
        views.about.setOnClickListener(this);
        views.backupCheckbox.setOnCheckedChangeListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions();
        }

        if (savedInstanceState != null) {
            romFile = savedInstanceState.getString(SAVED_ROM_FILE);
            patchFile = savedInstanceState.getString(SAVED_PATCH_FILE);
            currentFolder = savedInstanceState.getString(SAVED_CURRENT_FOLDER);
            docFile = savedInstanceState.getString(SAVED_DOC_FILE);
            doBackupRom = savedInstanceState.getBoolean(SAVED_DO_BACKUP_ROM);
            dialogModel = savedInstanceState.getParcelable(SAVED_DIALOG_MODEL);
            patchingResultReceiver = savedInstanceState.getParcelable(SAVED_PATCHING_RECEIVER);
        } else {
            dialogModel = new PatchingDialogModel();
            patchingResultReceiver = new PatchingResultReceiver(new Handler());
        }
        patchingResultReceiver.setReceiver(this);
        updateViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshPatchingDialog();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //unregisterReceiver(patchingTaskReceiver);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        state.putString(SAVED_ROM_FILE, romFile);
        state.putString(SAVED_PATCH_FILE, patchFile);
        state.putString(SAVED_CURRENT_FOLDER, currentFolder);
        state.putString(SAVED_DOC_FILE, docFile);
        state.putBoolean(SAVED_DO_BACKUP_ROM, doBackupRom);
        state.putParcelable(SAVED_DIALOG_MODEL, dialogModel);
        state.putParcelable(SAVED_PATCHING_RECEIVER, patchingResultReceiver);
    }

    @Override
    public void onAttachFragment(@NonNull Fragment fragment) {
        super.onAttachFragment(fragment);
        progressDialog = (DFragment) fragment;
    }

    private void requestPermissions() {
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
                dFragment.show(getSupportFragmentManager(), "");

            } else {
                actuallyRequestPermissions();
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {// If request is cancelled, the result arrays are empty
            boolean checkPerms = true;
            if (permissions.length != NUM_PERMISSIONS || grantResults.length != NUM_PERMISSIONS) {
                checkPerms = false;
            }

            for (int i = 0; i < grantResults.length && checkPerms; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    checkPerms = false;
                    break;
                }
            }
            if (!checkPerms) {
                showFatalPermissionsError();
            }
        }
    }

    private void patch(boolean checkAlreadyPatched) {
        Intent i = new Intent(this, PatchingTask.class);
        i.setAction(PatchingTask.ACTION_PATCH);
        i.putExtra(PatchingTask.ROM_FILE, romFile);
        if (patchFile != null && !"".equals(patchFile)) {
            i.putExtra(PatchingTask.PATCH_FILE, patchFile);
        }
        i.putExtra(PatchingTask.CHECK_ALREADY_PATCHED, checkAlreadyPatched);
        i.putExtra(PatchingTask.BACKUP, doBackupRom);
        i.putExtra(PatchingTask.RECEIVER, patchingResultReceiver);
        PatchingTask.enqueueWork(this, PatchingTask.class, PatchingTask.PATCHING_JOB_ID, i);
    }

    /**
     * Processes result from FileBrowserActivity
     * @param requestCode identifies the initial request when the activity was requested
     * @param resultCode user action: RESULT_OK or RESULT_CANCELED
     * @param data detailed user choices
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                currentFolder = extras.getString((FileBrowserActivity.FOLDER));
                switch (requestCode) {
                    case FileBrowserActivity.BROWSE_FOR_ROM:
                        romFile = extras.getString(FileBrowserActivity.FILE);
                        break;
                    case FileBrowserActivity.BROWSE_FOR_PATCH:
                        patchFile = extras.getString(FileBrowserActivity.FILE);
                        patch(true);
                        break;
                }
            }
            updateViews();
        } else if (resultCode == RESULT_CANCELED && requestCode == FileBrowserActivity.BROWSE_FOR_PATCH) {
            showPatchingCanceled();
        }
    }

    /**
     * Processes result from PatchingTask
     * @param resultCode identifies the task
     * @param resultData contains the result from the task
     */
    @Override
    public void onPatchingResult(int resultCode, Bundle resultData) {
        if (resultCode == PatchingTask.REFRESH_DIALOG) {
            dialogModel.set(resultData.getInt(PatchingTask.RESULT_STEP),
                    resultData.getString(PatchingTask.RESULT_MSG,
                            resultData.getString(PatchingTask.RESULT_FILE)));
            refreshPatchingDialog();
        }
    }

    private void updateViews() {
        TextView romText = views.romText;
        boolean hasRom = !"".equals(romFile);
        views.applyPatch.setEnabled(hasRom);
        views.applyPatch.setFocusable(hasRom);
        boolean hasDoc = !"".equals(docFile);
        //hasDoc = true;
        views.openDoc.setVisibility(hasDoc ? View.VISIBLE : View.INVISIBLE);
        views.website.setVisibility(hasDoc ? View.INVISIBLE : View.VISIBLE);
        if (romFile.lastIndexOf("/") > -1) {
            romText.setText(romFile.substring(romFile.lastIndexOf("/") + 1));
        } else {
            romText.setText(romFile);
        }
        views.backupCheckbox.setChecked(doBackupRom);
        romText.postInvalidate();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.romButton) {
            Intent intent = new Intent(this, FileBrowserActivity.class);
            intent.putExtra(FileBrowserActivity.DISPLAY_FILTER, ROM_FORMATS_EXTENDED);
            intent.putExtra(FileBrowserActivity.DISPLAY_FILTER_NAME, getBaseContext().getString(R.string.only_show_roms));
            intent.putExtra(FileBrowserActivity.TARGET_FILTER, ROM_FORMATS);
            intent.putExtra(FileBrowserActivity.TARGET_ICON, "\uD83C\uDFAE"); // Game controller emoji
            if (!"".equals(currentFolder)) {
                intent.putExtra(FileBrowserActivity.FOLDER, currentFolder);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivityForResult(intent, FileBrowserActivity.BROWSE_FOR_ROM);
            /*Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("file/*");
            startActivityForResult(intent, FileBrowserActivity.BROWSE_FOR_ROM);*/
        } else if (view.getId() == R.id.applyPatch) {
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
                dFragment.show(getSupportFragmentManager(), "");
            } else {
                patch(true);
            }
        } else if (view.getId() == R.id.website) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse("http://mother3vf.free.fr"));
            startActivity(i);
        } else if (view.getId() == R.id.open_doc) {
            Intent docIntent = new Intent(this, DocActivity.class);
            docIntent.putExtra(DocActivity.DOC_FILE, docFile);
            docIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(docIntent);
        } else if (view.getId() == R.id.about) {
            DialogFragment dFragment = new DFragment();
            Bundle args = new Bundle();
            args.putString(DFragment.MESSAGE, getResources().getString(R.string.about));
            args.putInt(DFragment.BUTTONS, 1);
            dFragment.setArguments(args);
            dFragment.show(getSupportFragmentManager(), "");
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton.getId() == R.id.backupCheckbox) {
            doBackupRom = b;
        }
    }

    /**
     * Processes the user response on a dialog
     * @param dialogType what the dialog was
     * @param response what the user responded
     */
    protected void onDialogResponse(int dialogType, boolean response) {
        switch(dialogType) {
            case DIALOG_CONFIRM_PATCH:  // User has responded he still wanted to apply the patch even though something unexpected had occurred
                if (response) {
                    patch(true);
                } else {
                    showPatchingCanceled();
                }
                break;
            case DIALOG_OVERWRITE: // User has responded whether he wanted to "un-patch" the already patched ROM
                if (response) {
                    patch(false);
                } else {
                    patchFile = "";
                    showPatchingCanceled();
                }
                break;
            case DIALOG_PERMISSIONS: // User has responded to permission request
                if (response) {
                    actuallyRequestPermissions();
                } else {
                    showFatalPermissionsError();
                }
                break;
            case DIALOG_ERROR_PERMISSIONS: // User has closed the permission error alert
                finish();
                break;
            case DIALOG_BROWSE_PATCH: // User has responded whether he wanted to browse for the patching file after the app could not find it
                if (response) {
                    Intent intent = new Intent(this, FileBrowserActivity.class);
                    intent.putExtra(FileBrowserActivity.DISPLAY_FILTER, ".*\\.ups");
                    intent.putExtra(FileBrowserActivity.DISPLAY_FILTER_NAME, getBaseContext().getString(R.string.only_show_patches));
                    intent.putExtra(FileBrowserActivity.TARGET_FILTER, "mother3vf.*\\.ups");
                    intent.putExtra(FileBrowserActivity.TARGET_ICON, "\uD83C\uDDEB\uD83C\uDDF7"); // French flag emoji
                    if (!"".equals(currentFolder)) {
                        intent.putExtra(FileBrowserActivity.FOLDER, currentFolder);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivityForResult(intent, FileBrowserActivity.BROWSE_FOR_PATCH);
                } else {
                    showPatchingCanceled();
                }
                break;
            case DIALOG_PATCH_FAILED: // User has closed the alert after a failed patching process
                dialogModel.reset();
                patchFile = "";
                break;
            case DIALOG_PATCH_SUCCESS: // User has closed the alert after a successful patching process
                dialogModel.reset();
                if (!"".equals(docFile)) {
                    Intent docIntent = new Intent(this, DocActivity.class);
                    docIntent.putExtra(DocActivity.DOC_FILE, docFile);
                    docIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(docIntent);
                }
                romFile = "";
                patchFile = "";
                updateViews();
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
        dFragment.show(getSupportFragmentManager(), "");
    }

    private void showPatchingCanceled() {
        dialogModel.reset();
        DialogFragment dFragment = new DFragment();
        Bundle args = new Bundle();
        args.putString(DFragment.MESSAGE, getResources().getString(R.string.nopatch));
        args.putInt(DFragment.BUTTONS, 1);
        dFragment.setArguments(args);
        dFragment.show(getSupportFragmentManager(), "");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        views.romButton.setOnClickListener(null);
        views.applyPatch.setOnClickListener(null);
        views.website.setOnClickListener(null);
        views.openDoc.setOnClickListener(null);
        views.about.setOnClickListener(null);
        views.backupCheckbox.setOnCheckedChangeListener(null);
        views = null;
    }

    /**
     * Generates message dialog depending on what happened during the patching process
     */
    private void refreshPatchingDialog() {
        PatchingDialogModel patchingDialogModel = dialogModel;
        String message = patchingDialogModel.getMessage();
        switch (patchingDialogModel.getResultCode()) {
            case PatchingDialogModel.STEP_FAILED: // The patching process failed
            case PatchingDialogModel.STEP_SUCCESS: // The patching process succeeded
                if (progressDialog != null) {
                    progressDialog.dismissAllowingStateLoss();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ((Vibrator)getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
                }
                progressDialog = new DFragment();
                Bundle alertArgs = new Bundle();
                if (patchingDialogModel.getResultCode() == PatchingDialogModel.STEP_SUCCESS) {
                    alertArgs.putInt(DFragment.ID, DIALOG_PATCH_SUCCESS);
                } else {
                    alertArgs.putInt(DFragment.ID, DIALOG_PATCH_FAILED);
                }
                alertArgs.putInt(DFragment.TITLE, R.string.app_name_dialogs);
                alertArgs.putString(DFragment.MESSAGE, message);
                alertArgs.putInt(DFragment.BUTTONS, 1);
                progressDialog.setArguments(alertArgs);
                progressDialog.setCancelable(false);
                //alertFragment.show(getFragmentManager(), "");
                docFile = patchingDialogModel.getFileParam();
                updateViews();
                getSupportFragmentManager().beginTransaction().add(progressDialog, "").commitAllowingStateLoss();
                patchingDialogModel.reset();
                break;
            case PatchingDialogModel.STEP_RUNNING: // The patching process is running
                if (progressDialog == null || progressDialog.isDismissed()) {
                    progressDialog = new DFragment();
                    Bundle progressArgs = new Bundle();
                    progressArgs.putInt(DFragment.TITLE, R.string.wait);
                    progressArgs.putString(DFragment.MESSAGE, message);
                    progressArgs.putBoolean(DFragment.PROGRESS, true);
                    progressDialog.setArguments(progressArgs);
                    progressDialog.setCancelable(false);
                    getSupportFragmentManager().beginTransaction().add(progressDialog, DFragment.TAG_PROGRESS).commitAllowingStateLoss();
                } else {
                    progressDialog.updateMessage(message);
                }
                patchingDialogModel.reset();
                break;
            case PatchingDialogModel.STEP_ALREADY: // The patching process has detected the ROM had already been patched
                if (progressDialog != null) {
                    progressDialog.dismissAllowingStateLoss();
                }
                progressDialog = new DFragment();
                Bundle alreadyArgs = new Bundle();
                alreadyArgs.putInt(DFragment.TITLE, R.string.warning);
                alreadyArgs.putString(DFragment.MESSAGE, message);
                alreadyArgs.putInt(DFragment.ID, DIALOG_OVERWRITE);
                alreadyArgs.putInt(DFragment.ICON, android.R.drawable.ic_dialog_alert);
                alreadyArgs.putInt(DFragment.BUTTONS, 2);
                progressDialog.setArguments(alreadyArgs);
                getSupportFragmentManager().beginTransaction().add(progressDialog, "").commitAllowingStateLoss();
                dialogModel.reset();
                docFile = "";
                patchFile = patchingDialogModel.getFileParam();
                updateViews();
                break;
            case PatchingDialogModel.STEP_BROWSE: // The patching process was interrupted because the patching file was nowhere to be found
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
                getSupportFragmentManager().beginTransaction().add(progressDialog, "").commitAllowingStateLoss();
                dialogModel.reset();
                docFile = "";
                updateViews();
                break;
        }

    }
}
