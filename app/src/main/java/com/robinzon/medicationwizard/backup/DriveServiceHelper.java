package com.robinzon.medicationwizard.backup;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Helper class to interact with Google Drive API.
 */
public class DriveServiceHelper {

    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;

    public DriveServiceHelper(Drive driveService) {
        mDriveService = driveService;
    }

    /**
     * Uploads a file to Google Drive. If a file with the same name exists, it updates it.
     */
    public Task<String> uploadFile(String fileName, String content) {
        return Tasks.call(mExecutor, () -> {
            File metadata = new File()
                    .setParents(Collections.singletonList("appDataFolder"))
                    .setName(fileName)
                    .setMimeType("application/json");

            ByteArrayContent contentStream = ByteArrayContent.fromString("application/json", content);

            // Check if file exists
            String existingId = findFileId(fileName);
            if (existingId != null) {
                // Update
                File updatedFile = mDriveService.files().update(existingId, null, contentStream).execute();
                return updatedFile.getId();
            } else {
                // Create
                File googleFile = mDriveService.files().create(metadata, contentStream).execute();
                if (googleFile == null) {
                    throw new IOException("Null drawing when requesting file creation.");
                }
                return googleFile.getId();
            }
        });
    }

    /**
     * Downloads a file from Google Drive.
     */
    public Task<String> downloadFile(String fileId) {
        return Tasks.call(mExecutor, () -> {
            try (java.io.InputStream is = mDriveService.files().get(fileId).executeMediaAsInputStream();
                 java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is))) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                return stringBuilder.toString();
            }
        });
    }

    /**
     * Finds a file ID by name in the appDataFolder.
     */
    public String findFileId(String fileName) throws IOException {
        FileList result = mDriveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '" + fileName + "'")
                .execute();
        if (result.getFiles() != null && result.getFiles().size() > 0) {
            return result.getFiles().get(0).getId();
        }
        return null;
    }
}