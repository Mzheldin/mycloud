package geekbrains.java.cloud.common;

public enum DataType {
    EMPTY((byte) -1),FILE((byte)15), COMMAND((byte)16);

    byte firstByteMessage;

    DataType(byte firstByteMessage){
            this.firstByteMessage = firstByteMessage;
    }
    public static DataType getDataTypeFromByte(byte b){
        if (b == FILE.firstByteMessage) return FILE;
        if (b == COMMAND.firstByteMessage) return COMMAND;
        else return EMPTY;
    }
}
