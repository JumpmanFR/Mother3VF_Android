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
 * xperia64 - port to Android support
 * JumpmanFR - adaptation for MOTHER3VF
 ******************************************************************************/
package fr.mother3vf.mother3vf;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import fr.mother3vf.mother3vf.patcher.UPS;


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
            String msg;
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
                PatchFinder.getInstance().setContext(getBaseContext());
                String patchFilePath;
                if (intent.hasExtra(PATCH_FILE)) {
                    patchFilePath = intent.getStringExtra(PATCH_FILE);
                } else {
                    patchFilePath = PatchFinder.getInstance().findAndSmartSelectInMainFolders(romFile.getParentFile(), getApplicationContext());
                }
                if (patchFilePath == null || "".equals(patchFilePath)) {
                    sendMessage(PatchingDialogModel.STEP_BROWSE, getResources().getString(R.string.patch_not_found), getBaseContext());
                    return;
                }
                docFile = PatchFinder.getInstance().findJointDoc(patchFilePath);
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
                    } catch (Exception e) {
                    }
                }

                UPS ups = new UPS(getBaseContext(), patchFile, romFile, outputFile);
                try {
                    ups.apply(false);
                } catch (Exception e) {
                    e.printStackTrace();
                    File f = new File(romFilePath + ".temp");
                    if (f.exists()) {
                        f.delete();
                    }
                    sendMessage(PatchingDialogModel.STEP_FAILED, e.getLocalizedMessage(), docFile, getBaseContext());
                    return;
                }

                File oldrom = new File(romFilePath);
                File bkrom = new File(romFilePath + ".original");
                oldrom.renameTo(bkrom);
                File newrom = new File(romFilePath + ".temp");
                newrom.renameTo(oldrom);
                if (!backup) {
                    File f = new File(romFilePath + ".original");
                    if (f.exists()) {
                        f.delete();
                    }
                }

                Log.v(PatchingTask.class.getSimpleName(), "Terminé");
                msg = getResources().getString(R.string.success);
                sendMessage(PatchingDialogModel.STEP_SUCCESS, msg, docFile, getBaseContext());

            } else {
                msg = getResources().getString(R.string.rom_not_found);
                sendMessage(PatchingDialogModel.STEP_FAILED, msg, docFile, getBaseContext());
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

    // Error message based on error code.
    private String parseError(int e) {
        switch (e) {
            case -1:
                return getResources().getString(R.string.upsNegativeOne);
            case -2:
                return getResources().getString(R.string.upsNegativeTwo);
            case -3:
                return getResources().getString(R.string.upsNegativeThree);
            case -4:
                return getResources().getString(R.string.upsNegativeFour);
            case -5:
                return getResources().getString(R.string.upsNegativeFive);
            case -6:
                return getResources().getString(R.string.upsNegativeSix);
            case -7:
                return getResources().getString(R.string.upsNegativeSeven);
            default:
                return getResources().getString(R.string.upsDefault) + e;
        }
    }
}
