import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

public class SmallChat_Client {

    public void init(String host, int port) throws Exception {
        Selector selector = Selector.open();
        SocketChannel channel = SocketChannel.open(new InetSocketAddress(host, port));
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        System.out.println("Client connecting to server.");
        new Thread(() -> {

//            try {
//                setRawMode();
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//
//            Console console = System.console();
            Scanner in = new Scanner(System.in);
            while (in.hasNextLine()){
                String s = in.nextLine();
                try {
                    channel.write(ByteBuffer.wrap(s.getBytes()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        while (true){
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()){
                SelectionKey key = iterator.next();
                if (key.isReadable()){
                    receiveMsg(key);
                }
                iterator.remove();
            }
        }
    }

    private void receiveMsg(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();

        ByteBuffer buffer = ByteBuffer.allocate(128);
        channel.read(buffer);
        String msg = new String(buffer.array());
        System.out.println(msg.trim());
    }

    private void setRawMode() throws Exception {
        String[] cmd = {"/bin/sh", "-c", "stty raw </dev/tty"};
        Runtime.getRuntime().exec(cmd).waitFor();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2){
            System.out.println("Usage: java SmallChat_Client <host> <port>\n");
            return;
        }
        SmallChat_Client client = new SmallChat_Client();
        client.init(args[0], Integer.parseInt(args[1]));
    }
}
