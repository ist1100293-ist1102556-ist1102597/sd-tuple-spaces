package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;

public class ClientService {

  /*TODO: The gRPC client-side logic should be here.
        This should include a method that builds a channel and stub,
        as well as individual methods for each remote operation of this service. */

  private final TupleSpacesGrpc.TupleSpacesBlockingStub stub;
  private final ManagedChannel channel;

  public ClientService(String host, Integer port) {
    this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    this.stub = TupleSpacesGrpc.newBlockingStub(this.channel);
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
}
