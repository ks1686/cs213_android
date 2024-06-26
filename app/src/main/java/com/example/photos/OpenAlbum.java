package com.example.photos;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.movies.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class OpenAlbum extends AppCompatActivity {

    public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
        private Context context;
        private List<Photo> photos;

        // constructor to create the image adapter
        public ImageAdapter(Context context, List<Photo> photos) {
            this.context = context;
            this.photos = photos;
        }

        // method to create the image view holder
        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.image_item, parent, false);
            return new ImageViewHolder(view);
        }

        // method to bind the image view holder
        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");

            Photo photo = photos.get(position);
            holder.imageView.setImageURI(Uri.parse(photo.getFilePath()));
        }

        // method to get the number of photos
        @Override
        public int getItemCount() {
            return photos.size();
        }

        // method to update the photos
        public void updatePhotos(List<Photo> newPhotos) {
            photos = newPhotos;
            notifyDataSetChanged();
        }

        // inner class to hold the image view
        public class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            public ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.image_view);
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // open display photo view here
                        System.out.println("Photo clicked");

                        // get the photo that was clicked
                        Photo photo = photos.get(getAdapterPosition());
                        String photoFilePath = photo.getFilePath();

                        // ! Testing: print the photoFilePath
                        System.out.println("photoFilePath: " + photoFilePath);

                        // create a new intent to open the photo
                        Intent intent = new Intent(context, OpenPhoto.class);

                        // create a bundle with albumIndex and photoFilePath
                        Bundle bundle = new Bundle();
                        bundle.putInt("albumIndex", albumIndex);
                        bundle.putString("photoFilepath", photoFilePath);
                        intent.putExtras(bundle);
                        context.startActivity(intent);
                    }
                });
            }
        }

    }

    public static String ALBUM_NAME = "albumName";
    public static String ALBUM_INDEX = "albumIndex";
    private int albumIndex;

    private EditText albumName;
    private Button deleteAlbumButton;
    private Toolbar myToolbar;
    private Button addPhotoButton;
    private RecyclerView imageListView;
    private ImageAdapter imageAdapter;

    private Button renameAlbumButton;

    //! ERROR: NEVER PASSING THE PROPER albumIndex
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.open_album);
        myToolbar = findViewById(R.id.my_toolbar);

        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        deleteAlbumButton = findViewById(R.id.delete_album_button);
        deleteAlbumButton.setOnClickListener(view -> deleteAlbum());

        addPhotoButton = findViewById(R.id.add_photo_button);
        addPhotoButton.setOnClickListener(view -> selectImage());

        renameAlbumButton = findViewById(R.id.rename_album_button);
        renameAlbumButton.setOnClickListener(view -> renameAlbum());


        imageListView = findViewById(R.id.image_list_view);
        int numberOfColumns = 3;
        imageListView.setLayoutManager(new GridLayoutManager(this, numberOfColumns));

        // print the album name and index
        // ! ERROR: NEVER PASSING THE PROPER albumIndex or albumName
        System.out.println("Test: " + albumIndex + " " + Photos.albums.get(albumIndex).getAlbumName());
        //print the ALBUM_INDEX and ALBUM_NAME
        System.out.println("Test: " + ALBUM_INDEX + " " + ALBUM_NAME);
        Album album = Photos.albums.get(albumIndex);
        imageAdapter = new ImageAdapter(this, album.getPhotos());
        imageListView.setAdapter(imageAdapter);

        myToolbar.setNavigationOnClickListener(view -> returnToAlbumsList());

        albumName = findViewById(R.id.album_name);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            albumIndex = extras.getInt("albumIndex", -1);
            albumName.setText(extras.getString("albumName"));

            // handle if albumIndex is -1
            if (albumIndex == -1) {
                Toast.makeText(this, "Album index not found", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            Toast.makeText(this, "No album index found", Toast.LENGTH_SHORT).show();
            return;
        }

        //Print albumIndex and albumName
        System.out.println("albumIndex: " + albumIndex);

        albumName.setText(extras.getString(ALBUM_NAME));



        // update the view with the correct album (based on the corret albumIndex)
        imageAdapter.updatePhotos(Photos.albums.get(albumIndex).getPhotos());
    }

    static final int REQUEST_IMAGE_GET = 1;

    public void onImageChosen(ActivityResult result) {
        System.out.println("In onImageChosen");
    }


    public void returnToAlbumsList(){
        // save the albums to the file
        Photos.saveAlbumsToFile(this);


        // go back to the main activity
        Intent intent = new Intent(this, Photos.class);
        startActivity(intent);

        finish();
    }

    //custom result value for deleteAlbum
    public static final int DELETE_ALBUM_RESULT = 2;

    public void deleteAlbum() {
        Intent intent = new Intent();
        intent.putExtra(ALBUM_INDEX, albumIndex);
        setResult(DELETE_ALBUM_RESULT, intent);
        finish();
    }

    // method to rename the album
    public void renameAlbum() {
        String albumNameString = albumName.getText().toString();
        try {
            Photos.albums.get(albumIndex).setAlbumName(albumNameString);
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "An album with name already exists.", Toast.LENGTH_SHORT).show();
            return;
        }
        Photos.saveAlbumsToFile(this);

        //toast to show that the album has been renamed
        Toast.makeText(this, "Album renamed", Toast.LENGTH_SHORT).show();
    }

    public void selectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra("albumIndex", albumIndex);
        intent.putExtra("albumName", albumName.getText().toString());
        startActivityForResult(intent, REQUEST_IMAGE_GET);

    }

    // method to save the updated album with the new photo
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        System.out.println("In onActivityResult in OpenAlbum.java");
        if (requestCode == REQUEST_IMAGE_GET && resultCode == RESULT_OK) {

            if (data != null) {
                Uri uri = data.getData();

                // get READ_URI_PERMISSION
                getContentResolver()
                        .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // get the string filepath for the single photo
                String filePath = uri.toString();
                // if filepath not null, add to the album
                if (filePath != null) {
                    try {
                        Photos.albums.get(albumIndex).addPhoto(new Photo(filePath));
                    } catch (IllegalArgumentException e) {
                        Toast.makeText(this, "Photo already exists in album", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    imageAdapter.updatePhotos(Photos.albums.get(albumIndex).getPhotos());
                } else {
                    Toast.makeText(this, "No photo selected", Toast.LENGTH_SHORT).show();
                    return;
                }

                // print all albums and their photos
                System.out.println("Printing out all albums and their photo filepaths");
                for (Album album : Photos.albums) {
                    System.out.println(album.getAlbumName());
                    for (Photo photo : album.getPhotos()) {
                        System.out.println(photo.getFilePath());
                    }
                }
            }

            System.out.println("In onActivityResult in OpenAlbum.java, albums is " + Photos.albums);
            Photos.saveAlbumsToFile(this);
        }
    }

    // override back button to save the albums to the file and open the main activity
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Photos.saveAlbumsToFile(this);
        Intent intent = new Intent(this, Photos.class);
        startActivity(intent);
        finish();
    }
}