package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;
import pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;

public class ClientService {

    /* TODO: This class should implement the front-end of the replicated TupleSpaces service 
        (according to the Xu-Liskov algorithm)*/

  private final TupleSpacesGrpc.TupleSpacesBlockingStub stub;
  private final ManagedChannel channel;
  private final OrderedDelayer delayer;

  public ClientService(String host, Integer port, Integer numServers) {
    this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    this.stub = TupleSpacesGrpc.newBlockingStub(this.channel);
    this.delayer = new OrderedDelayer(numServers);
  }

  public void shutdownChannel() {
    this.channel.shutdownNow();
  }

  public void put(String tuple){
    PutRequest request = PutRequest.newBuilder().setNewTuple(tuple).build();
    this.stub.put(request);
  }

  public String read(String pattern){
    ReadRequest request = ReadRequest.newBuilder().setSearchPattern(pattern).build();
    ReadResponse response = this.stub.read(request);
    return response.getResult();
  }

  public String take(String pattern){
    TakeRequest request = TakeRequest.newBuilder().setSearchPattern(pattern).build();
    TakeResponse response = this.stub.take(request);
    return response.getResult();
  }

  public List<String> getTupleSpacesState(){
    getTupleSpacesStateRequest request = getTupleSpacesStateRequest.newBuilder().build();
    getTupleSpacesStateResponse response = this.stub.getTupleSpacesState(request);
    return response.getTupleList();
  }

  /* This method allows the command processor to set the request delay assigned to a given server */
  public void setDelay(int id, int delay) {
    delayer.setDelay(id, delay);

    /* TODO: Remove this debug snippet */
    System.out.println("[Debug only]: After setting the delay, I'll test it");
    for (Integer i : delayer) {
      System.out.println("[Debug only]: Now I can send request to stub[" + i + "]");
  }
  System.out.println("[Debug only]: Done.");
}

  /* TODO: individual methods for each remote operation of the TupleSpaces service */

  /* Example: How to use the delayer before sending requests to each server 
  *          Before entering each iteration of this loop, the delayer has already 
  *          slept for the delay associated with server indexed by 'id'.
  *          id is in the range 0..(numServers-1).

      for (Integer id : delayer) {
          //stub[id].some_remote_method(some_arguments);
      }
  */
}
