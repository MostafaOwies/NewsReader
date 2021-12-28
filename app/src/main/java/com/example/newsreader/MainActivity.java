package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> newsList = new ArrayList<String>();
    ArrayList<String> content = new ArrayList<String>();
    ListView newsL;
    ArrayAdapter arrayAdapter;
    SQLiteDatabase database;

    public class News extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
           String result = "";
            URL url;
            HttpURLConnection urlConnection = null ;

            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream= urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(inputStream);

                int data = reader.read();

                while(data !=-1){

                    char current = (char) data;
                    result+=current;
                    data=reader.read();
                }
                JSONArray jsonArray = new JSONArray(result);
                int nOfItems = 20;
                if (jsonArray.length() <20){
                    nOfItems = jsonArray.length();
                }
                database.execSQL("DELETE FROM articles");
                for (int i = 0; i <nOfItems; i++) {
                    String articleID = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+articleID+".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                     inputStream= urlConnection.getInputStream();
                     reader = new InputStreamReader(inputStream);

                     data = reader.read();
                     String articleInfo = "";

                    while(data !=-1){

                        char current = (char) data;
                        articleInfo+=current;
                        data=reader.read();
                    }
                    JSONObject jsonObject = new JSONObject(articleInfo);
                    if (!jsonObject.isNull("title")  && !jsonObject.isNull("url")){
                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");

                        url = new URL(articleUrl);
                        urlConnection=(HttpURLConnection) url.openConnection();
                        inputStream= urlConnection.getInputStream();
                        reader= new InputStreamReader(inputStream);
                        data = reader.read();

                        String articelHTML = "";

                        while (data !=-1){
                            char current = (char) data ;
                            articelHTML += current;
                            data=inputStream.read();
                        }
                        Log.i("HTML" , articelHTML);

                        String sql = "INSERT INTO articles (articleID,title, content) VALUES(?,?,?)";
                        SQLiteStatement statement = database.compileStatement(sql);
                        statement.bindString(1,articleID);
                        statement.bindString(2,articleTitle);
                        statement.bindString(3,articelHTML);
                        statement.execute();
                    }
                }

                Log.i("results :", result);
                return result;
            }
            catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListview();
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        database.execSQL("CREATE TABLE IF NOT EXISTS articles(id INTEGER PRIMARY KEY, articleID INTEGER , title VARCHAR, content VARCHAR)");


        try {
            News news1 = new News();
            news1.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }
        catch (Exception e){
            e.printStackTrace();
        }
        newsL=findViewById(R.id.news);

       arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1,newsList);
        newsL.setAdapter(arrayAdapter);
        newsL.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(),newsPage.class);
                intent.putExtra("content",content.get(position));

                startActivity(intent);
            }
        });
        updateListview();
    }
    public void  updateListview(){
        Cursor c = database.rawQuery("SELECT * FROM articles",null);
        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");
        if (c.moveToFirst()){
            newsList.clear();
            content.clear();
            do {
                newsList.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));
            }
            while (c.moveToNext());

            arrayAdapter.notifyDataSetChanged();
        }
    }
}