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

    private Lock lock = new ReentrantLock(); // Lock for mutual exclusion
    private ServerState serverState = new ServerState();
    private List<DeferedTake> deferedTakes = new ArrayList<>(); // List of defered take operations
    private Condition totalOrderCondition = lock.newCondition(); // Condition for total order for the entire tuple space
    private int currSeq = 1; // Current sequence number

    private class DeferedTake {
        // Class to represent a defered take operation
        private String pattern; // Pattern to match the tuple
        private Lock lock; // Lock for mutual exclusion
        private Condition condition; // Exclusive Condition to wait for the tuple
        private String tuple; // Tuple is stored here when it arrives in the server and matches the pattern

        public DeferedTake(String pattern, Lock lock) {
            this.pattern = pattern;
            this.lock = lock; // Same lock used for the mutual exclusion in the server
            this.condition = lock.newCondition(); // Condition for this defered take operation
        }

        // Should be called in lock's mutual exclusion region
        public String awaitTuple() {
            while(tuple == null) { // Wait for the tuple to arrive
                try {
                    condition.await(); // If the tuple is not here, wait
                } catch (InterruptedException e) {
                    return null;
                }
            }
            return tuple; // Return the tuple when it arrives
        }

        // Should be called in lock's mutual exclusion region
        public void giveTuple(String tuple) {
            // Store the tuple and signal the condition to wake up the defered take operation
            this.tuple = tuple;
            condition.signalAll();
        }

        public boolean matches(String tuple) { 
            // Check if the tuple matches the pattern
            return tuple.matches(pattern);
        }
    }

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        lock.lock(); // Acquire the lock
        awaitTurn(request.getSeqNumber()); // Wait for the turn to execute the operation

        for (DeferedTake deferedTake : deferedTakes) {
            // First check it there are any pending take operations that can be satisfied
            if (deferedTake.matches(request.getNewTuple())) {
                // If the tuple matches the pattern of the defered take, give the tuple to the take operation
                deferedTake.giveTuple(request.getNewTuple());
                deferedTakes.remove(deferedTake); // Remove the defered take operation from the list
                // Signal the total order condition to continue the total order
                responseObserver.onNext(PutResponse.newBuilder().build());
                responseObserver.onCompleted();

                nextTurn(); // Move to the next turn in total order
                lock.unlock(); // Release the lock
                return;
            }
        }
        // If there are no pending take operations that can be satisfied, put the tuple in the tuple space
        serverState.put(request.getNewTuple());
        totalOrderCondition.signalAll(); // Signal every waiting operation in the tuple spaces, except the deferred take operations

        // Send the response to the client
        PutResponse response = PutResponse.newBuilder().build(); 

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        nextTurn(); // Move to the next turn in total order
        lock.unlock(); // Release the lock
    }

    @Override
    public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
        lock.lock(); // Acquire the lock

        String tuple = serverState.read(request.getSearchPattern()); // Try to read the tuple from the tuple space

        while(tuple == null){
            try {
                totalOrderCondition.await(); // Wait for the tuple to arrive
            } catch (InterruptedException e) {
                responseObserver.onError(CANCELLED.withDescription("Read operation interrupted").asRuntimeException());
            }
            // Try to read the tuple again
            tuple = serverState.read(request.getSearchPattern());
        }
        // Send the response to the client
        ReadResponse response = ReadResponse.newBuilder().setResult(tuple).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        // Release the lock
        // The read function does not affect the total order, so we do not change the sequence number
        lock.unlock();
    }

    @Override
    public void take(TakeRequest request, StreamObserver<TakeResponse> responseObserver) {
        lock.lock(); // Acquire the lock
        // Try to take the tuple from the tuple space
        awaitTurn(request.getSeqNumber());
        String tuple = serverState.take(request.getSearchPattern());
        if (tuple == null) { // If the tuple is not in the tuple space
            // Defer the take operation
            DeferedTake deferedTake = new DeferedTake(request.getSearchPattern(), lock);
            deferedTakes.add(deferedTake); // Add the defered take operation to the list
            nextTurn(); // Move to the next turn in total order
            tuple = deferedTake.awaitTuple(); // Wait for the tuple to arrive
        } else {
            // If the tuple is in the tuple space, increase the sequence number
            nextTurn();
        }
        // Send the response to the client
        TakeResponse response = TakeResponse.newBuilder().setResult(tuple).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
        // Release the lock
        lock.unlock();
    }

    @Override
    public void getTupleSpacesState(getTupleSpacesStateRequest request, StreamObserver<getTupleSpacesStateResponse> responseObserver) {
        lock.lock(); // Acquire the lock
        List<String> tuples = serverState.getTupleSpacesState(); // Get the state of the tuple space
        // Send the response to the client
        getTupleSpacesStateResponse response = getTupleSpacesStateResponse.newBuilder().addAllTuple(tuples).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        lock.unlock(); // Release the lock
    }

    // Should Be executed in lock's mutual exclusion region
    private void awaitTurn(int seqNumber) {
        // Wait for the turn to execute the operation
        while (currSeq != seqNumber) {
            try {
                // Wait for the total order condition to signal the operation
                totalOrderCondition.await();
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    // Should Be executed in lock's mutual exclusion region
    private void nextTurn() {
        // Move to the next turn in total order
        currSeq++;
        // Signal the total order condition to continue to next operation in the intended order
        totalOrderCondition.signalAll();
    }

}