import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class SmallChat_Client {
    byte[] buffer = new byte[128];
    int idx = 0;

    public void init(String host, int port) throws Exception {
        Selector selector = Selector.open();
        SocketChannel channel = SocketChannel.open(new InetSocketAddress(host, port));
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        System.out.println("Client connecting to server.");
        new Thread(() -> {
            try {
                setRawMode();

                while (true){
                    int b = System.in.read();
                    if (b == '\r'){
                        try {
                            buffer[idx++] = '\r';
                            buffer[idx++] = '\n';
                            cleanCurrentLine();
                            showBuffer();
                            channel.write(ByteBuffer.wrap(buffer, 0, idx));
                            idx = 0;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }else if (b == 127){
                        cleanCurrentLine();
                        if(idx > 0){
                            idx --;
                            showBuffer();
                        }
                    }else{
                        buffer[idx++] = (byte) b;
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }finally {
                try {
                    restoreOriginalMode();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
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
        cleanCurrentLine();
        System.out.println(msg.trim());
        cleanCurrentLine();
        showBuffer();
    }

    private void setRawMode() throws Exception {
        String[] cmd = {"/bin/sh", "-c", "stty raw </dev/tty"};
        Process process = Runtime.getRuntime().exec(cmd);
        process.waitFor();
        process.destroy();
    }

    private static void restoreOriginalMode() throws IOException, InterruptedException {
        // 执行 stty 命令以恢复终端原始设置
        String[] cmd = {"/bin/sh", "-c", "stty sane </dev/tty"};
        Process process = Runtime.getRuntime().exec(cmd);
        process.waitFor();
        process.destroy();
    }

    /**
     * 清除当前行， 并将光标移到最前
     */
    private void cleanCurrentLine(){
        System.out.print("\033[2K\033[G");
    }

    private void showBuffer(){
        System.out.print(new String(buffer, 0, idx));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2){
            System.out.println("Usage: java SmallChat_Client <host> <port>\n");
            return;
        }
        SmallChat_Client client = new SmallChat_Client();
        client.init(args[0], Integer.parseInt(args[1]));
        Signal.handle(new Signal("INT"), new SignalHandler() {
            public void handle(Signal signal) {
                System.out.println("Ctrl+C pressed. Exiting...");
                try {
                    restoreOriginalMode();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.exit(0);
            }
        });
    }
}
