package geekbrains.java.cloud.common;

public enum UnitedType {
    EMPTY((byte) -1),
    FILE((byte)15),
    DOWNLOAD((byte)16),
    UPLOAD((byte)17),
    LIST((byte)18),
    DELETE((byte)19),
    END((byte)20),
    AUTH((byte)21),
    REG((byte)22),
    CREATE((byte)23),
    FORWARD((byte)24),
    BACK((byte)25),
    EXIT((byte)26),
    WARNING((byte)27);
    //RELOG((byte)26);

    byte firstByteMessage;

    UnitedType(byte firstByteMessage){
        this.firstByteMessage = firstByteMessage;
    }

    public static UnitedType getTypeFromByte(byte b){
        if (b == FILE.firstByteMessage) return FILE;
        if (b == DOWNLOAD.firstByteMessage) return DOWNLOAD;
        if (b == UPLOAD.firstByteMessage) return UPLOAD;
        if (b == LIST.firstByteMessage) return LIST;
        if (b == DELETE.firstByteMessage) return DELETE;
        if (b == END.firstByteMessage) return END;
        if (b == AUTH.firstByteMessage) return AUTH;
        if (b == REG.firstByteMessage) return REG;
        if (b == CREATE.firstByteMessage) return CREATE;
        if (b == FORWARD.firstByteMessage) return FORWARD;
        if (b == BACK.firstByteMessage) return BACK;
        if (b == EXIT.firstByteMessage) return EXIT;
        if (b == WARNING.firstByteMessage) return WARNING;
        //if (b == RELOG.firstByteMessage) return RELOG;
        else return EMPTY;
    }

    public static byte getByteFromType(UnitedType type){
        switch (type){
            case FILE: return 15;
            case DOWNLOAD: return 16;
            case UPLOAD: return 17;
            case LIST: return 18;
            case DELETE: return 19;
            case END: return 20;
            case AUTH: return 21;
            case REG: return 22;
            case CREATE: return 23;
            case FORWARD: return 24;
            case BACK: return 25;
            case EXIT: return 26;
            case WARNING: return 27;
            //case RELOG: return 26;
            default: return -1;
        }
    }
}
