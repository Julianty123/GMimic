import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.extra.harble.HashSupport;
import gearth.extensions.parsers.HEntity;
import gearth.extensions.parsers.HGender;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;

import javax.swing.Timer;
import java.util.HashMap;
import java.util.Map;

@ExtensionInfo(
        Title = "GMimic ",
        Description = "So Fucking Funny Extension!",
        Version = "1.0",
        Author = "Julianty"
)

public class GMimic extends ExtensionForm {
    private HashSupport mHashSupport;
    public CheckBox checkSpeech, checkLook, checkMotto, checkTile, checkName, checkBot;

    Map<Integer,Integer> IdAndIndex = new HashMap<Integer,Integer>();
    Map<Integer,String> IdAndName = new HashMap<Integer,String>();
    Map<Integer,String> IdAndFigureId = new HashMap<Integer,String>();
    Map<Integer,HGender> IdAndGender = new HashMap<Integer,HGender>();
    Map<Integer,String> IdAndMotto = new HashMap<Integer,String>();

    public int UserId = -1;
    public int X, Y;

    // Timer para definir el numero de clicks
    Timer timer1 = new Timer(1, e -> {
        sendToServer(new HPacket(3725, X, Y));
    });

    public static void main(String[] args) {
        runExtensionForm(args, GMimic.class);
    }

    @Override
    public ExtensionForm launchForm(Stage primaryStage) throws Exception {
        primaryStage.close();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GMimic.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("GMimic");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.setAlwaysOnTop(true);

        return loader.getController();
    }

