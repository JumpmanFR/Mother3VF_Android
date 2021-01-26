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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class DFragment extends DialogFragment {
    public static final String ID = "ID";
    public static final String TITLE = "TITLE";
    public static final String ICON = "ICON";
    public static final String MESSAGE = "MESSAGE";
    public static final String BUTTONS = "BUTTONS";
    public static final String PROGRESS = "PROGRESS";

    public static final String TAG_PROGRESS = "PROGRESS";

    private static final String SAVED_MESSAGE = "SAVED_MESSAGE";
    private static final String SAVED_DISMISSED = "SAVED_DISMISSED";

    private String dialogMessage;
    private boolean dismissed = false;

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            dialogMessage = savedInstanceState.getString(SAVED_MESSAGE);
            dismissed = savedInstanceState.getBoolean(SAVED_DISMISSED);
        }
        if (dialogMessage == null) {
            dialogMessage = getArguments().getString(MESSAGE);
        }

        if (getArguments().getBoolean(PROGRESS, false)) { // if (PROGRESS est à true sachant que sa valeur par défaut est false)
            ProgressDialog pd = ProgressDialog.show(getActivity(), getResources().getString(getArguments().getInt(TITLE)),
                    dialogMessage, true);
            pd.setIcon(getArguments().getInt(ICON, R.mipmap.ic_launcher));
            return pd;
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setIcon(getArguments().getInt(ICON, R.mipmap.ic_launcher))
                    .setTitle(getArguments().getInt(TITLE, R.string.app_name_dialogs))
                    .setMessage(Html.fromHtml(dialogMessage));
            if (getArguments().getInt(BUTTONS) > 0) { // Positive button
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ((MainActivity) getActivity()).onDialogResponse(getArguments().getInt(ID), true);
                    }
                });
            }

            if (getArguments().getInt(BUTTONS) > 1) { // Negative Button
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ((MainActivity) getActivity()).onDialogResponse(getArguments().getInt(ID), false);
                    }
                });
            }
            final AlertDialog dialog = builder.create();
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface di) {
                    ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                }
            });
            return dialog;
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        ((MainActivity) getActivity()).onDialogResponse(getArguments().getInt(ID), false);
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString(SAVED_MESSAGE, dialogMessage);
        state.putBoolean(SAVED_DISMISSED, dismissed);
    }

    public void updateMessage(String text) {
        dialogMessage = text;
        refreshView();
    }

    private void refreshView() {
        Dialog dialog = getDialog();
        if (dialog != null) {
            ((AlertDialog) dialog).setMessage(dialogMessage);
        }
    }

    public boolean isDismissed() {
        return dismissed;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        dismissed = true;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();;
    }
}
