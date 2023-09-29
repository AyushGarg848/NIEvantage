package com.example.nievantage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class UploadImage extends AppCompatActivity {
    private CardView selectImage;
    private ImageView galleyImageView;
    private final int REQ=1;
    private Bitmap bitmap;
    private Spinner imageCategory;
    private Button uploadImageBtn;
    private String category;
    private ProgressDialog pd;
    private DatabaseReference reference;
    private StorageReference storageReference;
    private String downloadURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_image);
        selectImage=findViewById(R.id.addGalleryImage);
        galleyImageView=findViewById(R.id.galleryImageView);
        imageCategory=findViewById(R.id.imageCategory);
        uploadImageBtn=findViewById(R.id.uploadImageBtn);
        pd=new ProgressDialog(this);
        reference= FirebaseDatabase.getInstance().getReference().child("Gallery");
        storageReference= FirebaseStorage.getInstance().getReference().child("Gallery");
        // Database & Storage Reference Child are already defined
        String[] items=new String[]{"Select Category","Admin Block","Golden Jubilee","Diamond Jubilee","Hostel","Others"};
        imageCategory.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,items));
        imageCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                category=imageCategory.getSelectedItem().toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Toast.makeText(UploadImage.this, "Please Select any Category", Toast.LENGTH_SHORT).show();
            }
        });
        selectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGalley();
            }
        });
        uploadImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bitmap==null){ //If Image is NOT Selected
                    Toast.makeText(UploadImage.this, "Please Select an Image", Toast.LENGTH_SHORT).show();
                }
                else if(category.equals("Select Category")){ //If Category is NOT Selected
                    Toast.makeText(UploadImage.this, "Please Select any Category", Toast.LENGTH_SHORT).show();
                }
                else{
                    pd.setMessage("Uploading...");
                    pd.show();
                    uploadImage();
                }
            }
        });
    }

    private void uploadImage() {
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,50,baos);
        byte[] finalImage=baos.toByteArray();
        final StorageReference filePath;
        filePath=storageReference.child(finalImage+"jpg");
        final UploadTask uploadTask=filePath.putBytes(finalImage);
        uploadTask.addOnCompleteListener(UploadImage.this, new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if(task.isSuccessful()){
                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    downloadURL=String.valueOf(uri);
                                    uploadData();
                                }
                            });
                        }
                    });
                }
                else{
                    pd.dismiss();
                    Toast.makeText(UploadImage.this, "Something went Wrong :(", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void uploadData() {
        // A new DatabaseReference variable is defined, to ensure that child doesn't Stack into one another
        DatabaseReference newReference=reference.child(category);
        final String uniqueKey=newReference.push().getKey();
        // Only Image is passed into the Database unlike UploadNotice
        newReference.child(uniqueKey).setValue(downloadURL).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                pd.dismiss();
                Toast.makeText(UploadImage.this, "Success, Image Uploaded :)", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pd.dismiss();
                Toast.makeText(UploadImage.this, "Something went Wrong :(", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openGalley() {
        Intent i=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i,REQ);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==REQ && resultCode==RESULT_OK){
            Uri uri=data.getData();
            try {
                bitmap=MediaStore.Images.Media.getBitmap(getContentResolver(),uri);
            } catch (IOException e) {
                Toast.makeText(this, "Something went Wrong :(", Toast.LENGTH_SHORT).show();
                throw new RuntimeException();
            }
            galleyImageView.setImageBitmap(bitmap);
        }
    }
}