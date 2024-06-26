package com.example.photos;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.NestedScrollView;

import com.example.movies.R;

import java.util.List;
import java.util.Map;

public class OpenPhoto extends AppCompatActivity {

    public static String PHOTO_FILEPATH = "photoFilepath";
    public static String ALBUM_INDEX = "albumIndex";
    private String photoFilepath = "";
    public int albumIndex;

    private Toolbar displayPhotoToolbar;
    private ImageView photoView;
    private Button prevPhotoButton;
    private Button nextPhotoButton;
    private Button addTagButton;
    private Button removeTagButton;
    private Button moveButton;
    private Button deleteButton;
    private Button backButton;

    private NestedScrollView tagsScrollView;
    private ConstraintLayout tagsLinearLayout;
    private TextView tagsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_photo);

        // Initialize your UI components

        //! bug, after moving a photo, if you press the back button in the toolbar, it will not go back to the album view
        displayPhotoToolbar = findViewById(R.id.display_photo_toolbar);
        displayPhotoToolbar.setTitle("");

        photoView = findViewById(R.id.photo_view);
        prevPhotoButton = findViewById(R.id.prev_photo_button);
        nextPhotoButton = findViewById(R.id.next_photo_button);
        addTagButton = findViewById(R.id.add_tag);
        removeTagButton = findViewById(R.id.remove_tag);
        moveButton = findViewById(R.id.move_button);
        deleteButton = findViewById(R.id.delete_button);
        tagsTextView = findViewById(R.id.tags_textView);
        backButton = findViewById(R.id.back_button);


        // Set the toolbar as the action bar for the activity
        setSupportActionBar(displayPhotoToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                this.photoFilepath = bundle.getString(PHOTO_FILEPATH);
                this.albumIndex = bundle.getInt(ALBUM_INDEX);
            }
        }

        // display the given photo in the photoView
        System.out.println("photoFilepath: " + photoFilepath);
        displayPhoto(photoFilepath);

        // set listeners for each of the buttons
        prevPhotoButton.setOnClickListener(view -> prevPhoto());
        nextPhotoButton.setOnClickListener(view -> nextPhoto());
        addTagButton.setOnClickListener(view -> addTag());
        removeTagButton.setOnClickListener(view -> removeTag());
        moveButton.setOnClickListener(view -> movePhoto());
        deleteButton.setOnClickListener(view -> deletePhoto());
        backButton.setOnClickListener(view -> backToAlbum());

        setTagsText();


    }

    public void setTagsText() {
        // set the text of tagsTextView to the tags of the photo
        Album album = Photos.albums.get(albumIndex);
        List<Photo> photos = album.getPhotos();
        for (Photo p : photos) {
            if (p.getFilePath().equals(photoFilepath)) {
                List<Map<String, String>> tags = p.getTags();
                StringBuilder tagsString = new StringBuilder();
                for (Map<String, String> tag : tags) {
                    String key = tag.keySet().iterator().next();
                    String value = tag.get(key);
                    tagsString.append(key).append(": ").append(value).append("\n");
                }
                tagsTextView.setText(tagsString.toString());
                break;
            }
        }
    }

    public void removeTag() {
        // create a pop up dialog (AlertDialog) to select the tag to remove
        // remove the selected tag from the photo

        System.out.println("removeTag");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a tag to remove");

        // create a list of tags to select from
        Album album = Photos.albums.get(albumIndex);
        List<Photo> photos = album.getPhotos();
        final Photo[] photo = new Photo[1];
        for (Photo p : photos) {
            if (p.getFilePath().equals(photoFilepath)) {
                photo[0] = p;
                break;
            }
        }


        List<Map<String, String>> tags = photo[0].getTags();
        String[] tagStrings = new String[tags.size()];
        for (int i = 0; i < tags.size(); i++) {
            Map<String, String> tag = tags.get(i);
            String key = tag.keySet().iterator().next();
            String value = tag.get(key);
            tagStrings[i] = key + ": " + value;
        }

        builder.setItems(tagStrings, (dialog, which) -> {
            Map<String, String> tag = tags.get(which);
            String key = tag.keySet().iterator().next();
            String value = tag.get(key);
            photo[0].deleteTag(key, value);
            setTagsText();
        });
        builder.create().show();
    }

    public void addTag() {
        System.out.println("addTag");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Tag");

        // tags can only be "person" or "location", so we can use a single-choice item dialog
        String[] tags = {"person", "location"};
        final int[] checkedItem = {0}; // this will be used to get the selected item

        builder.setSingleChoiceItems(tags, 0, (dialog, which) -> checkedItem[0] = which);

        EditText valueEditText = new EditText(this);
        valueEditText.setHint("Value");
        valueEditText.setInputType(InputType.TYPE_CLASS_TEXT);

        builder.setView(valueEditText);

        builder.setPositiveButton("Add", (dialog, which) -> {
            System.out.println("Pressed ok button");
            String key = tags[checkedItem[0]];
            String value = valueEditText.getText().toString();

            // get the current photo from the current album
            Album album = Photos.albums.get(albumIndex);
            List<Photo> photos = album.getPhotos();
            final Photo[] photo = new Photo[1];
            for (Photo p : photos) {
                if (p.getFilePath().equals(photoFilepath)) {
                    photo[0] = p;
                    break;
                }
            }
            // photo should not be null atp
            try {
                photo[0].addTag(key.toLowerCase(), value.toLowerCase());
                setTagsText();
                Photos.saveAlbumsToFile(this);
            } catch (NullPointerException e) {
                Toast.makeText(this, "Key or value cannot be null", Toast.LENGTH_SHORT).show();
                return;
            } catch (IllegalArgumentException e) {
                Toast.makeText(this, "Key or value cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
        });

        // save the changes
        System.out.println("Added tag, saving changes");
        Photos.saveAlbumsToFile(this);
        builder.create().show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        // go back to main activity
        Intent intent = new Intent(this, Photos.class);
        startActivity(intent);
        return true;
    }

    public void nextPhoto() {
        // go to the next photo in the album
        // if there is no next photo, display a message saying that there are no more photos in the album
        System.out.println("nextPhoto");
        Album album = Photos.albums.get(albumIndex);
        List<Photo> photos = album.getPhotos();
        int index = -1;
        for (int i = 0; i < photos.size(); i++) {
            if (photos.get(i).getFilePath().equals(photoFilepath)) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            System.out.println("Error: photo not found in album");
            return;
        }
        if (index == photos.size() - 1) {
            Toast.makeText(this, "No more photos in the album", Toast.LENGTH_SHORT).show();
            return;
        }
        String nextPhotoFilepath = photos.get(index + 1).getFilePath();
        Intent intent = new Intent(this, OpenPhoto.class);
        Bundle bundle = new Bundle();
        bundle.putString(OpenPhoto.PHOTO_FILEPATH, nextPhotoFilepath);
        bundle.putInt(OpenPhoto.ALBUM_INDEX, albumIndex);
        intent.putExtras(bundle);
        startActivity(intent);
        finish();
    }


    public void movePhoto() {
        // create a pop up dialog (AlertDialog) to select the album to move the photo to
        // move the photo to the selected album
        // go back to the album view
        System.out.println("movePhoto");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select an album to move the photo to");

        // create a list of albums to select from
        String[] albumNames = new String[Photos.albums.size()];
        for (int i = 0; i < Photos.albums.size(); i++) {
            albumNames[i] = Photos.albums.get(i).getAlbumName();
        }

        builder.setItems(albumNames, (dialog, which) -> {
            // make sure the album selected is not the current album

            // move the photo to the selected album
            Album album = Photos.albums.get(which);

            Photo photoToAdd = new Photo(photoFilepath);
            try {
                album.addPhoto(photoToAdd);
            } catch (IllegalArgumentException e) {
                Toast.makeText(this, "Photo already exists in the album", Toast.LENGTH_SHORT).show();
                return;
            }

            // delete the photo from the current album
            Album currentAlbum = Photos.albums.get(albumIndex);
            System.out.println("(before) current album size: " + currentAlbum.getSize());
            for (int i = 0; i < currentAlbum.getSize(); i++) {
                if (currentAlbum.getPhotos().get(i).getFilePath().equals(photoFilepath)) {
                    // remove item at index i
                    currentAlbum.removePhoto(currentAlbum.getPhotos().get(i));
                }
            }
            // print current album size
            System.out.println("(after) current album size: " + currentAlbum.getSize());
            // finish();
            backToAlbum();
            // it works when you do backToAlbum, but not finish()
            // problem: back button only works properly when you do finish(). same with deletePhoto().
        });
        builder.create().show();

    }

    public void deletePhoto() {
        // delete the photo from the album
        // go back to the album view
        System.out.println("deletePhoto");
        Album album = Photos.albums.get(albumIndex);
        for (int i = 0; i < album.getSize(); i++) {
            if (album.getPhotos().get(i).getFilePath().equals(photoFilepath)) {
                album.removePhoto(album.getPhotos().get(i));
            }
        }


        backToAlbum();
    }


    public void prevPhoto() {
        // go to the previous photo in the album
        // if there is no previous photo, display a message saying that there are no more photos in the album
        System.out.println("prevPhoto");
        Album album = Photos.albums.get(albumIndex);
        List<Photo> photos = album.getPhotos();
        int index = -1;
        for (int i = 0; i < photos.size(); i++) {
            if (photos.get(i).getFilePath().equals(photoFilepath)) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            System.out.println("Error: photo not found in album");
            return;
        }
        if (index == 0) {
            Toast.makeText(this, "No more photos in the album", Toast.LENGTH_SHORT).show();
            return;
        }
        String prevPhotoFilepath = photos.get(index - 1).getFilePath();
        Intent intent = new Intent(this, OpenPhoto.class);
        Bundle bundle = new Bundle();
        bundle.putString(OpenPhoto.PHOTO_FILEPATH, prevPhotoFilepath);
        bundle.putInt(OpenPhoto.ALBUM_INDEX, albumIndex);
        intent.putExtras(bundle);
        startActivity(intent);
        finish();
    }

    public void backToAlbum() {
        // save the changes
        Photos.saveAlbumsToFile(this);
        // go back to the album view
        Intent intent = new Intent(this, OpenAlbum.class);
        Bundle bundle = new Bundle();
        bundle.putInt(OpenAlbum.ALBUM_INDEX, albumIndex);
        intent.putExtras(bundle);
        startActivity(intent);
        finish();
    }

    public void displayPhoto(String photoFilepath) {
        photoView.setImageURI(Uri.parse(photoFilepath));
    }

    // on destroy, save the changes
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Photos.saveAlbumsToFile(this);
    }

    // on pause, save the changes
    @Override
    protected void onPause() {
        super.onPause();
        Photos.saveAlbumsToFile(this);
    }


}