package com.project.guardianalertcapstone;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private ArrayList<NotificationModel> notificationList;
    private String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
            ? FirebaseAuth.getInstance().getCurrentUser().getUid()
            : "anonymous";

    public NotificationAdapter(ArrayList<NotificationModel> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationModel model = notificationList.get(position);
        holder.titleTextView.setText("Motion Detected!");
        holder.timestampTextView.setText(model.getTimestamp());

        if (!model.isRead()) {
            holder.tvNewLabel.setVisibility(View.VISIBLE);
            holder.cardView.setCardBackgroundColor(0xFFFFF8E1);
        } else {
            holder.tvNewLabel.setVisibility(View.GONE);
            holder.cardView.setCardBackgroundColor(0xFFFFFFFF);
        }

        // Decode and save image
        String base64Image = model.getBase64Image();
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.imageView.setImageBitmap(bitmap);

                // Save image to proper storage depending on Android version
                Context context = holder.itemView.getContext();
                String fileName = "alert_" + position + ".jpg";

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10 and above (Scoped Storage)
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GuardianAlert");

                    ContentResolver resolver = context.getContentResolver();
                    OutputStream os = resolver.openOutputStream(
                            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values));
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os);
                    os.close();
                    Log.d("ImageSave", "✅ Image saved to Pictures/GuardianAlert (Scoped)");

                } else {
                    // Android 7 to 9
                    File folder = new File(context.getExternalFilesDir(null), "GuardianAlert");
                    if (!folder.exists()) folder.mkdirs();
                    File file = new File(folder, fileName);
                    FileOutputStream fos = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                    fos.close();
                    Log.d("ImageSave", "✅ Image saved to: " + file.getAbsolutePath());
                }

            } catch (Exception e) {
                e.printStackTrace();
                holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // On click: mark as read + preview modal
        holder.itemView.setOnClickListener(v -> {
            if (!model.isRead()) {
                model.setRead(true);
                notifyItemChanged(position);

                FirebaseFirestore.getInstance()
                        .collection("motion_alerts")
                        .document(model.getDocId())
                        .update("readBy", FieldValue.arrayUnion(currentUid));
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle("Motion Detected!");
            builder.setMessage("Timestamp: " + model.getTimestamp());

            ImageView img = new ImageView(v.getContext());
            if (model.getBase64Image() != null && !model.getBase64Image().isEmpty()) {
                byte[] decodedBytes = Base64.decode(model.getBase64Image(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                img.setImageBitmap(bitmap);
            }
            builder.setView(img);
            builder.setPositiveButton("Close", null);
            builder.show();
        });

        // Long press to delete from list
        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Warning!")
                    .setMessage("Are you sure you want to delete this notification?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        notificationList.remove(position);
                        notifyItemRemoved(position);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, timestampTextView, tvNewLabel;
        ImageView imageView;
        androidx.cardview.widget.CardView cardView;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.notification_title);
            timestampTextView = itemView.findViewById(R.id.notification_timestamp);
            imageView = itemView.findViewById(R.id.notification_image);
            tvNewLabel = itemView.findViewById(R.id.tvNewLabel);
            cardView = itemView.findViewById(R.id.notification_card);
        }
    }

    public void markAllAsRead() {
        for (NotificationModel model : notificationList) {
            if (!model.isRead()) {
                model.setRead(true);
                FirebaseFirestore.getInstance()
                        .collection("motion_alerts")
                        .document(model.getDocId())
                        .update("readBy", FieldValue.arrayUnion(currentUid));
            }
        }
        notifyDataSetChanged();
    }
}
