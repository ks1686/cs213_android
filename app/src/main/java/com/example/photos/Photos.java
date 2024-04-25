package com.example.photos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.movies.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/* class Movie {
    String name;
    String year;
    String director;
    Movie(String name, String year) {
        this.name = name;
        this.year = year;
        this.director = "";
    }
    Movie(String name, String year, String director) {
        this(name, year);
        this.director = director;
    }
    public String toString() {   // used by ListView
        return name + "\n(" + year + ")";
    }
    public String getString() {
        return name + "|" + year + "|" + director;
    }
} */
public class Photos extends AppCompatActivity {

    private ListView listView;
    private List<Album> albums;
    private ActivityResultLauncher<Intent> startForAlbumOpen;

    public List<String> getAlbumNames() {
        List<String> albumNames = new ArrayList();
        for (Album a : albums) {
            albumNames.add(a.getAlbumName());
        }
        return albumNames;
    }

    private void saveAlbumsToFile() {
        File file = new File(getFilesDir(), "albums.json");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            JSONArray jsonArray = new JSONArray();
            for (Album album : albums) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("albumName", album.getAlbumName());
                JSONArray photosJsonArray = new JSONArray();
                for (Photo photo : album.getPhotos()) {
                    JSONObject photoJsonObject = new JSONObject();
                    photoJsonObject.put("name", photo.getFilePath());
                    photosJsonArray.put(photoJsonObject);
                }
                jsonObject.put("photos", photosJsonArray);
                jsonArray.put(jsonObject);
            }
            fos.write(jsonArray.toString().getBytes());
            fos.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private List<Album> loadAlbums() {
        List<Album> albums = new ArrayList<>();
        File file = new File(getFilesDir(), "albums.json");
        try {
            if (!file.exists()) {
                return albums;
            }
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder jsonString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }
            JSONArray jsonArray = new JSONArray(jsonString.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Album album = new Album(jsonObject.getString("albumName"));
                JSONArray photosJsonArray = jsonObject.getJSONArray("photos");
                for (int j = 0; j < photosJsonArray.length(); j++) {
                    JSONObject photoJsonObject = photosJsonArray.getJSONObject(j);
                    album.addPhoto(new Photo(photoJsonObject.getString("name")));
                }
                albums.add(album);
            }
            fis.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return albums;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.albums_list);

        Toolbar myToolbar = findViewById(R.id.albums_toolbar);
        setSupportActionBar(myToolbar);

        // get the FloatingActionButton with id create_album_button
        // set an OnClickListener on the button to call addMovie

        FloatingActionButton createAlbumButton = findViewById(R.id.create_album_button);
        createAlbumButton.setOnClickListener(view -> createAlbum());

        albums = new ArrayList<>();

        albums = loadAlbums();
        saveAlbumsToFile();
        // albums.json will store the list of albums and their photos
        // albums.json will be stored in the data directory

        listView = findViewById(R.id.albums_list);
        listView.setAdapter(
                new ArrayAdapter<>(this, R.layout.album, getAlbumNames()));

        listView.setOnItemClickListener((list, view, pos, id) -> showAlbum(pos));

        // register add/edit activities in onCreate
        // registration must be done before creation is completed
        registerActivities();
    }

    public void registerActivities() {
        startForAlbumOpen =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                applyAlbumEdit(result);
                            }
                        });
    }

    private void applyAlbumEdit(ActivityResult result) {
        Intent intent = result.getData();
        Bundle bundle = intent.getExtras();

        if (bundle == null) {
            return;
        }

        // gather all info passed back by launched activity
        String albumName = bundle.getString(OpenAlbum.ALBUM_NAME);
        int albumIndex = bundle.getInt(OpenAlbum.ALBUM_INDEX);

        listView.setAdapter(new ArrayAdapter<>(Photos.this, R.layout.album, getAlbumNames()));
    }

    private void showAlbum(int pos) {
        // TODO: implement this method
        Bundle bundle = new Bundle();
        Album movie = albums.get(pos);
        bundle.putInt(OpenAlbum.ALBUM_INDEX, pos);
        bundle.putString(OpenAlbum.ALBUM_NAME, movie.getAlbumName());

        // launch for edit
        Intent intent = new Intent(this, OpenAlbum.class);
        intent.putExtras(bundle);
        startForAlbumOpen.launch(intent);
    }

    private void createAlbum() {
        System.out.println("Create album function called");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter album name");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String albumName = input.getText().toString();
                Album album = new Album(albumName);
                albums.add(album);
                listView.setAdapter(new ArrayAdapter<>(Photos.this, R.layout.album, getAlbumNames()));
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });


        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.add_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            createAlbum();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}