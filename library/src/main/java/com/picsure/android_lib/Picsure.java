package com.picsure.android_lib;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.net.Uri;
import android.os.Build;
import android.support.media.ExifInterface;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.TlsVersion;


public class Picsure {


    private final String APP_TEMP_FOLDER = "picsure_files"; //directory for temporary storage of images from the camera

    private final int PHOTO_UPLOAD_QUALITY_SMALL = 85;
    private final int maxSize = 1024;
    private final MediaType MEDIA_TYPE_JPG = MediaType.parse("image/jpg");
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    //API URLs
    private String apiOrigin;
    private String apiUrl;


    private final String apiKey;
    private ExifInterface exifInterface;
    private Context mContext;
    private String status;
    private String imageId;
    private String imageThumbnailUrl;
    private String imageMediumUrl;
    private String imageLargeUrl;
    private String language;
    private JSONObject jsonResult;
    private String errorMessage;
    private PicsureListener mPicsureListener;
    private JSONObject exifData;


    public Picsure(Context mContext, String apiKey) {
        this.mContext = mContext;
        this.apiKey = apiKey;
        this.apiOrigin = "https://api.picsure.ai";
        this.apiUrl = "https://api.picsure.ai";
        this.language = "en";

    }

    //Getter and Setter
    public String getApiKey() {
        return apiKey;
    }


    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getStatus() {
        return status;
    }

    private void setStatus(String status) {
        this.status = status;
    }

    public String getImageId() {
        return imageId;
    }

    private void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getImageThumbnailUrl() {
        return imageThumbnailUrl;
    }

    public void setImageThumbnailUrl(String imageThumbnailUrl) {
        this.imageThumbnailUrl = imageThumbnailUrl;
    }

    public String getImageMediumUrl() {
        return imageMediumUrl;
    }

    public void setImageMediumUrl(String imageMediumUrl) {
        this.imageMediumUrl = imageMediumUrl;
    }

    public String getImageLargeUrl() {
        return imageLargeUrl;
    }

    public void setImageLargeUrl(String imageLargeUrl) {
        this.imageLargeUrl = imageLargeUrl;
    }

    public JSONObject getJsonResult() {
        return jsonResult;
    }

    private void setJsonResult(JSONObject jsonResult) {
        this.jsonResult = jsonResult;
    }


    public String getErrorMessage() {
        return errorMessage;
    }

    private void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    //End model definiton


