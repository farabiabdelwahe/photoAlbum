package com.kerthi.photoalbum;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.ParsedRequestListener;
import com.androidnetworking.interfaces.StringRequestListener;
import com.androidnetworking.interfaces.UploadProgressListener;
import com.developer.filepicker.controller.DialogSelectionListener;
import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;

import org.json.JSONObject;

import java.io.File;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final int FILE_PICKER_REQUEST_CODE = 100;
    private static final String TAG ="MainActivity" ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View mainLayout = findViewById(R.id.main);


        mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogProperties properties = new DialogProperties();
                String[] extensions = {"jpeg" ,"jpg" };



                properties.extensions = extensions;
                properties.show_hidden_files = false;
                properties.selection_mode = DialogConfigs.MULTI_MODE ;
                FilePickerDialog dialog = new FilePickerDialog(MainActivity.this,properties);
                dialog.setDialogSelectionListener(new DialogSelectionListener() {
                    @Override
                    public void onSelectedFilePaths(String[] files) {

                        for (String file : files) {
                            Log.d(file ,file);
                            UUID uuid = UUID.randomUUID();
                            Random rand = new Random();

                            String id = uuid + "-"+String.format("%04d", rand.nextInt(10000));

                            AndroidNetworking.upload("https://expensesplit.safeml.de/cgi-bin/photoSharingUpload.py")
                                    .addMultipartFile("file",new File(file))
                                    .addMultipartParameter("pw","PilotProjectAtTheISSE22154b")
                                    .addMultipartParameter("id",id)

                                    .setTag("uploadTest")
                                    .setPriority(Priority.HIGH)
                                    .build()
                                    .setUploadProgressListener(new UploadProgressListener() {
                                        @Override
                                        public void onProgress(long bytesUploaded, long totalBytes) {
                                            // do anything with progress
                                        }
                                    })
                                    .getAsString( new StringRequestListener() {
                                        @Override
                                        public void onResponse(String user) {
                                            // do anything with response
                                            Log.d(TAG, user);

                                        }
                                        @Override
                                        public void onError(ANError anError) {
                                            // handle error
                                            Log.d(TAG, anError.getMessage());

                                        }
                                    });
                        }
                     }
                });
                dialog.setTitle("Select a File");
                dialog.show();

            }
        });
    }
}