package geekbrains.java.cloud.server;

import geekbrains.java.cloud.common.UnitedType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;

import static io.netty.buffer.Unpooled.buffer;

public class ServerFileMethods {

    private String serverPath = "server_storage/";

    private ChannelHandlerContext ctx;
    private Channel currentChannel;

    public void setServerPath(String serverPath){
        this.serverPath = serverPath;
    }

    public String getServerPath(){
        return serverPath;
    }

    public void setCtx(ChannelHandlerContext ctx){
        this.ctx = ctx;
    }

    public void writeFile(String fileName, Path path){
        try {
            if (Files.exists(path) && !Files.isDirectory(path)){
                sendData("UPLOAD " + fileName);
                InputStream inputStream = Files.newInputStream(path);
                byte[] data = new byte[1024];
                while (inputStream.available() >= 1024){
                    inputStream.read(data);
                    sendData(data);
                    Arrays.fill(data, (byte) 0);
                }
                byte[] dataEnd = new byte[inputStream.available()];
                while (inputStream.available() > 0){
                    inputStream.read(dataEnd);
                    sendData(dataEnd);
                }
                byte[] end = {20};
                sendData(end);
                inputStream.close();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void createFile(Path path){
        try{
            Files.deleteIfExists(path);
            Files.createFile(path);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void sendFilesList(){
        Path path = Paths.get(serverPath);
        ArrayList<String> filesList = new ArrayList<>();
        StringBuilder result = new StringBuilder();
        System.out.println("path " + serverPath);
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    filesList.add(file.getFileName().toString());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (dir.compareTo(path) != 0) {
                        filesList.add(dir.getFileName().toString());
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e){
            e.printStackTrace();
        }
        for (String o: filesList) result.append(o).append(" ");
        sendData("LIST " + result);
        System.out.println(result.toString());
    }

    private void sendData(Object data){
        currentChannel = ctx.channel();
        ByteBuf buf = buffer(1024);
        if (data instanceof String){
            try{
                switch (((String) data).split(" ", 2)[0]){
                    case "UPLOAD":{
                        buf.writeByte(UnitedType.getByteFromType(UnitedType.FILE));
                        buf.writeInt(((String) data).split(" ")[1].getBytes().length);
                        buf.writeBytes(((String) data).split(" ")[1].getBytes());
                        buf.writeLong(Files.size(Paths.get(serverPath + ((String) data).split(" ")[1])));
                    } break;
                    case "LIST":{
                        buf.writeByte(UnitedType.getByteFromType(UnitedType.LIST));
                        buf.writeInt(((String) data).split(" ", 2)[1].getBytes().length);
                        buf.writeBytes(((String) data).split(" ", 2)[1].getBytes());
                    } break;
                    case "AUTH": buf.writeByte(UnitedType.getByteFromType(UnitedType.AUTH));
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            buf.writeBytes((byte[]) data);
        }
        currentChannel.writeAndFlush(buf);
    }

    public void createDir(Path path) {
        if (!Files.exists(path)){
            try {
                Files.createDirectory(path);
                System.out.println("new dir " + path.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void moveForward(String fileName) {
        if (Files.isDirectory(Paths.get(serverPath + fileName))) {
            serverPath += fileName;
            System.out.println("path after forward " + serverPath);
        }
    }

    public void moveBack() {
        String[] currentPathArr = serverPath.split("/");
        if (currentPathArr.length >= 2){
            StringBuilder previousPath = new StringBuilder();
            for (int i = 0; i <= currentPathArr.length - 2; i++) previousPath.append(currentPathArr[i]).append("/");
            serverPath = previousPath.toString();
            System.out.println("path after back " + serverPath);
            sendFilesList();
        }
    }

    public void deleteFile(Path path) {
        try {
            if (!Files.isDirectory(path)){
                Files.deleteIfExists(path);
            } else {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>(){
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void sendAuthOk(){
        sendData("AUTH");
    }
}
