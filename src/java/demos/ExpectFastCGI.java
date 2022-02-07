
import java.io.FileDescriptor;
import org.newsclub.net.unix.AFUNIXServerSocket;

public class ExpectFastCGI {
    public static void main(String[] args) throws Exception {
        AFUNIXServerSocket sock =
            AFUNIXServerSocket.newInstance(FileDescriptor.in, 1000, 1001);
    }
}
