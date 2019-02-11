package geekbrains.java.cloud.server;

import geekbrains.java.cloud.common.DataBase;
import geekbrains.java.cloud.common.StageType;
import geekbrains.java.cloud.common.UnitedType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.file.*;

public class ClientHandler extends ChannelInboundHandlerAdapter {

    private DataBase dataBase;
    private ServerFileMethods serverFileMethods = new ServerFileMethods();
    private Path path;
    private String clientFolder;
    private long fileReqLength = 0;
    private long loadedLength = 0;
    private UnitedType uType = UnitedType.EMPTY;
    private StageType sType = StageType.START_TYPE;
    private String fileName;
    private String userInfo;
    private int reqNameLength = 0;
    private BufferedOutputStream bos;

    public ClientHandler(DataBase dataBase){
        this.dataBase = dataBase;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        serverFileMethods.setCtx(ctx);
        ByteBuf byteBuf = (ByteBuf) msg;

        if (sType == StageType.START_TYPE){
            uType = UnitedType.getTypeFromByte(byteBuf.readByte());
            switch (uType){
                case AUTH:
                case REG: sType = StageType.GET_USER_LENGTH;
                break;
            }
        }

        if (sType == StageType.GET_USER_LENGTH){
            if (byteBuf.readableBytes() < 4) return;
            reqNameLength = byteBuf.readInt();
            sType = StageType.GET_USER;
        }

        if (sType == StageType.GET_USER){
            if (byteBuf.readableBytes() < reqNameLength) return;
            byte[] authNameArr = new byte[reqNameLength];
            byteBuf.readBytes(authNameArr);
            userInfo = new String(authNameArr);
            switch (uType){
                case REG: sType = StageType.GET_REG;
                break;
                case AUTH: sType = StageType.GET_AUTH;
                break;
            }
        }

        if (sType == StageType.GET_AUTH){
            if (dataBase.getAuth(userInfo)) {
                clientFolder = dataBase.getFolder(userInfo);
                serverFileMethods.setServerPath(clientFolder);
                serverFileMethods.sendAuthOk();
                sType = StageType.SEND_LIST;
            }
            else {
                sType = StageType.START_TYPE;
            }
        }

        if (sType == StageType.GET_REG){
            if (dataBase.getReg(userInfo)){
                Files.createDirectory(Paths.get(dataBase.getFolder(userInfo)));
                sType = StageType.GET_AUTH;
            } else {
                sType = StageType.START_TYPE;
            }
        }

        if (sType == StageType.GET_COMMAND){
            uType = UnitedType.getTypeFromByte(byteBuf.readByte());
            switch (uType){
                case DOWNLOAD:
                case UPLOAD:
                case CREATE:
                case FORWARD:
                case DELETE: sType = StageType.GET_FILE_NAME_LENGTH;
                break;
                case BACK: sType = StageType.MOVE_BACK;
                break;
                case LIST: sType = StageType.SEND_LIST;
                break;
//                case RELOG: sType = StageType.START_TYPE;
//                break;
            }
        }

        if (sType == StageType.GET_FILE_NAME_LENGTH){
            if (byteBuf.readableBytes() < 4) return;
            reqNameLength = byteBuf.readInt();
            sType = StageType.GET_FILE_NAME;
        }

        if (sType == StageType.GET_FILE_NAME){
            if (byteBuf.readableBytes() < reqNameLength) return;
            byte[] fileNameArr = new byte[reqNameLength];
            byteBuf.readBytes(fileNameArr);
            fileName = new String(fileNameArr);
            path = Paths.get(serverFileMethods.getServerPath() + fileName);
            switch (uType){
                case DOWNLOAD: sType = StageType.SEND_FILE;
                break;
                case UPLOAD: sType = StageType.GET_FILE_LENGTH;
                break;
                case DELETE: sType = StageType.DELETE_FILE;
                break;
                case CREATE: sType = StageType.CREATE_DIR;
                break;
                case FORWARD: sType = StageType.MOVE_FORWARD;
                break;
                default: sType = StageType.GET_COMMAND;
                break;
            }
        }

        if (sType == StageType.CREATE_DIR){
            serverFileMethods.createDir(path);
            sType = StageType.SEND_LIST;
        }

        if (sType == StageType.MOVE_FORWARD){
            serverFileMethods.moveForward(fileName);
            sType = StageType.SEND_LIST;
        }

        if (sType == StageType.DELETE_FILE){
            serverFileMethods.deleteFile(path);
            sType = StageType.SEND_LIST;
        }

        if (sType == StageType.SEND_FILE){
            serverFileMethods.writeFile(fileName, path);
            byteBuf.release();
            sType = StageType.GET_COMMAND;
        }

        if (sType == StageType.MOVE_BACK){
            serverFileMethods.moveBack();
            sType = StageType.GET_COMMAND;
        }

        if (sType == StageType.SEND_LIST){
            serverFileMethods.sendFilesList();
            sType = StageType.GET_COMMAND;
        }

        if (sType == StageType.GET_FILE_LENGTH){
            if (byteBuf.readableBytes() < 8) return;
            fileReqLength = byteBuf.readLong();
            loadedLength = 0;
            serverFileMethods.createFile(path);
            bos = new BufferedOutputStream(new FileOutputStream(path.toString(), true));
            sType = StageType.GET_FILE;
        }

        if (sType == StageType.GET_FILE){
            while (byteBuf.readableBytes() > 0 && loadedLength < fileReqLength){
                bos.write(byteBuf.readByte());
                loadedLength++;
            }
            if (loadedLength < fileReqLength) return;
            bos.flush();
            byteBuf.release();
            bos.close();
            sType = StageType.SEND_LIST;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}