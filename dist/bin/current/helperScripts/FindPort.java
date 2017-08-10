import java.net.*;

public class FindPort {

    public static void main(String[] args) {
        try {
            ServerSocket s = new ServerSocket(0);
            System.out.println(s.getLocalPort());
            System.exit(0);
        } catch (Exception ex) {
        }
    }
}

