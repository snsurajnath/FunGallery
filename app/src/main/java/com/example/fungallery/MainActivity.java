package com.example.fungallery;

import androidx.annotation.LongDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Permission;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Set;


import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int TAKE_PHOTO = 101;
    public static final int CREATE_IMAGE = 14;
    RecyclerView recyclerView;
    FloatingActionButton cameraButton;
    ArrayList<Image> imageArrayList;
    Adapter adapter;
    int columns = 3;
    StaggeredGridLayoutManager staggeredGridLayoutManager;
    boolean denied = false;
    String currentPhotoPath;
    Dialog dialog;
    Bitmap bitmap = null;
    ProgressBar progressBar;
    ImageView imageView;
    RelativeLayout relativeLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        layOut();
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        Toast.makeText(this, "Long Pressed", Toast.LENGTH_SHORT).show();
        return super.onKeyLongPress(keyCode, event);
    }

    void layOut() {
        staggeredGridLayoutManager = new StaggeredGridLayoutManager(columns, LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(staggeredGridLayoutManager);
        ImageView emptyView = findViewById(R.id.emptyView);
        imageArrayList = new ArrayList<>();

        if (imageArrayList.size() == 0) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }

        imageArrayList = getImage();
        Collections.reverse(imageArrayList);
        recyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        adapter = new Adapter(MainActivity.this, imageArrayList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        relativeLayout = findViewById(R.id.mainActivity);

    }

    private ArrayList<Image> getImage() {
        ArrayList<Image> imageList = new ArrayList<>();
        ContentResolver imageResolver = getContentResolver();
        Uri imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor imageCursor = imageResolver.query(imageUri, null, null, null, null);
        if (imageCursor != null && imageCursor.moveToFirst()) {
            // path uri
            int imageCol = imageCursor.getColumnIndex(MediaStore.Images.Media.DATA);
            //content uri
            long id = imageCursor.getLong(imageCursor.getColumnIndexOrThrow(BaseColumns._ID));
            do {
                String pathId = imageCursor.getString(imageCol);
                Uri uri = Uri.parse(pathId);
                Uri cUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                Log.d("###qq", String.valueOf(cUri));
                Log.d("###qq", String.valueOf(uri));
                imageList.add(new Image(uri));
            }
            while (imageCursor.moveToNext());
        }
        return imageList;
    }

    private void initUI() {
        recyclerView = findViewById(R.id.recyclerView);
        cameraButton = findViewById(R.id.camera);
        progressBar = findViewById(R.id.progressBar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.more_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.newer:
                Collections.reverse(imageArrayList);
                adapter.notifyDataSetChanged();
                return true;

            case R.id.older:
                Collections.reverse(imageArrayList);
                adapter.notifyDataSetChanged();
                return true;

            case R.id.two:
                columns = 2;
                layOut();
                adapter.notifyDataSetChanged();
                return true;

            case R.id.three:
                columns = 3;
                layOut();
                adapter.notifyDataSetChanged();
                return true;

            case R.id.four:
                columns = 4;
                layOut();
                adapter.notifyDataSetChanged();
                return true;

            case R.id.five:
                columns = 5;
                layOut();
                adapter.notifyDataSetChanged();
                return true;
            case R.id.setting:
                goToSettings();
                break;
            case R.id.refresh:
                progressBar.setVisibility(View.VISIBLE);
                layOut();
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                break;

        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onStart() {
        askStoragePermission();
        adapter.notifyDataSetChanged();
        super.onStart();
    }


    private void goToSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.parse("package:" + getPackageName());
        intent.setData(uri);
        startActivity(intent);
    }

    public void openCamera(View view) {
        askCameraPermission();

    }


    // permission check

    private void askCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{CAMERA}, TAKE_PHOTO);
        } else {
            openCameraApp();
        }
    }

    private void askStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        } else {

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == TAKE_PHOTO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCameraApp();
            } else {
                Toast.makeText(this, "Camera permission required to take picture", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Fun Gallery", Toast.LENGTH_SHORT).show();
                layOut();
            } else {
                Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show();
//                askStoragePermission();

            }
        }

    }

    private void openCameraApp() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Intent saving 
            //createImage();

            // Create the File where the photo should go
            File photoFile = null;
            //API > 29
            //photoFile = createImageFile();
            //photoFile = new File(currentPhotoPath);
            //Log.d("PHOTO_FILE", photoFile.toString());
            // Continue only if the File was successfully created
            //if (photoFile != null) {
            //scanFile(this, new File(currentPhotoPath), "image/*");
            Uri photoURI = saveImage();
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, TAKE_PHOTO);
//            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("ResultActivity", "Result");

        if (requestCode == TAKE_PHOTO) {
            AssetFileDescriptor fileDescriptor = null;
            long fileSize = 0;
            try {
                fileDescriptor = getApplicationContext().getContentResolver().openAssetFileDescriptor(Uri.parse(currentPhotoPath), "r");
                fileSize = fileDescriptor.getLength();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            if (fileSize == 0) {
                try {
                    deleteCacheImage(Uri.parse(currentPhotoPath));
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }

            }
            if (resultCode == Activity.RESULT_OK) {
                layOut();
                Log.d("SIZE_FILE", String.valueOf(fileSize));
                if (fileSize != 0) {
                    Toast.makeText(this, "Image captured", Toast.LENGTH_SHORT).show();
                    File imgFile = new File(currentPhotoPath);
                    dialog = new Dialog(MainActivity.this, R.style.DialogBox) {
                        public boolean dispatchTouchEvent(MotionEvent event) {
                            dialog.dismiss();
                            return false;
                        }
                    };
                    dialog.setContentView(R.layout.image_preview);

                    imageView = dialog.findViewById(R.id.imageView);
                    Glide.with(dialog.getContext())
                            .load(Uri.parse(currentPhotoPath))
                            .error(R.drawable.ic_launcher_foreground)
                            .into(imageView);
                    dialog.show();

                    galleryAddPicNotify(imgFile);
                }
            }
        }

        if (requestCode == adapter.DELETE_REQUEST_CODE) {
            if (resultCode != 0) {
                adapter.notifyItemRemoved(adapter.getPos());
            }
        }


        if (requestCode == CREATE_IMAGE) {
            Toast.makeText(this, "Image saved", Toast.LENGTH_SHORT).show();
        }
    }


    // saving picture API < 29
    private File createImageFile() throws IOException {
        // Create an Image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFilename = String.valueOf(System.currentTimeMillis());
//        File storageDirectory = getExternalFilesDir("FunApp");
        File volNames = getExternalFilesDir(null);
        Log.d("VOL_NAMES_", volNames.getAbsolutePath());
        String[] name = volNames.getAbsolutePath().split("/");
        String path = "/" + name[1] + "/" + name[2] + "/" + name[3] + "/Pictures";
        Log.d("VOL_NAMES_", "/" + name[1] + "/" + name[2] + "/" + name[3]);
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        Log.d("VOL_NAMES_", storageDirectory.toString());
        File dir = new File(path + "/Fun Gallery/");

        Log.d("VOL_NAMES_", dir.getAbsolutePath());

        if (!dir.exists())
            dir.mkdirs();
        // creating and saving image file to Fun gallery folder
        File image = File.createTempFile(
                imageFilename,// prefix
                ".jpg",//extension or suffix
                dir// storage directory
        );
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPicNotify(File imgFile) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imgFile.getAbsolutePath());
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }


    // save image by Intent parsing
    private void createImage() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
