package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.tuplespaces.client.TupleSpacesObserver;
import pt.ulisboa.tecnico.tuplespaces.client.collector.GetTupleSpacesStateResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.client.collector.PutResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.client.collector.ReadResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc.TupleSpacesReplicaStub;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.PutRequest;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.PutResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.ReadRequest;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.ReadResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.getTupleSpacesStateRequest;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.getTupleSpacesStateResponse;

public class ClientService {

  private final Map<String, TupleSpacesReplicaStub> stubs = new HashMap<>();
  private final List<ManagedChannel> channels = new ArrayList<>();

  public ClientService(Map<String, String> servers) {
    
    for (Map.Entry<String, String> entry : servers.entrySet()) {
      String qualifier = entry.getKey();
      String server = entry.getValue();
      String[] parts = server.split(":");

      ManagedChannel channel = ManagedChannelBuilder.forAddress(parts[0], Integer.parseInt(parts[1])).usePlaintext().build();
      this.channels.add(channel);
      this.stubs.put(qualifier, TupleSpacesReplicaGrpc.newStub(channel));
    }

  }

  public void shutdownChannel() {
    this.channels.stream().forEach(ManagedChannel::shutdownNow);
  }

  public void put(String tuple){
    PutRequest request = PutRequest.newBuilder().setNewTuple(tuple).build();
    PutResponseCollector collector = new PutResponseCollector(this.stubs.size());

    this.stubs.entrySet().stream().forEach(entry -> {
      String qualifier = entry.getKey();
      TupleSpacesReplicaStub stub = entry.getValue();

      TupleSpacesObserver<PutResponse> observer = new TupleSpacesObserver<>(collector, qualifier);
      stub.put(request, observer);
    });
    
    collector.waitForResponses();
  }

  public String read(String pattern){
    ReadRequest request = ReadRequest.newBuilder().setSearchPattern(pattern).build();
    ReadResponseCollector collector = new ReadResponseCollector(this.stubs.size());

    this.stubs.entrySet().stream().forEach(entry -> {
      String qualifier = entry.getKey();
      TupleSpacesReplicaStub stub = entry.getValue();

      TupleSpacesObserver<ReadResponse> observer = new TupleSpacesObserver<>(collector, qualifier);
      stub.read(request, observer);
    });

    return collector.getResponse().getResult();
  }

  public String take(String pattern){
    /*
    TakeRequest request = TakeRequest.newBuilder().setSearchPattern(pattern).build();
    TakeResponse response = this.stub.take(request);
    return response.getResult();
    */
    return "TODO";
  }

  public List<String> getTupleSpacesState(String qualifier){
    
    TupleSpacesReplicaStub stub = stubs.get(qualifier);

    if (stub == null) {
      throw new IllegalArgumentException("Unknown qualifier: " + qualifier);
    }

    getTupleSpacesStateRequest request = getTupleSpacesStateRequest.newBuilder().build();
    GetTupleSpacesStateResponseCollector collector = new GetTupleSpacesStateResponseCollector();
    
    TupleSpacesObserver<getTupleSpacesStateResponse> observer = new TupleSpacesObserver<>(collector, qualifier);
    stub.getTupleSpacesState(request, observer);
    
    return collector.getResponse().getTupleList();
  }
}
