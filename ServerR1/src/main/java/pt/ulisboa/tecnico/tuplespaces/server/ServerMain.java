package pt.ulisboa.tecnico.tuplespaces.server;

import java.io.IOException;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

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

    try {
      server.start();
    } catch (IOException e) {
      System.err.println("Server could not start: " + e);
    }

    System.out.printf("%s Server %s started on %s:%s%n", args[3], args[2], args[0], args[1]);

    try {
      server.awaitTermination();
    } catch (InterruptedException e) {
      System.err.println("Server interrupted: " + e);
    }

  }
}