    @Override
    protected void initExtension() {
        mHashSupport = new HashSupport(this);

        // Detecta cuando el usuario cierra la ventana (En este proyecto solo funciona aqui)
        primaryStage.setOnCloseRequest(e -> {
            IdAndIndex.clear(); IdAndName.clear();  IdAndFigureId.clear();  IdAndGender.clear();    IdAndMotto.clear();
            UserId = -1;    X = 0;  Y = 0;  checkLook.setSelected(false);   checkSpeech.setSelected(false); checkBot.setSelected(false);
            Platform.runLater(() -> {
                checkName.setText("Restart the room and the click user");
            }); //Platform.exit();
        });

        intercept(HMessage.Direction.TOSERVER, 1991, hMessage -> {
            if(checkName.isSelected()){ // Obtengo la id del usuario para mostrar el nombre
                UserId = hMessage.getPacket().readInteger();
                Platform.runLater(() -> { // Actualiza la GUI de un Control
                    checkName.setText(IdAndName.get(UserId));
                });
            }
            if(checkLook.isSelected()){ // Copia el look del usuario
                sendToServer(new HPacket(3621, IdAndGender.get(UserId).toString(), IdAndFigureId.get(UserId)));
            }
            if(checkMotto.isSelected()){ // Copia la mision
                sendToServer(new HPacket(3404, IdAndMotto.get(UserId)));
            }
        });

        intercept(HMessage.Direction.TOCLIENT, 812, hMessage -> {
            //IdAndIndex.clear(); Al no borrar la lista esos datos se almacenan ojo con eso
            try {
                HPacket hPacket = hMessage.getPacket();
                HEntity[] roomUsersList = HEntity.parse(hPacket);
                for (HEntity hEntity: roomUsersList){
                    // El ID del usuario no esta en el Map (Dictionary en c#)
                    if(!IdAndIndex.containsKey(hEntity.getId())){
                        IdAndIndex.put(hEntity.getId(), hEntity.getIndex());
                        IdAndName.put(hEntity.getId(), hEntity.getName());
                        IdAndFigureId.put(hEntity.getId(), hEntity.getFigureId());
                        IdAndGender.put(hEntity.getId(), hEntity.getGender());
                        IdAndMotto.put(hEntity.getId(), hEntity.getMotto());
                    }
                    else { // Se especifica la key, para remplazar el value por uno nuevo
                        IdAndIndex.replace(hEntity.getId(), hEntity.getIndex());
                        IdAndName.replace(hEntity.getId(), hEntity.getName());
                        IdAndFigureId.replace(hEntity.getId(), hEntity.getFigureId());
                        IdAndGender.replace(hEntity.getId(), hEntity.getGender());
                        IdAndMotto.replace(hEntity.getId(), hEntity.getMotto());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Obtiene las coordenadas del usuario
        intercept(HMessage.Direction.TOCLIENT, 2519, hMessage -> {
            // Hay otra forma de obtener el Index con el HEntityUpdate
            int Thing1 = hMessage.getPacket().readInteger();
            if(checkTile.isSelected()){ // Obtengo el index del usuario que camina y lo comparo
                if(hMessage.getPacket().readInteger() == IdAndIndex.get(UserId)){
                    X = hMessage.getPacket().readInteger(); Y = hMessage.getPacket().readInteger();
                    sendToServer(new HPacket(3725, X, Y));
                }
            }
        });

        // Intercepta cuando un bot o usuario habla
        intercept(HMessage.Direction.TOCLIENT, 3139, hMessage -> {
            int CurrentIndex = hMessage.getPacket().readInteger();
            String SaySomething = hMessage.getPacket().readString();
            if(checkSpeech.isSelected()){
                if(CurrentIndex == IdAndIndex.get(UserId)){  // Obtengo el index del usuario que habla y lo comparo
                    sendToServer(new HPacket(654,SaySomething, 0, 1));
                }
            }
            if(checkBot.isSelected()){
                // Esto es para imitar solo bots (Por lo general los bots tienen Index 0, 1 o 2)
                if((CurrentIndex == 0 || CurrentIndex == 1) || (CurrentIndex == 2 || CurrentIndex == 3)){
                    sendToServer(new HPacket(654,SaySomething, 0, 1));
                }
            }
        });

        // Intercepta cuando un bot o usuario "GRITA"
        intercept(HMessage.Direction.TOCLIENT, 432, hMessage -> {
            int CurrentIndex = hMessage.getPacket().readInteger();
            String SaySomething = hMessage.getPacket().readString();
            if(checkSpeech.isSelected()){   // Obtengo el index del usuario que habla y lo comparo
                if(CurrentIndex == IdAndIndex.get(UserId)){
                    sendToServer(new HPacket(3471,SaySomething, 0));
                }
            }
            if(checkBot.isSelected()){
                // Esto es para imitar solo bots (Por lo general los bots tienen Index 0, 1 o 2)
                if((CurrentIndex == 0 || CurrentIndex == 1) || (CurrentIndex == 2 || CurrentIndex == 3)){
                    sendToServer(new HPacket(654,SaySomething, 0, 1));
                }
            }
        });

        /* Detecta cuando un usuario esta escribiendo (burbuja)
        mHashSupport.intercept(HMessage.Direction.TOCLIENT, "UserTyping", hMessage -> {
        }); */
    }

    public void handlecheckBot(ActionEvent actionEvent) {
        if(checkBot.isSelected()){
            checkSpeech.setSelected(false); checkSpeech.setDisable(true);  checkLook.setSelected(false); checkLook.setDisable(true);
            checkMotto.setSelected(false); checkMotto.setDisable(true);  checkTile.setSelected(false); checkTile.setDisable(true);
        }
        else {
            checkSpeech.setDisable(false);  checkLook.setDisable(false);    checkMotto.setDisable(false);   checkTile.setDisable(false);
        }
    }

    // Se chequea solo si el nombre es diferente a vacio...
    public void handlecheckSpeech(ActionEvent actionEvent) {
        if(checkName.getText() == "" || "Restart the room and the click user".equals(checkName.getText())){
            checkSpeech.setSelected(false);
        }
    }

    public void handlecheckLook(ActionEvent actionEvent) {
        if(checkName.getText() == "" || "Restart the room and the click user".equals(checkName.getText())){
            checkLook.setSelected(false);
        }
    }

    public void handlecheckMotto(ActionEvent actionEvent) {
        if(checkName.getText() == "" || "Restart the room and the click user".equals(checkName.getText())){
            checkMotto.setSelected(false);
        }
    }

    public void handlecheckTile(ActionEvent actionEvent) {
        if(checkName.getText() == "" || "Restart the room and the click user".equals(checkName.getText())){
            checkTile.setSelected(false);
        }
        else {
            if(checkTile.isSelected()){
                timer1.start(); // Inicia el timer
            }
            else if(!checkTile.isSelected()){
                timer1.stop();  // Para el timer
            }
        }
    }
}