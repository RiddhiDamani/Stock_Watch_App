package com.riddhidamani.stock_watch;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.JsonWriter;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener  {

    private static final String TAG = "MainActivity";
    private final List<Stock> stocksList = new ArrayList<>();
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private StockAdapter stockAdapter;
    private static final String STOCK_URL = "http://www.marketwatch.com/investing/stock/";
    private String someStock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        stockAdapter = new StockAdapter(stocksList, this);
        recyclerView.setAdapter(stockAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Code to refresh stock
            }
        });

        // Load the initial data
        NameDownloaderRunnable symbolND = new NameDownloaderRunnable(this);
        new Thread(symbolND).start();

        readFromJSON();
        refreshStocksFirstTime();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.addStockMenu) {
            addStockToMainDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addStockToMainDialog(){

        if(!checkNetworkConnection()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("No Network Connection");
            builder.setMessage("Cannot add content without a network connection");
            AlertDialog dialog = builder.create();
            dialog.show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        editText.setGravity(Gravity.CENTER_HORIZONTAL);

        builder.setView(editText);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                someStock = editText.getText().toString().trim();
                ArrayList<String> result = NameDownloaderRunnable.findMatch(someStock);

                if(result.size() == 0) {
                    showErrorDialog(someStock);
                }
                else if(result.size() == 1) {
                    oneSelection(result.get(0));
                }
                else{
                    String[] arr = result.toArray(new String[0]);
                    AlertDialog.Builder builder = new AlertDialog.Builder((MainActivity.this));
                    builder.setTitle("Make a selection");
                    builder.setItems(arr, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int pos) {
                            String stockSymbol = result.get(pos);
                            doStockSelection(stockSymbol);
                        }
                    });
                    builder.setNegativeButton("Nevermind", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // cancelled the dialog
                        }
                    });
                    AlertDialog dialog2 = builder.create();
                    dialog2.show();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        builder.setMessage("Please enter a Stock Symbol or a Company Name:");
        builder.setTitle("Stock Selection");

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void doStockSelection(String searchResult){
        String[] data = searchResult.split("-");
        StockDownloaderRunnable stockDownloaderRunnable = new StockDownloaderRunnable(this, data[0].trim());
        new Thread(stockDownloaderRunnable).start();
    }

    private void showErrorDialog(String someStock){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("No data for specified symbol/name");
        builder.setTitle("No Data Found:" + someStock);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void oneSelection(String someStock){
        String[] data = someStock.split("-");
        StockDownloaderRunnable stockDownloaderRunnable = new StockDownloaderRunnable(this, data[0].trim());
        new Thread(stockDownloaderRunnable).start();
    }

    @Override
    public void onClick(View view) {
        int position = recyclerView.getChildLayoutPosition(view);
        String symbol = stocksList.get(position).getStockSymbol();

        Uri.Builder uriBuilder = Uri.parse(STOCK_URL + symbol).buildUpon();
        //String urlBuilt = uriBuilder.toString();

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(STOCK_URL + symbol));
        startActivity(browserIntent);
    }

    @Override
    public boolean onLongClick(View view) {
        return false;
    }

    private void readFromJSON() {
        Log.d(TAG, "readFromJSON: Reading Stocks Data from the JSON File");
        try {
            FileInputStream inputStream = getApplicationContext().openFileInput("StockData.json");
            byte[] data = new byte[inputStream.available()];
            int loadData = inputStream.read(data);
            inputStream.close();
            String jsonData = new String(data);

            // Creating JSON Array from JSON object
            JSONArray stockArray = new JSONArray(jsonData);
            for (int i = 0; i < stockArray.length(); i++) {
                JSONObject jsonObject = stockArray.getJSONObject(i);
                String stockSymbol = jsonObject.getString("stockSymbol");
                String companyName = jsonObject.getString("companyName");
                Double price = jsonObject.getDouble("price");
                Double priceChange = jsonObject.getDouble("priceChange");
                Double changePercentage = jsonObject.getDouble("changePercentage");
                Stock stock = new Stock(stockSymbol, companyName, price, priceChange, changePercentage);
                stocksList.add(stock);
            }
        }
        catch (Exception exception) {
            Log.d(TAG, "readFromJSON: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    private void writeToJSON() {
        Log.d(TAG, "writeToJSON: Saving Stocks Data into the JSON File");
        try {
            FileOutputStream outputStream = getApplicationContext().openFileOutput("StockData.json", Context.MODE_PRIVATE);
            JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            jsonWriter.setIndent(" ");
            jsonWriter.beginArray();
            for(Stock stock: stocksList) {
                jsonWriter.beginObject();
                jsonWriter.name("stockSymbol").value(stock.getStockSymbol());
                jsonWriter.name("companyName").value(stock.getCompanyName());
                jsonWriter.name("price").value(stock.getPrice());
                jsonWriter.name("priceChange").value(stock.getPriceChange());
                jsonWriter.name("changePercentage").value(stock.getChangePercentage());
                jsonWriter.endObject();
            }
            jsonWriter.endArray();
            jsonWriter.close();
        }
        catch (Exception exception) {
            Log.d(TAG, "writeToJSON: "+ exception.getMessage());
            exception.printStackTrace();
        }
    }

    public void addStock(Stock stock) {
        if(stock == null) {
            return;
        }

        if(stocksList.contains(stock)) {
            AlertDialog.Builder builder =new AlertDialog.Builder(this);
            builder.setMessage(stock.getStockSymbol() + "is already displayed.");
            builder.setTitle("Duplicate Stock");

            AlertDialog dialog = builder.create();
            dialog.show();
            return;
        }
        stocksList.add(stock);
        Collections.sort(stocksList);
        writeToJSON();
        stockAdapter.notifyDataSetChanged();
    }

    public void downloadErrorToast() {
        Toast.makeText(this, "Failed to Download Symbols or Names", Toast.LENGTH_LONG).show();
    }

    private boolean checkNetworkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    private void defaultValues() {
        for(Stock stock: stocksList){
            stock.setPrice(0.0);
            stock.setPriceChange(0.0);
            stock.setChangePercentage(0.0);
        }
    }

    private void refreshStocksFirstTime(){
        if (!checkNetworkConnection()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("No Network Connection");
            builder.setMessage("Content Cannot Be Refreshed Without A Network Connection");
            AlertDialog dialog = builder.create();
            dialog.show();
            defaultValues();
            swipeRefresh.setRefreshing(false);
            return;
        }
        List<Stock> tmpStockList = new ArrayList<Stock>();
        for(Stock stock: stocksList){
            tmpStockList.add(stock);
        }
        stocksList.clear();

        for(Stock s: tmpStockList){
            String symbol = s.getStockSymbol();
            StockDownloaderRunnable stockDownloader = new StockDownloaderRunnable(this, symbol);
            new Thread(stockDownloader).start();
        }

        swipeRefresh.setRefreshing(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        writeToJSON();
    }
}