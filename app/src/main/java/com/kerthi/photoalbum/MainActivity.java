package com.kerthi.photoalbum;

import android.Manifest;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.developer.filepicker.controller.DialogSelectionListener;
import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int FILE_PICKER_REQUEST_CODE = 100;
    private static final int BUFFER_SIZE = 4096;

    private static final String TAG = "MainActivity";
    private static final String URL_UPLOAD_VIDEO = "https://expensesplit.safeml.de/cgi-bin/photoSharingUpload.py";
    private static final String URL_DOWNLOAD = "https://expensesplit.safeml.de/cgi-bin/photoSharingDownload.py";

    private final UUID ALBUM_UUID = UUID.randomUUID();
    private int currentPhotoiD = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View mainLayout = findViewById(R.id.main);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        mainLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                DialogProperties properties = new DialogProperties();
                String[] extensions = {"jpeg", "jpg"};


                properties.extensions = extensions;
                properties.show_hidden_files = false;
                properties.selection_mode = DialogConfigs.MULTI_MODE;
                FilePickerDialog dialog = new FilePickerDialog(MainActivity.this, properties);
                dialog.setDialogSelectionListener(new DialogSelectionListener() {
                    @Override
                    public void onSelectedFilePaths(String[] files) {

                        for (final String file : files) {
                            Log.d(file, file);
                            Random rand = new Random();

                            final String id = ALBUM_UUID + "-" + String.format("%04d", currentPhotoiD).substring(0, 4);


                            new Thread() {
                                public void run() {
                                    Map<String, String> params = new HashMap<String, String>(2);
                                    params.put("pw", "PilotProjectAtTheISSE22154b");
                                    params.put("id", id);
                                    currentPhotoiD++;

                                    try {
                                        final String result = multipartRequest(URL_UPLOAD_VIDEO, params, file, "file", "video/mp4");

                                        Log.d("result", result);
                                        runOnUiThread(new Runnable() {

                                            @Override
                                            public void run() {
                                                Toast.makeText(getApplicationContext(), "File uploaded :" + result, Toast.LENGTH_SHORT).show();

                                            }
                                        });
                                    } catch (Exception e) {

                                        e.printStackTrace();
                                    }

                                }
                            }.start();


                        }

                        new Thread() {
                            public void run() {
                                try {
                                    downloadFiles();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }


                            }
                        }.start();
                    }
                });
                dialog.setTitle("Select a File");
                dialog.show();

            }
        });
    }

    private void downloadFiles() throws InterruptedException {
        for (int i = 1; i < 10; i++) {
            final String downloadId = ALBUM_UUID + "-" + String.format("%04d", i).substring(0, 4);
            final Map<String, String> params = new HashMap<String, String>(2);
            params.put("pw", "PilotProjectAtTheISSE22154b");
            params.put("id", downloadId);

            Thread.sleep(i * 10000);


            boolean isDownloadSuccessful = false;
            try {
                isDownloadSuccessful = downloadFile(URL_DOWNLOAD, params, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/final" + i + ".jpeg");
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!isDownloadSuccessful) {
                return;
            }
        }


    }


    public boolean downloadFile(String urlTo, Map<String, String> parmas, String saveDir)
            throws IOException {
        String boundary = "*****" + System.currentTimeMillis() + "*****";

        String twoHyphens = "--";
        String lineEnd = "\r\n";

        URL url = new URL(urlTo);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setDoInput(true);
        httpConn.setDoOutput(true);
        httpConn.setUseCaches(false);

        httpConn.setRequestMethod("POST");
        httpConn.setRequestProperty("Connection", "Keep-Alive");
        httpConn.setRequestProperty("User-Agent", "Android Multipart HTTP Client 1.0");
        httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);


        DataOutputStream dataOutputStream = new DataOutputStream(httpConn.getOutputStream());


        // Upload POST Data
        Iterator<String> keys = parmas.keySet().iterator();
        while (keys.hasNext()) {


            String key = keys.next();
            String value = parmas.get(key);

            dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
            dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + lineEnd);
            dataOutputStream.writeBytes("Content-Type: text/plain" + lineEnd);
            dataOutputStream.writeBytes(lineEnd);
            dataOutputStream.writeBytes(value);
            dataOutputStream.writeBytes(lineEnd);
        }


        // opens input stream from the HTTP connection
        InputStream inputStream = httpConn.getInputStream();

        String saveFilePath = saveDir;

        // opens an output stream to save into file
        FileOutputStream fileOutput = new FileOutputStream(saveFilePath);

        int bytesRead = -1;

        byte[] buffer = new byte[BUFFER_SIZE];
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            String result = new String(buffer, StandardCharsets.UTF_8);
            if (result.trim().equals("Error: Photo not found.")) {
                new File(saveFilePath).delete();
                return false;

            }
            Log.wtf("bytesRea", result);
            fileOutput.write(buffer, 0, bytesRead);


        }


        fileOutput.close();
        inputStream.close();
        dataOutputStream.flush();
        dataOutputStream.close();
        Log.d(getClass().getCanonicalName(), "File downloaded");

        httpConn.disconnect();
        return true;
    }

    public String multipartRequestBitmap(String urlTo, Map<String, String> parmas, Bitmap bitmap, String filefield, String fileMimeType, String targetName) throws Exception {
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        InputStream inputStream = null;

        String twoHyphens = "--";
        String boundary = "*****" + Long.toString(System.currentTimeMillis()) + "*****";
        String lineEnd = "\r\n";

        String result = "";

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;


        try {

            int byteSize = bitmap.getRowBytes() * bitmap.getHeight();
            ByteBuffer byteBuffer = ByteBuffer.allocate(byteSize);
            bitmap.copyPixelsToBuffer(byteBuffer);

            byte[] byteArray = byteBuffer.array();

// Get the ByteArrayInputStream.
            ByteArrayInputStream bs = new ByteArrayInputStream(byteArray);

            URL url = new URL(urlTo);
            connection = (HttpURLConnection) url.openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("User-Agent", "Android Multipart HTTP Client 1.0");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);
            outputStream.writeBytes("Content-Disposition: form-data; name=\"" + filefield + "\"; filename=\"" + targetName + "\"" + lineEnd);
            outputStream.writeBytes("Content-Type: " + fileMimeType + lineEnd);
            outputStream.writeBytes("Content-Transfer-Encoding: binary" + lineEnd);

            outputStream.writeBytes(lineEnd);

            bytesAvailable = bs.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            bytesRead = bs.read(buffer, 0, bufferSize);
            while (bytesRead > 0) {
                outputStream.write(buffer, 0, bufferSize);
                bytesAvailable = bs.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = bs.read(buffer, 0, bufferSize);
            }

            outputStream.writeBytes(lineEnd);

            // Upload POST Data
            Iterator<String> keys = parmas.keySet().iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = parmas.get(key);

                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + lineEnd);
                outputStream.writeBytes("Content-Type: text/plain" + lineEnd);
                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(value);
                outputStream.writeBytes(lineEnd);
            }

            outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);


            if (200 != connection.getResponseCode()) {
                throw new Exception("Failed to upload code:" + connection.getResponseCode() + " " + connection.getResponseMessage());
            }

            inputStream = connection.getInputStream();

            result = this.convertStreamToString(inputStream);

            bs.close();
            inputStream.close();
            outputStream.flush();
            outputStream.close();

            return result;
        } catch (Exception e) {
            Log.d("exception while upload", e.getMessage());
            e.printStackTrace();


        }

        return "no response";

    }


    public String multipartRequest(String urlTo, Map<String, String> parmas, String filepath, String filefield, String fileMimeType) throws Exception {
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        InputStream inputStream = null;

        String twoHyphens = "--";
        String boundary = "*****" + Long.toString(System.currentTimeMillis()) + "*****";
        String lineEnd = "\r\n";

        String result = "";

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;

        String[] q = filepath.split("/");
        int idx = q.length - 1;

        try {
            File file = new File(filepath);
            FileInputStream fileInputStream = new FileInputStream(file);

            URL url = new URL(urlTo);
            connection = (HttpURLConnection) url.openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("User-Agent", "Android Multipart HTTP Client 1.0");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);
            outputStream.writeBytes("Content-Disposition: form-data; name=\"" + filefield + "\"; filename=\"" + q[idx] + "\"" + lineEnd);
            outputStream.writeBytes("Content-Type: " + fileMimeType + lineEnd);
            outputStream.writeBytes("Content-Transfer-Encoding: binary" + lineEnd);

            outputStream.writeBytes(lineEnd);

            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            while (bytesRead > 0) {
                outputStream.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            outputStream.writeBytes(lineEnd);

            // Upload POST Data
            Iterator<String> keys = parmas.keySet().iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = parmas.get(key);

                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + lineEnd);
                outputStream.writeBytes("Content-Type: text/plain" + lineEnd);
                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(value);
                outputStream.writeBytes(lineEnd);
            }

            outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);


            if (200 != connection.getResponseCode()) {
                throw new Exception("Failed to upload code:" + connection.getResponseCode() + " " + connection.getResponseMessage());
            }

            inputStream = connection.getInputStream();

            result = this.convertStreamToString(inputStream);

            fileInputStream.close();
            inputStream.close();
            outputStream.flush();
            outputStream.close();

            return result;
        } catch (Exception e) {
            Log.d("exception while upload", e.getMessage());
            e.printStackTrace();


        }

        return "no response";

    }


    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

}