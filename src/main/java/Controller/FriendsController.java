package Controller;

import ClientAccountNetworking.OkClient;
import PeerNetworking.PeerConnection;
import QueryObjects.FriendData;
import QueryObjects.UserData;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

//Source for ListView: https://www.youtube.com/watch?v=9uubyM6oHAY
public class FriendsController{
    private OkClient client = null;
    private UserData user = null;
    //TODO: fList and friends can be merged?
    private ArrayList<FriendData> friends = new ArrayList<FriendData>();

    @FXML
    ListView<FriendData> friendsList;
    ObservableList<FriendData> fList = FXCollections.observableArrayList();

    @FXML
    public void initialize(){
        System.out.println("Trace: in initialize");
        if (user != null) System.out.println("User is not null");
        if (friendsList != null) System.out.println("friendsList is not null");
    }

    public void initData(UserData usr){
        System.out.println("Trace: in initData");
        client = new OkClient();
        user = usr;
        try {
            String res = client.getFriends(user, friends);
            //TODO: handle res
        }
        catch(IOException e){
            e.printStackTrace();
            //TODO: handle this
        }
        if (friends.size() > 0){
            for (FriendData friend : friends){
                fList.add(friend);
            }
            friendsList.setItems(fList);
        }
    }

    public void msgFriend(ActionEvent actionEvent){
        //get selected friend from listview
        FriendData friend = friendsList.getSelectionModel().getSelectedItem();
        //feed friend ip and port to PeerConnection
        PeerConnection peer = new PeerConnection(user, friend);
        //attempt connection
        int ok = peer.connectNatless();
        //if connection succesful open chatInterface
        if (ok == 0){
            Node source = (Node) actionEvent.getSource();
            Stage theStage = (Stage)source.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chatInterface.fxml"));
            try {
                Parent root = loader.<Parent>load();
                ChatInterface controller = loader.<ChatInterface>getController();
                controller.initController(peer);
                Scene chatScene = new Scene(root, 300, 550);
                theStage.setScene(chatScene);
            }
            catch(IOException e){
                e.printStackTrace();
                System.out.println("Exception in msgFriend");
            }
        }
        //else popup
        else System.out.println("Could not open peer connection. Error: " + ok);
    }

    public void addFriend(){
        //Source: https://stackoverflow.com/questions/15041760/javafx-open-new-window
        Parent root;
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/addFriend.fxml"));
            root = loader.<Parent>load();
            AddFriendController controller = loader.<AddFriendController>getController();
            controller.initData(user);
            Stage stage = new Stage();
            stage.setTitle("Add Friend");
            stage.setScene(new Scene(root, 400, 150));
            stage.show();
        }
        catch(IOException e){
            e.printStackTrace();
            //TODO: handle this better
        }
    }

    public  void setUser(UserData usr){
        System.out.println("Trace: setUser called");
        user = usr;
    }

}
