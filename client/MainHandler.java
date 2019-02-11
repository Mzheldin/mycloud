package geekbrains.java.cloud.client;

import geekbrains.java.cloud.common.StageType;
import geekbrains.java.cloud.common.UnitedType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javafx.application.Platform;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainHandler extends ChannelInboundHandlerAdapter {

    private ClientFileMethods clientFileMethods;
    private Path path = null;
    private long fileReqLength = 0;
    private long loadedLength = 0;
    private UnitedType uType = UnitedType.EMPTY;
    private StageType sType = StageType.GET_COMMAND;
    private String fileName;
    private int fileNameLength = 0;
    private int listLength = 0;
    private BufferedOutputStream bos;
    private Controller controller;

    public MainHandler(Controller controller){
        this.controller = controller;
        clientFileMethods = controller.getClientFileMethods();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        ByteBuf byteBuf = (ByteBuf) msg;

        if (sType == StageType.GET_COMMAND){
            uType = UnitedType.getTypeFromByte(byteBuf.readByte());
            switch (uType){
                case FILE: sType = StageType.GET_FILE_NAME_LENGTH;
                break;
                case LIST: sType = StageType.GET_LIST_LENGTH;
                break;
                case AUTH: Platform.runLater(() -> controller.closeAuth());
                break;
            }
        }

        if (sType == StageType.GET_FILE_NAME_LENGTH){
            if (byteBuf.readableBytes() < 4) return;
            fileNameLength = byteBuf.readInt();
            sType = StageType.GET_FILE_NAME;
        }

        if (sType == StageType.GET_LIST_LENGTH){
            if (byteBuf.readableBytes() < 4) return;
            listLength = byteBuf.readInt();
            loadedLength = 0;
            sType = StageType.GET_LIST;
        }

        if (sType == StageType.GET_LIST){
            if (byteBuf.readableBytes() < listLength) return;
            byte[] listArr = new byte[listLength];
            byteBuf.readBytes(listArr);
            controller.refreshServerFilesList(new String(listArr).split(" "));
            sType = StageType.GET_COMMAND;
        }

        if (sType == StageType.GET_FILE_NAME){
            if (byteBuf.readableBytes() < fileNameLength) return;
            byte[] fileNameArr = new byte[fileNameLength];
            byteBuf.readBytes(fileNameArr);
            fileName = new String(fileNameArr);
            path = Paths.get("client_storage/" + fileName);
            sType = StageType.GET_FILE_LENGTH;
        }

        if (sType == StageType.GET_FILE_LENGTH){
            if (byteBuf.readableBytes() < 8) return;
            fileReqLength = byteBuf.readLong();
            loadedLength = 0;
            clientFileMethods.createFile(path);
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
            controller.refreshLocalFilesList();
            sType = StageType.SEND_LIST;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
