package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.CANCELLED;

import java.util.List;

import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;

public class ServerCentralizedImpl extends TupleSpacesGrpc.TupleSpacesImplBase {

    private ServerState serverState = new ServerState();

    @Override
    public synchronized void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        serverState.put(request.getNewTuple());
        notifyAll();
        PutResponse response = PutResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public synchronized void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
        String tuple = serverState.read(request.getSearchPattern());
        while(tuple == null){
            try {
                wait();
            } catch (InterruptedException e) {
                responseObserver.onError(CANCELLED.withDescription("Read operation interrupted").asRuntimeException());
            }
            tuple = serverState.read(request.getSearchPattern());
        }
        ReadResponse response = ReadResponse.newBuilder().setResult(tuple).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void take(TakeRequest request, StreamObserver<TakeResponse> responseObserver) {
        String tuple = serverState.take(request.getSearchPattern());
        while(tuple == null){
            try {
                wait();
            } catch (InterruptedException e) {
                responseObserver.onError(CANCELLED.withDescription("Take operation interrupted").asRuntimeException());
            }
            tuple = serverState.take(request.getSearchPattern());
        }
        TakeResponse response = TakeResponse.newBuilder().setResult(tuple).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getTupleSpacesState(getTupleSpacesStateRequest request, StreamObserver<getTupleSpacesStateResponse> responseObserver) {
        List<String> tuples = serverState.getTupleSpacesState();
        getTupleSpacesStateResponse response = getTupleSpacesStateResponse.newBuilder().addAllTuple(tuples).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted(); 
    }

}