package fr.mother3vf.mother3vf;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import fr.mother3vf.mother3vf.patcher.PatchException;
import fr.mother3vf.mother3vf.patcher.UPS;


/*******************************************************************************
 * This file is part of MOTHER 3 VF for Android (2017, JumpmanFR)
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * <p>
 * Contributors:
 * Paul Kratt - MultiPatch app for macOS
 * JumpmanFR - adaptation for MOTHER 3 VF
 ******************************************************************************/
public class PatchingTask extends IntentService {
    public static final String ACTION_PATCH = "fr.mother3vf.mother3vf.action.ACTION_PATCH";

    public static final String REFRESH_DIALOG = "fr.mother3vf.mother3vf.action.REFRESH_PATCHING_DIALOG";

    public static final String ROM_FILE = "ROM_FILE";
    public static final String PATCH_FILE = "PATCH_FILE";
    public static final String BACKUP = "BACKUP";
    public static final String CHECK_ALREADY_PATCHED = "CHECK_ALREADY_PATCHED";

    public PatchingTask() {
        super("PatchingTask");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (ACTION_PATCH.equals(intent.getAction())) {
            String romFilePath = intent.getStringExtra(ROM_FILE);
            boolean backup = intent.getBooleanExtra(BACKUP, true);
            boolean checkAlreadyPatched = intent.getBooleanExtra(CHECK_ALREADY_PATCHED, true);
            String docFile = "";
            if (romFilePath.toLowerCase().endsWith(".zip")) {
                sendMessage(PatchingDialogModel.STEP_RUNNING, getResources().getString(R.string.wait_unzip_rom), getBaseContext());
                try {
                    romFilePath = FileUtils.unzip(romFilePath, MainActivity.ROM_FORMATS, "");
                    if ("".equals(romFilePath)) {
                        sendMessage(PatchingDialogModel.STEP_FAILED, getResources().getString(R.string.rom_zip_not_found), getBaseContext());
                        return;
                    }
                } catch (IOException e) {
                    sendMessage(PatchingDialogModel.STEP_FAILED, getResources().getString(R.string.rom_zip_error), getBaseContext());
                    return;
                }
            }
            File romFile = new File(romFilePath);
            if (romFile.exists()) {
                // ÉTAPE 1 : RECHERCHE DU PATCH (+ TÉLÉCHARGEMENT, DÉCOMPRESSION, ETC.)
                sendMessage(PatchingDialogModel.STEP_RUNNING, getResources().getString(R.string.wait_prepare), getBaseContext());

                PatchFinder patchFinder = new PatchFinder(getBaseContext());
                String patchFilePath;
                if (intent.hasExtra(PATCH_FILE)) {
                    patchFilePath = intent.getStringExtra(PATCH_FILE);
                } else {
                    patchFilePath = patchFinder.findAndSmartSelectInMainFolders(romFile.getParentFile(), getApplicationContext());
                }
                if (patchFilePath == null || "".equals(patchFilePath)) {
                    sendMessage(PatchingDialogModel.STEP_BROWSE, getResources().getString(R.string.patch_not_found), getBaseContext());
                    return;
                }
                docFile = patchFinder.findJointDoc(patchFilePath);
                if (!romFile.canWrite()) {
                    sendMessage(PatchingDialogModel.STEP_FAILED, getResources().getString(R.string.cantwrite), docFile, getBaseContext());
                    return;
                }
                // ÉTAPE 2 : APPLICATION DU PATCH
                sendMessage(PatchingDialogModel.STEP_RUNNING, getResources().getString(R.string.wait_patching), getBaseContext());
                Log.v(PatchingTask.class.getSimpleName(), "Application du patch");
                //int e = MainActivity.upsPatchRom(romFile, patchFile, romFile + ".temp", 0);

                File patchFile = new File (patchFilePath);
                File outputFile = new File(romFile.getAbsolutePath() + ".temp");

                if (checkAlreadyPatched) {
                    try {
                        UPS.UpsCrc crc = UPS.readUpsCrc(getBaseContext(), patchFile);
                        if (crc.getOutputFileCRC() == org.apache.commons.io.FileUtils.checksumCRC32(romFile)) {
                            sendMessage(PatchingDialogModel.STEP_ALREADY, getResources().getString(R.string.already_exists), getBaseContext());
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
                    sendMessage(PatchingDialogModel.STEP_FAILED, msg, docFile, getBaseContext());
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
                    Log.v(PatchingTask.class.getSimpleName(), "Terminé");
                    sendMessage(PatchingDialogModel.STEP_SUCCESS, getResources().getString(R.string.success), docFile, getBaseContext());
                }

                if (!successMoveOldRom || !successMoveNewRom) {
                    Log.v(PatchingTask.class.getSimpleName(), "Échec renommages : successMoveOldRom=" + successMoveOldRom + "; successMoveNewRom=" + successMoveNewRom);
                    sendMessage(PatchingDialogModel.STEP_SUCCESS, getResources().getString(R.string.cant_change_temp_files), docFile, getBaseContext());
                }

            } else {
                sendMessage(PatchingDialogModel.STEP_FAILED, getResources().getString(R.string.rom_not_found), docFile, getBaseContext());
            }
        }
    }

    public static void sendMessage(int step, String msg, Context context) {
        sendMessage(step, msg, "", context);
    }

    public static void sendMessage(int step, String msg, String docFile, Context context) {
        PatchingDialogModel.getInstance().set(step, msg, docFile);
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(REFRESH_DIALOG);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        context.sendBroadcast(broadcastIntent);
    }
}
