package org.shadowice.flocke.andotp.Helpers;

import android.net.Uri;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.annotation.Nullable;

public class DriveServiceHelper {

    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;

    public DriveServiceHelper(Drive driveService) {
        mDriveService = driveService;
    }

    public Task<String> createFolder(String foldername) {
        return Tasks.call(mExecutor, () -> {
            File body = new File();
            body.setName(foldername);
            body.setMimeType("application/vnd.google-apps.folder");
            File googleFile = mDriveService.files().create(body).execute();

            if (googleFile == null) {
                throw new IOException("Null result when requesting file creation.");
            }

            return googleFile.getId();

        });
    }

    public Task<File> getExistsFolder(String title, String parentId) {

        return Tasks.call(mExecutor, () -> {
            Drive.Files.List request;
            request = mDriveService.files().list();
            String query = "mimeType='application/vnd.google-apps.folder' AND trashed=false AND name='" + title + "' AND '" + parentId + "' in parents";
            request = request.setQ(query);
            FileList files = request.execute();
            if (files.getFiles().size() == 0) //if the size is zero, then the folder doesn't exist
                return null;
            else
                //since google drive allows to have multiple folders with the same title (name)
                //we select the first file in the list to return
                return files.getFiles().get(0);
        });

    }

    public Task<File> getExistsFilesFromFolder(String mimeType, String title, String parentId) {

        return Tasks.call(mExecutor, () -> {
            Drive.Files.List request;
            request = mDriveService.files().list();
            String query = "mimeType='" + mimeType + "' AND trashed=false AND name='" + title + "' AND '" + parentId + "' in parents";
            request = request.setQ(query);
            FileList files = request.execute();
            if (files.getFiles().size() == 0) //if the size is zero, then the folder doesn't exist
                return null;
            else
                //since google drive allows to have multiple folders with the same title (name)
                //we select the first file in the list to return
                return files.getFiles().get(0);
        });

    }

 /*   public Task<FileList> queryFiles() {
        return Tasks.call(mExecutor, () ->
                mDriveService.files().list().setSpaces("drive").execute());
    }*/

    public Task<Void> deleteFile(String fileid) {
        return Tasks.call(mExecutor, () -> mDriveService.files().delete(fileid).execute());
    }

    /**
     * Creates a text file in the user's My Drive folder and returns its file ID.
     */
    public Task<String> createFile(String mimeType, Uri uri, String filename, String folderid) {
        return Tasks.call(mExecutor, () -> {
            File metadata = new File()
                    .setParents(Collections.singletonList(folderid))
                    .setMimeType(mimeType)
                    .setName(filename);

            java.io.File filePath = new java.io.File(uri.getPath());
            FileContent mediaContent = new FileContent(mimeType, filePath);

            File googleFile = mDriveService.files().create(metadata, mediaContent).execute();
            if (googleFile == null) {
                throw new IOException("Null result when requesting file creation.");
            }

            return googleFile.getId();
        });
    }

    public Task<byte[]> readBytesFromGoogleDrive(String fileId) {
        return Tasks.call(mExecutor, () -> {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try {

                InputStream is = mDriveService.files().get(fileId).executeMediaAsInputStream();

                try {
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = is.read(buffer)) != -1) {
                        bytes.write(buffer, 0, count);
                    }
                    return bytes.toByteArray();
                } finally {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bytes.toByteArray();
        });
    }

    public Task<List<GoogleDriveFileHolder>> queryFiles(@Nullable final String folderId, String mimeType) {

        return Tasks.call(mExecutor, new Callable<List<GoogleDriveFileHolder>>() {
                    @Override
                    public List<GoogleDriveFileHolder> call() throws Exception {
                        List<GoogleDriveFileHolder> googleDriveFileHolderList = new ArrayList<>();
                        String parent = "root";
                        if (folderId != null) {
                            parent = folderId;
                        }

//                        FileList result = mDriveService.files().list().setQ("'" + parent + "' in parents").setFields("files(id, name,size,createdTime,modifiedTime,starred)").setSpaces("drive").execute();

                        Drive.Files.List request;
                        request = mDriveService.files().list();
                        String query = "mimeType='" + mimeType + "' AND trashed=false AND '" + parent + "' in parents";
                        request = request.setQ(query);
                        FileList result = request.execute();

                        for (int i = 0; i < result.getFiles().size(); i++) {

                            GoogleDriveFileHolder googleDriveFileHolder = new GoogleDriveFileHolder();
                            googleDriveFileHolder.setId(result.getFiles().get(i).getId());
                            googleDriveFileHolder.setName(result.getFiles().get(i).getName());
                            if (result.getFiles().get(i).getSize() != null) {
                                googleDriveFileHolder.setSize(result.getFiles().get(i).getSize());
                            }

                            if (result.getFiles().get(i).getModifiedTime() != null) {
                                googleDriveFileHolder.setModifiedTime(result.getFiles().get(i).getModifiedTime());
                            }

                            if (result.getFiles().get(i).getCreatedTime() != null) {
                                googleDriveFileHolder.setCreatedTime(result.getFiles().get(i).getCreatedTime());
                            }

                            if (result.getFiles().get(i).getStarred() != null) {
                                googleDriveFileHolder.setStarred(result.getFiles().get(i).getStarred());
                            }

                            googleDriveFileHolderList.add(googleDriveFileHolder);

                        }

                        return googleDriveFileHolderList;

                    }
                }
        );
    }

}