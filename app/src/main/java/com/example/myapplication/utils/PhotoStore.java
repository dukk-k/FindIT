package com.example.myapplication.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public final class PhotoStore {
    private final Context context;

    public PhotoStore(Context context) {
        this.context = context.getApplicationContext();
    }

    public String importPhoto(Uri source) {
        if (source == null) return "";
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream stream = context.getContentResolver().openInputStream(source)) {
            BitmapFactory.decodeStream(stream, null, bounds);
        } catch (IOException | SecurityException failure) {
            throw new IllegalArgumentException("Foto tidak dapat dibuka.", failure);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IllegalArgumentException("Pilih berkas gambar yang dapat dibuka.");
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 1600) {
            options.inSampleSize *= 2;
        }

        String name = UUID.randomUUID() + ".jpg";
        File file = file(name);
        try (InputStream stream = context.getContentResolver().openInputStream(source)) {
            Bitmap bitmap = BitmapFactory.decodeStream(stream, null, options);
            if (bitmap == null) throw new IOException("Undecodable image");
            try (FileOutputStream output = new FileOutputStream(file)) {
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)) throw new IOException("Encode failed");
            } finally {
                bitmap.recycle();
            }
            return name;
        } catch (IOException | SecurityException failure) {
            discard(name);
            throw new IllegalArgumentException("Foto tidak dapat disimpan.", failure);
        }
    }

    public Bitmap load(String name, int targetPixels) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        String path = file(name).getAbsolutePath();
        BitmapFactory.decodeFile(path, bounds);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / options.inSampleSize > targetPixels * 2) {
            options.inSampleSize *= 2;
        }
        return BitmapFactory.decodeFile(path, options);
    }

    public void discard(String name) {
        if (!name.isEmpty()) file(name).delete();
    }

    private File file(String name) { return new File(context.getFilesDir(), name); }
}
