package org.shadowice.flocke.andotp.Helpers.DropboxServices;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class UploadTask extends AsyncTask<String, Void, FileMetadata> {

    private DbxClientV2 dbxClient;
    private File file;
    private Context context;
    private String foldername;
    private final Callback mCallback;
    private Exception mException;

    public interface Callback {
        void onUploadComplete(FileMetadata result);
        void onError(Exception e);
    }

    public UploadTask(DbxClientV2 dbxClient, File file, String foldername, Context context, Callback callback) {
        this.dbxClient = dbxClient;
        this.file = file;
        this.context = context;
        this.foldername = foldername;
        this.mCallback = callback;
    }

    @Override
    protected FileMetadata doInBackground(String... params) {
        try {
            // Upload to Dropbox
            InputStream inputStream = new FileInputStream(file);
            dbxClient.files().uploadBuilder("/" + foldername + "/" + file.getName()) //Path in the user's Dropbox to save the file.
                    .withMode(WriteMode.OVERWRITE) //always overwrite existing file
                    .uploadAndFinish(inputStream);
        } catch (DbxException | IOException e) {
            e.printStackTrace();
            mException = e;
        }
        return null;
    }

    @Override
    protected void onPostExecute(FileMetadata result) {
        super.onPostExecute(result);
        Log.e("onPostExecute", " "+result);
        if (mException != null) {
            mCallback.onError(mException);
        } else {
            mCallback.onUploadComplete(result);
        }
    }
}
