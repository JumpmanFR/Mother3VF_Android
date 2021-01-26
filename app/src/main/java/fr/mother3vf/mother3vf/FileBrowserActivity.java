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

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Pattern;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class FileBrowserActivity extends ListActivity {
    public static final String SHOW_UPS = "SHOW_UPS";
    public static final String TARGET_TYPE = "TARGET_TYPE";
    public static final String TARGET_ICON = "TARGET_ICON";
    public static final String FOLDER = "FOLDER";
    public static final String FILE = "FILE";

    public static final int BROWSE_FOR_ROM = 1;
    public static final int BROWSE_FOR_PATCH = 2;

    private static final String CURRENT_FOLDER = "CURRENT_FOLDER";
    private static final String HISTORY = "HISTORY";

    private ArrayList<String> itemsList;
    private ArrayList<String> filesList;
    private ArrayList<String> history;
    private String currentFolder;

    private boolean showUps;
    private String targetType = "";
    private String targetIcon = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filebrowser);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            showUps = extras.getBoolean(SHOW_UPS);
            targetType = extras.getString(TARGET_TYPE);
            targetIcon = extras.getString(TARGET_ICON);
        }

        if (savedInstanceState != null) {
            history = savedInstanceState.getStringArrayList(HISTORY);
            getDir(savedInstanceState.getString(CURRENT_FOLDER), false);
        } else {
            history = new ArrayList<>();
            String folder = extras.getString(FOLDER, Environment.getExternalStorageDirectory().getAbsolutePath());
            File ff = new File(folder);
            if (ff == null || TextUtils.isEmpty(folder)) {
                folder = "/";
            } else if (!ff.exists() || !ff.canRead() || ff.isFile()) {
                folder = "/";
            }
            getDir(folder, true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString(CURRENT_FOLDER, currentFolder);
        state.putStringArrayList(HISTORY, history);
    }

    @Override
    public boolean onKeyDown(int KeyCode, KeyEvent event) {
        if (KeyCode == KeyEvent.KEYCODE_BACK) {
            getParentDir();
            return true;
        }
        return super.onKeyDown(KeyCode, event);
    }

    private void getParentDir() {
        int historySize = history.size();
        if (historySize > 1) {
            history.remove(historySize - 1);
            getDir(history.get(historySize - 2), false);
        } else {
            this.finish();
        }
    }

    private void getDir(String folderPath, boolean addToHistory) {
        itemsList = new ArrayList<>();

        filesList = new ArrayList<>();
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        if (files != null) {
            if (files.length > 0) {

                Arrays.sort(files, new Comparator<Object>() {
                    @Override
                    public int compare(Object o1, Object o2) {
                        if ((o1 == o2) || (TextUtils.isEmpty(((File) o1).getName()) || (TextUtils.isEmpty(((File) o2).getName()))))
                            return 0;

                        File f1 = (File) o1;
                        File f2 = (File) o2;

                        if (f1.isDirectory() && f2.isFile())
                            return -1;
                        if (f1.isFile() && f2.isDirectory())
                            return 1;

                        return Collator.getInstance(Locale.FRENCH).compare(f1.getName(), f2.getName());
                    }
                });


                if (!folderPath.equals("/") && !(folderPath.equals("/storage/") && !(new File(File.separator).canRead()))) {
                    itemsList.add("⤴️");
                    // Thank you Marshmallow.
                    // Disallowing access to /storage/emulated has now prevent billions of hacking attempts daily.
                    if (new File(folder.getParent()).canRead()) {
                        filesList.add(folder.getParent());
                    } else if (new File("/").canRead()) {
                        filesList.add("/");
                    } else {
                        filesList.add("/storage/");
                    }
                }
                currentFolder = folderPath;


                for (int i = 0; i < files.length; i++) {
                    File file = files[i];

                    if (files[i].isFile()) {
                        String fileIcon;
                        if (Pattern.matches(targetType, files[i].getName())) {
                            fileIcon = targetIcon;
                        } else if (files[i].getName().endsWith(".zip")) {
                            fileIcon = (Build.VERSION.SDK_INT > 22 ? "\uD83D\uDDDC" : "\uD83D\uDCE6");
                        } else {
                            fileIcon = "\uD83D\uDCC4";
                        }

                        int dotPosition = files[i].getName().lastIndexOf(".");
                        String extension = "";
                        if (dotPosition != -1) {
                            extension = (files[i].getName().substring(dotPosition)).toLowerCase(Locale.FRENCH);
                            if (extension != null) {
                                if ((".ups".equals(extension) && showUps) || (!showUps && !(".ups".equals(extension)))) {
                                    filesList.add(file.getPath());
                                    itemsList.add(fileIcon + " " + file.getName());
                                }
                            } else if (files[i].getName().endsWith("/")) {
                                filesList.add(file.getPath() + "/");
                                itemsList.add(fileIcon + " " + file.getName() + "/");
                            } else {
                                filesList.add(file.getPath());
                                itemsList.add(fileIcon + " " + file.getName());
                            }
                        } else {
                            filesList.add(file.getPath());
                            itemsList.add(fileIcon + " " + file.getName());
                        }
                    } else {

                        filesList.add(file.getPath() + "/");

                        itemsList.add("\uD83D\uDCC1 " + file.getName());
                    }
                }

            } else {
                if (!folderPath.equals("/") && !(folderPath.equals("/storage/") && !(new File(File.separator).canRead()))) {
                    itemsList.add("⤴️");
                    if (new File(folder.getParent()).canRead()) {
                        filesList.add(folder.getParent());
                    } else if (new File("/").canRead()) {
                        filesList.add("/");
                    } else {
                        filesList.add("/storage/");
                    }
                }
                currentFolder = folderPath;
            }
            ArrayAdapter<String> fileList = new ArrayAdapter<>(this, R.layout.row, itemsList);
            setListAdapter(fileList);
            getListView().setFastScrollEnabled(true);
            ((TextView) findViewById(R.id.path)).setText(folderPath);
            if (addToHistory) {
                history.add(folderPath);
            }
        } else {
            getDir("/", addToHistory);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, final int position, long id) {
        File file = new File(filesList.get(position));
        if (file.isDirectory()) {
            if (file.canRead()) {
                getDir(filesList.get(position), true);
            } else if (file.getAbsolutePath().equals("/storage/emulated") &&
                    (canReadFile("/storage/emulated/0") || canReadFile("/storage/emulated/legacy") || canReadFile("storage/self/primary"))) {
                if (canReadFile("/storage/emulated/0")) {
                    getDir("/storage/emulated/0", true);
                } else if (canReadFile("/storage/emulated/legacy")) {
                    getDir("/storage/emulated/legacy", true);
                } else {
                    getDir("/storage/self/primary", true);
                }
            } else {
                System.out.println(file.getAbsolutePath());

                new AlertDialog.Builder(this) // TODO
                        .setIcon(R.mipmap.ic_launcher)
                        .setTitle("[" + file.getName() + "] " + getResources().getString(R.string.cantRead))
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                }).show();
            }
        } else {
            if (file.canRead()) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra(FILE, filesList.get(position));
                returnIntent.putExtra(FOLDER, currentFolder);
                setResult(RESULT_OK, returnIntent);
                this.finish();
            }

        }

    }

    private boolean canReadFile(String file) {
        return new File(file).exists() && new File(file).canRead();
    }

}




