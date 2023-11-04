import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ClientDetails {
    private PrintWriter writer;
    private List<String> messages;

    public ClientDetails(PrintWriter writer) {
        this.writer = writer;
        this.messages = new ArrayList<>();
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public List<String> getMessages() {
        return messages;
    }

}
