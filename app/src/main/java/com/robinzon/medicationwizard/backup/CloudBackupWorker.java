package com.robinzon.medicationwizard.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.robinzon.medicationwizard.utils.Logger;

import java.util.Collections;

/**
 * Background worker to perform cloud backups periodically.
 */
public class CloudBackupWorker extends Worker {

    public CloudBackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Logger.log("CloudBackupWorker", "Periodic backup task started");
        
        Context context = getApplicationContext();
        
        if (!com.robinzon.medicationwizard.AppConfig.isPremium(getApplicationContext())) {
            Logger.log("CloudBackupWorker", "User is not premium, skipping backup");
            return Result.success();
        }

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        
        if (account == null) {
            Logger.log("CloudBackupWorker", "No signed in account, skipping backup");
            return Result.success();
        }

        try {
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    context, Collections.singleton(DriveScopes.DRIVE_APPDATA));
            credential.setSelectedAccount(account.getAccount());

            Drive googleDriveService = new Drive.Builder(
                    new NetHttpTransport(),
                    new GsonFactory(),
                    credential)
                    .setApplicationName("Medication Wizard")
                    .build();

            DriveServiceHelper driveHelper = new DriveServiceHelper(googleDriveService);
            CloudBackupManager manager = new CloudBackupManager(context, driveHelper);

            // This is synchronous in the worker
            Tasks.await(manager.backupToCloud());
            
            Logger.log("CloudBackupWorker", "Periodic backup successful");
            return Result.success();
        } catch (Exception e) {
            Logger.log("CloudBackupWorker", "Periodic backup failed: " + e.getMessage());
            return Result.retry();
        }
    }
}
