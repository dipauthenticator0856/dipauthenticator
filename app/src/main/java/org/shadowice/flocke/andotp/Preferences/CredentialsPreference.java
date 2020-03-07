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

import android.app.AlertDialog;
import android.content.Context;
import android.preference.DialogPreference;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.shadowice.flocke.andotp.R;
import org.shadowice.flocke.andotp.Utilities.Constants;
import org.shadowice.flocke.andotp.Utilities.Settings;
import org.shadowice.flocke.andotp.Utilities.UIHelper;

import java.util.Arrays;
import java.util.List;

import static org.shadowice.flocke.andotp.Utilities.Constants.AuthMethod;
import static org.shadowice.flocke.andotp.Utilities.Constants.EncryptionType;

public class CredentialsPreference extends DialogPreference
    implements AdapterView.OnItemClickListener, View.OnClickListener, TextWatcher {
    public static final AuthMethod DEFAULT_VALUE = AuthMethod.PIN;

    public interface EncryptionChangeCallback {
        boolean testEncryptionChange(byte[] newKey);
    }

    private List<String> entries;
 /*   private static final List<AuthMethod> entryValues = Arrays.asList(
            AuthMethod.NONE,
            AuthMethod.PASSWORD,
            AuthMethod.PIN,
            AuthMethod.DEVICE
    );*/

    private static final List<AuthMethod> entryValues = Arrays.asList(
            AuthMethod.PIN
    );

    private int minLength = 0;

    private Settings settings;
    private AuthMethod value = AuthMethod.PIN;
    private EncryptionChangeCallback encryptionChangeCallback = null;

    private LinearLayout credentialsLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText passwordInput;
    private EditText passwordConfirm;
    private TextView toShortWarning;

    private Button btnSave;

    public CredentialsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        settings = new Settings(context);
        entries = Arrays.asList(context.getResources().getStringArray(R.array.settings_entries_auth));

        setDialogLayoutResource(R.layout.component_authentication);
    }

    public void setEncryptionChangeCallback(EncryptionChangeCallback cb) {
        this.encryptionChangeCallback = cb;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        builder.setPositiveButton(null, null);
        builder.setNegativeButton(null, null);
    }

    @Override
    protected void onBindDialogView(View view) {
        value = settings.getAuthMethod();

        ListView listView = view.findViewById(R.id.credentialSelection);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_single_choice, entries);
        listView.setAdapter(adapter);

        int index = entryValues.indexOf(value);
        listView.setSelection(index);
        listView.setItemChecked(index,true);
        listView.setOnItemClickListener(this);

        credentialsLayout = view.findViewById(R.id.credentialsLayout);

        passwordLayout = view.findViewById(R.id.passwordLayout);
        passwordInput = view.findViewById(R.id.passwordEdit);
        passwordConfirm = view.findViewById(R.id.passwordConfirm);

        toShortWarning = view.findViewById(R.id.toShortWarning);

        passwordInput.addTextChangedListener(this);
        passwordConfirm.addTextChangedListener(this);

        Button btnCancel = view.findViewById(R.id.btnCancel);
        btnSave = view.findViewById(R.id.btnSave);

        btnCancel.setOnClickListener(this);
        btnSave.setOnClickListener(this);

        updateLayout();

        super.onBindDialogView(view);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            String stringValue = getPersistedString(DEFAULT_VALUE.name().toLowerCase());
            value = AuthMethod.valueOf(stringValue.toUpperCase());
        } else {
            value = DEFAULT_VALUE;
            persistString(value.name().toLowerCase());
        }

        setSummary(entries.get(entryValues.indexOf(value)));
    }

    private void saveValues() {
        byte[] newKey = null;



        if (value == AuthMethod.PIN) {
            String password = passwordInput.getText().toString();
            if (!password.isEmpty()) {
                newKey = settings.setAuthCredentials(password);
            } else {
                return;
            }
        }

        if (settings.getEncryption() == EncryptionType.PASSWORD) {
            if (newKey == null || encryptionChangeCallback == null)
                return;

            if (! encryptionChangeCallback.testEncryptionChange(newKey))
                return;
        }

        persistString(value.toString().toLowerCase());
        setSummary(entries.get(entryValues.indexOf(value)));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case (R.id.btnCancel):
                getDialog().dismiss();
                break;
            case (R.id.btnSave):
                saveValues();
                getDialog().dismiss();
                break;
            default:
                break;
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String password = passwordInput.getEditableText().toString();

        if (password.length() >= minLength) {
            toShortWarning.setVisibility(View.GONE);

            String confirm = passwordConfirm.getEditableText().toString();

            if (!password.isEmpty() && !confirm.isEmpty() && password.equals(confirm)) {
                btnSave.setEnabled(true);
            } else {
                btnSave.setEnabled(false);
            }
        } else {
            toShortWarning.setVisibility(View.VISIBLE);
        }
    }

    private void updateLayout() {
        if (value == AuthMethod.PIN) {
            credentialsLayout.setVisibility(View.VISIBLE);

            passwordLayout.setHint(getContext().getString(R.string.settings_hint_pin));
            passwordConfirm.setHint(R.string.settings_hint_pin_confirm);

            passwordInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            passwordConfirm.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

            passwordInput.setTransformationMethod(new PasswordTransformationMethod());
            passwordConfirm.setTransformationMethod(new PasswordTransformationMethod());

            minLength = Constants.AUTH_MIN_PIN_LENGTH;
            toShortWarning.setText(getContext().getString(R.string.settings_label_short_pin, minLength));

            passwordInput.requestFocus();
            UIHelper.showKeyboard(getContext(), passwordInput);

            btnSave.setEnabled(false);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        value = entryValues.get(position);
        updateLayout();
    }

    // Needed stub functions
    @Override
    public void afterTextChanged(Editable s) {}

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}


}
