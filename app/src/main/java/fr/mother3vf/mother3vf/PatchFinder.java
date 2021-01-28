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

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PatchFinder {
    private static ArrayList<String> EXCLUDE_LIST;

    private static final String ZIPFILES_TO_FIND = "Mother3VF_v(\\d+(\\.\\d+)*)\\.zip";
    private static final String DOWNLOAD_URL = "http://mother3vf.free.fr/dev/release/download_latest.php?app_request=1";

    private static final String PATCH_FILE_TO_FIND = "mother3vf.*\\.ups";
    private static final String DOC_FILE_TO_FIND = "doc_mother3vf.*\\.txt";

    private static PatchFinder instance;

    private Context context;

    public static PatchFinder getInstance() {
        if (instance == null) {
            instance = new PatchFinder();
        }
        return instance;
    }

    private PatchFinder() {
        EXCLUDE_LIST = new ArrayList<String>();
        EXCLUDE_LIST.add("/etc");
        EXCLUDE_LIST.add("/proc");
        EXCLUDE_LIST.add("/sys");
        EXCLUDE_LIST.add("/system");
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public String findAndSmartSelectInMainFolders(File romFolder, Context context) {
        // On nettoie le dossier de l’application (pour ne pas se reposer sur des versions obsolètes du patch)
        FileUtils.clearFiles(context.getFilesDir(), PATCH_FILE_TO_FIND + "|" + ZIPFILES_TO_FIND + "|" + DOC_FILE_TO_FIND);

        ArrayList<String> files = findInMainFolders(romFolder); // Première tentative : on recherche dans la mémoire
        if (files.size() > 0) {
            return bestFile(files, context.getFilesDir().getAbsolutePath());
        }

        String downloaded = downloadFromSite(context.getFilesDir()); // Deuxième tentative : on recherche sur le site
        if (!"".equals(downloaded)) {
            return downloaded;
        }

        PatchingTask.sendMessage(PatchingDialogModel.STEP_RUNNING, context.getString(R.string.wait_default_patch), context);
        return findInAssets(context); // Troisième tentative : on récupère le fichier dans les assets
    }

    public ArrayList<String> findInMainFolders(File romFolder) {
        File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File documentsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File externalStorageFolder = Environment.getExternalStorageDirectory();
        File rootFolder = new File("/");
        return findInFolders(romFolder, downloadsFolder, documentsFolder, externalStorageFolder, rootFolder);
    }

    public ArrayList<String> findInFolders(File... folders) {
        for (File folder : folders) {
            Log.v(PatchFinder.class.getSimpleName(), "Recherche du patch dans le dossier " + folder.getAbsolutePath());
            ArrayList<String> result = findInFolder(folder);
            if (!result.isEmpty()) {
                return result;
            }
        }
        return new ArrayList<>();
    }

    public ArrayList<String> findInFolder(File folder) {
        ArrayList<String> result = new ArrayList<>();
        if (folder.exists() && folder.isDirectory() && !EXCLUDE_LIST.contains(folder.getAbsolutePath())) { // est-ce bien un dossier
            File[] files = folder.listFiles();
            if (files == null) {
                return result;
            }
            for (File file : files) { // on parcourt tous les fichiers
                if (file.isDirectory()) { // si le fichier est un dossier => recherche récursive dans les sous-dossiers
                    result.addAll(findInFolder(file));
                } else if (file.getName().matches(PATCH_FILE_TO_FIND) || file.getName().matches(ZIPFILES_TO_FIND)) { // si le fichier correspond au nom attendu => on le renvoie
                    result.add(file.getAbsolutePath());
                }
            }
        }
        return result;
    }

    /**
     * Recherche et extrait le fichier du dossier des assets
     * @param context
     * @return
     */
    public String findInAssets(Context context) {
        AssetManager assetManager = context.getAssets();

        InputStream in = null;
        OutputStream out = null;

        try {
            String[] list = assetManager.list("");
            String fileToReturn = "";
            for (String fileStr : list) {
                if (fileStr.matches(PATCH_FILE_TO_FIND) || fileStr.matches(DOC_FILE_TO_FIND)) {
                    in = assetManager.open(fileStr);
                    File newFile = new File(context.getFilesDir(), fileStr);
                    out = new FileOutputStream(newFile);

                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = in.read(buffer)) > 0) {
                        out.write(buffer, 0, read);
                    }
                    in.close();
                    out.flush();
                    out.close();
                    if (fileStr.matches(PATCH_FILE_TO_FIND)) {
                        fileToReturn = newFile.getAbsolutePath();
                    }
                }
            }
            return fileToReturn;
        } catch (Exception e) {
            Log.e("tag", e.getMessage());
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    public String findJointDoc(String patchFile) {
        if ("".equals(patchFile)) {
            return "";
        }
        File[] list = new File(patchFile).getParentFile().listFiles();
        for (File file : list) {
            if (file.getName().matches(DOC_FILE_TO_FIND)) {
                return file.getAbsolutePath();
            }
        }
        return "";
    }

    /**
     * Télécharge l’archive et en extrait le fichier recherché
     * @param destination
     * @return
     */
    private String downloadFromSite(File destination) {
        PatchingTask.sendMessage(PatchingDialogModel.STEP_RUNNING, context.getString(R.string.wait_download), context);
        String downloadedZipFile = FileUtils.downloadFile(DOWNLOAD_URL, destination);
        if ("".equals(downloadedZipFile)) {
            return "";
        }
        try {
            PatchingTask.sendMessage(PatchingDialogModel.STEP_RUNNING, context.getString(R.string.wait_unzip_patch), context);
            return FileUtils.unzip(downloadedZipFile, PATCH_FILE_TO_FIND, DOC_FILE_TO_FIND);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Détermine la meilleure occurrence parmi les fichiers trouvés, incluant celles se trouvant dans le zip connu
     * @param files
     * @param appFolder
     * @return
     */
    private String bestFile(ArrayList<String> files, String appFolder) {
        String bestFile = "";
        int bestFilePathDepth = 0;
        for (String file : files) { // on parcourt tous les fichiers et on trie ceux de type patch pour garder celui le moins profond dans l'arborescence
            int pathDepth = getPathDepth(file);
            if (file.matches(".*" + PATCH_FILE_TO_FIND) && (bestFilePathDepth == 0 || pathDepth < bestFilePathDepth)) {
                bestFile = file;
                bestFilePathDepth = pathDepth;
            }
        }
        if (bestFilePathDepth != 0) { // on a trouvé au moins un fichier de type recherché
            return bestFile;
        } else { // on n'a trouvé que des fichiers de type zip
            Collections.sort(files, new Comparator<String>() {
                @Override
                public int compare(String file1, String file2) {
                    return versionCompare(file2, file1);
                }
            });
            try {
                PatchingTask.sendMessage(PatchingDialogModel.STEP_RUNNING, context.getString(R.string.wait_unzip_patch), context);
                return FileUtils.unzip(files.get(0), PATCH_FILE_TO_FIND, DOC_FILE_TO_FIND, appFolder);
            } catch (IOException e) {
                e.printStackTrace();
                return "";
            }
        }
    }

    private int getPathDepth(String file) {
        int count = 0;
        for (int i = 0; i < file.length(); i++) {
            if (file.charAt(i)=='/') {
                count++;
            }
        }
        return count;
    }

    private int versionCompare(String str1, String str2) {
        Pattern pattern = Pattern.compile(".*" + ZIPFILES_TO_FIND);
        Matcher matcher = pattern.matcher(str1);
        if (matcher.find()) {
            str1 = matcher.group(1);
        }
        matcher = pattern.matcher(str2);
        if (matcher.find()) {
            str2 = matcher.group(1);
        }

        String[] vals1 = str1.split("\\.");
        String[] vals2 = str2.split("\\.");
        int i = 0;
        // set index to first non-equal ordinal or length of shortest version string
        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
            i++;
        }
        // compare first non-equal ordinal number
        if (i < vals1.length && i < vals2.length) {
            int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
            return Integer.signum(diff);
        }
        // the strings are equal or one string is a substring of the other
        // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
        return Integer.signum(vals1.length - vals2.length);
    }
}