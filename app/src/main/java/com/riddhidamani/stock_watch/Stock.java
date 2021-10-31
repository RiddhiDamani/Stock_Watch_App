package com.riddhidamani.stock_watch;

import java.io.Serializable;
import java.util.Objects;

public class Stock implements Serializable, Comparable<Stock> {
    private String stockSymbol;
    private String companyName;
    private double price;
    private double priceChange;
    private double changePercentage;

    public Stock(String stockSymbol, String companyName, double price, double priceChange, double changePercentage) {
        this.stockSymbol = stockSymbol;
        this.companyName = companyName;
        this.price = price;
        this.priceChange = priceChange;
        this.changePercentage = changePercentage;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public double getPrice() {
        return price;
    }

    public double getPriceChange() {
        return priceChange;
    }

    public double getChangePercentage() {
        return changePercentage;
    }

    public void setStockSymbol(String stockSymbol) {
        this.stockSymbol = stockSymbol;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setPriceChange(double priceChange) {
        this.priceChange = priceChange;
    }

    public void setChangePercentage(double changePercentage) {
        this.changePercentage = changePercentage;
    }

    @Override
    public String toString() {
        return "Stock{" +
                "stockSymbol='" + stockSymbol + '\'' +
                ", companyName='" + companyName + '\'' +
                ", price=" + price +
                ", priceChange=" + priceChange +
                ", changePercentage=" + changePercentage +
                '}';
    }

    @Override
    public int compareTo(Stock stock) {
        return this.stockSymbol.compareTo(stock.getStockSymbol());
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        Stock stock = (Stock) object;

        return Objects.equals(stockSymbol, stock.stockSymbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stockSymbol);
    }
}
