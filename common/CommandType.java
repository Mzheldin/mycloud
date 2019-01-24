package geekbrains.java.cloud.common;

public enum CommandType {

    DOWNLOAD("DOWNLOAD"),
    UPLOAD("UPLOAD");

    String commandType;

    CommandType(String commandType){
        this.commandType = commandType;
    }

    public static CommandType getCommandType(String s){
        if (s.equals("DOWNLOAD")) return DOWNLOAD;
        else return UPLOAD;
    }

}
