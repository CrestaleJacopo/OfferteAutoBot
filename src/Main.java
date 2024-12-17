import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(new File("token.txt"));
            String botToken = scanner.nextLine();
            TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(botToken, new OfferteAutoBot());
        } catch (TelegramApiException e) {
            System.out.println("Errore api telegram");
        } catch(FileNotFoundException e) {
            System.out.println("Impossibile trovare il file del token");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}