package pt.ulisboa.tecnico.tuplespaces.client.collector;

import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase1ReleaseResponse;

public class TakePhase1ReleaseResponseCollector implements ResponseCollector<TakePhase1ReleaseResponse> {

    private int numServers;
    private int numResponses = 0;

    public TakePhase1ReleaseResponseCollector(int numServers) {
        this.numServers = numServers;
    }

	@Override
	public synchronized void addResponse(TakePhase1ReleaseResponse r, Integer server) {
        numResponses++;
        notifyAll();
    }

	@Override
	public synchronized void sendError(RuntimeException t) {
        numResponses++;
        notifyAll();
    }
    
    public synchronized void waitForResponses() {
        while (numResponses < numServers) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
	}
    
}
