package pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.server;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.CANCELLED;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.*;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaGrpc.TupleSpacesReplicaImplBase;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.*;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.server.domain.ServerState;

public class ServerTotalOrderImpl extends TupleSpacesReplicaImplBase {

    private Lock lock = new ReentrantLock();
    private ServerState serverState = new ServerState();
    private List<DeferedTake> deferedTakes = new ArrayList<>();
    private Condition totalOrderCondition = lock.newCondition();
    private int currSeq = 0;

    private class DeferedTake {
        private String pattern;
        private Lock lock;
        private Condition condition;
        private String tuple;

        public DeferedTake(String pattern, Lock lock) {
            this.pattern = pattern;
            this.lock = lock;
            this.condition = lock.newCondition();
        }

        // Should be called in lock's mutual exclusion region
        public String awaitTuple() {
            while(tuple == null) {
                try {
                    condition.await();
                } catch (InterruptedException e) {
                    return null;
                }
            }
            return tuple;
        }

        // Should be called in lock's mutual exclusion region
        public void giveTuple(String tuple) {
            this.tuple = tuple;
            condition.signalAll();
        }

        public boolean matches(String tuple) {
            return tuple.matches(pattern);
        }
    }

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        lock.lock();
        awaitTurn(request.getSeqNumber());

        for (DeferedTake deferedTake : deferedTakes) {
            if (deferedTake.matches(request.getNewTuple())) {
                deferedTake.giveTuple(request.getNewTuple());
                deferedTakes.remove(deferedTake);

                responseObserver.onNext(PutResponse.newBuilder().build());
                responseObserver.onCompleted();

                nextTurn();
                lock.unlock();
                return;
            }
        }

        serverState.put(request.getNewTuple());
        totalOrderCondition.signalAll();

        PutResponse response = PutResponse.newBuilder().build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        nextTurn();
        lock.unlock();
    }

    @Override
    public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
        lock.lock();

        String tuple = serverState.read(request.getSearchPattern());

        while(tuple == null){
            try {
                totalOrderCondition.await();
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

        // TODO: implement deferd takes
        String tuple = serverState.take(request.getSearchPattern());
        while(tuple == null){
            try {
                totalOrderCondition.await();
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

    // Should Be executed in lock's mutual exclusion region
    private void awaitTurn(int seqNumber) {
        while (currSeq != seqNumber) {
            try {
                totalOrderCondition.await();
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    // Should Be executed in lock's mutual exclusion region
    private void nextTurn() {
        currSeq++;
        totalOrderCondition.signalAll();
    }

}