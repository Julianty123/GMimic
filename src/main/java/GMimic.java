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
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javax.swing.Timer;
import java.nio.charset.StandardCharsets;
import java.util.*;

@ExtensionInfo(
        Title = "GMimic ",
        Description = "So Fucking Funny Extension!",
        Version = "1.2.1",
        Author = "Julianty"
)

public class GMimic extends ExtensionForm {
    public CheckBox checkLook, checkMotto, checkTile, checkBoxName;
    public TextField txtDelayFollow, txtDelayAutoStart, textFieldSaySomething, textFieldListenSay;
    public RadioButton radioButtonBot, radioCustomSpeech, radioMimicSpeech, radioButtonOff;
    public Text textListenSay, textSay;
    public CheckBox checkUnfollow;

    public HashMap<Integer,String> BotIndexAndBotName = new HashMap<>();
    public HashMap<Integer,Integer> mapUserIdAndIndex = new HashMap<>();
    public HashMap<Integer,String> UserIdAndName = new HashMap<>();
    public HashMap<Integer,String> UserIdAndFigureId = new HashMap<>();
    public HashMap<Integer,HGender> UserIdAndGender = new HashMap<>();
    public HashMap<Integer,String> UserIdAndMotto = new HashMap<>();

    public String yourUserName;
    public String userToMimic;
    public int userId;
    public int yourId = -1;
    public int xUser, yUser;