    public void uploadPhoto(File file) {

        setStatus("wait");

        //Create File and get URI
        File photoDir = new File(mContext.getFilesDir(), APP_TEMP_FOLDER);

        if (!photoDir.exists()) {

            photoDir.mkdirs();

        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss", java.util.Locale.getDefault());
        String timestamp = simpleDateFormat.format(new Date());

        //Resize file
        File photoResizedFile = new File(photoDir, "photo_resized" + timestamp + ".jpg");

        String filePath = file.getPath();
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);


        //TEMPORARY DISABLED
        //Bitmap resizedBitmap = bitmap;

        //Resize to max 1920 width or height
        Bitmap resizedBitmap = Utils.resize(bitmap, maxSize);

        try {
            //Write file
            FileOutputStream out = new FileOutputStream(photoResizedFile);
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, PHOTO_UPLOAD_QUALITY_SMALL, out);
            out.flush();
            out.close();

            try {
                // copy paste exif information from original file to new
                // file


                InputStream fileStream = mContext.getContentResolver().openInputStream(Uri.fromFile(file));


                try {
                    //File pictureFile = new File(imgDecodableString);
                    if (Build.VERSION.SDK_INT >= 24) {
                        exifInterface = new ExifInterface(fileStream);
                    } else {
                        exifInterface = new ExifInterface(Uri.fromFile(file).getPath());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                exifData = readEXIF(exifInterface);

                if (fileStream != null) {
                    try {
                        fileStream.close();
                    } catch (IOException ignored) {
                    }
                }

            } catch (IOException e) {
                Log.e("Picsure", e.getMessage());
            }


        } catch (Exception e) {
            e.printStackTrace();
        }


        uploadFilePhoto(file);

    }


    public Boolean uploadFilePhoto(File file) {

        //Get protocol
        String[] splittedUrl = apiUrl.split(":");
        String protocol = splittedUrl[0];

        OkHttpClient client;

        //different client-options for HTTPS
        if (protocol.equals("https")) {
            ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2)
                    .cipherSuites(
                            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                            CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                            CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256)
                    .build();


            client = new OkHttpClient.Builder()
                    .connectionSpecs(Collections.singletonList(spec))
                    .build();

        } else {
            client = new OkHttpClient();
        }


        try {
            RequestBody requestBody;


            String jsonString = "";
            try {
                jsonString = new String(exifData.toString().getBytes("UTF-8"), "ISO-8859-1");
            } catch (java.io.UnsupportedEncodingException e) {
                return null;
            }

            if (exifData.length() > 0) {
                requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("upload", file.getName(), RequestBody.create(MEDIA_TYPE_JPG, file))
                        .addFormDataPart("exif", jsonString)
                        .build();

            } else {
                requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("upload", file.getName(), RequestBody.create(MEDIA_TYPE_JPG, file))
                        .build();
            }


            Request request = new Request.Builder()
                    .url(apiUrl + "/1/upload")
                    .addHeader("Accept", "application/json")
                    .addHeader("Origin", apiOrigin)
                    // .addHeader("Content-Type", "multipart/form-data")
                    .addHeader("Authorization", "Bearer " + this.getApiKey())
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {

                    setStatus("fail");
                    setErrorMessage(e.getMessage());

                    if (mPicsureListener != null) {
                        //Error callback
                        mPicsureListener.onError(getErrorMessage());
                    }

                    Log.e("failure", e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {

                    String jsonData = response.body().string();
                    if (response.code() == 404) {

                        setStatus("fail");
                        setErrorMessage("Unauthorized. Please check your API_KEY.");
                        if (mPicsureListener != null) {
                            //Error callback
                            mPicsureListener.onError("Unauthorized. Please check your API_KEY.");
                        }

                    }  else if (response.code() == 500) {

                        setStatus("fail");
                        setErrorMessage("An Error occured - please check your API-KEY or shout a message to support@picsure.ai");
                        if (mPicsureListener != null) {
                            //Error callback
                            mPicsureListener.onError("An Error occured - please check your API-KEY or shout a message to support@picsure.ai");
                        }

                    }  else if (response.code() == 413) {

                        setStatus("fail");
                        setErrorMessage("Image file too large - try to resize the image before uploading");
                        if (mPicsureListener != null) {
                            //Error callback
                            mPicsureListener.onError("Image file too large - try to resize the image before uploading");
                        }
                    } else {

                        try {

                            JSONObject result = new JSONObject(jsonData);

                            if (result.getString("token").length() > 0) {

                                //Set id from server response to item
                                setImageId(result.getString("token"));
                                setImageThumbnailUrl(result.getString("image_thumbnail_url"));
                                setImageMediumUrl(result.getString("image_medium_url"));
                                setImageLargeUrl(result.getString("image_large_url"));

                                //Start requesting lookup-url
                                requestResult();

                            } else {

                                setStatus("fail");
                                setErrorMessage(mContext.getResources().getString(R.string.status_fail_upload));

                                if (mPicsureListener != null) {
                                    mPicsureListener.onError(getErrorMessage()); // event object :)
                                }


                            }

                            Log.d("Response", response.toString());

                        } catch (Throwable t) {
                            if (response.code() == 404) {

                                setStatus("fail");
                                setErrorMessage("Unauthorized. Please check your API_KEY.");

                            } else {

                                setStatus("fail");
                                setErrorMessage(t.getMessage());

                            }


                            if (mPicsureListener != null) {
                                mPicsureListener.onError(getErrorMessage()); // event object :)
                            }

                        } finally {

                            Log.d("response", jsonData);

                        }

                    }

                }
            });

            return true;

        } catch (Exception ex) {
            // Handle the error
            setStatus("fail");


            setErrorMessage(ex.getMessage());

            if (mPicsureListener != null) {
                mPicsureListener.onError(getErrorMessage()); // event object :)
            }

        }

        return false;
    }