//                .addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_TITLE, "file1.jpg");
        startActivityForResult(intent, CREATE_IMAGE);

    }

    // to notify storage that new file is created/ added
    public void scanFile(Context ctxt, File f, String mimeType) {
        MediaScannerConnection
                .scanFile(ctxt, new String[]{f.getAbsolutePath()},
                        new String[]{mimeType}, null);
    }

    // save image for API <= 29
    private Uri saveImage() {
        ContentResolver contentResolver = getContentResolver();
        String imageFilename = String.valueOf(System.currentTimeMillis());

        Uri imageCollection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            imageCollection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, imageFilename + ".jpg");
        /**
         * if you use VOLUME_EXTERNAL_PRIMARY
         * Pictures/ or DCIM/ only
         *contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/"+"My Custom Directory");
         */
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + "Fun Zone/");
        Uri uri = contentResolver.insert(imageCollection, contentValues);
        File imgFile = new File(String.valueOf(uri));
        Log.d("SAVED_IMAGE", uri.toString());
        Log.d("SAVED_IMAGE", imgFile.getAbsolutePath());
        Log.d("SAVED_IMAGE", imageCollection.toString());
        currentPhotoPath = String.valueOf(uri);
        return uri;
    }

    private void deleteCacheImage(Uri uri) throws IntentSender.SendIntentException {
        // Remove a specific media item.
        ContentResolver resolver = getContentResolver();

        // URI of the image to remove.
        Uri imageUri = Uri.parse(currentPhotoPath);

        // WHERE clause.
        String selection = "...";
        String[] selectionArgs = null;

        // Perform the actual removal.
        int numImagesRemoved = resolver.delete(
                imageUri,
                null,
                null);
    }
}
