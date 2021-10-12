import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.parsers.HEntity;
import gearth.extensions.parsers.HEntityType;
import gearth.extensions.parsers.HEntityUpdate;
import gearth.extensions.parsers.HGender;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javax.swing.Timer;
import java.nio.charset.StandardCharsets;
import java.util.*;

@ExtensionInfo(
        Title = "GMimic ",
        Description = "So Fucking Funny Extension!",
        Version = "1.2",
        Author = "Julianty"
)

public class GMimic extends ExtensionForm {
    public CheckBox checkSpeech, checkLook, checkMotto, checkTile, checkName, checkBot;
    public TextField textFieldDelay;

    TreeMap<Integer,String> BotIndexAndBotName = new TreeMap<>();
    TreeMap<Integer,Integer> UserIdAndIndex = new TreeMap<>();
    TreeMap<Integer,String> UserIdAndName = new TreeMap<>();
    TreeMap<Integer,String> UserIdAndFigureId = new TreeMap<>();
    TreeMap<Integer,HGender> UserIdAndGender = new TreeMap<>();
    TreeMap<Integer,String> UserIdAndMotto = new TreeMap<>();

    public String YourName;
    public int UserId, YourId = -1;
    public int X, Y;

    // Timer para definir el numero de clicks
    Timer timer1 = new Timer(1, e -> sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, X, Y)));

    @Override
    protected void initExtension() {

        primaryStage.setOnShowing(e->{
            // sendToServer("GetOccupiedTiles") // Get RoomEntryTile
            // sendToServer("GetRoomEntryTile") // Get RoomOccupiedTiles for furnitures and the coords (could be important)
            sendToServer(new HPacket("InfoRetrieve", HMessage.Direction.TOSERVER));
            sendToServer(new HPacket("GetHeightMap", HMessage.Direction.TOSERVER)); // Get Flooritems, Wallitems, etc. Without restart room
        });

        // When the user close the extension (only works here!)
        primaryStage.setOnCloseRequest(e -> {
            BotIndexAndBotName.clear(); UserIdAndIndex.clear(); UserIdAndName.clear();  UserIdAndFigureId.clear();
            UserIdAndGender.clear();    UserIdAndMotto.clear(); UserId = -1;    X = 0;  Y = 0;
            checkLook.setSelected(false);   checkSpeech.setSelected(false); checkBot.setSelected(false);
            Platform.runLater(() -> checkName.setText("Click the user to mimic")); // Platform.exit();
        });

        intercept(HMessage.Direction.TOCLIENT, "UserObject", hMessage -> {
            YourId = hMessage.getPacket().readInteger();
            YourName = hMessage.getPacket().readString();
        });

        // Runs when the textfield changes
        textFieldDelay.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                Integer.parseInt(textFieldDelay.getText());
            } catch (NumberFormatException e) {
                if("".equals(textFieldDelay.getText())){
                    textFieldDelay.setText("1");
                }
                else {
                    textFieldDelay.setText(oldValue);
                }
            }
        });

        intercept(HMessage.Direction.TOSERVER, "GetSelectedBadges", hMessage -> {
            UserId = hMessage.getPacket().readInteger();
            try {
                if(!UserIdAndName.get(UserId).equals(YourName)){
                    if(checkName.isSelected()){ // Get user id for show the name
                        Platform.runLater(() -> checkName.setText(UserIdAndName.get(UserId))); // This update GUI
                    }
                    if(checkLook.isSelected()){ // Copy user look
                        sendToServer(new HPacket("UpdateFigureData", HMessage.Direction.TOSERVER,
                                UserIdAndGender.get(UserId).toString(), UserIdAndFigureId.get(UserId)));
                    }
                    if(checkMotto.isSelected()){ // Copy motto
                        sendToServer(new HPacket("ChangeMotto", HMessage.Direction.TOSERVER, UserIdAndMotto.get(UserId)));
                    }
                }
            }catch (NullPointerException ignored){}
        });

        intercept(HMessage.Direction.TOCLIENT, "Users", hMessage -> {
            //IdAndIndex.clear(); Al no borrar la lista esos datos se almacenan ojo con eso
            try {
                HPacket hPacket = hMessage.getPacket();
                HEntity[] roomUsersList = HEntity.parse(hPacket);
                for (HEntity hEntity: roomUsersList){
                    if (hEntity.getEntityType().equals(HEntityType.HABBO)){ // Si es Habbo hace esto
                        // El ID del usuario no esta en el Map (Dictionary en c#)
                        if(!UserIdAndIndex.containsKey(hEntity.getId())){
                            UserIdAndIndex.put(hEntity.getId(), hEntity.getIndex());
                            UserIdAndName.put(hEntity.getId(), hEntity.getName());
                            UserIdAndFigureId.put(hEntity.getId(), hEntity.getFigureId());
                            UserIdAndGender.put(hEntity.getId(), hEntity.getGender());
                            UserIdAndMotto.put(hEntity.getId(), hEntity.getMotto());
                        }
                        else { // Se especifica la key, para remplazar el value por uno nuevo
                            UserIdAndIndex.replace(hEntity.getId(), hEntity.getIndex());
                            UserIdAndName.replace(hEntity.getId(), hEntity.getName());
                            UserIdAndFigureId.replace(hEntity.getId(), hEntity.getFigureId());
                            UserIdAndGender.replace(hEntity.getId(), hEntity.getGender());
                            UserIdAndMotto.replace(hEntity.getId(), hEntity.getMotto());
                        }
                    }
                    else if(hEntity.getEntityType().equals(HEntityType.BOT)){
                        if(!BotIndexAndBotName.containsKey(hEntity.getIndex())){
                            BotIndexAndBotName.put(hEntity.getIndex(), hEntity.getName());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println(UserIdAndName);
        });

        // Obtiene las coordenadas del usuario
        intercept(HMessage.Direction.TOCLIENT, "UserUpdate", hMessage -> {
            /*int notImportant = hMessage.getPacket().readInteger();
            int UserIndex = hMessage.getPacket().readInteger();
            X = hMessage.getPacket().readInteger(); Y = hMessage.getPacket().readInteger();
            */
            for (HEntityUpdate hEntityUpdate: HEntityUpdate.parse(hMessage.getPacket())){
                if(checkTile.isSelected()){
                    try {
                        if(hEntityUpdate.getIndex() == UserIdAndIndex.get(UserId)){
                            X = hEntityUpdate.getTile().getX(); Y = hEntityUpdate.getMovingTo().getY();
                            System.out.println("x: " + X + ", y: " + Y); // Necesito mejorar el algoritmo
                            sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, X, Y));
                        }
                    }catch (NullPointerException ignored){ }
                }
            }
        });

        // Intercepta cuando alguien te susurra
        intercept(HMessage.Direction.TOCLIENT, "Whisper", hMessage -> {
            int CurrentIndex = hMessage.getPacket().readInteger();
            //String SaySomething = hMessage.getPacket().readString(StandardCharsets.UTF_8); // read the string as UTF8
            String SaySomething = hMessage.getPacket().readString();
            int Ignore = hMessage.getPacket().readInteger();
            int BubbleColor = hMessage.getPacket().readInteger();
            try {
                Thread.sleep(Integer.parseInt(textFieldDelay.getText()));
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
            //  That should support the turkish characters
            HPacket hPacket = hMessage.getPacket().appendString(SaySomething, StandardCharsets.UTF_8);
            System.out.println("usuario susurro: " + hPacket);
            if(checkSpeech.isSelected()){
                if(CurrentIndex == UserIdAndIndex.get(UserId)){  // Obtengo el index del usuario que susurra
                    // SaySomething
                    sendToServer(new HPacket("Whisper", HMessage.Direction.TOSERVER,
                            checkName.getText() + " " + SaySomething, BubbleColor));
                }
            }
            if(checkBot.isSelected()){
                // Generally, bots have firs numbers as index (0, 1, 2...)
                if(BotIndexAndBotName.containsKey(CurrentIndex)){
                    sendToServer(new HPacket("Chat", HMessage.Direction.TOSERVER,
                            BotIndexAndBotName.get(CurrentIndex) + ": " + SaySomething, BubbleColor));
                }
            }
        });

        // Intercepta cuando un bot o usuario habla
        intercept(HMessage.Direction.TOCLIENT, "Chat", hMessage -> {
            int CurrentIndex = hMessage.getPacket().readInteger();
            // String SaySomething = hMessage.getPacket().readString(StandardCharsets.UTF_8);
            String SaySomething = hMessage.getPacket().readString();
            System.out.println("saysomethign " + SaySomething);
            int Ignore = hMessage.getPacket().readInteger();
            int BubbleColor = hMessage.getPacket().readInteger();
            try {
                Thread.sleep(Integer.parseInt(textFieldDelay.getText()));
            } catch (InterruptedException interruptedException) { interruptedException.printStackTrace(); }
            HPacket hPacket2 = hMessage.getPacket().appendString(SaySomething, StandardCharsets.UTF_8);
            System.out.println("usuario hablo: " + hPacket2.readString());

            if(checkSpeech.isSelected()){
                if(CurrentIndex == UserIdAndIndex.get(UserId)){  // Obtengo el index del usuario que habla y lo comparo
                    sendToServer(new HPacket("Chat", HMessage.Direction.TOSERVER, SaySomething, BubbleColor, 1));
                }
            }
            if(checkBot.isSelected()){
                // Por lo general los bots tienen Index con los primeros numeros 0, 1, 2...
                if(BotIndexAndBotName.containsKey(CurrentIndex)){
                    sendToServer(new HPacket("Chat", HMessage.Direction.TOSERVER,
                            BotIndexAndBotName.get(CurrentIndex) + ": " + SaySomething, BubbleColor, 1));
                }
            }
        });

        // Intercepta cuando un bot o usuario "GRITA"
        intercept(HMessage.Direction.TOCLIENT, "Shout", hMessage -> {
            int CurrentIndex = hMessage.getPacket().readInteger();
            //String SaySomething = hMessage.getPacket().readString(StandardCharsets.UTF_8);
            String SaySomething = hMessage.getPacket().readString();
            int Ignore = hMessage.getPacket().readInteger();
            int BubbleColor = hMessage.getPacket().readInteger();
            try {
                Thread.sleep(Integer.parseInt(textFieldDelay.getText()));
            } catch (InterruptedException interruptedException) { interruptedException.printStackTrace(); }
            HPacket hPacket = hMessage.getPacket().appendString(SaySomething, StandardCharsets.UTF_8);

            if(checkSpeech.isSelected()){   // Obtengo el index del usuario que habla y lo comparo
                if(CurrentIndex == UserIdAndIndex.get(UserId)){
                    sendToServer(new HPacket("Shout", HMessage.Direction.TOSERVER, SaySomething, BubbleColor,0));
                }
            }
            if(checkBot.isSelected()){
                if(BotIndexAndBotName.containsKey(CurrentIndex)){
                    sendToServer(new HPacket("Shout", HMessage.Direction.TOSERVER,
                            BotIndexAndBotName.get(CurrentIndex) + ": " + hPacket.readString(), BubbleColor, 1));
                }
            }
        });

        // When the user is typing
        intercept(HMessage.Direction.TOCLIENT, "UserTyping", hMessage -> {
            if(checkSpeech.isSelected()){
                int CurrentIndex = hMessage.getPacket().readInteger();
                int stateTyping = hMessage.getPacket().readInteger(); // "1" when starts and "0" when ends
                if(CurrentIndex == UserIdAndIndex.get(UserId) && stateTyping == 1){
                    sendToServer(new HPacket("StartTyping", HMessage.Direction.TOSERVER));
                }
                else if(CurrentIndex == UserIdAndIndex.get(UserId) && stateTyping == 0){
                    sendToServer(new HPacket("CancelTyping", HMessage.Direction.TOSERVER));
                }
            }
        });
    }

    public void handleCheckSpeech(){
        if(checkSpeech.isSelected() && checkName.getText().equals("Click the user to mimic")){
            checkSpeech.setSelected(false);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information!");
            alert.setHeaderText("You forgot to select a user");
            alert.setContentText("You should select the user that you want to mimic");

            Stage stage = (Stage) checkName.getScene().getWindow(); // Get the scene where is the control
            double xAlert = stage.getX();               double yAlert = stage.getY();
            alert.setX(xAlert + 400.0);                 alert.setY(yAlert + 10.0);
            alert.show();
        }
    }

    public void handlecheckBot() {
        if(checkBot.isSelected()){
            checkSpeech.setSelected(false); checkSpeech.setDisable(true);
            checkLook.setSelected(false);   checkLook.setDisable(true);
            checkMotto.setSelected(false); checkMotto.setDisable(true);
            checkTile.setSelected(false);   checkTile.setDisable(true);
            checkName.setSelected(false);   checkName.setDisable(true);
        }
        else {
            checkSpeech.setDisable(false);  checkLook.setDisable(false);    checkMotto.setDisable(false);
            checkTile.setDisable(false);    checkName.setDisable(false);
        }
    }

    public void handlecheckLook() {
        if(checkLook.isSelected()){
            sendToServer(new HPacket("UpdateFigureData", HMessage.Direction.TOSERVER,
                    UserIdAndGender.get(UserId).toString(), UserIdAndFigureId.get(UserId)));
        }
    }

    public void handlecheckMotto() {
        if(checkMotto.isSelected()){
            sendToServer(new HPacket("ChangeMotto", HMessage.Direction.TOSERVER, UserIdAndMotto.get(UserId)));
        }
    }

    public void handlecheckTile() {
        if(checkTile.isSelected()){
            timer1.start(); // The timer starts
        }
        else if(!checkTile.isSelected()){
            timer1.stop();  // The timer stops
        }
    }
}