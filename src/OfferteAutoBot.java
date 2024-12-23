import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class OfferteAutoBot implements LongPollingSingleThreadUpdateConsumer {
    private TelegramClient telegramClient;
    private Connection connection;

    private String queryType = "";
    private final HashMap<String, String> choices = new HashMap<String, String>();

    private final List<String> carBrands = new ArrayList<>();
    private final List<String> models = new ArrayList<>();
    private final List<String> fuels = new ArrayList<>();
    private final List<String> transmissions = new ArrayList<>();
    private final List<String> orderOptions = new ArrayList<>();
    private final List<String> results = new ArrayList<>();

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
            buttons++;
            var btn = new InlineKeyboardButton(lst.get(i));
            btn.setCallbackData(lst.get(i));
            row.add(btn);
            if((buttons%4 == 0 && buttons!=0) || i==lst.size()-1) {
                rows.add(row);
                row = new InlineKeyboardRow();
            }
        }
        return rows;
    }

    private void displayHelp(Update update) {
        //PREPARING MESSAGE
        Long chatId = update.getMessage().getChatId();
        String replyText = "/start è il comando per iniziare a interagire con il bot\n" +
                "/restart va usato una volta finita una richiesta al bot per tornare al menù principale\n" +
                "Cerca auto permette di cercare in base ai criteri: marca, modello, carburante, cambio e ordinare in base a prezzo, km, o il rapporto tra prezzo e km\n" +
                "Report auto fornisce delle statistiche su un modello: numero annunci, media e range di prezzo, km e anno, il numero di annunci per ogni carburante e per ogni tipo di cambio";
        SendMessage message = SendMessage.builder().chatId(chatId).text(replyText).build();

        //SENDING MESSAGE
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            System.err.println("telegram error in displayMenu");
        }
    }

    private void displayMenu(Update update) {
        //PREPARING BUTTONS
        List<InlineKeyboardRow> rows = new ArrayList<>();
        var r1 = new InlineKeyboardRow();
        var tmp = new InlineKeyboardButton("Cerca auto\uD83D\uDD0E");
        tmp.setCallbackData("cerca_auto");
        r1.add(tmp);
        rows.add(r1);

        var r2 = new InlineKeyboardRow();
        tmp = new InlineKeyboardButton("Report auto\uD83D\uDCC8");
        tmp.setCallbackData("report_auto");
        r2.add(tmp);
        rows.add(r2);

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
            System.err.println("telegram error in displayFuels");
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

    private void displaySearchResults(Update update) {
        List<String> replies = new ArrayList<String>();
        try {
            String sql = "SELECT DISTINCT * FROM annunci WHERE marca = ? AND modello = ? AND carburante = ? AND cambio = ? ORDER BY "+choices.get("ordinamento")+";";

            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, choices.get("marca"));
            statement.setString(2, choices.get("modello"));
            statement.setString(3, choices.get("carburante"));
            statement.setString(4, choices.get("cambio"));

            ResultSet rs = statement.executeQuery();
            int size = 0;
            while(rs.next()) {
                replies.add("id:"+ rs.getString("id") + "\n"+
                        "km: " +rs.getString("km") +
                        "\nanno: " + rs.getInt("anno")+
                        "\nposizione: " + rs.getString("provincia") + " \n" +
                        "<a href=\""+rs.getString("link")+"\">Visita il sito \uD83D\uDD17</a>");
                size++;
            }
            String summary = "La tua ricerca:\n" + choices.get("marca") + " " + choices.get("modello") + " " +
                    choices.get("carburante") + " " + choices.get("cambio") + "\n" + "La ricerca ha prodotto " +
                    size + ((size==1)?" risultato":" risultati");
            replies.addFirst(summary);
        } catch(Exception e) {
            System.err.println("db error in displayResults");
        }

        //SENDING MESSAGES
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        for(var reply : replies) {
            SendMessage message = SendMessage.builder().chatId(chatId).text(reply).build();
            message.setParseMode("HTML");
            try {
                telegramClient.execute(message);
            } catch (TelegramApiException e) {
                System.err.println("telegram error in displayMenu");
            }
        }
    }

    private void displayReportResults(Update update) {
        String reply = "Report per " + choices.get("marca") + " " + choices.get("modello") + ":\n\n";
        List<String> results = new ArrayList<String>();
        try {
            Statement statement = connection.createStatement();
            ResultSet rs;
            String where = " WHERE marca = \"" + choices.get("marca") + "\" AND modello = \"" + choices.get("modello") + "\" ";
            //numero annunci
            rs = statement.executeQuery("SELECT COUNT(*) FROM annunci " + where + ";");
            if (rs.next()) { reply += "Numero annunci: " + rs.getInt(1) + "\n\n"; }

            //prezzo medio e range prezzo(min-max)
            rs = statement.executeQuery("SELECT AVG(prezzo), MIN(prezzo), MAX(prezzo) FROM annunci" + where + ";");
            if (rs.next()) { reply += "Prezzo medio: " + rs.getInt(1) + "€\n" + "Range prezzo: " +
                    rs.getInt(2) + "€ - " + rs.getInt(3) + "€\n\n"; }

            //km medi e range km(min-max)
            rs = statement.executeQuery("SELECT AVG(km), MIN(km), MAX(km) FROM annunci" + where + ";");
            if (rs.next()) { reply += "Km medi: " + rs.getInt(1) + "km\n" + "Range km: " +
                    rs.getInt(2) + "km - " + rs.getInt(3) + "km\n\n"; }


            //anno medio e range anno (min-max)
            rs = statement.executeQuery("SELECT AVG(anno), MIN(anno), MAX(anno) FROM annunci" + where + ";");
            if (rs.next()) { reply += "Anno medio: " + rs.getInt(1) + "\n" + "Range anni: " +
                    rs.getInt(2) + " - " + rs.getInt(3) + "\n"; }

            //numero per tipi carburante
            reply += "\nAnnunci per tipo di carburante:\n";
            rs = statement.executeQuery("SELECT carburante, COUNT(*) FROM annunci " + where + " GROUP BY carburante;");
            while(rs.next()) {
                reply += rs.getString(1) + " " + rs.getInt(2) + "\n";
            }


            //numero per tipi cambio
            reply += "\nAnnunci per tipo di cambio:\n";
            rs = statement.executeQuery("SELECT cambio, COUNT(*) FROM annunci " + where + " GROUP BY cambio;");
            while(rs.next()) {
                reply += rs.getString(1) + " " + rs.getInt(2) + "\n";
            }

        } catch(SQLException e) {
          System.err.println("db error in displayReportResults");
        }

        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        SendMessage message = SendMessage.builder().chatId(chatId).text(reply).build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            System.err.println("telegram error in displayReportResults");
        }
    }

    private void handleSearch(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        if(carBrands.contains(callbackData) && choices.get("marca")==null) {
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
            displaySearchResults(update);
        }
    }

    private void handleReport(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        if(carBrands.contains(callbackData) && choices.get("marca")==null) {
            choices.put("marca", callbackData);
            displayModels(update);
        } else if(models.contains(callbackData) && choices.get("modello")==null) {
            choices.put("modello", callbackData);
            displayReportResults(update);
        }
    }

    private void registerUser(Message message) {
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO UTENTI(id, username, first_access) VALUES (?, ?, ?);");
            Long userId = message.getFrom().getId();
            statement.setLong(1, userId);

            String username = message.getFrom().getUserName();
            statement.setString(2, username);

            statement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));

            statement.execute();
        } catch(SQLIntegrityConstraintViolationException e) {
            System.err.println("constraint violation in registerUser, not inserting");
        } catch (Exception e) {
            System.err.println("db error in registerUser");
        }
    }

    private void saveAnnouncement(Update update) {
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO SALVATI(id_utente, id_annuncio) VALUES (?, ?);");
            statement.setLong(1, update.getMessage().getFrom().getId());
            String msgText = update.getMessage().getText();
            statement.setInt(2, Integer.parseInt(msgText.split(" ")[1]));
            statement.execute();
        } catch (Exception e) {
            System.err.println("db error in saveAnnouncement");
            e.printStackTrace();
        }
    }

    private void resetData() {
        queryType = "";
        choices.clear();
        carBrands.clear();
        models.clear();
        fuels.clear();
        transmissions.clear();
        results.clear();
    }

    @Override
    public void consume(Update update)
    {
        if (update.hasMessage() && update.getMessage().hasText()) {
            registerUser(update.getMessage());

            String msgText = update.getMessage().getText();
            if(msgText.equals("/start")) {
                resetData();    //per sicurezza
                displayMenu(update);
            } else if(msgText.equals("/restart")) {
                resetData();
                displayMenu(update);
            } else if(msgText.equals("/help")) {
                displayHelp(update);
            } else if(msgText.startsWith("/save")) {
                saveAnnouncement(update);
            }
        } else if(update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            if(callbackData.equals("cerca_auto")) {
                displayCarBrands(update);
                queryType = "search";
            } else if(callbackData.equals("report_auto")) {
                displayCarBrands(update);
                queryType = "report";
            }

            if(queryType.equals("search")) handleSearch(update);
            else if(queryType.equals("report")) handleReport(update);

        }
    }
}