    private void requestResult() {
        //Build lookup url
        String serverURL = apiUrl + "/2/lookup/" + getImageId();


        String[] splittedUrl = apiOrigin.split(":");
        String protocol = splittedUrl[0];
        OkHttpClient client;

        //different client-options for HTTPS
        if (protocol.equals("https")) {
            ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2)
                    .cipherSuites(
                            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                            CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                            CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256)
                    .build();


            client = new OkHttpClient.Builder()
                    .connectionSpecs(Collections.singletonList(spec))
                    .build();

        } else {
            client = new OkHttpClient();
        }

        try {

            //Request header data
            Request request = new Request.Builder()
                    .url(serverURL)
                    .addHeader("Accept", "application/json")
                    .addHeader("Origin", apiOrigin)
                    .addHeader("Authorization", "Bearer " + this.getApiKey())
                    .addHeader("accept-language", getLanguage())
                    .get()
                    .build();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {

                    setStatus("fail");
                    setErrorMessage(e.getMessage());

                    Log.e("failure", e.getMessage());

                    if (mPicsureListener != null) {
                        mPicsureListener.onError(getErrorMessage()); // event object
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    int responseCode = response.code();
                    //No result yet - skip 404 error
                    if (responseCode != 404) {

                        String jsonData = response.body().string();

                        try {

                            JSONObject result = new JSONObject(jsonData);

                            //Got result for photo

                            setJsonResult(result);
                            setStatus("success");


                            if (mPicsureListener != null) {
                                //Pass object to onResponse callback
                                mPicsureListener.onResponse(result);

                            }

                            Log.d("Picsure", response.toString());

                        } catch (Throwable t) {


                            setStatus("fail");
                            setErrorMessage(t.getMessage());

                            if (mPicsureListener != null) {
                                mPicsureListener.onError(getErrorMessage()); // event object :)
                            }

                        } finally {

                            Log.d("response", jsonData);

                        }
                    }
                }
            });

        } catch (Exception e) {

            Log.e("Picsure", e.getMessage());
            setStatus("fail");
            setErrorMessage(e.getMessage());

            if (mPicsureListener != null) {
                mPicsureListener.onError(getErrorMessage()); // event object :)
            }
        }

    }


    public void setOnEventListener(PicsureListener listener) {
        mPicsureListener = listener;
    }


    public JSONObject readEXIF(ExifInterface exifInterface) {
        JSONObject exifData = new JSONObject();

        try {
            if (exifInterface.getAttribute("Make") != null) {
                exifData.put("Make", exifInterface.getAttribute("Make"));
            }
            if (exifInterface.getAttribute("Model") != null) {
                exifData.put("Model", exifInterface.getAttribute("Model"));
            }
            if (exifInterface.getAttribute("Software") != null) {
                exifData.put("Software", exifInterface.getAttribute("Software"));
            }
            if (exifInterface.getAttribute("Copyright") != null) {
                exifData.put("Copyright", exifInterface.getAttribute("Copyright"));
            }
            if (exifInterface.getAttribute("DateTimeOriginal") != null) {
                exifData.put("DateTimeOriginal", exifInterface.getAttribute("DateTimeOriginal"));
            }
            if (exifInterface.getAttribute("DateTimeDigitized") != null) {
                exifData.put("DateTimeDigitized", exifInterface.getAttribute("DateTimeDigitized"));
            }
            if (exifInterface.getAttribute("DateTime") != null) {
                exifData.put("ModifyDate", exifInterface.getAttribute("DateTime"));
            }
            if (exifInterface.getAttribute("DateTimeDigitized") != null) {
                exifData.put("CreateDate", exifInterface.getAttribute("DateTimeDigitized"));
            }
            if (exifInterface.getAttribute("Orientation") != null) {
                exifData.put("Orientation", exifInterface.getAttribute("Orientation"));
            }

            JSONObject gpsInfoObject = new JSONObject();

            if (exifInterface.getAttribute("GPSVersionID") != null) {
                    // Convert from Unicode to UTF-8
                    String versionId = unicodeToUTF8(exifInterface.getAttribute("GPSVersionID"));
                    gpsInfoObject.put("GPSVersionID",  versionId);
            }

            if (exifInterface.getAttribute("GPSLatitudeRef") != null) {

                gpsInfoObject.put("GPSLatitudeRef", exifInterface.getAttribute("GPSLatitudeRef"));
            }
            if (exifInterface.getAttribute("GPSLongitudeRef") != null) {
                gpsInfoObject.put("GPSLongitudeRef", exifInterface.getAttribute("GPSLongitudeRef"));
            }
            if (exifInterface.getAttribute("GPSLongitude") != null) {
                try {
                    String lng = exifInterface.getAttribute("GPSLongitude");
                    String[] arrayStringLng = lng.split(",");
                    JSONArray convertedArrayStringLng = new JSONArray();

                    for( int i = 0; i < arrayStringLng.length; i++) {
                        String element = arrayStringLng[i];
                        String[] arrayElement = element.split("/");
                        convertedArrayStringLng.put(i, Integer.parseInt(arrayElement[0]));

                    }

                    gpsInfoObject.put("GPSLongitude", convertedArrayStringLng);
                } catch(java.lang.NullPointerException e) {
                    Log.d("Picsure", e.getMessage());
                }

            }
            if (exifInterface.getAttribute("GPSLatitude") != null) {
                try {
                    String lng = exifInterface.getAttribute("GPSLatitude");
                    String[] arrayStringLng = lng.split(",");
                    JSONArray convertedArrayStringLng = new JSONArray();

                    for( int i = 0; i < arrayStringLng.length; i++) {
                        String element = arrayStringLng[i];
                        String[] arrayElement = element.split("/");
                        convertedArrayStringLng.put(i, Integer.parseInt(arrayElement[0]));

                    }
                    gpsInfoObject.put("GPSLatitude", convertedArrayStringLng);

                } catch(java.lang.NullPointerException e) {
                    Log.d("Picsure", e.getMessage());
                }

            }
            if (exifInterface.getAttribute("GPSAltitudeRef") != null) {
                gpsInfoObject.put("GPSAltitudeRef", exifInterface.getAttribute("GPSAltitudeRef"));
            }
            if (exifInterface.getAttribute("GPSAltitude") != null) {
                gpsInfoObject.put("GPSAltitude", exifInterface.getAttribute("GPSAltitude"));
            }
            if (exifInterface.getAttribute("GPSTimeStamp") != null) {
                gpsInfoObject.put("GPSTimeStamp", exifInterface.getAttribute("GPSTimeStamp"));
            }
            if (exifInterface.getAttribute("GPSDate") != null) {
                gpsInfoObject.put("GPSDate", exifInterface.getAttribute("GPSDate"));
            }

            if(gpsInfoObject.length() > 0) {
               exifData.put("GPSInfo", gpsInfoObject);
            }


        } catch (JSONException e) {
            setStatus("fail");
            setErrorMessage(e.getMessage());

            if (mPicsureListener != null) {
                mPicsureListener.onError(getErrorMessage()); // event object :)
            }
        }

        return exifData;



    }

    public static String unicodeToUTF8(String unicodeStr) {
        // Convert from Unicode to UTF-8
        String UTF8Str = "";
        try {
            byte[] utf8 = unicodeStr.getBytes("UTF-8");

            UTF8Str = String.format("%d.%d.%d.%d", utf8[0], utf8[1], utf8[2], utf8[3]);


        } catch (java.io.UnsupportedEncodingException e) {

        }
        return UTF8Str;
    }


}
