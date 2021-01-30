package fr.mother3vf.mother3vf;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import fr.mother3vf.mother3vf.databinding.ActivityBrowserBinding;

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
public class FileBrowserActivity extends AppCompatActivity implements FileBrowserAdapter.ItemClickListener, CompoundButton.OnCheckedChangeListener, View.OnClickListener {
    public static final String DISPLAY_FILTER_NAME = "DISPLAY_FILTER_NAME";
    public static final String DISPLAY_FILTER = "DISPLAY_FILTER";
    public static final String TARGET_FILTER = "TARGET_FILTER";
    public static final String TARGET_ICON = "TARGET_ICON";
    public static final String FOLDER = "FOLDER";
    public static final String FILE = "FILE";

    public static final int BROWSE_FOR_ROM = 1;
    public static final int BROWSE_FOR_PATCH = 2;

    private static final String DISPLAY_ONLY = "DISPLAY_ONLY";
    private static final String CURRENT_FOLDER = "CURRENT_FOLDER";
    private static final String HISTORY = "HISTORY";

    private ActivityBrowserBinding views;

    private ArrayList<String> filesList;
    private ArrayList<String> history;
    private String currentFolder;

    private boolean displayOnly;

    private String displayTypeName = "";
    private String displayType = "";
    private String targetType = "";
    private String targetIcon = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        views = ActivityBrowserBinding.inflate(getLayoutInflater());
        setContentView(views.getRoot());

        setSupportActionBar(views.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setIcon(R.drawable.ic_actionbar);
        }

        views.list.addItemDecoration(new DividerItemDecoration(views.list.getContext(), DividerItemDecoration.VERTICAL));

        Bundle extras = getIntent().getExtras();
        String startFolder = "";
        if (extras != null) {
            displayTypeName = extras.getString(DISPLAY_FILTER_NAME);
            displayType = extras.getString(DISPLAY_FILTER);
            targetType = extras.getString(TARGET_FILTER);
            targetIcon = extras.getString(TARGET_ICON);
            startFolder = extras.getString(FOLDER, Environment.getExternalStorageDirectory().getAbsolutePath()); // getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()) ?
        }

        if (savedInstanceState != null) {
            displayOnly = savedInstanceState.getBoolean(DISPLAY_ONLY);
            history = savedInstanceState.getStringArrayList(HISTORY);
            getDir(savedInstanceState.getString(CURRENT_FOLDER), false);
        } else {
            displayOnly = true;
            history = new ArrayList<>();
            File ff = new File(startFolder);
            if (TextUtils.isEmpty(startFolder) || !ff.exists() || !ff.canRead() || ff.isFile()) {
                startFolder = Environment.getExternalStorageDirectory().getAbsolutePath();
            }
            getDir(startFolder, true);
        }
        views.dismissButton.setOnClickListener(this);
        views.showOnlyCheckbox.setText(getBaseContext().getString(R.string.only_show_files).replace("%s", displayTypeName));
        views.showOnlyCheckbox.setChecked(displayOnly);
        views.showOnlyCheckbox.setOnCheckedChangeListener(this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        state.putBoolean(CURRENT_FOLDER, displayOnly);
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
        ArrayList<String> itemsList = new ArrayList<>();

        filesList = new ArrayList<>();
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        if (files != null) {

            if (!folderPath.equals("/") && !(folderPath.equals("/storage/") && !(new File("/").canRead()))) {
                itemsList.add("⤴️");
                if (folder.getParent() != null && new File(folder.getParent()).canRead()) {
                    filesList.add(folder.getParent());
                } else if (new File("/").canRead()) {
                    filesList.add("/");
                } else {
                    filesList.add("/storage/");
                }
            }
            currentFolder = folderPath;

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


                for (File file : files) {
                    if (file.canRead() && file.isFile()) {
                        String fileName = file.getName();
                        String fileIcon;
                        if (Pattern.matches(targetType, fileName)) {
                            fileIcon = targetIcon;
                        } else if (fileName.endsWith(".zip")) {
                            fileIcon = (Build.VERSION.SDK_INT > 22 ? "\uD83D\uDDDC" : "\uD83D\uDCE6"); // Archive emoji
                        } else {
                            fileIcon = "\uD83D\uDCC4"; // Generic file emoji
                        }

                        if (!displayOnly || Pattern.matches(displayType, fileName)) {
                            filesList.add(file.getPath());
                            itemsList.add(fileIcon + " " + fileName);
                        }
                    } else if (file.isDirectory()) {

                        filesList.add(file.getPath() + "/");
                        itemsList.add("\uD83D\uDCC1 " + file.getName()); // Folder icon
                    }
                }
            }
            FileBrowserAdapter fileAdapter = new FileBrowserAdapter(this, itemsList);
            views.list.setLayoutManager(new LinearLayoutManager(this));
            views.list.setAdapter(fileAdapter);
            fileAdapter.setClickListener(this);
            views.path.setText(folderPath);
            if (addToHistory) {
                history.add(folderPath);
            }
        } else {
            getDir("/", addToHistory);
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        if (view.getId() == R.id.list_item) {
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

                    new AlertDialog.Builder(this)
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

    }

    private boolean canReadFile(String file) {
        return new File(file).exists() && new File(file).canRead();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton.getId() == R.id.showOnlyCheckbox) {
            displayOnly = b;
            getDir(currentFolder, false);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.dismissButton) {
            this.finish();
        }
    }
}




