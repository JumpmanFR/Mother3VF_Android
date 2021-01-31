package fr.mother3vf.mother3vf.patching;

import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import fr.mother3vf.mother3vf.mainactivity.MainActivity;
import fr.mother3vf.mother3vf.mainactivity.PatchingDialogModel;
import fr.mother3vf.mother3vf.R;
import fr.mother3vf.mother3vf.patching.upspatcher.PatchException;
import fr.mother3vf.mother3vf.patching.upspatcher.UPS;


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
public class PatchingTask extends JobIntentService {
    public static final String ACTION_PATCH = "fr.mother3vf.mother3vf.action.ACTION_PATCH";

    public static final int PATCHING_JOB_ID = 10000;

    public static final int REFRESH_DIALOG = 12345;
    public static final String RESULT_STEP = "RESULT_STEP";
    public static final String RESULT_MSG = "RESULT_MSG";
    public static final String RESULT_FILE = "RESULT_FILE";

    public static final String RECEIVER = "RECEIVER";
    public static final String ROM_FILE = "ROM_FILE";
    public static final String PATCH_FILE = "PATCH_FILE";
    public static final String BACKUP = "BACKUP";
    public static final String CHECK_ALREADY_PATCHED = "CHECK_ALREADY_PATCHED";

    private ResultReceiver resultReceiver;

    public PatchingTask() {
        super();
    }

    public class PatchingCallback {
        void sendMessage(int messageId) {
            PatchingTask.this.sendMessage(PatchingDialogModel.STEP_RUNNING, getBaseContext().getString(messageId));
        }
    }

    private final PatchingCallback patchingCallback = new PatchingCallback();

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (ACTION_PATCH.equals(intent.getAction())) {
            resultReceiver = intent.getParcelableExtra(RECEIVER);
            String romFilePath = intent.getStringExtra(ROM_FILE);
            if (romFilePath == null) {
                romFilePath = "";
            }
            boolean backup = intent.getBooleanExtra(BACKUP, true);
            boolean checkAlreadyPatched = intent.getBooleanExtra(CHECK_ALREADY_PATCHED, true);
            String docFile = "";
            if (romFilePath.toLowerCase().endsWith(".zip")) {
                sendMessage(PatchingDialogModel.STEP_RUNNING, getResources().getString(R.string.wait_unzip_rom));
                try {
                    romFilePath = FileUtils.unzip(romFilePath, MainActivity.ROM_FORMATS, "");
                    if ("".equals(romFilePath)) {
                        sendMessage(PatchingDialogModel.STEP_FAILED, getResources().getString(R.string.result_rom_zip_not_found));
                        return;
                    }
                } catch (IOException e) {
                    sendMessage(PatchingDialogModel.STEP_FAILED, getResources().getString(R.string.result_rom_zip_error));
                    return;
                }
            }
            File romFile = new File(romFilePath);
            if (romFile.exists()) {
                // STEP 1: FINDING THE PATCH FILE (+ DOWNLOADING, UNZIPPING, ETC.)
                sendMessage(PatchingDialogModel.STEP_RUNNING, getResources().getString(R.string.wait_prepare));

                PatchFinder patchFinder = new PatchFinder(patchingCallback);
                String patchFilePath;
                if (intent.hasExtra(PATCH_FILE)) {
                    patchFilePath = intent.getStringExtra(PATCH_FILE);
                } else {
                    patchFilePath = patchFinder.findAndSmartSelectInMainFolders(romFile.getParentFile(), getApplicationContext());
                }
                if (patchFilePath == null || "".equals(patchFilePath)) {
                    sendMessage(PatchingDialogModel.STEP_BROWSE, getResources().getString(R.string.pending_patch_not_found));
                    return;
                }
                docFile = patchFinder.findAttachedDoc(patchFilePath);
                if (!romFile.canWrite()) {
                    sendMessage(PatchingDialogModel.STEP_FAILED, getResources().getString(R.string.result_cantwrite), docFile);
                    return;
                }
                // STEP 2: APPLYING THE PATCH
                sendMessage(PatchingDialogModel.STEP_RUNNING, getResources().getString(R.string.wait_patching));
                Log.v(PatchingTask.class.getSimpleName(), "Applying patch");
                //int e = MainActivity.upsPatchRom(romFile, patchFile, romFile + ".temp", 0);

                File patchFile = new File (patchFilePath);
                File outputFile = new File(romFile.getAbsolutePath() + ".temp");

                if (checkAlreadyPatched) {
                    try {
                        UPS.UpsCrc crc = UPS.readUpsCrc(getBaseContext(), patchFile);
                        if (crc.getOutputFileCRC() == org.apache.commons.io.FileUtils.checksumCRC32(romFile)) {
                            sendMessage(PatchingDialogModel.STEP_ALREADY, getResources().getString(R.string.pending_already_exists), patchFilePath);
                            return;
                        }
                    } catch (Exception ignored) {
                    }
                }

                UPS ups = new UPS(getBaseContext(), patchFile, romFile, outputFile);
                try {
                    ups.apply(false);
                } catch (PatchException | IOException e) {
                    e.printStackTrace();
                    File f = new File(romFilePath + ".temp");
                    if (f.exists()) {
                        //noinspection ResultOfMethodCallIgnored
                        f.delete();
                    }

                    String msg = e.getLocalizedMessage();
                    if (e instanceof IOException) {
                        msg = getResources().getString(R.string.patching_error_io) + msg;
                    }
                    sendMessage(PatchingDialogModel.STEP_FAILED, msg, docFile);
                    return;
                }

                boolean successMoveOldRom;
                File baseRom = new File(romFilePath);
                if (backup) {
                    File backupRom = new File(romFilePath + ".original");
                    int i = 1;
                    while (backupRom.exists()) {
                        i++;
                        backupRom = new File(romFilePath + ".original" + i);
                    }
                    successMoveOldRom = baseRom.renameTo(backupRom);
                } else {
                    successMoveOldRom = baseRom.delete();
                }

                boolean successMoveNewRom = false;
                if (successMoveOldRom) {
                    File tempRom = new File(romFilePath + ".temp");
                    successMoveNewRom = tempRom.renameTo(baseRom);
                    Log.v(PatchingTask.class.getSimpleName(), "Done");
                    sendMessage(PatchingDialogModel.STEP_SUCCESS, getResources().getString(R.string.result_success), docFile);
                }

                if (!successMoveOldRom || !successMoveNewRom) {
                    //noinspection ConstantConditions
                    Log.v(PatchingTask.class.getSimpleName(), "Renaming failure: successMoveOldRom=" + successMoveOldRom + "; successMoveNewRom=" + successMoveNewRom);
                    sendMessage(PatchingDialogModel.STEP_SUCCESS, getResources().getString(R.string.result_cant_change_temp_files), docFile);
                }

            } else {
                sendMessage(PatchingDialogModel.STEP_FAILED, getResources().getString(R.string.result_rom_not_found), docFile);
            }
        }
    }

    public void sendMessage(int step, String msg) {
        sendMessage(step, msg, "");
    }

    public void sendMessage(int step, String msg, String paramFile) {
        Bundle bundle = new Bundle();
        bundle.putInt(RESULT_STEP, step);
        bundle.putString(RESULT_MSG, msg);
        bundle.putString(RESULT_FILE, paramFile);
        resultReceiver.send(REFRESH_DIALOG, bundle);
    }
}
