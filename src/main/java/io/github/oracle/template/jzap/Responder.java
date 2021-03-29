package io.github.oracle.template.jzap;

import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.domain.Coins.MarketChart;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;


public class Responder {
    public CoinGeckoApiClient client;

    public Responder() {
        client = new CoinGeckoApiClientImpl();
    }

    public String getResponse(String coin, String currency, Integer days) {
        MarketChart data = client.getCoinMarketChartById(coin.toLowerCase(), currency, days);
        String price = data.prices.get(data.prices.size()-1).get(1);
        System.out.println("From Coin Gecko Api Price: " + price);
        return price;
    }

    public int getResponseInt(String coin, String currency, Integer days) {
        MarketChart data = client.getCoinMarketChartById(coin.toLowerCase(), currency, days);
        String price = data.prices.get(data.prices.size()-1).get(1);
        int ret = Integer.parseInt(price);
        return ret;
    }
}
