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

package org.shadowice.flocke.andotp.Utilities;

import android.os.Environment;

import com.google.android.gms.wallet.WalletConstants;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Constants {
    // Enums
 /*   public enum AuthMethod {
        NONE, PASSWORD, PIN, DEVICE
    }*/

    // Intents (Format: A0x with A = parent Activity, x = number of the intent)
    public final static int INTENT_MAIN_AUTHENTICATE = 100;
    public final static int INTENT_MAIN_SETTINGS = 101;
    public final static int INTENT_MAIN_BACKUP = 102;
    public final static int INTENT_MAIN_INTRO = 103;
    public final static int INTENT_BACKUP_OPEN_DOCUMENT_PLAIN = 200;
    public final static int INTENT_BACKUP_SAVE_DOCUMENT_PLAIN = 201;
    public final static int INTENT_BACKUP_OPEN_DOCUMENT_CRYPT = 202;
    public final static int INTENT_BACKUP_SAVE_DOCUMENT_CRYPT = 203;
    public final static int INTENT_BACKUP_OPEN_DOCUMENT_PGP = 204;
    public final static int INTENT_BACKUP_SAVE_DOCUMENT_PGP = 205;
    public final static int INTENT_BACKUP_ENCRYPT_PGP = 206;
    public final static int INTENT_BACKUP_DECRYPT_PGP = 207;
    public final static int INTENT_BACKUP_SAVE_CLOUD_CRYPT = 208;
    public final static int INTENT_BACKUP_OPEN_CLOUD_CRYPT = 209;
    public static final int INTENT_SETTINGS_AUTHENTICATE = 300;
    // Permission requests (Format: A1x with A = parent Activity, x = number of the request)
    public final static int PERMISSIONS_BACKUP_READ_IMPORT_PLAIN = 210;
    public final static int PERMISSIONS_BACKUP_WRITE_EXPORT_PLAIN = 211;
    public final static int PERMISSIONS_BACKUP_READ_IMPORT_CRYPT = 212;
    public final static int PERMISSIONS_BACKUP_WRITE_EXPORT_CRYPT = 213;
    public final static int PERMISSIONS_BACKUP_READ_IMPORT_PGP = 214;
    public final static int PERMISSIONS_BACKUP_WRITE_EXPORT_PGP = 215;
    // Intent extras
    public final static String EXTRA_AUTH_PASSWORD_KEY = "password_key";
    public final static String EXTRA_AUTH_NEW_ENCRYPTION = "new_encryption";
    public final static String EXTRA_AUTH_MESSAGE = "message";
    public final static String EXTRA_BACKUP_ENCRYPTION_KEY = "encryption_key";
    public final static String EXTRA_SETTINGS_ENCRYPTION_CHANGED = "encryption_changed";
    public final static String EXTRA_SETTINGS_ENCRYPTION_KEY = "encryption_key";
    // Authentication
    public final static int AUTH_MIN_PIN_LENGTH = 4;
    public final static int AUTH_MIN_PASSWORD_LENGTH = 6;
    // KeyStore
    public final static String KEYSTORE_ALIAS_PASSWORD = "password";
    public final static String KEYSTORE_ALIAS_WRAPPING = "settings";
    // Database files
    public final static String FILENAME_ENCRYPTED_KEY = "otp.key";
    public final static String FILENAME_DATABASE = "secrets.dat";
    public final static String FILENAME_DATABASE_BACKUP = "secrets.dat.bck";
    // Database files
    public final static String DIP_FILENAME_ENCRYPTED_KEY = "dipotp.key";
    public final static String DIP_FILENAME_DATABASE = "dipsecrets.dat";
    public final static String DIP_FILENAME_DATABASE_BACKUP = "dipsecrets.dat.bck";
    public final static String BACKUP_FOLDER_NAME = "Dip 2FA Backup";
    public final static String APP_FOLDER_NAME = "Dip Auth";
    public final static String AUTOBACKUP_FILE_NAME = "autobackup.json.aes";
    //    public final static String AUTOBACKUP_FILE_NAME = "autobackup_%s.json.aes";
    // Backup files
    public final static String BACKUP_FOLDER = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + APP_FOLDER_NAME + File.separator + BACKUP_FOLDER_NAME;

    public final static String BACKUP_FILENAME_PLAIN = "otp_accounts.json";
    public final static String BACKUP_FILENAME_CRYPT = "otp_accounts.json.aes";
    public final static String BACKUP_FILENAME_PGP = "otp_accounts.json.gpg";

    public final static String BACKUP_FILENAME_PLAIN_FORMAT = "otp_accounts_%s.json";
    public final static String BACKUP_FILENAME_CRYPT_FORMAT = "otp_accounts_%s.json.aes";
    public final static String BACKUP_FILENAME_PGP_FORMAT = "otp_accounts_%s.json.gpg";

    public final static String BACKUP_MIMETYPE_PLAIN = "application/json";
    public final static String BACKUP_MIMETYPE_CRYPT = "binary/aes";
    public final static String BACKUP_MIMETYPE_PGP = "application/pgp-encrypted";
    public static final int PAYMENTS_ENVIRONMENT = WalletConstants.ENVIRONMENT_TEST;
    /**
     * The allowed networks to be requested from the API. If the user has cards from networks not
     * specified here in their account, these will not be offered for them to choose in the popup.
     *
     * @value #SUPPORTED_NETWORKS
     */
    public static final List<String> SUPPORTED_NETWORKS = Arrays.asList(
            "AMEX",
            "DISCOVER",
            "JCB",
            "MASTERCARD",
            "VISA");
    /**
     * The Google Pay API may return cards on file on Google.com (PAN_ONLY) and/or a device token on
     * an Android device authenticated with a 3-D Secure cryptogram (CRYPTOGRAM_3DS).
     *
     * @value #SUPPORTED_METHODS
     */
    public static final List<String> SUPPORTED_METHODS =
            Arrays.asList(
                    "PAN_ONLY",
                    "CRYPTOGRAM_3DS");
    /**
     * Required by the API, but not visible to the user.
     *
     * @value #COUNTRY_CODE Your local country
     */
    public static final String COUNTRY_CODE = "US";
    /**
     * Required by the API, but not visible to the user.
     *
     * @value #CURRENCY_CODE Your local currency
     */
    public static final String CURRENCY_CODE = "USD";
    /**
     * Supported countries for shipping (use ISO 3166-1 alpha-2 country codes). Relevant only when
     * requesting a shipping address.
     *
     * @value #SHIPPING_SUPPORTED_COUNTRIES
     */
    public static final List<String> SHIPPING_SUPPORTED_COUNTRIES = Arrays.asList("US", "GB");
    /**
     * The name of your payment processor/gateway. Please refer to their documentation for more
     * information.
     *
     * @value #PAYMENT_GATEWAY_TOKENIZATION_NAME
     */
    public static final String PAYMENT_GATEWAY_TOKENIZATION_NAME = "example";
    /**
     * Custom parameters required by the processor/gateway.
     * In many cases, your processor / gateway will only require a gatewayMerchantId.
     * Please refer to your processor's documentation for more information. The number of parameters
     * required and their names vary depending on the processor.
     *
     * @value #PAYMENT_GATEWAY_TOKENIZATION_PARAMETERS
     */
    public static final HashMap<String, String> PAYMENT_GATEWAY_TOKENIZATION_PARAMETERS =
            new HashMap<String, String>() {
                {
                    put("gateway", PAYMENT_GATEWAY_TOKENIZATION_NAME);
                    put("gatewayMerchantId", "exampleGatewayMerchantId");
                    // Your processor may require additional parameters.
                }
            };
    /**
     * Only used for {@code DIRECT} tokenization. Can be removed when using {@code PAYMENT_GATEWAY}
     * tokenization.
     *
     * @value #DIRECT_TOKENIZATION_PUBLIC_KEY
     */
//    public static final String DIRECT_TOKENIZATION_PUBLIC_KEY = "REPLACE_ME";
    public static final String DIRECT_TOKENIZATION_PUBLIC_KEY = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEGnJ7Yo1sX9b4kr4Aa5uq58JRQfzD8bIJXw7WXaap\\/hVE+PnFxvjx4nVxt79SdRuUVeu++HZD0cGAv4IOznc96w==";
    /**
     * Parameters required for {@code DIRECT} tokenization.
     * Only used for {@code DIRECT} tokenization. Can be removed when using {@code PAYMENT_GATEWAY}
     * tokenization.
     *
     * @value #DIRECT_TOKENIZATION_PARAMETERS
     */
    public static final HashMap<String, String> DIRECT_TOKENIZATION_PARAMETERS =
            new HashMap<String, String>() {
                {
                    put("protocolVersion", "ECv2");
                    put("publicKey", DIRECT_TOKENIZATION_PUBLIC_KEY);
                }
            };
    // Encryption algorithms and definitions
    final static String ALGORITHM_SYMMETRIC = "AES/GCM/NoPadding";
    final static String ALGORITHM_ASYMMETRIC = "RSA/ECB/PKCS1Padding";
    final static int ENCRYPTION_KEY_LENGTH = 16;           // 128-bit encryption key (KeyStore-mode)
    final static int ENCRYPTION_IV_LENGTH = 12;
    final static int PBKDF2_MIN_ITERATIONS = 1000;
    final static int PBKDF2_MAX_ITERATIONS = 5000;
    final static int PBKDF2_DEFAULT_ITERATIONS = 1000;
    final static int PBKDF2_LENGTH = 256;      // 128-bit encryption key (Password-mode)
    final static int PBKDF2_SALT_LENGTH = 16;

    public enum AuthMethod {
        PIN
    }

    public enum EncryptionType {
        KEYSTORE, PASSWORD
    }

    public enum SortMode {
        UNSORTED, LABEL, LAST_USED
    }

    public enum BackupType {
        PLAIN_TEXT, ENCRYPTED, OPEN_PGP
    }

    public enum TagFunctionality {
        OR, AND, SINGLE
    }

    public enum NotificationChannel {
        BACKUP_FAILED, BACKUP_SUCCESS
    }
}
