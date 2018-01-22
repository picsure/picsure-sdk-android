package com.picsure.android_lib.sample;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.picsure.android_lib.Picsure;
import com.picsure.android_lib.PicsureListener;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_TAKE_PHOTO = 1;
    private static final int REQUEST_SELECT_PICTURE = 2;
    public static final int RESULT_OK = -1;
    public static final String TAG = "TAG";
    private AlertDialog mAlertDialog;
    private String mCurrentPhotoPath;


    Button cameraButton;
    Picsure picsure;
    TextView resultTextView;
    ProgressBar progressBar;

    public Uri photoURI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            photoURI = Uri.parse(savedInstanceState.getString("photoUri"));
        }

        cameraButton = (Button) findViewById(R.id.button_photo);
        resultTextView = (TextView) findViewById(R.id.result);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);


        //Check for Starage permissions- enable/disable camera button
        if (isStoragePermissionGranted()) {

            cameraButton.setEnabled(true);

        } else {

            cameraButton.setEnabled(false);

        }

        //Init Picsure SDK
        picsure = new Picsure(this, "YOUR_API_KEY");

        //Set Language to english (ISO 4217)
        picsure.setLanguage("en");


        picsure.setOnEventListener(new PicsureListener() {
            @Override
            public void onResponse(JSONObject response) {



                final JSONObject myResponse = response;

                //Use runnable to Change UI-Elements because called from another Thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        //Hide Spinner
                        progressBar.setVisibility(View.GONE);

                        //Set text color
                        resultTextView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.bootstrap_success));

                        //Parse JSON
                        Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        String json = gson.toJson(myResponse);
                        resultTextView.setText(json);
                    }
                });


                Log.d("Picsure", response.toString());


            }

            @Override
            public void onError(String errorMessage) {


                final String myErrorMessage = errorMessage;

                //Use runnable to Change UI-Elements because called from another Thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {


                        //Hide Spinner
                        progressBar.setVisibility(View.GONE);

                        //Set text color
                        resultTextView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.bootstrap_danger));

                        //Set error to TextView
                        resultTextView.setText(myErrorMessage);

                    }
                });

                Log.e("Picsure", errorMessage);


            }
        });

    }

    public void performClickPhoto(View view) {
        //Take photo by button click
        imageFromCamera();


    }

    public void performClickGallery(View view) {
        //Take photo by button click
        imageFromGallery();


    }


    public void imageFromCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
                mCurrentPhotoPath = photoFile.getAbsolutePath();

            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(this, "Error occured. Please try again later.", Toast.LENGTH_SHORT).show();

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.picsure.android_lib.sample.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }


    private void imageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_SELECT_PICTURE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            progressBar.setVisibility(View.VISIBLE);


            try {

                File photoFile = new File(mCurrentPhotoPath);

                //Send photo to Picsure-API
                picsure.uploadPhoto(photoFile);


            } catch (Exception ex) {

                Log.v("OnCameraCallBack", ex.getMessage());
            }

        }

        if (requestCode == REQUEST_SELECT_PICTURE && resultCode == RESULT_OK
                && data != null && data.getData() != null) {

            progressBar.setVisibility(View.VISIBLE);


            Uri uri = data.getData();
            String[] projection = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(projection[0]);
            String filepath = cursor.getString(columnIndex);
            cursor.close();

            File photoFile = new File(filepath);

            //Send photo to Picsure-API
            picsure.uploadPhoto(photoFile);
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );


        return image;
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (this.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {

                Log.v(TAG, "Permission is granted");
                return true;

            } else {

                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;

            }
        } else { //permission is automatically granted on sdk<23 upon installation

            Log.v(TAG, "Permission is granted");
            return true;

        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);

            cameraButton.setEnabled(true);


        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("photoUri", String.valueOf(photoURI));
        super.onSaveInstanceState(outState);
    }


    /**
     * Requests given permission.
     * If the permission has been denied previously, a Dialog will prompt the user to grant the
     * permission, otherwise it is requested directly.
     */
    protected void requestPermission(final String permission, String rationale, final int requestCode) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            showAlertDialog(getString(R.string.permission_title_rationale), rationale,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{permission}, requestCode);
                        }
                    }, getString(R.string.label_ok), null, getString(R.string.label_cancel));
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        }
    }

    /**
     * This method shows dialog with given title & message.
     * Also there is an option to pass onClickListener for positive & negative button.
     *
     * @param title                         - dialog title
     * @param message                       - dialog message
     * @param onPositiveButtonClickListener - listener for positive button
     * @param positiveText                  - positive button text
     * @param onNegativeButtonClickListener - listener for negative button
     * @param negativeText                  - negative button text
     */
    protected void showAlertDialog(@Nullable String title, @Nullable String message,
                                   @Nullable DialogInterface.OnClickListener onPositiveButtonClickListener,
                                   @NonNull String positiveText,
                                   @Nullable DialogInterface.OnClickListener onNegativeButtonClickListener,
                                   @NonNull String negativeText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(positiveText, onPositiveButtonClickListener);
        builder.setNegativeButton(negativeText, onNegativeButtonClickListener);
        mAlertDialog = builder.show();
    }
}
