import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class SmallChat_Server {
    public static final int PORT = 7711;
    public static final int MAX_CLIENTS = 128;
    public static final String DEFAULT_NAME_PRE = "user_";
    public static final Map<SocketChannel, Client> CLIENT_MAP = new HashMap<>(MAX_CLIENTS);

    static class Client{
        private String nick;

        public Client( String nick){
            this.nick = nick;
        }
    }

    public void init(){
        try (Selector selector = Selector.open();
             ServerSocketChannel channel = ServerSocketChannel.open())
        {
            channel.bind(new InetSocketAddress(PORT));
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_ACCEPT);

            while (true){
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    if (key.isAcceptable()) {
                        createClient(selector, key);
                    }else if (key.isReadable()){
                        receiveMsg(key);
                    }
                    iterator.remove();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void receiveMsg(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();

        ByteBuffer buffer = ByteBuffer.allocate(127);
        int read = channel.read(buffer);
        if (read == -1){
            removeClient(key);
            return;
        }

        buffer.flip();
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        String msg = new String(bytes);

        handlerMsg(channel, msg);
    }

    private void handlerMsg(SocketChannel channel, String msg) throws IOException {
        assert msg != null && msg.length() > 0;
        if (msg.charAt(0) == '/'){
            if (msg.startsWith("/nick ")){
                assert msg.length() >= 7;
                String newName = msg.substring(6, msg.length() - 1);
                Client client = CLIENT_MAP.get(channel);
                client.nick = newName;
                channel.write(ByteBuffer.wrap(String.format("your nickname change to %s\n", newName).getBytes()));
            }else{
                channel.write(ByteBuffer.wrap("unknown command".getBytes()));
            }
        }else{
            broadcastMsg(channel, msg);
        }
    }

    private void broadcastMsg(SocketChannel channel, String msg) throws IOException {
        Client send = CLIENT_MAP.get(channel);
        for (SocketChannel c : CLIENT_MAP.keySet()){
            if (c == channel) {
                continue;
            }
            c.write(ByteBuffer.wrap(String.format("%s>%s", send.nick, msg).getBytes()));
        }
    }

    private void removeClient(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        CLIENT_MAP.remove(channel);
        channel.close();
    }

    private void createClient(Selector selector, SelectionKey key) throws IOException {

        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
        SocketChannel sc = channel.accept();
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ);

        Client client = new Client(DEFAULT_NAME_PRE + CLIENT_MAP.size());
        CLIENT_MAP.put(sc, client);

        sc.write(ByteBuffer.wrap("welcome to smallchat, use /nick <nick> to change your nickname\n".getBytes()));
    }


    public static void main(String[] args) {
        SmallChat_Server server = new SmallChat_Server();
        server.init();
    }
}
