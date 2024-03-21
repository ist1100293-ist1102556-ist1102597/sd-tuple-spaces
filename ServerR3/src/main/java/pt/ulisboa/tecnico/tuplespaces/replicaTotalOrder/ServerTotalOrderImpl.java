package pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.server;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.CANCELLED;

import java.util.List;
import java.util.concurrent.locks.*;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaGrpc.TupleSpacesReplicaImplBase;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.*;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.server.domain.ServerState;

public class ServerTotalOrderImpl extends TupleSpacesReplicaImplBase {

    private Lock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();
    private ServerState serverState = new ServerState();

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        lock.lock();
        serverState.put(request.getNewTuple());
        condition.signalAll();
        PutResponse response = PutResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        lock.unlock();
    }

    @Override
    public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
        lock.lock();
        String tuple = serverState.read(request.getSearchPattern());
        while(tuple == null){
            try {
                condition.await();
            } catch (InterruptedException e) {
                responseObserver.onError(CANCELLED.withDescription("Read operation interrupted").asRuntimeException());
            }
            tuple = serverState.read(request.getSearchPattern());
        }
        ReadResponse response = ReadResponse.newBuilder().setResult(tuple).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        lock.unlock();
    }

    @Override
    public void take(TakeRequest request, StreamObserver<TakeResponse> responseObserver) {
        lock.lock();
        String tuple = serverState.take(request.getSearchPattern());
        while(tuple == null){
            try {
                condition.await();
            } catch (InterruptedException e) {
                responseObserver.onError(CANCELLED.withDescription("Take operation interrupted").asRuntimeException());
            }
            tuple = serverState.take(request.getSearchPattern());
        }
        TakeResponse response = TakeResponse.newBuilder().setResult(tuple).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        lock.unlock();
    }

    @Override
    public void getTupleSpacesState(getTupleSpacesStateRequest request, StreamObserver<getTupleSpacesStateResponse> responseObserver) {
        lock.lock();
        List<String> tuples = serverState.getTupleSpacesState();
        getTupleSpacesStateResponse response = getTupleSpacesStateResponse.newBuilder().addAllTuple(tuples).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        lock.unlock();
    }

}