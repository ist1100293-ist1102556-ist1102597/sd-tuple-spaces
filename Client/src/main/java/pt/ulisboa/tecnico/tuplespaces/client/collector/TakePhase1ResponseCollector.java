package pt.ulisboa.tecnico.tuplespaces.client.collector;

import java.util.HashMap;
import java.util.Map;

import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase1Response;

public class TakePhase1ResponseCollector implements ResponseCollector<TakePhase1Response> {

    private int numServers;
    private int numResponses = 0;
    Map<Integer, TakePhase1Response> responses = new HashMap<>();
    private RuntimeException error = null;

    public TakePhase1ResponseCollector(int numServers) {
        this.numServers = numServers;
    }

	@Override
	public synchronized void addResponse(TakePhase1Response r, Integer server) {
        responses.put(server, r);
        numResponses++;
        notifyAll();
    }

	@Override
	public synchronized void sendError(RuntimeException t) {
        error = t;
        numResponses++;
        notifyAll();
	}
    
    public synchronized Map<Integer, TakePhase1Response> waitForResponses() {
        while (numResponses < numServers) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (error != null){
                throw error;
            }
        }
        return responses;
    }

}
