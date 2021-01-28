package fr.mother3vf.mother3vf;

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
public class FileUtils {

    public static String unzip(String fileString, String wantedFile, String bonusFile) throws IOException {
        File zipFile = new File(fileString);
        String targetDirectory = zipFile.getParent();
        return unzip(fileString, wantedFile, bonusFile, targetDirectory);
    }

    public static String unzip(String fileString, String wantedFile, String bonusFile, String targetDirectory) throws IOException {
        Log.v(PatchingTask.class.getSimpleName(), "Décompression de " + fileString);
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileString));
        ZipInputStream zis = new ZipInputStream(bis);
        String resultFileName = "";
        try {
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
                if (!dir.isDirectory() && !dir.mkdirs()) {
                    throw new FileNotFoundException("Failed to ensure directory: " + dir.getAbsolutePath());
                }
                if (ze.isDirectory()) {
                    continue;
                }
                try (FileOutputStream fout = new FileOutputStream(file)) {
                    while ((count = zis.read(buffer)) > 0) {
                        if (count == 0) {
                            fout.close();
                            bis.close();
                            throw new IOException();
                        }
                        fout.write(buffer, 0, count);
                    }
                }
                if (count == 0) {
                    throw new IOException();
                }
            }
        } finally {
            bis.close();
            //zis.close();
        }

        if ("".equals(resultFileName)) {
            return "";
        }
        return new File(targetDirectory, resultFileName).getAbsolutePath();
    }

    public static String downloadFile(String url, File folder) {
        Log.v(PatchingTask.class.getSimpleName(), "Téléchargement de " + url);
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

    public static void clearFiles(File folder, String filter) {
        if (folder.exists()) {
            String[] fileNames = folder.list();
            for (String fileName : fileNames) {
                if (fileName.matches(filter)) {
                    //noinspection ResultOfMethodCallIgnored
                    new File(folder, fileName).delete();
                }
            }
        }
    }

}
