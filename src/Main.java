import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Scanner;

public class Main
{
    private static String urlDatabase = "jdbc:mysql://localhost/offerteautodb";

    private static void botInit() throws BotException{
        try {
            Scanner scanner = new Scanner(new File("token.txt"));
            String botToken = scanner.nextLine();
            TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(botToken, new OfferteAutoBot(urlDatabase));
        } catch (TelegramApiException e) {
            System.out.println("Telegram API Error");
        } catch(FileNotFoundException e) {
            System.out.println("Couldn't find bot token");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        try {
//            WebScraperDB scraperDB = new WebScraperDB(urlDatabase);
//            scraperDB.autoscout();
            botInit();
        } catch (BotException e) {
            System.out.println("Couldn't initialise bot");
            return;
        } //catch (SQLException e) {
//            System.out.println("Couldn't connect to db");
//        }
    }
}