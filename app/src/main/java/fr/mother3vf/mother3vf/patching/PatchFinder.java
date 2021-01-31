package fr.mother3vf.mother3vf.patching;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.mother3vf.mother3vf.R;


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
public class PatchFinder {
    private static ArrayList<String> EXCLUDE_LIST;

    private static final String ZIP_FILES_TO_FIND = "Mother3VF_v(\\d+(\\.\\d+)*)\\.zip";
    private static final String DOWNLOAD_URL = "http://mother3vf.free.fr/dev/release/download_latest.php?app_request=1";

    private static final String PATCH_FILE_TO_FIND = "mother3vf.*\\.ups";
    private static final String DOC_FILE_TO_FIND = "doc_mother3vf.*\\.txt";

    private final PatchingTask.PatchingCallback patchingCallback;

    public PatchFinder(PatchingTask.PatchingCallback pc) {
        this.patchingCallback = pc;
        EXCLUDE_LIST = new ArrayList<>();
        EXCLUDE_LIST.add("/etc");
        EXCLUDE_LIST.add("/proc");
        EXCLUDE_LIST.add("/sys");
        EXCLUDE_LIST.add("/system");
    }

    public String findAndSmartSelectInMainFolders(File romFolder, Context context) {
        // Cleaning the application folder (so that we don’t rely on obsolete versions of the patch)
        FileUtils.clearFiles(context.getFilesDir(), PATCH_FILE_TO_FIND + "|" + ZIP_FILES_TO_FIND + "|" + DOC_FILE_TO_FIND);

        ArrayList<String> files = findInMainFolders(romFolder); // First attempt: searching in memory
        if (files.size() > 0) {
            return bestFile(files, context.getFilesDir().getAbsolutePath());
        }

        String downloaded = downloadFromSite(context.getFilesDir()); // Second attempt: searching on the website
        if (!"".equals(downloaded)) {
            return downloaded;
        }

        patchingCallback.sendMessage(R.string.wait_default_patch);
        return findInAssets(context); // Third attempt: searching in assets
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
            Log.v(PatchFinder.class.getSimpleName(), "Searching patch in folder " + folder.getAbsolutePath());
            ArrayList<String> result = findInFolder(folder);
            if (!result.isEmpty()) {
                return result;
            }
        }
        return new ArrayList<>();
    }

    public ArrayList<String> findInFolder(File folder) {
        ArrayList<String> result = new ArrayList<>();
        if (folder.exists() && folder.isDirectory() && !EXCLUDE_LIST.contains(folder.getAbsolutePath())) { // Is that indeed a folder
            File[] files = folder.listFiles();
            if (files == null) {
                return result;
            }
            for (File file : files) { // Parsing files in folder
                if (file.isDirectory()) { // If the file is a folder => recursive search in subdirectories
                    result.addAll(findInFolder(file));
                } else if (file.getName().matches(PATCH_FILE_TO_FIND) || file.getName().matches(ZIP_FILES_TO_FIND)) { // If file matches the expected name => return it
                    result.add(file.getAbsolutePath());
                }
            }
        }
        return result;
    }

    /**
     * Searches the patch is the assets and extracts it from there
     * @param context the context, for various needs
     * @return the patch as an absolute path, "" if not found
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
            if (e != null) {
                Log.e("tag", e.getMessage());
            }
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

    public String findAttachedDoc(String patchFile) {
        File parentFolder = new File(patchFile).getParentFile();
        if (!"".equals(patchFile) && parentFolder != null) {
            File[] list = parentFolder.listFiles();
            if (list != null) {
                for (File file : list) {
                    if (file.getName().matches(DOC_FILE_TO_FIND)) {
                        return file.getAbsolutePath();
                    }
                }
            }
        }
        return "";
    }

    /**
     * Downloads the archive from the site and extracts the patch from it; also broadcasts progress messages for the UI (yeah, it’s a bit dirty)
     * @param destination the folder where we want to extract the archive
     * @return the extracted patch, as an absolute path
     */
    private String downloadFromSite(File destination) {
        patchingCallback.sendMessage(R.string.wait_download);
        String downloadedZipFile = FileUtils.downloadFile(DOWNLOAD_URL, destination);
        if ("".equals(downloadedZipFile)) {
            return "";
        }
        try {
            patchingCallback.sendMessage(R.string.wait_unzip_patch);
            return FileUtils.unzip(downloadedZipFile, PATCH_FILE_TO_FIND, DOC_FILE_TO_FIND);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Determines the best occurrence among the files (.ups or .zip) found
     * @param files as an ArrayList of absolute paths, need to match either PATCH_FILE_TO_FIND or ZIP_FILES_TO_FIND
     * @param appFolder the app folder as a destination folder in case we need to unzip
     * @return the best file’s name
     */
    private String bestFile(ArrayList<String> files, String appFolder) {
        String bestFile = "";
        int bestFilePathDepth = 0;
        for (String file : files) {
            int pathDepth = getPathDepth(file);
            if (file.matches(".*" + PATCH_FILE_TO_FIND) && (bestFilePathDepth == 0 || pathDepth < bestFilePathDepth)) {
                bestFile = file;
                bestFilePathDepth = pathDepth;
            }
        }
        if (bestFilePathDepth != 0) { // at least one patch file (PATCH_FILE_TO_FIND) has been found
            return bestFile;
        } else { // only zip files (ZIP_FILES_TO_FIND) were found
            Collections.sort(files, new Comparator<String>() {
                @Override
                public int compare(String file1, String file2) {
                    return versionCompare(file2, file1);
                }
            });
            try {
                patchingCallback.sendMessage(R.string.wait_unzip_patch);
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

    /**
     * Tells which one of two version numbers is the higher
     * @param str1 version number
     * @param str2 version number
     * @return 1 or -1 if str1 or str2 is higher respectively, 0 if they are equal
     */
    private int versionCompare(String str1, String str2) {
        Pattern pattern = Pattern.compile(".*" + ZIP_FILES_TO_FIND);
        Matcher matcher = pattern.matcher(str1);
        if (matcher.find()) {
            str1 = matcher.group(1);
        }
        matcher = pattern.matcher(str2);
        if (matcher.find()) {
            str2 = matcher.group(1);
        }

        if (str1 == null && str2 == null) {
            return 0;
        } else if (str1 == null) {
            return -1;
        } else if (str2 == null) {
            return 1;
        }

        String[] values1 = str1.split("\\.");
        String[] values2 = str2.split("\\.");
        int i = 0;
        // Set index to first non-equal ordinal or length of shortest version string
        while (i < values1.length && i < values2.length && values1[i].equals(values2[i])) {
            i++;
        }
        // Compare first non-equal ordinal number
        if (i < values1.length && i < values2.length) {
            int diff = Integer.valueOf(values1[i]).compareTo(Integer.valueOf(values2[i]));
            return Integer.signum(diff);
        }
        // The strings are equal or one string is a substring of the other
        // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
        return Integer.signum(values1.length - values2.length);
    }
}
