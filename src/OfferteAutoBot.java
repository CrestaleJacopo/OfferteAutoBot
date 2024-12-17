import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class OfferteAutoBot implements LongPollingSingleThreadUpdateConsumer {
    private TelegramClient telegramClient;

    public OfferteAutoBot() {
        try {
            Scanner scanner = new Scanner(new File("token.txt"));
            String botToken = scanner.nextLine();
            telegramClient = new OkHttpTelegramClient(botToken);
        } catch(FileNotFoundException e) {
            System.out.println("Impossibile trovare il file del token");
        } catch(Exception e) {
            System.out.println("Errore nella creazione del bot");
        }
    }

    @Override
    public void consume(Update update)
    {
        if (update.hasMessage() && update.getMessage().hasText())
        {
            var incomingMsg = update.getMessage();
            SendMessage sendMessage = new SendMessage(incomingMsg.getChatId().toString(), incomingMsg.getText());
            try {
                telegramClient.execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
}