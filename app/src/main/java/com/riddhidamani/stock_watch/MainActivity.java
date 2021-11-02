package com.riddhidamani.stock_watch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
import org.json.JSONObject;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
        swipeRefresh.setOnRefreshListener(() -> swipeRefreshStocks());

        // Loading the Initial Stock Data
        NameDownloaderRunnable symbolND = new NameDownloaderRunnable(this);
        new Thread(symbolND).start();
        readFromJSON();
        swipeRefreshStocksFirst();
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

    // Add Stock Dialog
    private void addStockToMainDialog(){

        if(!checkNetConnection()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("No Network Connection");
            builder.setMessage("Stocks Cannot Be Added Without A Network Connection");
            builder.setPositiveButton("OK", (dialogInterface, i) -> {
                // Do nothing!
            });
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
        builder.setPositiveButton("OK", (dialogInterface, i) -> {
            someStock = editText.getText().toString().trim();
            ArrayList<String> result = NameDownloaderRunnable.findMatch(someStock);

            if(result.size() == 0) {
                noStockDataDialog(someStock);
            }
            else if(result.size() == 1) {
                fetchStock(result.get(0));
            }
            else{
                String[] arr = result.toArray(new String[0]);
                AlertDialog.Builder builder1 = new AlertDialog.Builder((MainActivity.this));
                builder1.setTitle("Make a selection");
                builder1.setItems(arr, (dialog, pos) -> {
                    String stockSymbol = result.get(pos);
                    fetchStock(stockSymbol);
                });
                builder1.setNegativeButton("Nevermind", (dialog, pos) -> {
                    // do nothing
                });
                AlertDialog dialog = builder1.create();
                dialog.show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, id) -> {
            // do nothing
        });

        builder.setMessage("Please enter a Stock Symbol or a Company Name:");
        builder.setTitle("Stock Selection");
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Fetch Stock data
    private void fetchStock(String someStock){
        String[] data = someStock.split("-");
        StockDownloaderRunnable stockDownloaderRunnable = new StockDownloaderRunnable(this, data[0].trim());
        new Thread(stockDownloaderRunnable).start();
    }

    // Dialog for no stock data found
    private void noStockDataDialog(String someStock){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Symbol Not Found: " + someStock);
        builder.setMessage("Data for stock symbol " + someStock + " not found");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Do nothing!
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Extra Credit Functionality
    @Override
    public void onClick(View view) {
        int position = recyclerView.getChildLayoutPosition(view);
        String symbol = stocksList.get(position).getStockSymbol();

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(STOCK_URL + symbol));
        startActivity(browserIntent);
    }

    // On Long Click - Performing Delete stock from the Main Activity.
    @Override
    public boolean onLongClick(View view) {
        final int position = recyclerView.getChildLayoutPosition(view);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_delete_icon));
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int pos) {
                stocksList.remove(position);
                stockAdapter.notifyDataSetChanged();
                writeToJSON();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int pos) {
                // cancelling the dialog, doing nothing!
            }
        });

        builder.setMessage("Delete Stock Symbol " + stocksList.get(position).getStockSymbol()+ "?");
        builder.setTitle("Delete Stock");
        AlertDialog dialog = builder.create();
        dialog.show();
        return true;
    }

    private void readFromJSON() {
        Log.d(TAG, "readFromJSON: Reading Stocks Data from the JSON File");
        try {
            double price;
            double priceChange;
            double changePercentage;
            FileInputStream inputStream = getApplicationContext().openFileInput("StockData.json");
            byte[] data = new byte[inputStream.available()];
            int loadData = inputStream.read(data);
            Log.d(TAG, "readFromJSON: Loaded Data: " + loadData + " bytes");
            inputStream.close();
            String jsonData = new String(data);

            // Creating JSON Array from JSON object
            JSONArray stockArray = new JSONArray(jsonData);
            for (int i = 0; i < stockArray.length(); i++) {
                JSONObject jsonObject = stockArray.getJSONObject(i);
                String stockSymbol = jsonObject.getString("stockSymbol");
                String companyName = jsonObject.getString("companyName");
                price = jsonObject.getDouble("price");
                priceChange = jsonObject.getDouble("priceChange");
                changePercentage = jsonObject.getDouble("changePercentage");
                Stock stock = new Stock(stockSymbol, companyName, price, priceChange, changePercentage);
                stocksList.add(stock);
            }
        }
        catch (Exception exception) {
            Log.d(TAG, "readFromJSON: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    // Writing Stock Data to JSON File
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

    // Add Stock Logic
    public void addStock(Stock stock) {
        if(stock == null) {
            noStockDataDialog(someStock);
            return;
        }
        if(stocksList.contains(stock)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_info_icon));
            builder.setTitle("Duplicate Stock");
            builder.setMessage("Stock Symbol " + stock.getStockSymbol() + " is already displayed.");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Do nothing!
                }
            });
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

    // Checking for Network Connection
    private boolean checkNetConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    // If the stock values i.e. "latestPrice", "change", and "changePercent" is null, set to default
    // values i.e. 0.0
    private void defaultValues() {
        for(Stock stock: stocksList){
            stock.setPrice(0.0);
            stock.setPriceChange(0.0);
            stock.setChangePercentage(0.0);
        }
    }

    // Swipe Refresh Stock Data
    private void swipeRefreshStocks() {
        if(!checkNetConnection()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("No Network Connection");
            builder.setMessage("Stocks Cannot Be Updated Without A Network Connection");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                   // Do nothing!
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
            swipeRefresh.setRefreshing(false);
            return;
        }

        NameDownloaderRunnable nameDownloaderRunnable = new NameDownloaderRunnable(this);
        new Thread(nameDownloaderRunnable).start();
        List<Stock> tempStockList = new ArrayList<>();
        for(Stock stock: stocksList) {
            tempStockList.add(stock);
        }
        stocksList.clear();
        for(Stock stock: tempStockList){
            String symbol = stock.getStockSymbol();
            StockDownloaderRunnable stockDownloaderRunnable = new StockDownloaderRunnable(this, symbol);
            new Thread(stockDownloaderRunnable).start();
        }
        swipeRefresh.setRefreshing(false);
    }

    // Swipe Refreshing Data on Initial Loading to Main Activity
    private void swipeRefreshStocksFirst() {
        if (!checkNetConnection()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("No Network Connection");
            builder.setMessage("Stocks Cannot Be Updated Without A Network Connection");
            AlertDialog dialog = builder.create();
            dialog.show();
            defaultValues();
            swipeRefresh.setRefreshing(false);
            return;
        }

        List<Stock> tempStockList = new ArrayList<>();
        tempStockList.addAll(stocksList);
        stocksList.clear();

        for(Stock stock: tempStockList){
            String symbol = stock.getStockSymbol();
            StockDownloaderRunnable stockDownloaderRunnable = new StockDownloaderRunnable(this, symbol);
            new Thread(stockDownloaderRunnable).start();
        }

        swipeRefresh.setRefreshing(false);
    }

    // Pause method
    @Override
    protected void onPause() {
        super.onPause();
        writeToJSON();
    }
}