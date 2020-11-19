import Controllers.*;
import Models.Message;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;


public class SocketServer extends Thread {
    protected Socket socket;
    private IListingController listingController;
    private IUserController userController;
    private ISearchController searchController;
    private ICommunicationController communicationController;
    private ICompanyController companyController;
    private IChatController chatController;
    private InputStream is = null;
    private OutputStream os = null;

    public SocketServer(Socket clientSocket) throws IOException
    {
        this.socket = clientSocket;
        communicationController = new CommunicationsController();
        listingController = new ListingController(communicationController);
        companyController = new CompanyController(communicationController);
        userController = new UserController(communicationController);
        searchController = new SearchController(communicationController);
        chatController = new ChatController(); // Takes companyController as input
    }

    //Listens for bytes and echos back to sender
    public void run(){
        try {
            is = socket.getInputStream();
            os = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true){
            try{
                // Receiving
                byte[] lenBytes = new byte[4];
                is.read(lenBytes, 0, 4);
                int len = (((lenBytes[3] & 0xff) << 24) | ((lenBytes[2] & 0xff) << 16) |
                        ((lenBytes[1] & 0xff) << 8) | (lenBytes[0] & 0xff));
                byte[] receivedBytes = new byte[len];
                is.read(receivedBytes, 0, len);
                String received = new String(receivedBytes, 0, len);

                ObjectMapper objectMapper = new ObjectMapper();
                ArrayList<String> r = objectMapper.readValue(received, new TypeReference<ArrayList<String>>(){});
                String toSend="";
                //Match action
                switch (r.get(0)){
                    case "search": toSend = searchController.search(r.get(1)); break;
                    case "login": toSend = userController.login(r.get(1)); break;
                    case "getlisting": toSend = listingController.getListing(r.get(1)); break;
                    case "createlisting": toSend = listingController.createListing(r.get(1)); break;
                    case "getcompany": toSend = companyController.getCompany(r.get(1)); break;
                    case "createcompany": toSend = companyController.createCompany(r.get(1)); break;
                    case "getproducts": toSend = listingController.getProducts(); break;
                    case "getproductcategories": toSend = listingController.getProductCategories(); break;
                    case "uploadImage": toSend = listingController.uploadImage(r.get(1)); break;
                    case "sendMessage" : SendMessageToIp(chatController.generateMessage(r.get(1))); break;
                    case "recieveMessage" : Receive(chatController.generateMessage(r.get(1))); break;
                  default:
                    System.out.println("Recieved unrecognised command: " + r);
                }

                if (toSend.length() > 0)
                    SendBack(toSend);

            } catch (SocketException e) {
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void SendBack(String toSend) throws IOException
    {
        byte[] toSendBytes = toSend.getBytes();
        int toSendLen = toSendBytes.length;
        byte[] toSendLenBytes = new byte[4];
        toSendLenBytes[0] = (byte) (toSendLen & 0xff);
        toSendLenBytes[1] = (byte) ((toSendLen >> 8) & 0xff);
        toSendLenBytes[2] = (byte) ((toSendLen >> 16) & 0xff);
        toSendLenBytes[3] = (byte) ((toSendLen >> 24) & 0xff);
        os.write(toSendLenBytes);
        os.write(toSendBytes);
    }

    public void SendMessageToIp(Message message) throws IOException
    {
        String[] connectionAddress = message.getToCompany().getConnectionAddress().split(":");

        Socket socketToReceiver = new Socket(connectionAddress[0],
            Integer.parseInt(connectionAddress[1]));

        OutputStream outputStream = socketToReceiver.getOutputStream();

        ObjectMapper mapper = new ObjectMapper();

        String[] toSend = {"recieveMessage", mapper.writeValueAsString(message)};

        byte[] toSendBytes = mapper.writeValueAsString(toSend).getBytes();
        int toSendLen = toSendBytes.length;
        byte[] toSendLenBytes = new byte[4];
        toSendLenBytes[0] = (byte) (toSendLen & 0xff);
        toSendLenBytes[1] = (byte) ((toSendLen >> 8) & 0xff);
        toSendLenBytes[2] = (byte) ((toSendLen >> 16) & 0xff);
        toSendLenBytes[3] = (byte) ((toSendLen >> 24) & 0xff);
        outputStream.write(toSendLenBytes);
        outputStream.write(toSendBytes);
    }

    public String Receive(Message message)
    {
        System.out.println(message.getMessage() + message.getTimestamp().toString());
     return null;
    }

}