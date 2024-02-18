import gearth.extensions.parsers.HEntity;
import gearth.extensions.parsers.HGender;

import java.util.ArrayList;

// For Users and Bots (A bot has almost the same attributes as a user)
public class User {
    public static ArrayList<User> userArrayList = new ArrayList<>();

    private String userName;
    private int userId;
    private int userIndex;
    private String userFigure; // Clothes
    private HGender userGender;
    private String userMotto;

    public User(String userName, int userId, int userIndex, String userFigure, HGender userGender, String userMotto) {
        this.userName = userName;
        this.userId = userId;
        this.userIndex = userIndex;
        this.userFigure = userFigure;
        this.userGender = userGender;
        this.userMotto = userMotto;
    }

    public String getUserName() {
        return userName;
    }

    public int getUserId() {
        return userId;
    }

    public int getUserIndex() {
        return userIndex;
    }

    public String getUserFigure() {
        return userFigure;
    }

    public HGender getUserGender() {
        return userGender;
    }

    public String getUserMotto() {
        return userMotto;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setUserIndex(int userIndex) {
        this.userIndex = userIndex;
    }

    public void setUserFigure(String userFigure) {
        this.userFigure = userFigure;
    }

    public void setUserGender(HGender userGender) {
        this.userGender = userGender;
    }

    public void setUserMotto(String userMotto) {
        this.userMotto = userMotto;
    }

    public static void setUser(HEntity hEntity) {
        for (User user : userArrayList) {
            if (user.getUserId() == hEntity.getId()) {
                user.setUserName(hEntity.getName());
                user.setUserIndex(hEntity.getIndex());
                user.setUserFigure(hEntity.getFigureId());
                user.setUserGender(hEntity.getGender());
                user.setUserMotto(hEntity.getMotto());

                break;
            }
        }
    }

    public static int getUserIndexByUserId(int userId) {
        for (User user : userArrayList) {
            if (user.getUserId() == userId) {
                return user.getUserIndex();
            }
        }
        return -1;
    }

    public static String getUserNameByUserId(int userId) {
        for (User user : userArrayList) {
            if (user.getUserId() == userId) {
                return user.getUserName();
            }
        }
        return "";
    }

    public static String getUserNameByUserIndex(int userIndex) {
        for (User user : userArrayList) {
            if (user.getUserIndex() == userIndex) {
                return user.getUserName();
            }
        }
        return "";
    }

    public static String getGenderByUserId(int userId) {
        for (User user : userArrayList) {
            if (user.getUserId() == userId) {
                return user.getUserGender().toString();
            }
        }
        return "";
    }

    public static String getFigureByUserId(int userId) {
        for (User user : userArrayList) {
            if (user.getUserId() == userId) {
                return user.getUserFigure();
            }
        }
        return "";
    }

    public static String getUserMottoByUserId(int userId) {
        for (User user : userArrayList) {
            if (user.getUserId() == userId) {
                return user.getUserMotto();
            }
        }
        return "";
    }

    public static boolean containsUserId(int userId) {
        for (User user : userArrayList) {
            if (user.getUserId() == userId) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsUserIndex(int userIndex) {
        for (User user : userArrayList) {
            if (user.getUserIndex() == userIndex) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "User{" +
                "userName='" + userName + '\'' +
                ", userId=" + userId +
                ", userIndex=" + userIndex +
                ", userFigure='" + userFigure + '\'' +
                ", userGender=" + userGender +
                ", userMotto='" + userMotto + '\'' +
                '}';
    }
}
