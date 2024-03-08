package pt.ulisboa.tecnico.tuplespaces.client;

import java.util.Random;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.nameServer.contract.NameServerGrpc;
import pt.ulisboa.tecnico.nameServer.contract.NameServerGrpc.NameServerBlockingStub;
import pt.ulisboa.tecnico.nameServer.contract.NameServerOuterClass.LookupRequest;
import pt.ulisboa.tecnico.nameServer.contract.NameServerOuterClass.LookupResponse;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

public class ClientMain {
    static final int numServers = 3;
    public static void main(String[] args) {
        
        // check arguments
        if (args.length != 3) {
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: mvn exec:java -Dexec.args=<host> <port>");
            return;
        }

        // get the host and the port
        final String nameServerHost = args[0];
        final Integer nameServerPort = Integer.parseInt(args[1]);
        final String serviceName = args[2];

        String[] hostParts = getServerInformation(nameServerHost, nameServerPort, serviceName).split(":");
        if (hostParts.length != 2) {
            System.err.println("Invalid host:port");
            return;
        }

        final String host = hostParts[0];
        final Integer port = Integer.parseInt(hostParts[1]);


        CommandProcessor parser = new CommandProcessor(new ClientService(host, port, ClientMain.numServers));
        parser.parseInput();
    }

    public static String getServerInformation(String nameServerHostname, Integer nameServerPort, String serviceName) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(nameServerHostname, nameServerPort).usePlaintext().build();
        NameServerBlockingStub stub = NameServerGrpc.newBlockingStub(channel);

        LookupResponse response = stub.lookup(LookupRequest.newBuilder().setName(serviceName).build());

        channel.shutdownNow();
        int choice = (new Random()).nextInt(response.getHostsCount());
        return response.getHosts(choice);
    }
}
