/*
 * Copyright (C) 2017-2018 Jakob Nixdorf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.shadowice.flocke.andotp.Preferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.preference.DialogPreference;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.shadowice.flocke.andotp.Activities.MainActivity;
import org.shadowice.flocke.andotp.R;
import org.shadowice.flocke.andotp.Utilities.Constants;
import org.shadowice.flocke.andotp.Utilities.EncryptionHelper;
import org.shadowice.flocke.andotp.Utilities.KeyStoreHelper;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

public class PasswordEncryptedPreference extends DialogPreference
        implements View.OnClickListener, TextWatcher {

    private static final String DEFAULT_VALUE = "";
    private KeyPair key;
    private Mode mode = Mode.PASSWORD;
    private TextInputEditText passwordInput;
    private EditText passwordConfirm;
    private Button btnSave;
    private String value = DEFAULT_VALUE;
    private Context context1;

    public PasswordEncryptedPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        context1 = context;

        try {
            key = KeyStoreHelper.loadOrGenerateAsymmetricKeyPair(context, Constants.KEYSTORE_ALIAS_PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setDialogLayoutResource(R.layout.component_password);
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        builder.setPositiveButton(null, null);
        builder.setNegativeButton(null, null);
    }

    @Override
    protected void onBindDialogView(View view) {
        TextInputLayout passwordLayout = view.findViewById(R.id.passwordLayout);
        passwordInput = view.findViewById(R.id.passwordEdit);
        passwordConfirm = view.findViewById(R.id.passwordConfirm);

        Button btnCancel = view.findViewById(R.id.btnCancel);
        btnSave = view.findViewById(R.id.btnSave);
        btnSave.setEnabled(false);

        btnCancel.setOnClickListener(this);
        btnSave.setOnClickListener(this);

        if (!value.isEmpty()) {
            passwordInput.setText(value);
        }

        if (mode == Mode.PASSWORD) {
            passwordLayout.setHint(getContext().getString(R.string.settings_hint_password));
            passwordConfirm.setHint(R.string.settings_hint_password_confirm);

            passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else if (mode == Mode.PIN) {
            passwordLayout.setHint(getContext().getString(R.string.settings_hint_pin));
            passwordConfirm.setHint(R.string.settings_hint_pin_confirm);

            passwordInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            passwordConfirm.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        }

        passwordInput.setTransformationMethod(new PasswordTransformationMethod());
        passwordConfirm.setTransformationMethod(new PasswordTransformationMethod());

        passwordConfirm.addTextChangedListener(this);
        passwordInput.addTextChangedListener(this);

        super.onBindDialogView(view);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    private void encryptAndPersist(String value) {
        try {
            byte[] encBytes = EncryptionHelper.encrypt(key.getPublic(), value.getBytes(StandardCharsets.UTF_8));
            persistString(Base64.encodeToString(encBytes, Base64.URL_SAFE));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void restoreAndDecrypt(String encValue) {
        try {
            byte[] encBytes = Base64.decode(encValue, Base64.URL_SAFE);
            value = new String(EncryptionHelper.decrypt(key.getPrivate(), encBytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            restoreAndDecrypt(getPersistedString(DEFAULT_VALUE));
        } else {
            value = (String) defaultValue;
            encryptAndPersist(value);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case (R.id.btnCancel):
                getDialog().dismiss();
                break;
            case (R.id.btnSave):

                passwordNoteDialog(context1);

        /*        AlertDialog.Builder builder = new AlertDialog.Builder(context1);
                builder.setTitle("Note:")
                        .setMessage("Save/Remember this password carefully and place your encrypted backup file into any cloud, desktop, laptop and other locations. Because after change or lost your mobile, you can recover your 2FA accounts using backup file and this encrypted password only.")
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //do things
                                dialog.dismiss();
                                MainActivity.getInstance().BackupPasswordChanged();
                                getDialog().dismiss();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();*/


                break;
            default:
                break;
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (passwordConfirm.getEditableText().toString().equals(passwordInput.getEditableText().toString())) {
            btnSave.setEnabled(true);
        } else {
            btnSave.setEnabled(false);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    private void passwordNoteDialog(Context context) {

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_passwordnote);

        Button btnContinue = dialog.findViewById(R.id.btnContinue);
        CheckBox chkCondition = dialog.findViewById(R.id.chkCondition);
        btnContinue.setEnabled(false);

        chkCondition.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    btnContinue.setEnabled(true);
                } else {
                    btnContinue.setEnabled(false);
                }
            }
        });

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                value = passwordInput.getText().toString();
                encryptAndPersist(value);

                dialog.dismiss();
                if (MainActivity.getInstance() != null){
                    MainActivity.getInstance().BackupPasswordChanged();
                }
                getDialog().dismiss();

                Log.e("classname"," "+((Activity) context1).getClass().getSimpleName());

                ((Activity) context1).finish();

            }
        });

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        dialog.show();

    }

    public enum Mode {
        PASSWORD, PIN
    }
}
