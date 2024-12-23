import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.sql.*;

import java.util.HashMap;
import java.util.Map;

public class WebScraperDB
{
    private final static int PAGE_LIMIT = 25; //max pages scraped for each site
    private static  HashMap<String, String> ZIP_TO_PROVINCE;     //TODO: convert zip code to province name
    private final Connection connection;

    public WebScraperDB(String db_url) throws SQLException{
        try {
            connection = DriverManager.getConnection(db_url, "root", "");
            ZIP_TO_PROVINCE = new HashMap<String, String>();
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT sigla, cap from PROVINCE");
            while(rs.next()) {
                ZIP_TO_PROVINCE.put(rs.getString("cap"), rs.getString("sigla"));
            }
        } catch (SQLException e) {
            throw new SQLException();
        }
    }

    public void autoscout() {
        String url = "https://www.autoscout24.it/lst?offer=U&page=";
        try {
            for(int i = 1; i < PAGE_LIMIT; i++) {
                Document doc = Jsoup.connect(url + i).get();
                Elements articles = doc.select("article");
//                System.out.println("url = " + (url + i)); //DEBUG
                if(articles.isEmpty()) {
                    System.err.println("FINE "+(i-1));
                    break;
                }
                for(var article : articles) {
                    try {
                        String query = "INSERT INTO ANNUNCI (marca, modello, prezzo, link, km, anno, provincia, carburante, cambio) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                        PreparedStatement statement = connection.prepareStatement(query);

                        statement.setString(1, article.attr("data-make").toLowerCase());
                        statement.setString(2, article.attr("data-model").toLowerCase());
                        statement.setInt(3, Integer.parseInt(article.attr("data-price")));
                        statement.setString(4, "autoscout24.it" +
                                        article.select("div[class=\"ListItem_header__J6xlG ListItem_header_new_design__Rvyv_\"]").
                                        select("a[href]").attr("href"));
                        statement.setInt(5, Integer.parseInt(article.attr("data-mileage")));
                        statement.setInt(6, Integer.parseInt(article.attr("data-first-registration").split("-")[1]));

                        var siglaProvincia = ZIP_TO_PROVINCE.get(article.attr("data-listing-zip-code").strip().substring(0, 2));
                        if(siglaProvincia == null) break;
                        statement.setString(7, siglaProvincia.toUpperCase());

                        statement.setString(8, article.select("span[data-testid=\"VehicleDetails-gas_pump\"]").text().toLowerCase());
                        statement.setString(9, article.select("span[data-testid=\"VehicleDetails-transmission\"]").text().toLowerCase());

                        if (statement.executeUpdate() < 1)  System.err.println("No rows were inserted");

                    } catch(SQLIntegrityConstraintViolationException e) {
                        System.err.println("Constraint violation, not inserting");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
