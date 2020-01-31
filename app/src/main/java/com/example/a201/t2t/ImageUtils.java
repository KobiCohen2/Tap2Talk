package com.example.a201.t2t;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.media.ExifInterface;
import android.util.Log;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A class with static methods for different image utils
 */
class ImageUtils {

    static final int GALLERY_PICTURE = 2;
    static final int CAMERA_REQUEST = 3;
    private static final long MAX_SIZE = 10*(1024 * 1024);
    static Map<String, Bitmap> usersThumbnails = Collections.synchronizedMap(new HashMap<>());

    /**
     * A method to upload image to firebase storage
     * @param phone - phone of the user
     * @param userPhoto - bitmap image to be upload
     * @param imageUri - image uri
     * @param context - context from which the picture is upload to the cloud
     */
    static void uploadImage(String phone, Bitmap userPhoto, Uri imageUri, Context context) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();
        final StorageReference filePath = storageReference.child("images").child(phone);
        if(userPhoto == null)
        {
            filePath.putFile(imageUri).addOnSuccessListener(taskSnapshot ->
                    Log.d("T2T_UPLOAD_IMAGE", "Image uploaded successfully"));
        }
        else
        {
            filePath.putFile(getImageUri(context, userPhoto)).addOnSuccessListener(taskSnapshot ->
                    Log.d("T2T_UPLOAD_IMAGE", "Image uploaded successfully"));
        }
    }

    /**
     * A method that rotates image if required
     * @param bitmap - image to rotate
     * @param imageUri - image uri
     * @param context - context from which the picture is need to rotate
     * @return rotated image as bitmap object
     */
    static Bitmap rotateImageIfRequired(Bitmap bitmap, Uri imageUri, Context context) {
        try {
            ExifInterface ei = new ExifInterface(getRealPathFromUri(context, imageUri));
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);
            Bitmap rotatedBitmap;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotatedBitmap = rotateImage(bitmap, 90);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotatedBitmap = rotateImage(bitmap, 180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotatedBitmap = rotateImage(bitmap, 270);
                    break;

                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    rotatedBitmap = bitmap;
            }
            return rotatedBitmap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * A private method that does the rotation job in practice
     * @param source - the bitmap image to rotate
     * @param angle - angle of rotation
     * @return rotated image as bitmap object
     */
    private static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    /**
     * A method to convert uri to real path
     * @param context - context from which to convert the uri
     * @param contentUri - uri to be converted
     * @return real path from uri as string
     */
    private static String getRealPathFromUri(Context context, Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        try(Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null)) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
    }

    /**
     * A method to get uri from bitmap image
     * @param inContext - context from which the action take place
     * @param inImage - image to find uri
     * @return uri of the given image
     */
    static Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 50, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    /**
     * A method to download from firebase storage the user's thumbnails,
     * and store them locally
     * @param phone - phone of the user
     */
    static void updateUsersThumbnails(String phone)
    {
        new Thread(() -> {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageReference = storage.getReference();
            storageReference.child("images/thumb_" + phone).getBytes(MAX_SIZE)
                    .addOnSuccessListener(bytes -> usersThumbnails.put(phone, BitmapFactory.decodeByteArray(bytes, 0, bytes.length)))
                    .addOnFailureListener(exception -> Log.d("Connection Error", "Failed to receive photo due to connection error"));
        }).start();
    }
}
