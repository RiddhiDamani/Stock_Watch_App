package com.riddhidamani.stock_watch;

import android.net.Uri;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class StockDownloaderRunnable implements Runnable {

    private static final String TAG = "StockDownloaderRunnable";
    private static final String STOCK_URL = "https://cloud.iexapis.com/stable/stock/";
    private static final String API_TOKEN = "/quote?token=sk_c200e7a98be1490ba819a01078935da2";
    private MainActivity mainActivity;
    private String searchSS;

    public StockDownloaderRunnable(MainActivity mainActivity, String searchSS) {
        this.mainActivity = mainActivity;
        this.searchSS = searchSS;
    }

    @Override
    public void run() {
        Uri.Builder uriBuilder = Uri.parse(STOCK_URL + searchSS + API_TOKEN).buildUpon();
        String urlBuilt = uriBuilder.toString();
        Log.d(TAG, "run: " + urlBuilt);
        StringBuilder stringBuilder = new StringBuilder();

        try {
            URL url = new URL(urlBuilt);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "run: HTTP ResponseCode = NOT OK" + connection.getResponseCode());
                return;
            }

            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            Log.d(TAG, "run: " + stringBuilder.toString());
        }
        catch (Exception exception) {
            exception.printStackTrace();
            Log.e(TAG, "run: ", exception);
        }

        processStock(stringBuilder.toString());
    }

    private void processStock(String processStock) {
        try {
            JSONObject jsonObjectStock = new JSONObject(processStock);

            final String stockSymbol = jsonObjectStock.getString("symbol");
            String companyName = jsonObjectStock.getString("companyName");

            String currentPrice = jsonObjectStock.getString("latestPrice");
            double price = 0.0;
            if(!currentPrice.trim().isEmpty())
                price = Double.parseDouble(currentPrice);

            String changePrice = jsonObjectStock.getString("change");
            double priceChange = 0.0;
            if(!changePrice.trim().isEmpty())
                priceChange = Double.parseDouble(changePrice);

            String changePercentage = jsonObjectStock.getString("changePercent");
            double changePercent = 0.0;
            if(!changePercentage.trim().isEmpty())
                changePercent = Double.parseDouble(changePercentage);

            Stock stock = new Stock(stockSymbol, companyName, price, priceChange, changePercent);

            mainActivity.runOnUiThread(() -> {
                mainActivity.addStock(stock);
                Log.d(TAG, "processStock: Stock added successfully! runOnUiThread: " + stockSymbol);
            });

        }
        catch (Exception exception) {
            Log.e(TAG, "processStock: " + exception.getMessage());
            exception.printStackTrace();
        }
    }
}
