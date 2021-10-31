package com.riddhidamani.stock_watch;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class StockAdapter extends RecyclerView.Adapter<StockViewHolder> {

    private static final String TAG = "StockAdapter";
    private List<Stock> stocksList;
    private MainActivity mainActivity;

    public StockAdapter(List<Stock> stocksList, MainActivity mainActivity) {
        this.stocksList = stocksList;
        this.mainActivity = mainActivity;
    }

    @NonNull
    @Override
    public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        Log.d(TAG, "onCreateViewHolder: Creating new ViewHolder for stock");
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.stock_entry, parent, false);

        itemView.setOnClickListener(mainActivity);
        itemView.setOnLongClickListener(mainActivity);

        return new StockViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull StockViewHolder holder, int position) {
        Stock stock = stocksList.get(position);
        holder.stockSymbol.setText(stock.getStockSymbol());
        holder.companyName.setText(stock.getCompanyName());
        holder.price.setText(String.format(Locale.getDefault(),"%.2f", stock.getPrice()));
        Double priceChange = stock.getPriceChange();
        Double pricePercent = stock.getChangePercentage();
        String priceChangeStr = String.format(Locale.getDefault(), "%.2f", priceChange);
        String pricePercentStr = String.format(Locale.getDefault(), "%.2f", pricePercent);

        if(priceChange < 0) {
            holder.priceChange.setText("▼ "+ priceChangeStr + "(" + pricePercentStr + "%)");
            holder.companyName.setTextColor(Color.RED);
            holder.price.setTextColor(Color.RED);
            holder.priceChange.setTextColor(Color.RED);
            holder.stockSymbol.setTextColor(Color.RED);
        }
        else {
            holder.priceChange.setText("▲ "+ priceChangeStr + "(" + pricePercentStr + "%)");
            holder.companyName.setTextColor(Color.GREEN);
            holder.price.setTextColor(Color.GREEN);
            holder.priceChange.setTextColor(Color.GREEN);
            holder.stockSymbol.setTextColor(Color.GREEN);
        }
    }

    @Override
    public int getItemCount() {
        return stocksList.size();
    }
}
