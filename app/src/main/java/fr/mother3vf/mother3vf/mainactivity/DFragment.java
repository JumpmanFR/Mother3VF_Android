package fr.mother3vf.mother3vf.mainactivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.DialogFragment;
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
public class DFragment extends DialogFragment {
    public static final String ID = "ID";
    public static final String TITLE = "TITLE";
    public static final String ICON = "ICON";
    public static final String MESSAGE = "MESSAGE";
    public static final String BUTTONS = "BUTTONS";
    public static final String PROGRESS = "PROGRESS";

    public static final String TAG_PROGRESS = "PROGRESS";

    private static final String SAVED_MESSAGE = "SAVED_MESSAGE";
    private static final String SAVED_IS_PROGRESS = "SAVED_IS_PROGRESS";
    private static final String SAVED_DISMISSED = "SAVED_DISMISSED";

    private String dialogMessage;
    private boolean isProgress;
    private boolean dismissed = false;

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        boolean newDialog = (savedInstanceState == null);
        if (!newDialog) {
            dialogMessage = savedInstanceState.getString(SAVED_MESSAGE);
            isProgress = savedInstanceState.getBoolean(SAVED_IS_PROGRESS);
            dismissed = savedInstanceState.getBoolean(SAVED_DISMISSED);
        }

        Bundle arguments = getArguments();
        if (arguments != null) {
            if (newDialog) {
                dialogMessage = arguments.getString(MESSAGE);
                isProgress = arguments.getBoolean(PROGRESS);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                    .setIcon(arguments.getInt(ICON, R.mipmap.ic_launcher))
                    .setTitle(arguments.getInt(TITLE, R.string.app_name_dialogs));

            if (isProgress) {
                View inflate = LayoutInflater.from(getContext()).inflate(R.layout.progress_dialog, null);
                ((TextView) inflate.findViewById(R.id.progress_message)).setText(dialogMessage);
                builder.setView(inflate);
            } else {
                builder.setMessage(HtmlCompat.fromHtml(dialogMessage, HtmlCompat.FROM_HTML_MODE_LEGACY));
            }

            DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    respondToActivity(which == DialogInterface.BUTTON_POSITIVE);
                }
            };
            if (arguments.getInt(BUTTONS) > 0) { // Positive button
                builder.setPositiveButton(R.string.ok, clickListener);
            }
            if (arguments.getInt(BUTTONS) > 1) { // Negative Button
                builder.setNegativeButton(R.string.cancel, clickListener);
            }

            final AlertDialog dialog = builder.create();

            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface di) {
                   ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                }
            });
            return dialog;
        } else {
            return new AlertDialog.Builder(getActivity())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.error)
                    .setMessage(R.string.unknown_error).create();
        }
    }

    private void respondToActivity(boolean response) {
        if (getActivity() != null && getArguments() != null) {
            ((MainActivity) getActivity()).onDialogResponse(getArguments().getInt(ID), response);
        }
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        respondToActivity(false);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        state.putString(SAVED_MESSAGE, dialogMessage);
        state.putBoolean(SAVED_IS_PROGRESS, isProgress);
        state.putBoolean(SAVED_DISMISSED, dismissed);
    }

    /**
     * Keep the same dialog, update the message (for steps on a long process)
     * @param text the message
     */
    public void updateMessage(String text) {
        dialogMessage = text;
        refreshView();
    }

    private void refreshView() {
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            if (isProgress) {
                ((TextView) dialog.findViewById(R.id.progress_message)).setText(dialogMessage);
            } else {
                dialog.setMessage(HtmlCompat.fromHtml(dialogMessage, HtmlCompat.FROM_HTML_MODE_LEGACY));
            }
        }
    }

    public boolean isDismissed() {
        return dismissed;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        dismissed = true;
    }
}
