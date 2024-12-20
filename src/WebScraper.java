import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.HashMap;

public class WebScraper
{
    private static int PAGE_LIMIT = 25; //max pages scraped for each site
    private static HashMap<String, String> ZIP_TO_PROVINCE;     //TODO: convert zip code to province name
    public static void autoscout()
    {
        String url = "https://www.autoscout24.it/lst?offer=U&page=";
        try {
            for(int i = 1; i < PAGE_LIMIT; i++) {
                Document doc = Jsoup.connect(url + i).get();
                Elements articles = doc.select("article");
//                System.out.println("url = " + (url + i)); //DEBUG
                if(articles.isEmpty()) {
                    System.out.println("FINITO"+i);
                    break;
                }
                for(var article : articles) {
                        // marca, modello, prezzo, km, anno, provincia, carburante, cambio, (potenza)
                    String marca = article.attr("data-make").toLowerCase();
                    String modello = article.attr("data-model").toLowerCase();
                    Integer prezzo = Integer.parseInt(article.attr("data-price"));
                    Integer km = Integer.parseInt(article.attr("data-mileage"));
                    Integer anno = Integer.parseInt(article.attr("data-first-registration").split("-")[1]);   //va sistemato a solo anno

//                    String provincia = article.attr("");    //DA SISTEMARE
                    String carburante = article.select("span[data-testid=\"VehicleDetails-gas_pump\"]").text().toLowerCase();
                    String cambio = article.select("span[data-testid=\"VehicleDetails-transmission\"]").text().toLowerCase();
//                    System.out.println(marca + " " + modello + " " + prezzo + " " + km + " " + anno + " " + provincia + " " + carburante + " " + cambio); //DEBUG
                }
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}
