import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Main
{
    private static boolean botInit() {
        try {
            Scanner scanner = new Scanner(new File("token.txt"));
            String botToken = scanner.nextLine();
            TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(botToken, new OfferteAutoBot());
        } catch (TelegramApiException e) {
            System.out.println("Errore api telegram");
            return false;
        } catch(FileNotFoundException e) {
            System.out.println("Impossibile trovare il file del token");
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    public static void main(String[] args)
    {
        //botInit();
        WebScraper.autoscout();
    }
}