package com.example.gender_rec;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.util.Locale;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {
    // API
    private final String API_ENDPOINT = "https://www.picpurify.com/analyse/1.1?task=face_gender_age_detection&API_KEY=MzAmuAp34vDDMpsAH4kaXIdQHfxRkPLT&url_image=";



    //Permission constants
    private  static final int CAMERA_REQUEST_CODE=100;
    private  static final int STORAGE_REQUEST_CODE=200;
    private  static final int IMAGE_PICK_CAMERA_REQUEST_CODE=300;
    private  static final int IMAGE_PICK_GALLERY_REQUEST_CODE=400;


    //Storage
    StorageReference storageReference;
    FirebaseStorage firebaseStorage;
    //ARRAY
    String[] cameraPermission,storagePermission;

    Uri imageUri;
    String url;
    //view
    FrameLayout frame_Layout;
    TextView gender;
    String gender_s,gender_show;
    ProgressDialog progressDialog;
    ImageView avtr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

       //Init array permission
        cameraPermission=new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission=new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};


        //Init view
        frame_Layout=findViewById(R.id.frameLayout);
        gender=findViewById(R.id.gender);
        avtr=findViewById(R.id.avatar);

        progressDialog=new ProgressDialog(this);
        progressDialog.setTitle("wait, Verifying gender");
        progressDialog.setMessage("please wait.....");

        //g_Textview.setVisibility(View.GONE);

        //init firebase storage
        firebaseStorage=FirebaseStorage.getInstance();
        storageReference=firebaseStorage.getReference();
        frame_Layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickDialog();

            }
        });
    }

    private void showImagePickDialog() {
        String[] option={"Camera","Gallery"};
        AlertDialog.Builder alert=new AlertDialog.Builder(this);
        alert.setTitle("Pick Image From");
        alert.setItems(option, new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                if(which==0){
                    if(!checkCameraPermission()){
                        requestCameraPermission();
                    }else {
                        pickFromCamera();

                    }
                }else if(which==1){
                    if(!checkStoragePermission()){
                        requestStoragePermission();
                    }else {
                        pickFromGallery();
                    }
                }
            }
        });
        //create and show dialog
        alert.create().show();
    }


    //permission handling


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onStart() {
        super.onStart();
        if(!checkCameraPermission()){
            requestCameraPermission();
        }
        if(!checkStoragePermission()){
            requestStoragePermission();
        }
    }

    //Permission func
    private boolean checkStoragePermission(){

        return  ContextCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.WRITE_EXTERNAL_STORAGE)==(PackageManager.PERMISSION_GRANTED);
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestStoragePermission(){
        requestPermissions(storagePermission,STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        boolean result=ContextCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.CAMERA)==(PackageManager.PERMISSION_GRANTED);
        boolean result1=ContextCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.WRITE_EXTERNAL_STORAGE)==(PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestCameraPermission(){
        requestPermissions(cameraPermission,CAMERA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;


                    if (cameraAccepted && storageAccepted) {
                        pickFromCamera();
                    } else {

                        Toast.makeText(this, "please enable camera and storage permission", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE:{
                if (grantResults.length > 0) {
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;


                    if (storageAccepted) {
                        pickFromGallery();
                    } else {

                        Toast.makeText(this, "please enable  storage permission", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK){
            if(requestCode==IMAGE_PICK_GALLERY_REQUEST_CODE){
                imageUri=data.getData();
                uploadPhoto(imageUri);
            }
            if(requestCode==IMAGE_PICK_CAMERA_REQUEST_CODE){
                uploadPhoto(imageUri);
            }
        }
    }

    private void uploadPhoto(Uri uri) {
        progressDialog.show();
        StorageReference storageRef=storageReference.child("image");
        storageRef.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Task<Uri> uriTask=taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isSuccessful() );
                Uri downloadUri=uriTask.getResult();
                //getting url to pass
               url=downloadUri.toString();
               String final_url=API_ENDPOINT+url;

               gender_show=networking(final_url);

                try {
                    Picasso.get().load(url).placeholder(R.drawable.ic_face_).into(avtr);
                    Log.d("snjy",gender_show);
                }
                catch (Exception e){
                    //empty
                }
                //use handler
                Handler handler=new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        gender.setText(gender_show);
                       // g_Textview.setVisibility(View.VISIBLE);
                        Toast.makeText(getApplicationContext(),"Verified",Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                },3000);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pickFromGallery() {
        Intent GalleryIntent=new Intent(Intent.ACTION_PICK);
        GalleryIntent.setType("image/*");
        startActivityForResult(GalleryIntent,IMAGE_PICK_GALLERY_REQUEST_CODE);
    }

    private void pickFromCamera() {
        ContentValues values=new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION,"Temp Description");

        imageUri=getApplicationContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        startActivityForResult(cameraIntent,IMAGE_PICK_CAMERA_REQUEST_CODE);
    }

    //API_network
    private String  networking(String url){
        AsyncHttpClient client=new AsyncHttpClient();
        client.get(url, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.d("myApp","JSON:"+response.toString());

                try{

                    gender_s=response.getJSONObject("face_detection").getJSONArray("results").getJSONObject(0).getJSONObject("gender").getString("decision");
                    gender_show=gender_s.toUpperCase(Locale.ROOT);
                    gender.setText(gender_show);
                }catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Log.d("myApp","Request FAIL:"+statusCode);
                Log.d("myApp","Response FAIL:"+errorResponse);
                Log.d("myApp","Error:"+throwable.toString());

            }
        });
        return gender_show;
    }
}