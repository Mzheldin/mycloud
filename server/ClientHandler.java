package geekbrains.java.cloud.server;

import geekbrains.java.cloud.common.CommandType;
import geekbrains.java.cloud.common.DataType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class ClientHandler extends ChannelInboundHandlerAdapter {

    private int stage = -1;
    private int reqLength = -1;
    private DataType type = DataType.EMPTY;
    private CommandType commandType;
    private ChannelHandlerContext ctx;
    private Path pathToLoad;
    private long fileReqLength = 0;
    private long loadedLength = 0;


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        this.ctx = ctx;

        ByteBuf byteBuf = (ByteBuf) msg;
        //получаем 1й байт, определяем тип сообщения и следующие 4 байта под длину команды или имени файла
        if (stage == -1){
            byte firstByte = byteBuf.readByte();
            type = DataType.getDataTypeFromByte(firstByte);
            reqLength = 4;
            System.out.println("type: " + type);
            stage = 0;
        }
        //принимаем и считываем длину команды или имени файла(4 байта)
        if (stage == 0){
            if (byteBuf.readableBytes() < reqLength) return;
            reqLength = byteBuf.readInt();
            System.out.println("text size: " + reqLength);
            stage = 1;
        }
        //принимаем и считываем команду или имя файла
        if (stage == 1) {
            if (byteBuf.readableBytes() < reqLength) return;
            byte[] data = new byte[reqLength];
            byteBuf.readBytes(data);
            // если тип сообщения - команда, то обрабатываем ее и сбрасываем "прогресс" приема данных
            if (type == DataType.COMMAND){
                byteBuf.release();
                doCommand(new String(data));
                reset();
            }
            // если тип - файл, то создаем путь для него в хранилище сервера и задаем длину размера файла - 8 байт
            if (type == DataType.FILE){
                pathToLoad = Paths.get("server_storage/" + new String(data));
                reqLength = 8;
                stage = 2;
            }
        }
        //считываем размер файла и запоминаем его
        if (stage == 2){
            if (byteBuf.readableBytes() < reqLength) return;
            fileReqLength = byteBuf.readLong();
            stage = 3;
        }
        //пока счетчик байт меньше размера файла, то по 1 байту принимаем и записываем в файл
        if (stage == 3){
            if (loadedLength < fileReqLength){
                byte[] data = new byte[1];
                data[0] = byteBuf.readByte();
                downloadFile(data, pathToLoad);
                loadedLength++;
                return;
            }
            reset(); //сбрасываем прием данных
        }
    }
    //определяем команду, если загружают на сервер - создаем пустой файл с принятым именем
    private void doCommand(String command){
        String[] line = command.split(" ");
        commandType = CommandType.getCommandType(line[0]);
        switch (commandType){
            case UPLOAD:{
                createFile(line[1]);
            } break;
    //если загружают с сервера, то передаем файл с соотв. именем
            case DOWNLOAD:{
                writeFile(line[1]);
            } break;
        }
    }
    //передача файла
    private void writeFile(String fileName){
        try {
            Path path = Paths.get("server_storage/" + fileName) ;
            if (Files.exists(path)){
                //формируем и передаем тип(файл), длину имени, имя и размер файла
                byte[] dataType = new byte[1];
                dataType[0] = 15;
                byte[] fileNameArr = (fileName).getBytes();
                ByteBuffer buff = ByteBuffer.allocate(4);
                buff.putInt(fileNameArr.length);
                ByteBuffer fileSize = ByteBuffer.allocate(8);
                fileSize.putLong(Files.size(path));

                ctx.writeAndFlush(dataType);
                ctx.writeAndFlush(buff.array());
                ctx.writeAndFlush(fileNameArr);
                ctx.writeAndFlush(fileSize.array());
                //считываем и передаем файл по 4 байта
                InputStream inputStream = Files.newInputStream(path);
                while (inputStream.available() > 0){
                    byte[] data = new byte[4];
                    inputStream.read(data);
                    ctx.writeAndFlush(data);
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void getFile(){

    }
    //создаем пустой файл для последующего принятия в него данных
    private void createFile(String fileName){
        try{
            Path path = Paths.get("server_storage/" + fileName);
            if (Files.notExists(path)) Files.createFile(path);
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    //записываем в созданный ранее пустой файл байты, добавляя их в конец
    private void downloadFile(byte[] data, Path pathToLoad){
        try {
            Files.write(pathToLoad, data, StandardOpenOption.APPEND);
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    //сброс параметров "прогресса" приема данных
    private void reset(){
        stage = -1;
        reqLength = -1;
        type = DataType.EMPTY;
        commandType = null;
        ctx = null;
        pathToLoad = null;
        fileReqLength = 0;
        loadedLength = 0;

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
