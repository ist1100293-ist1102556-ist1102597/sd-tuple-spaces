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
import pt.ulisboa.tecnico.tuplespaces.client.collector.TakePhase1ReleaseResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.client.collector.TakePhase1ResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.client.collector.TakePhase2ResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.client.util.InterruptedRuntimeException;
import pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc.TupleSpacesReplicaStub;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.PutRequest;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.PutResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.ReadRequest;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.ReadResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase1ReleaseRequest;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase1ReleaseResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase1Request;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase1Response;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase2Request;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase2Response;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.getTupleSpacesStateRequest;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.getTupleSpacesStateResponse;

public class ClientService {

  OrderedDelayer delayer;
  private final List<TupleSpacesReplicaStub> stubs = new ArrayList<>();
  private final List<ManagedChannel> channels = new ArrayList<>();
  private int clientId;

  public ClientService(List<String> servers, Integer clientId) {
    delayer = new OrderedDelayer(servers.size());
    this.clientId = clientId;

    for (String server : servers) {
      String[] parts = server.split(":");

      ManagedChannel channel = ManagedChannelBuilder.forAddress(parts[0], Integer.parseInt(parts[1])).usePlaintext()
          .build();
      this.channels.add(channel);
      this.stubs.add(TupleSpacesReplicaGrpc.newStub(channel));
    }

  }

  public void shutdownChannel() {
    this.channels.stream().forEach(ManagedChannel::shutdownNow);
  }

  /*
   * This method allows the command processor to set the request delay assigned to
   * a given server
   */
  public void setDelay(int id, int delay) {
    delayer.setDelay(id, delay);
  }

  public void put(String tuple) {
    PutRequest request = PutRequest.newBuilder().setNewTuple(tuple).build();
    PutResponseCollector collector = new PutResponseCollector(this.stubs.size());

    for (Integer index : delayer) {
      TupleSpacesObserver<PutResponse> observer = new TupleSpacesObserver<>(collector, index);
      stubs.get(index).put(request, observer);
    }

    collector.waitForResponses();
  }

  public String read(String pattern) {
    ReadRequest request = ReadRequest.newBuilder().setSearchPattern(pattern).build();
    ReadResponseCollector collector = new ReadResponseCollector(this.stubs.size());

    Thread thread = (new Thread(() -> {
      try {
        for (Integer index : delayer) {
          TupleSpacesObserver<ReadResponse> observer = new TupleSpacesObserver<>(collector, index);
          stubs.get(index).read(request, observer);
        }
      } catch (InterruptedRuntimeException e) {
        return;
      }
    }));
    
    thread.start();

    String result = collector.getResponse().getResult();

    thread.interrupt();

    return result;
  }

  public Map<Integer, List<String>> takePhase1(String pattern) {
    HashMap<Integer, List<String>> result = new HashMap<>();

    TakePhase1Request request = TakePhase1Request.newBuilder().setSearchPattern(pattern).setClientId(clientId).build();
    TakePhase1ResponseCollector collector = new TakePhase1ResponseCollector(stubs.size());

    for (Integer index : delayer) {
      TupleSpacesObserver<TakePhase1Response> observer = new TupleSpacesObserver<>(collector, index);
      stubs.get(index).takePhase1(request, observer);
    }

    Map<Integer, TakePhase1Response> responses = collector.waitForResponses();

    responses.entrySet().stream().forEach(entry -> {
      int index = entry.getKey();
      TakePhase1Response response = entry.getValue();

      result.put(index, response.getReservedTuplesList());
    });

    return result;
  }

  public List<String> takePhase1(String pattern, Integer index) {
    TakePhase1Request request = TakePhase1Request.newBuilder().setSearchPattern(pattern).setClientId(clientId).build();
    TakePhase1ResponseCollector collector = new TakePhase1ResponseCollector(1);

    for (Integer i : delayer) {
      if (index == i) {
        TupleSpacesObserver<TakePhase1Response> observer = new TupleSpacesObserver<>(collector, index);
        stubs.get(index).takePhase1(request, observer);
        break;
      }
    }

    return collector.waitForResponses().get(index).getReservedTuplesList();
  }

  public void takePhase1Release() {
    TakePhase1ReleaseRequest request = TakePhase1ReleaseRequest.newBuilder().setClientId(clientId).build();
    TakePhase1ReleaseResponseCollector collector = new TakePhase1ReleaseResponseCollector(stubs.size());

    for (Integer index : delayer) {
      TupleSpacesObserver<TakePhase1ReleaseResponse> observer = new TupleSpacesObserver<>(collector, index);
      stubs.get(index).takePhase1Release(request, observer);
    }

    collector.waitForResponses();
  }

  public void takePhase1Release(Integer index) {
    TakePhase1ReleaseRequest request = TakePhase1ReleaseRequest.newBuilder().setClientId(clientId).build();
    TakePhase1ReleaseResponseCollector collector = new TakePhase1ReleaseResponseCollector(1);

    for (Integer i : delayer) {
      if (index == i) {
        TupleSpacesObserver<TakePhase1ReleaseResponse> observer = new TupleSpacesObserver<>(collector, index);
        stubs.get(index).takePhase1Release(request, observer);
        break; // Break so that the higher delays are ignored
      }
    }

    collector.waitForResponses();
  }

  public void takePhase2(String tuple) {
    TakePhase2Request request = TakePhase2Request.newBuilder().setTuple(tuple).setClientId(clientId).build();
    TakePhase2ResponseCollector collector = new TakePhase2ResponseCollector(stubs.size());

    for (Integer index : delayer) {
      TupleSpacesObserver<TakePhase2Response> observer = new TupleSpacesObserver<>(collector, index);
      stubs.get(index).takePhase2(request, observer);
    }

    collector.waitForResponses();
  }

  public List<String> getTupleSpacesState(Integer index) {

    TupleSpacesReplicaStub stub = stubs.get(index);

    if (stub == null) {
      throw new IllegalArgumentException("Unknown server: " + index);
    }

    getTupleSpacesStateRequest request = getTupleSpacesStateRequest.newBuilder().build();
    GetTupleSpacesStateResponseCollector collector = new GetTupleSpacesStateResponseCollector();

    TupleSpacesObserver<getTupleSpacesStateResponse> observer = new TupleSpacesObserver<>(collector, index);
    stub.getTupleSpacesState(request, observer);

    return collector.getResponse().getTupleList();
  }
}
