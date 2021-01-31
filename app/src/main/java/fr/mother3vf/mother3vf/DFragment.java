package fr.mother3vf.mother3vf;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.DialogFragment;

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
    private static final String SAVED_DISMISSED = "SAVED_DISMISSED";

    private String dialogMessage;
    private boolean dismissed = false;

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            dialogMessage = savedInstanceState.getString(SAVED_MESSAGE);
            dismissed = savedInstanceState.getBoolean(SAVED_DISMISSED);
        }

        Bundle arguments = getArguments();
        if (arguments != null) {
            if (dialogMessage == null) {
                dialogMessage = arguments.getString(MESSAGE);
            }
            if (arguments.getBoolean(PROGRESS, false)) //noinspection SpellCheckingInspection
            { // if (PROGRESS is true considering his default value is false)

                /* TODO
                Reprendre 2ᵉ réponse de https://stackoverflow.com/questions/45373007/progressdialog-is-deprecated-what-is-the-alternate-one-to-use
                Factoriser la création d’un AlertDialog avec la partie else
                 */

                ProgressDialog pd = ProgressDialog.show(getActivity(), getResources().getString(arguments.getInt(TITLE)),
                        dialogMessage, true);
                pd.setIcon(arguments.getInt(ICON, R.mipmap.ic_launcher));
                return pd;
                /*AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                        .setIcon(arguments.getInt(ICON, R.mipmap.ic_launcher))
                        .setTitle(arguments.getInt(TITLE, arguments.getInt(TITLE)))
                        .setMessage(HtmlCompat.fromHtml(dialogMessage, HtmlCompat.FROM_HTML_MODE_LEGACY));
                ProgressBar pb = new ProgressBar(getActivity(), null, android.R.attr.progressBarStyleLarge);
                ConstraintLayout layout = getActivity().findViewById(R.id.inside_view);
                ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(100,100);
                //params.addRule(RelativeLayout.CENTER_IN_PARENT);
                layout.addView(pb, params);
                pb.setVisibility(View.VISIBLE);
                final AlertDialog dialog = builder.create();
                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface di) {
                        ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                    }
                });
                return dialog;*/
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                        .setIcon(arguments.getInt(ICON, R.mipmap.ic_launcher))
                        .setTitle(arguments.getInt(TITLE, R.string.app_name_dialogs))
                        .setMessage(HtmlCompat.fromHtml(dialogMessage, HtmlCompat.FROM_HTML_MODE_LEGACY));
                if (arguments.getInt(BUTTONS) > 0) { // Positive button
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            respondToActivity(true);
                        }
                    });
                }

                if (arguments.getInt(BUTTONS) > 1) { // Negative Button
                    builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            respondToActivity(false);
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
        Dialog dialog = getDialog();
        if (dialog != null) {
            ((AlertDialog) dialog).setMessage(dialogMessage);
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
