import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ClientHandler extends Thread {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;


    public ClientHandler(Socket socket) {
        this.socket = socket;
    }


    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            while (true) {
                out.println("Enter your username:");
                username = in.readLine();

                synchronized (MultiThreadedServer.usernames) {
                    if (username == null || username.isEmpty() || MultiThreadedServer.usernames.contains(username)) {
                        out.println("Invalid username. Please try again.");
                        continue;
                    } else {
                        MultiThreadedServer.usernames.add(username);
                        MultiThreadedServer.clients.put(username, new ClientDetails(out));
                        out.println("Welcome, " + username + "!");
                        break;
                    }
                }
            }

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.equals("viewmessages")) {
                    ClientDetails clientDetails = MultiThreadedServer.clients.get(username);
                    List<String> userMessages = clientDetails.getMessages();
                    if (userMessages.isEmpty()) {
                        out.println("You have no messages.");
                    } else {
                        for (int i = 0; i < userMessages.size(); i++) {
                            out.println((i + 1) + ". " + userMessages.get(i));
                        }
                        out.println("End of messages.");
                    }
                }else if (inputLine.equals("viewtags")) {
                    printTags(out);
                }else if (inputLine.startsWith("listusers")) {
                    synchronized (MultiThreadedServer.usernames) {
                        out.println("Online Users:");
                        for (String username : MultiThreadedServer.usernames) {
                            out.println("- " + username);
                        }
                        out.println("End of users list.");
                    }
                }else if (inputLine.startsWith("newmessage:")) {
                    String message = inputLine.substring("newmessage:".length()).trim();
                    System.out.println("costumer "+username+ "send :" + message);
                    processCustomMessage(message, out);
                    out.println("Message received!");
                } else if (inputLine.equals("exit")) {
                    break;
                } else {
                    out.println("Invalid command. Please try again.");
                }
            }
            out.println("Goodbye!");
            socket.close();

        } catch (SocketException e) {
            System.out.println("Connection reset: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
                synchronized (MultiThreadedServer.usernames) {
                    if (username != null) {
                        MultiThreadedServer.usernames.remove(username);
                        MultiThreadedServer.clients.remove(username);
                        System.out.println(username + " has left.");
                    }
                }
            } catch (IOException e) {
                System.out.println("Error occurred while closing the socket: " + e.getMessage());
            }
        }
    }

    private void processCustomMessage(String message, PrintWriter out) {
        StringTokenizer tokenizer = new StringTokenizer(message, "{}");
        String title = null;
        String to = null;
        String body = null;

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();
            if (token.equalsIgnoreCase("title")) {
                if (tokenizer.hasMoreTokens()) {
                    title = tokenizer.nextToken().trim();
                    if (tokenizer.hasMoreTokens() && tokenizer.nextToken().trim().equals(";")) {
                        continue;
                    }
                }
            } else if (token.equalsIgnoreCase("to")) {
                if (tokenizer.hasMoreTokens()) {
                    to = tokenizer.nextToken().trim();
                    if (tokenizer.hasMoreTokens() && tokenizer.nextToken().trim().equals(";")) {
                        continue;
                    }
                }
            } else if (token.equalsIgnoreCase("body")) {
                if (tokenizer.hasMoreTokens()) {
                    body = tokenizer.nextToken().trim();
                    if (tokenizer.hasMoreTokens() && tokenizer.nextToken().trim().equals(";")) {
                        continue;
                    }
                }
            } else {
                out.println("ERROR: Syntax error in message");
                return;
            }
        }

        if (title != null && to != null && body != null) {
            String newBody = parseBody(body,username);
            String formattedMessage = "Message from " + username + " => " + title + " | " + newBody;
            if (MultiThreadedServer.clients.containsKey(to)) {
                ClientDetails recipientDetails = MultiThreadedServer.clients.get(to);
                recipientDetails.getMessages().add(formattedMessage);
            } else {
                out.println("Error: User " + to + " not found.");
            }
        } else {
            out.println("ERROR: Invalid message format (missing fields)");
        }
    }
    public String parseBody(String body, String username) {

        StringBuilder parsedBody = new StringBuilder();
        StringTokenizer tokenizer = new StringTokenizer(body, "[];", true);
        boolean isTag = false;
        boolean isListItemContent = false;
        String currentTag = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");


        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();

            if ("[".equals(token)) {
                isTag = true;
                currentTag = tokenizer.nextToken().toLowerCase();
                tokenizer.nextToken();

                if ("divisoria".equals(currentTag))
                    parsedBody.append("\n___________________________________\n");

                if ("quebralinha".equals(currentTag))
                    parsedBody.append("\n");

                if ("item".equals(currentTag)) {

                    isListItemContent = true;
                    parsedBody.append("\n • ");
                }
            } else if ("]".equals(token)) {

                isTag = false;
                if ("item".equals(currentTag)) {
                    isListItemContent = false;
                }
            } else if (";".equals(token)) {

                isListItemContent = false;
            } else {
                if (isListItemContent) {

                    parsedBody.append(token);
                } else if (isTag) {
                    switch (currentTag) {
                        case "negrito":
                            parsedBody.append("\t \u001B[1m").append(token).append("\u001B[0m");
                            break;
                        case "italico":
                            parsedBody.append("\t \u001B[3m").append(token).append("\u001B[0m");
                            break;
                        case "sublinhado":
                            parsedBody.append("\t \u001B[4m").append(token).append("\u001B[0m");
                            break;
                        case "tachado":
                            parsedBody.append("\t \u001B[9m").append(token).append("\u001B[0m");
                            break;
                        case "citacao":
                            parsedBody.append("\t '").append(token).append("'");
                            break;
                        case "branca":
                            parsedBody.append("\t \u001B[37m").append(token).append("\u001B[0m");
                            break;
                        case "roxa":
                            parsedBody.append("\t \u001B[35m").append(token).append("\u001B[0m");
                            break;
                        case "vermelho":
                            parsedBody.append("\t \u001B[31m").append(token).append("\u001B[0m");
                            break;
                        case "verde":
                            parsedBody.append("\t \u001B[32m").append(token).append("\u001B[0m");
                            break;
                        case "azul":
                            parsedBody.append("\t \u001B[34m").append(token).append("\u001B[0m");
                            break;
                        case "amarelo":
                            parsedBody.append("\t \u001B[33m").append(token).append("\u001B[0m");
                            break;
                        case "ciano":
                            parsedBody.append("\t \u001B[36m").append(token).append("\u001B[0m");
                            break;
                        case "cinzaclaro":
                            parsedBody.append("\t \u001B[37;1m").append(token).append("\u001B[0m");
                            break;
                        case "lista":
                            isTag = true;
                            currentTag = "lista";
                            tokenizer.nextToken();
                            parsedBody.append("\n");
                            break;
                        case "link":
                            parsedBody.append("[Link]").append(token);
                            break;
                        case "quebralinha":
                            parsedBody.append("\n");
                        case "data":
                            try {
                                Date date = dateFormat.parse(token);
                                String formattedDate = dateFormat.format(date);
                                parsedBody.append(formattedDate);
                            } catch (ParseException e) {

                                parsedBody.append(token);
                            }
                            break;
                        case "hora":
                            try {
                                Date time = timeFormat.parse(token);
                                String formattedTime = timeFormat.format(time);
                                parsedBody.append(formattedTime);
                            } catch (ParseException e) {

                                parsedBody.append(token);
                            }
                            break;
                        case "assinatura":
                            parsedBody.append("Ass: ").append(username);
                            break;
                        default:
                            parsedBody.append("[").append(currentTag).append("]").append(token).append("[/").append(currentTag).append("]");
                            break;
                    }
                } else {
                    parsedBody.append(token);
                }
            }
        }

        return parsedBody.toString();
    }

    public void printTags(PrintWriter out){
        out.println("negrito:Texto em negrito\n" +
                "italico:Texto em itálico\n" +
                "sublinhado:Texto sublinhado\n" +
                "tachado:Texto tachado\n" +
                "citacao:citar um texto\n"+
                "divisoria:cria uma linha de divisão\n"+
                "branca:texto branco\n"+
                "roxa:texto roxo\n"+
                "vermelho:Texto vermelho\n" +
                "verde:Texto verde\n" +
                "azul:Texto azul\n" +
                "amarelo:Texto amarelo\n" +
                "ciano:Texto ciano\n" +
                "cinzaclaro:Texto cinza claro\n" +
                "lista:[lista][item]Item um[item] item dois[lista;] \n" +
                "paragrafo:Texto em parágrafo\n" +
                "link:[Link]Texto do link\n" +
                "data:[data]dd/MM/yyyy\n" +
                "hora:[hora]HH:mm:ss\n" +
                "assinatura:Ass: Assinatura do usuário\n" +
                "End of tags list.");
    }


}
