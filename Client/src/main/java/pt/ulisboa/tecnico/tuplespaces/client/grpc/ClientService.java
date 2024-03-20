package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import java.util.ArrayList;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.tuplespaces.client.TupleSpacesObserver;
import pt.ulisboa.tecnico.tuplespaces.client.collector.GetTupleSpacesStateResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.client.collector.PutResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.client.collector.ReadResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.client.collector.TakeResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.client.util.InterruptedRuntimeException;
import pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaGrpc.TupleSpacesReplicaStub;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.*;

public class ClientService {

  OrderedDelayer delayer;
  private final List<TupleSpacesReplicaStub> stubs = new ArrayList<>();
  private final List<ManagedChannel> channels = new ArrayList<>();

  public ClientService(List<String> servers) {
    delayer = new OrderedDelayer(servers.size());

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
    // TODO: Add sequence number coming from the sequencer
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

    // Launch a second thread with the objective of launching the grpc calls
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

    // After we get the result, we can interrupt the thread, meaning that very high
    // delays don't block the client when the main thread stops (exit).
    thread.interrupt();

    return result;
  }

  public void take(String tuple){
    // TODO: Add sequence number coming from the sequencer
    TakeRequest request = TakeRequest.newBuilder().setSearchPattern(tuple).build();
    TakeResponseCollector collector = new TakeResponseCollector(stubs.size());

    for (Integer index : delayer) {
      TupleSpacesObserver<TakeResponse> observer = new TupleSpacesObserver<>(collector, index);
      stubs.get(index).take(request, observer);
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
