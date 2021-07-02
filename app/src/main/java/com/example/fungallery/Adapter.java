package com.example.fungallery;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

    public static final int DELETE_REQUEST_CODE = 13;
    Context context;
    ArrayList<Image> imageArrayList;
    Dialog dialog;
    private boolean isImagePressed = false;
    public int pos = 0;
    int getPos(){
        return this.pos;
    }

    public Adapter(Context context, ArrayList<Image> imageArrayList) {
        this.context = context;
        this.imageArrayList = imageArrayList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_grid_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Adapter.ViewHolder holder, int position) {
        Image image = imageArrayList.get(position);
//        holder.image.setImageURI(image.getImageUri());
        Glide.with(context).
                load(Uri.fromFile(new File(image.getImageUri().toString())))
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(holder.image);


        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
//                showImage(image);
                alert(image, position);
                return true;
            }

        });

    }


    void requestDeletePermission(List<Uri> uriList) {
        PendingIntent pendingIntent = MediaStore.createDeleteRequest(context.getContentResolver(), uriList);
        try {
            Activity activity = (Activity) context;
            activity.startIntentSenderForResult(pendingIntent.getIntentSender(), 10, null, 0, 0,
                    0, null);
        } catch (Exception e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void alert(Image image, Integer position) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(context);
//        builder1.setMessage("Do you want to delete image ?");
        builder.setCancelable(true);
//
//      
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialoglayout = inflater.inflate(R.layout.image_preview, null);
        builder.setView(dialoglayout);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        ImageView imageView = dialoglayout.findViewById(R.id.imageView);
        ImageView delete = dialoglayout.findViewById(R.id.delete);

        Glide.with(dialoglayout.getContext())
                .load(Uri.fromFile(new File(image.getImageUri().toString())))
                .error(R.drawable.ic_launcher_foreground)
                .into(imageView);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

  
//                 1st thing:
//                 file uri --> content uri
//                 /storage/emulated/0/Pictures/image_name.jpg // file uri
//                 content://media/external/image/media/114 // content uri

                Uri uri = getContentUriId(image.getImageUri());
                try {
                    deleteAPI28(uri, context);
                    notifyItemRemoved(position);
                    alertDialog.dismiss();
                }catch (Exception e){
                    //  PendingIntent createDeleteRequest()
                    Toast.makeText(context,"Permission needed", Toast.LENGTH_SHORT).show();
                    pos = position;
                    try {
                        deleteAPI30(uri);
//                        notifyItemRemoved(position);
//                        Toast.makeText(context, "Image Deleted successfully", Toast.LENGTH_SHORT).show();
                    } catch (IntentSender.SendIntentException e1) {
                        e1.printStackTrace();
                    }
                    alertDialog.dismiss();

                }



            }
        });
    }

    private void deleteAPI30(Uri imageUri) throws IntentSender.SendIntentException {
        ContentResolver contentResolver = context.getContentResolver();
        // API 30

            List<Uri> uriList = new ArrayList<>();
            Collections.addAll(uriList, imageUri);
            PendingIntent pendingIntent = MediaStore.createDeleteRequest(contentResolver, uriList);
            ((Activity)context).startIntentSenderForResult(pendingIntent.getIntentSender(),
                    DELETE_REQUEST_CODE,null,0,
                    0,0,null);

    }

    private Uri getContentUriId(Uri imageUri) {
        String[] projections = {MediaStore.MediaColumns._ID};
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projections,
                MediaStore.MediaColumns.DATA + "=?",
                new String[]{imageUri.getPath()}, null);
        long id = 0;
        if (cursor != null){
            if (cursor.getCount() > 0){
                cursor.moveToFirst();
                id  = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
            }
        }
        cursor.close();
        return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf((int)id));
    }
    public static int deleteAPI28(Uri uri, Context context) {
        ContentResolver resolver = context.getContentResolver();
        return resolver.delete(uri, null, null);
    }
    
    @Override
    public int getItemCount() {
        return imageArrayList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            cardView = itemView.findViewById(R.id.cardView);
        }
    }
}