    Timer timerNumberClicks = null;
    Thread threadFollowUser = new Thread(()->{
        try {
            Thread.sleep(Integer.parseInt(txtDelayAutoStart.getText())); // sleep thread
            Platform.runLater(()-> checkTile.setSelected(true));
            timerNumberClicks.start(); // enabled timer again
            sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 999, "Follow user enabled! ", 0, 25, 0, -1));
        } catch (InterruptedException interruptedException) { interruptedException.printStackTrace();}
    });

    // When the user open the extension
    @Override
    protected void onShow() {
        // sendToServer("GetOccupiedTiles") // Get RoomEntryTile
        // sendToServer("GetRoomEntryTile") // Get RoomOccupiedTiles for furniture and the coords (could be important)
        sendToServer(new HPacket("InfoRetrieve", HMessage.Direction.TOSERVER)); // Get your username
        sendToServer(new HPacket("GetHeightMap", HMessage.Direction.TOSERVER)); // Get Flooritems, Wallitems, etc. Without restart room

        timerNumberClicks = new Timer(Integer.parseInt(txtDelayFollow.getText()), t -> {
            try {
                sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, xUser, yUser));
                System.out.println("x: " + xUser + " y: " + yUser);
            }catch (NullPointerException ignored){}
        });
    }

    // When the user close the extension
    @Override
    protected void onHide() {
        BotIndexAndBotName.clear(); mapUserIdAndIndex.clear(); UserIdAndName.clear();  UserIdAndFigureId.clear();

        UserIdAndGender.clear();    UserIdAndMotto.clear(); userId = -1;    xUser = 0;  yUser = 0;

        checkLook.setSelected(false);   textSay.setVisible(false);  textListenSay.setVisible(false);
        textFieldListenSay.setDisable(true);    textFieldSaySomething.setDisable(true); radioButtonOff.setSelected(true);

        Platform.runLater(() -> checkBoxName.setText("Click the user to mimic")); // Platform.exit();
    }

    @Override
    protected void initExtension() {
        // primaryStage.setOnShowing(e->{});
        // primaryStage.setOnCloseRequest(e -> {});

        intercept(HMessage.Direction.TOCLIENT, "UserObject", hMessage -> {
            yourId = hMessage.getPacket().readInteger();
            yourUserName = hMessage.getPacket().readString();
        });

        // Runs when the textField changes
        txtDelayFollow.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                Integer.parseInt(txtDelayFollow.getText());
            } catch (NumberFormatException e) {
                if("".equals(txtDelayFollow.getText()))
                    txtDelayFollow.setText("1");
                else
                    txtDelayFollow.setText(oldValue);
            }

            timerNumberClicks.setDelay(Integer.parseInt(txtDelayFollow.getText()));
        });

        intercept(HMessage.Direction.TOSERVER, "MoveAvatar", hMessage -> {
            try{
                if(primaryStage.isShowing() && checkUnfollow.isSelected()){
                    if(!threadFollowUser.isAlive() && checkTile.isSelected()){
                        Platform.runLater(()-> checkTile.setSelected(false));
                        timerNumberClicks.stop();
                        sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, 999, "Follow user disabled! ", 0, 25, 0, -1));
                        threadFollowUser.start();
                    }
                }
            }catch (Exception e) { System.out.println(e.getCause().getMessage()); }
        });

        intercept(HMessage.Direction.TOSERVER, "GetSelectedBadges", hMessage -> {
            int currentId = hMessage.getPacket().readInteger();
            // userId = hMessage.getPacket().readInteger();
            try {
                if(!UserIdAndName.get(currentId).equals(yourUserName)){
                    userId = currentId;
                    if(checkBoxName.isSelected()){ // Get user id for show the name
                        userToMimic = UserIdAndName.get(userId);
                        Platform.runLater(() -> checkBoxName.setText("User to mimic: "+ userToMimic)); // This update GUI
                    }
                    if(checkLook.isSelected()){ // Copy user look
                        sendToServer(new HPacket("UpdateFigureData", HMessage.Direction.TOSERVER,
                                UserIdAndGender.get(userId).toString(), UserIdAndFigureId.get(userId)));
                    }
                    if(checkMotto.isSelected()){ // Copy motto
                        sendToServer(new HPacket("ChangeMotto", HMessage.Direction.TOSERVER, UserIdAndMotto.get(userId)));
                    }

                    checkBoxName.setSelected(false);
                }
            }catch (NullPointerException ignored){}
        });

        intercept(HMessage.Direction.TOCLIENT, "Users", hMessage -> {
            //IdAndIndex.clear(); Al no borrar la lista esos datos se almacenan ojo con eso
            try {
                HPacket hPacket = hMessage.getPacket(); // System.out.println(hPacket);
                HEntity[] roomUsersList = HEntity.parse(hPacket);
                for (HEntity hEntity: roomUsersList){
                    if (hEntity.getEntityType().equals(HEntityType.HABBO)){
                        // El ID del usuario no esta en el Map (Dictionary en c#)
                        if(!mapUserIdAndIndex.containsKey(hEntity.getId())){
                            mapUserIdAndIndex.put(hEntity.getId(), hEntity.getIndex());
                            UserIdAndName.put(hEntity.getId(), hEntity.getName());
                            UserIdAndFigureId.put(hEntity.getId(), hEntity.getFigureId());
                            UserIdAndGender.put(hEntity.getId(), hEntity.getGender());
                            UserIdAndMotto.put(hEntity.getId(), hEntity.getMotto());
                        }
                        else { // Se especifica la key, para remplazar el value por uno nuevo
                            mapUserIdAndIndex.replace(hEntity.getId(), hEntity.getIndex());
                            UserIdAndName.replace(hEntity.getId(), hEntity.getName());
                            UserIdAndFigureId.replace(hEntity.getId(), hEntity.getFigureId());
                            UserIdAndGender.replace(hEntity.getId(), hEntity.getGender());
                            UserIdAndMotto.replace(hEntity.getId(), hEntity.getMotto());
                        }
                    }
                    if(hEntity.getEntityType().equals(HEntityType.BOT) || hEntity.getEntityType().equals(HEntityType.OLD_BOT)){
                        if(!BotIndexAndBotName.containsKey(hEntity.getIndex()))
                            BotIndexAndBotName.put(hEntity.getIndex(), hEntity.getName());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Get the user coords
        intercept(HMessage.Direction.TOCLIENT, "UserUpdate", hMessage -> {
            /* int notImportant = hMessage.getPacket().readInteger();
            int UserIndex = hMessage.getPacket().readInteger();
            X = hMessage.getPacket().readInteger(); Y = hMessage.getPacket().readInteger(); */
            for (HEntityUpdate hEntityUpdate: HEntityUpdate.parse(hMessage.getPacket())){
                if(checkTile.isSelected()){
                    try {
                        if(hEntityUpdate.getIndex() == mapUserIdAndIndex.get(userId)){
                            // hEntityUpdate.getMovingTo()... is more slow in this case getTile
                            xUser = hEntityUpdate.getMovingTo().getX(); yUser = hEntityUpdate.getMovingTo().getY();
                            // sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, X, Y));
                        }
                    }catch (NullPointerException ignored){ }
                }
            }
        });

        // Intercepts when a user whispers
        intercept(HMessage.Direction.TOCLIENT, "Whisper", hMessage -> {
            int currentIndex = hMessage.getPacket().readInteger();
            String WhisperSomething = hMessage.getPacket().readString(StandardCharsets.UTF_8); // Supports the turkish characters
            int unknown = hMessage.getPacket().readInteger();
            int bubbleColor = hMessage.getPacket().readInteger();
            try {
                Thread.sleep(Integer.parseInt(txtDelayFollow.getText()));
            } catch (InterruptedException ignored) {}

            if(radioMimicSpeech.isSelected()){
                if(currentIndex == mapUserIdAndIndex.get(userId)){  // Get the index of the whispering user
                    sendToServer(new HPacket("Whisper", HMessage.Direction.TOSERVER,
                            userToMimic + " " + WhisperSomething, bubbleColor));
                }
            }
            if(radioButtonBot.isSelected()){
                // Generally, bots have firs numbers as index (0, 1, 2...)
                if(BotIndexAndBotName.containsKey(currentIndex)){
                    sendToServer(new HPacket("Chat", HMessage.Direction.TOSERVER,
                            BotIndexAndBotName.get(currentIndex) + ": " + WhisperSomething, bubbleColor));
                }
            }
        });

        // Intercepts when a user or bot talks
        intercept(HMessage.Direction.TOCLIENT, "Chat", hMessage -> {
            int currentIndex = hMessage.getPacket().readInteger();
            String saySomething = hMessage.getPacket().readString(StandardCharsets.UTF_8); // accept any character as ş ğ ç ı ü ä ö ...
            int unknown = hMessage.getPacket().readInteger();
            int BubbleColor = hMessage.getPacket().readInteger();
            try {Thread.sleep(Integer.parseInt(txtDelayFollow.getText()));}
            catch (InterruptedException interruptedException) { interruptedException.printStackTrace();}

            if(radioMimicSpeech.isSelected()){
                if(currentIndex == mapUserIdAndIndex.get(userId)){  // Obtengo el index del usuario que habla y lo comparo
                    sendToServer(new HPacket("Chat", HMessage.Direction.TOSERVER, saySomething, BubbleColor, 1));
                }
            }
            if(radioCustomSpeech.isSelected()){
                if( currentIndex == mapUserIdAndIndex.get(userId) && saySomething.equalsIgnoreCase(textFieldListenSay.getText()) ){
                    sendToServer(new HPacket("Chat", HMessage.Direction.TOSERVER, textFieldSaySomething.getText(), BubbleColor, 1));
                }
            }
            if(radioButtonBot.isSelected()){
                // Por lo general los bots tienen Index con los primeros numeros 0, 1, 2...
                if(BotIndexAndBotName.containsKey(currentIndex)){
                    sendToServer(new HPacket("Chat", HMessage.Direction.TOSERVER,
                            BotIndexAndBotName.get(currentIndex) + ": " + saySomething, BubbleColor, 1));
                }
            }
        });

        // Intercepts when a user or bot shouts
        intercept(HMessage.Direction.TOCLIENT, "Shout", hMessage -> {
            int CurrentIndex = hMessage.getPacket().readInteger();
            String ShoutSomething = hMessage.getPacket().readString(StandardCharsets.UTF_8);
            int unknown = hMessage.getPacket().readInteger();
            int BubbleColor = hMessage.getPacket().readInteger();

            try {Thread.sleep(Integer.parseInt(txtDelayFollow.getText()));}
            catch (InterruptedException interruptedException) { interruptedException.printStackTrace();}

            if(radioMimicSpeech.isSelected()){   // Obtengo el index del usuario que habla y lo comparo
                if(CurrentIndex == mapUserIdAndIndex.get(userId))
                    sendToServer(new HPacket("Shout", HMessage.Direction.TOSERVER, ShoutSomething, BubbleColor,0));
            }
            if(radioButtonBot.isSelected()){
                if(BotIndexAndBotName.containsKey(CurrentIndex)){
                    sendToServer(new HPacket("Shout", HMessage.Direction.TOSERVER,
                            BotIndexAndBotName.get(CurrentIndex) + ": " + ShoutSomething, BubbleColor, 1));
                }
            }
        });

        // When the user is typing
        intercept(HMessage.Direction.TOCLIENT, "UserTyping", hMessage -> {
            if(radioMimicSpeech.isSelected()){
                int currentIndex = hMessage.getPacket().readInteger();
                int stateTyping = hMessage.getPacket().readInteger(); // "1" when starts and "0" when ends
                if(currentIndex == mapUserIdAndIndex.get(userId) && stateTyping == 1)
                    sendToServer(new HPacket("StartTyping", HMessage.Direction.TOSERVER));
                else if(currentIndex == mapUserIdAndIndex.get(userId) && stateTyping == 0)
                    sendToServer(new HPacket("CancelTyping", HMessage.Direction.TOSERVER));
            }
        });
    }

    public void handleMimicSpeech(){
        if(radioMimicSpeech.isSelected()){
            if(checkBoxName.getText().equals("Click the user to mimic")){
                radioMimicSpeech.setSelected(false);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Information!");
                alert.setHeaderText("You forgot to select a user");
                alert.setContentText("You should select the user that you want to mimic");

                Stage stage = (Stage) checkBoxName.getScene().getWindow(); // Get the scene where is the control
                double xAlert = stage.getX();               double yAlert = stage.getY();
                alert.setX(xAlert + 400.0);                 alert.setY(yAlert + 10.0);
                alert.show();
            }
            else {
                textSay.setVisible(false);  textListenSay.setVisible(false);
                textFieldListenSay.setDisable(true); textFieldSaySomething.setDisable(true);
            }
        }
    }

    public void handleCustomSpeech(){
        if(radioCustomSpeech.isSelected()){
            textSay.setVisible(true);  textListenSay.setVisible(true);
            textFieldListenSay.setDisable(false); textFieldSaySomething.setDisable(false);
        }
    }

    public void handleRadioButtonBot() {
        // i need to organize more this idk
        /*if(radioButtonBot.isSelected()){
            checkLook.setSelected(false);   checkLook.setDisable(true);
            checkMotto.setSelected(false); checkMotto.setDisable(true);
            checkTile.setSelected(false);   checkTile.setDisable(true);
            checkBoxName.setSelected(false);   checkBoxName.setDisable(true);
            System.out.println("activado");
        }
        else {
            System.out.println("desactivado");
            radioMimicSpeech.setDisable(false); radioCustomSpeech.setDisable(false);
            checkLook.setDisable(false);    checkMotto.setDisable(false);
            checkTile.setDisable(false);    checkBoxName.setDisable(false);
        }*/
    }

    public void handleCheckLook() {
        if(checkLook.isSelected()){
            try{
                sendToServer(new HPacket("UpdateFigureData", HMessage.Direction.TOSERVER,
                        UserIdAndGender.get(userId).toString(), UserIdAndFigureId.get(userId)));
            }catch (Exception ignored) { }
        }
    }

    public void handleCheckMotto() {
        if(checkMotto.isSelected())
            sendToServer(new HPacket("ChangeMotto", HMessage.Direction.TOSERVER, UserIdAndMotto.get(userId)));
    }

    public void handleCheckTile() {
        if(checkTile.isSelected()) timerNumberClicks.start();
        else if(!checkTile.isSelected()) timerNumberClicks.stop();
    }
}

//        if(response != null) sendToServer(new HPacket("12", HMessage.Direction.TOSERVER, 12));