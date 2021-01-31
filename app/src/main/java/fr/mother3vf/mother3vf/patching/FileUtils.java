package fr.mother3vf.mother3vf.patching;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import androidx.annotation.NonNull;


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
public class FileUtils {

    /**
     * Unzips specified files in an archive and places them in the same directory
     * @param fileString the archive, as an absolute path
     * @param wantedFile the file we want, as an absolute path
     * @param bonusFile another file we want, as an absolute path
     * @return the wanted file, as an absolute path
     * @throws IOException if file accesses fail
     */
    @NonNull
    public static String unzip(String fileString, String wantedFile, String bonusFile) throws IOException {
        File zipFile = new File(fileString);
        String targetDirectory = zipFile.getParent();
        return unzip(fileString, wantedFile, bonusFile, targetDirectory);
    }

    /**
     * Unzips specified files in an archive
     * @param fileString the archive, as an absolute path
     * @param wantedFile the file we want, as an absolute path
     * @param bonusFile another file we want, as an absolute path
     * @param targetDirectory the directory where to place it, as an absolute path
     * @return the wanted file, as an absolute path
     * @throws IOException if file accesses fail
     */
    public static String unzip(String fileString, String wantedFile, String bonusFile, String targetDirectory) throws IOException {
        Log.v(PatchingTask.class.getSimpleName(), "Unzipping " + fileString);
        String resultFileName = "";
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        ZipInputStream zis = null;
        try {
            fis = new FileInputStream(fileString);
            bis = new BufferedInputStream(fis);
            zis = new ZipInputStream(bis);
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                String fileName = ze.getName();
                if (!fileName.matches(wantedFile) && !fileName.matches(bonusFile)) {
                    continue;
                }
                if (fileName.matches(wantedFile)) {
                    resultFileName = fileName;
                }
                File file = new File(targetDirectory, fileName);
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (dir == null || (!dir.isDirectory() && !dir.mkdirs())) {
                    throw new FileNotFoundException("Failed to ensure directory: " + dir);
                }
                if (ze.isDirectory()) {
                    continue;
                }
                try (FileOutputStream fOut = new FileOutputStream(file)) {
                    while ((count = zis.read(buffer)) > 0) {
                        fOut.write(buffer, 0, count);
                    }
                }
                if (count == 0) {
                    throw new IOException();
                }
            }
        } finally {
            if (zis != null) {
                zis.close();
            } else if (bis != null) {
                bis.close();
            } else if (fis != null) {
                fis.close();
            }
        }

        if ("".equals(resultFileName)) {
            return "";
        }
        return new File(targetDirectory, resultFileName).getAbsolutePath();
    }

    /**
     * Downloads a file from the Web
     * @param url the file url
     * @param folder the destination folder, as an absolute path
     * @return the file, as an absolute path
     */
    public static String downloadFile(String url, File folder) {
        Log.v(PatchingTask.class.getSimpleName(), "Downloading " + url);
        try {
            URL u = new URL(url);
            URLConnection conn = u.openConnection();
            String fileName = URLDecoder.decode(conn.getURL().getFile(), "UTF-8");
            fileName = fileName.substring(fileName.lastIndexOf('/'));

            InputStream stream = conn.getInputStream();
            int length;
            byte[] buffer = new byte[4096];

            DataOutputStream fos = new DataOutputStream(new FileOutputStream(folder + fileName));
            while((length = stream.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            stream.close();

            fos.flush();
            fos.close();
            return folder + fileName;
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Deletes file in a folder if they match a specified filter (not in a recursive way)
     * @param folder the folder, as an absolute path
     * @param filter the regex filter
     */
    public static void clearFiles(File folder, String filter) {
        if (folder.exists()) {
            String[] fileNames = folder.list();
            if (fileNames != null) {
                for (String fileName : fileNames) {
                    if (fileName.matches(filter)) {
                        //noinspection ResultOfMethodCallIgnored
                        new File(folder, fileName).delete();
                    }
                }
            }
        }
    }

}
