package pt.ulisboa.tecnico.tuplespaces.server;

import java.io.IOException;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.ulisboa.tecnico.nameServer.contract.NameServerGrpc;
import pt.ulisboa.tecnico.nameServer.contract.NameServerGrpc.NameServerBlockingStub;
import pt.ulisboa.tecnico.nameServer.contract.NameServerOuterClass.DeleteRequest;
import pt.ulisboa.tecnico.nameServer.contract.NameServerOuterClass.RegisterRequest;

public class ServerMain {

  public static void main(String[] args) {
    System.out.println(ServerMain.class.getSimpleName());

    if (args.length < 4) {
      System.err.println("Argument(s) missing!");
      System.err.printf("Usage: java %s host port qual service%n", ServerMain.class.getName());
      return;
    }

    int port = Integer.parseInt(args[1]);
    BindableService service = new ServerCentralizedImpl();

    Server server = ServerBuilder.forPort(port).addService(service).build();

    boolean registered = false;

    // Try to register the server on the name service
    try {
      registerServer("localhost", 5001, args[3], args[0], port, args[2]);
      registered = true;
    } catch (Exception e) {
      System.err.println("Could not register server: " + e);
    }

    // If registered, schedule the unregister when the JVM is shutting down
    if (registered) {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        try {
          unregisterServer("localhost", 5001, args[3], args[0], port);
        } catch (Exception e) {
          System.err.println("Could not unregister server: " + e);
        }
      }));
    }

    try {
      server.start();
    } catch (IOException e) {
      System.err.println("Server could not start: " + e);
      return;
    }

    System.out.printf("%s Server %s started on %s:%s%n", args[3], args[2], args[0], args[1]);

    try {
      server.awaitTermination();
    } catch (InterruptedException e) {
      System.err.println("Server interrupted: " + e);
    }
  }

  public static void registerServer(String nameServerHostname, Integer nameServerPort, String serviceName, String host,
      Integer port, String qualifier) {
    ManagedChannel channel = ManagedChannelBuilder.forAddress(nameServerHostname, nameServerPort).usePlaintext()
        .build();
    NameServerBlockingStub stub = NameServerGrpc.newBlockingStub(channel);

    String hostname = host + ":" + port.toString();

    stub.register(RegisterRequest.newBuilder().setName(serviceName).setHost(hostname).setQualifier(qualifier).build());

    channel.shutdownNow();
  }

  public static void unregisterServer(String nameServerHostname, Integer nameServerPort, String serviceName,
      String host, Integer port) {
    ManagedChannel channel = ManagedChannelBuilder.forAddress(nameServerHostname, nameServerPort).usePlaintext()
        .build();
    NameServerBlockingStub stub = NameServerGrpc.newBlockingStub(channel);

    String hostname = host + ":" + port.toString();

    stub.delete(DeleteRequest.newBuilder().setName(serviceName).setHost(hostname).build());

    channel.shutdownNow();
  }
}
