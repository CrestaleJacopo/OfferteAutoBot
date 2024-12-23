import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import java.util.*;

public class OfferteAutoBot implements LongPollingSingleThreadUpdateConsumer {
    private TelegramClient telegramClient;
    private Connection connection;
    private final HashMap<String, String> choices = new HashMap<String, String>();

    private final List<String> carBrands = new ArrayList<>();
    private List<String> models = new ArrayList<>();
    private final List<String> fuels = new ArrayList<>();
    private final List<String> transmissions = new ArrayList<>();
    private final List<String> orderOptions = new ArrayList<>();

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

        //INIT FILTERS
        orderOptions.add("prezzo");
        orderOptions.add("km");
        orderOptions.add("prezzo/km");
    }

    private List<InlineKeyboardRow> prepareButtons(List<String> lst) {
        //PREPARING BUTTONS
        List<InlineKeyboardRow> rows = new ArrayList<>();
        int buttons = 0;
        var row = new InlineKeyboardRow();
        for(int i = 0; i<lst.size(); i++) {
            var btn = new InlineKeyboardButton(lst.get(i));
            btn.setCallbackData(lst.get(i));
            row.add(btn);
            if((buttons%4 == 0 && buttons!=0) || i==lst.size()-1) {
                rows.add(row);
                row = new InlineKeyboardRow();
            }
            buttons++;
        }
        return rows;
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
            System.err.println("telegram error in displayMenu");
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
            System.err.println("db error in displayCarBrands");
        }
        Collections.sort(carBrands);

        //PREPARING MESSAGE
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String replyText = "Seleziona il marchio:";
        SendMessage message = SendMessage.builder().chatId(chatId).text(replyText).replyMarkup(new InlineKeyboardMarkup(prepareButtons(carBrands))).build();

        //SENDING MESSAGE
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            System.err.println("telegram error in displayCarBrands");
        }
    }

    private void displayModels(Update update) {
        //RETRIEVING MODELS
        models = new ArrayList<>();
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT DISTINCT modello FROM annunci WHERE marca =\""+choices.get("marca")+"\";");
            while(rs.next()) {
                models.add(rs.getString("modello"));
            }
        } catch(Exception e) {
            System.err.println("db error in displayModels");
        }
        Collections.sort(models);

        //PREPARING MESSAGE
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String replyText = "Seleziona il modello di "+choices.get("marca")+":";
        SendMessage message = SendMessage.builder().chatId(chatId).text(replyText).replyMarkup(new InlineKeyboardMarkup(prepareButtons(models))).build();

        //SENDING MESSAGE
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            System.err.println("telegram error in displayModels");
        }
    }

    private void displayFuels(Update update) {
        //RETRIEVING
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT DISTINCT carburante FROM annunci WHERE marca = ? AND modello = ?;");
            statement.setString(1, choices.get("marca"));
            statement.setString(2, choices.get("modello"));
            ResultSet rs = statement.executeQuery();
            while(rs.next()) {
                fuels.add(rs.getString("carburante"));
            }
        } catch(Exception e) {
            System.err.println("db error in displayFuels");
        }
        Collections.sort(fuels);

        //PREPARING MESSAGE
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String replyText = "Seleziona il carburante:";
        SendMessage message = SendMessage.builder().chatId(chatId).text(replyText).replyMarkup(new InlineKeyboardMarkup(prepareButtons(fuels))).build();

        //SENDING MESSAGE
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            System.err.println("telegram error in displayModels");
        }
    }

    private void displayTransmissions(Update update) {
        //RETRIEVING
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT DISTINCT cambio FROM annunci WHERE marca = ? AND modello = ? AND carburante = ?;");
            statement.setString(1, choices.get("marca"));
            statement.setString(2, choices.get("modello"));
            statement.setString(3, choices.get("carburante"));
            ResultSet rs = statement.executeQuery();
            while(rs.next()) {
                transmissions.add(rs.getString("cambio"));
            }
        } catch(Exception e) {
            System.err.println("db error in displayTransmissions");
        }
        Collections.sort(transmissions);

        //PREPARING MESSAGE
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String replyText = "Seleziona il tipo di cambio:";
        SendMessage message = SendMessage.builder().chatId(chatId).text(replyText).replyMarkup(new InlineKeyboardMarkup(prepareButtons(transmissions))).build();

        //SENDING MESSAGE
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            System.err.println("telegram error in displayTransmissions");
        }
    }

    private void displayOrders(Update update) {
        //PREPARING MESSAGE
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String replyText = "Selezione l'ordinamento:";
        SendMessage message = SendMessage.builder().chatId(chatId).text(replyText).replyMarkup(new InlineKeyboardMarkup(prepareButtons(orderOptions))).build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            System.err.println("telegram error in displayFilters");
        }
    }

    private void displayResults(Update update) {

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
            } else if(carBrands.contains(callbackData) && choices.get("marca")==null) {
                choices.put("marca", callbackData);
                displayModels(update);
            } else if(models.contains(callbackData) && choices.get("modello")==null) {
                choices.put("modello", callbackData);
                displayFuels(update);
            } else if(fuels.contains(callbackData) && choices.get("carburante")==null) {
                choices.put("carburante", callbackData);
                displayTransmissions(update);
            } else if(transmissions.contains(callbackData) && choices.get("cambio")==null) {
                choices.put("cambio", callbackData);
                displayOrders(update);
            } else if(orderOptions.contains(callbackData) && choices.get("ordinamento")==null) {
                choices.put("ordinamento", callbackData);
                displayResults(update);
            }
        }
    }
}