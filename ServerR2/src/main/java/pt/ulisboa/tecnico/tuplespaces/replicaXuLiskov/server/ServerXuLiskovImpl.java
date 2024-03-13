package pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.server;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc.TupleSpacesReplicaImplBase;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.*;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.server.domain.ServerState;

import static io.grpc.Status.CANCELLED;
import static io.grpc.Status.INVALID_ARGUMENT;

import java.util.List;
import java.util.Optional;

public class ServerXuLiskovImpl extends TupleSpacesReplicaImplBase {

    private static final String BGN_TUPLE = "<";
    private static final String END_TUPLE = ">";

    private ServerState serverState = new ServerState();

    @Override
    public synchronized void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        if(!inputIsValid(request.getNewTuple())){
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Input has to be a valid tuple").asRuntimeException());
            return;
        }

        serverState.put(request.getNewTuple());
        notifyAll();
        PutResponse response = PutResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public synchronized void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
        if(!inputIsValid(request.getSearchPattern())){
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Input has to be a valid search pattern").asRuntimeException());
            return;
        }

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
    public synchronized void takePhase1(TakePhase1Request request, StreamObserver<TakePhase1Response> responseObserver) {
        Optional<List<String>> tuples;
        
        if(!inputIsValid(request.getSearchPattern())){
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Input has to be a valid search pattern").asRuntimeException());
            return;
        }

        while((tuples = serverState.takePhase1(request.getSearchPattern(), request.getClientId())).isEmpty()){
            try {
                wait();
            } catch (InterruptedException e) {
                responseObserver.onError(CANCELLED.withDescription("Take operation interrupted").asRuntimeException());
            }
        }
        
        TakePhase1Response response = TakePhase1Response.newBuilder().addAllReservedTuples(tuples.get()).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public synchronized void takePhase1Release(TakePhase1ReleaseRequest request, StreamObserver<TakePhase1ReleaseResponse> responseObserver) {
        serverState.takePhase1Release(request.getClientId());
        TakePhase1ReleaseResponse response = TakePhase1ReleaseResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public synchronized void takePhase2(TakePhase2Request request, StreamObserver<TakePhase2Response> responseObserver) {
        if(!inputIsValid(request.getTuple())){
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Input has to be a valid tuple").asRuntimeException());
            return;
        }
        
        serverState.takePhase2(request.getTuple(), request.getClientId());
        TakePhase2Response response = TakePhase2Response.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public synchronized void getTupleSpacesState(getTupleSpacesStateRequest request, StreamObserver<getTupleSpacesStateResponse> responseObserver) {
        List<String> tuples = serverState.getTupleSpacesState();
        getTupleSpacesStateResponse response = getTupleSpacesStateResponse.newBuilder().addAllTuple(tuples).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted(); 
    }

    private boolean inputIsValid(String input) {
        if (input.length() < 2
                ||
                !input.startsWith(BGN_TUPLE)
                ||
                !input.endsWith(END_TUPLE)) {
            return false;
        } else {
            return true;
        }
    }

}