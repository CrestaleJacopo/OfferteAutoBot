import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class OfferteAutoBot implements LongPollingSingleThreadUpdateConsumer {
    private TelegramClient telegramClient;
    private Connection connection;
    private final HashMap<String, String> choices = new HashMap<String, String>();

    private List<String> carBrands = new ArrayList<>();
    private List<String> models = new ArrayList<>();

    public OfferteAutoBot(String dbUrl) {
        try {
            Scanner scanner = new Scanner(new File("token.txt"));
            String botToken = scanner.nextLine();
            telegramClient = new OkHttpTelegramClient(botToken);
            connection = DriverManager.getConnection(dbUrl, "root", "");
        } catch(FileNotFoundException e) {
            System.err.println("Couldn't file token file");
        } catch(SQLException e) {
            System.err.println("Couldn't connect to db");
        } catch(Exception e) {
            System.err.println("Bot creation error");
        }
    }

    private void displayMenu(Update update) {
        //PREPARING BUTTONS
        List<InlineKeyboardRow> rows = new ArrayList<>();
        var r1 = new InlineKeyboardRow();
        var tmp = new InlineKeyboardButton("Cerca auto");
        tmp.setCallbackData("cerca_auto");
        r1.add(tmp);
        rows.add(r1);

        //PREPARING MESSAGE
        Long chatId = update.getMessage().getChatId();
        String replyText = "Cosa vuoi fare?";
        SendMessage message = SendMessage.builder().chatId(chatId).text(replyText).replyMarkup(new InlineKeyboardMarkup(rows)).build();

        //SENDING MESSAGE
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void displayCarBrands(Update update) {
        //RETRIEVING CAR BRANDS
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT DISTINCT marca FROM annunci;");
            while(rs.next()) {
                carBrands.add(rs.getString("marca"));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        Collections.sort(carBrands);

        //PREPARING BUTTONS
        List<InlineKeyboardRow> rows = new ArrayList<>();
        int buttons = 0;
        var row = new InlineKeyboardRow();
        for(var brand : carBrands) {
            if(buttons%4 == 0 && buttons!=0) {
                rows.add(row);
                row = new InlineKeyboardRow();
            }
            var btn = new InlineKeyboardButton(brand);
            btn.setCallbackData(brand);
            row.add(btn);
            buttons++;
        }

        //PREPARING MESSAGE
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String replyText = "Seleziona il marchio:";
        SendMessage message = SendMessage.builder().chatId(chatId).text(replyText).replyMarkup(new InlineKeyboardMarkup(rows)).build();

        //SENDING MESSAGE
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void displayModels(Update update) {
        //RETRIEVING MODELS
        models = new ArrayList<>();
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT DISTINCT modello FROM annunci WHERE marca =\""+choices.get("brand")+"\";");
            while(rs.next()) {
                models.add(rs.getString("modello"));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        Collections.sort(models);

        //PREPARING BUTTONS
        List<InlineKeyboardRow> rows = new ArrayList<>();
        int buttons = 0;
        var row = new InlineKeyboardRow();
        for(int i = 0; i<models.size(); i++) {
            if((buttons%4 == 0 && buttons!=0) || i==models.size()-1) {
                rows.add(row);
                row = new InlineKeyboardRow();
            }
            var btn = new InlineKeyboardButton(models.get(i));
            btn.setCallbackData(models.get(i));
            row.add(btn);
            buttons++;
        }

        //PREPARING MESSAGE
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String replyText = "Seleziona il modello di "+choices.get("brand")+":";
        SendMessage message = SendMessage.builder().chatId(chatId).text(replyText).replyMarkup(new InlineKeyboardMarkup(rows)).build();

        //SENDING MESSAGE
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void consume(Update update)
    {
        if (update.hasMessage() && update.getMessage().hasText()) {
            if(update.getMessage().getText().equals("/start")) {
                displayMenu(update);
            }
        } else if(update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            if(callbackData.equals("cerca_auto")) {
                displayCarBrands(update);
            } else if(carBrands.contains(callbackData)) {
                choices.put("brand", callbackData);
                displayModels(update);
            } else if(models.contains(callbackData)) {
                choices.put("model", callbackData);
            }

        }
    }
}