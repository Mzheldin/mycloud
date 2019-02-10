package geekbrains.java.cloud.client;

import geekbrains.java.cloud.common.UnitedType;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static io.netty.buffer.Unpooled.buffer;

public class ClientFileMethods {

    final String CLIENT_PATH = "client_storage/";
    private Network network;

    public ClientFileMethods(Network network){
        this.network = network;
    }

    public void createFile(Path path){
        try{
            Files.deleteIfExists(path);
            Files.createFile(path);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void writeFile(String fileName){
        try {
            Path path = Paths.get(CLIENT_PATH + fileName) ;
            if (Files.exists(path)){
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

    public void sendData(Object data){
        ByteBuf buf = buffer(1024);
        if (data instanceof String){
            try{
                switch (((String) data).split(" ", 2)[0]){
                    case "AUTH":{
                        buf.writeByte(UnitedType.getByteFromType(UnitedType.AUTH));
                        buf.writeInt(((String) data).split(" ", 2)[1].getBytes().length);
                        buf.writeBytes(((String) data).split(" ", 2)[1].getBytes());
                    } break;
                    case "REG":{
                        buf.writeByte(UnitedType.getByteFromType(UnitedType.REG));
                        buf.writeInt(((String) data).split(" ", 2)[1].getBytes().length);
                        buf.writeBytes(((String) data).split(" ", 2)[1].getBytes());
                    } break;
                    case "UPLOAD":{
                        buf.writeByte(UnitedType.getByteFromType(UnitedType.UPLOAD));
                        buf.writeInt(((String) data).split(" ")[1].getBytes().length);
                        buf.writeBytes(((String) data).split(" ")[1].getBytes());
                        buf.writeLong(Files.size(Paths.get("client_storage/" + ((String) data).split(" ")[1])));
                    } break;
                    case "DOWNLOAD":{
                        buf.writeByte(UnitedType.getByteFromType(UnitedType.DOWNLOAD));
                        buf.writeInt(((String) data).split(" ")[1].getBytes().length);
                        buf.writeBytes(((String) data).split(" ")[1].getBytes());
                    } break;
                    case "DELETE":{
                        buf.writeByte(UnitedType.getByteFromType(UnitedType.DELETE));
                        buf.writeInt(((String) data).split(" ")[1].getBytes().length);
                        buf.writeBytes(((String) data).split(" ")[1].getBytes());
                    }break;
                    case "CREATE":{
                        buf.writeByte(UnitedType.getByteFromType(UnitedType.CREATE));
                        buf.writeInt(((String) data).split(" ")[1].getBytes().length);
                        buf.writeBytes(((String) data).split(" ")[1].getBytes());
                    } break;
                    case "LIST": buf.writeByte(UnitedType.getByteFromType(UnitedType.LIST));
                        break;
                    case "FORWARD":{
                        buf.writeByte(UnitedType.getByteFromType(UnitedType.FORWARD));
                        buf.writeInt(((String) data).split(" ")[1].getBytes().length);
                        buf.writeBytes(((String) data).split(" ")[1].getBytes());
                    } break;
//                    case "RELOG": buf.writeByte(UnitedType.getByteFromType(UnitedType.RELOG));
//                        break;
                    case "BACK": buf.writeByte(UnitedType.getByteFromType(UnitedType.BACK));
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            buf.writeBytes((byte[]) data);
        }
        network.getCurrentChannel().writeAndFlush(buf);
    }
}
