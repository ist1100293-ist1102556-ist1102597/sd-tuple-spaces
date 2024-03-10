package pt.ulisboa.tecnico.tuplespaces.client.collector;

import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase2Response;

public class TakePhase2ResponseCollector implements ResponseCollector<TakePhase2Response> {

    private int numServers;
    private int numResponses = 0;

    public TakePhase2ResponseCollector(int numServers) {
        this.numServers = numServers;
    }

	@Override
	public synchronized void addResponse(TakePhase2Response r, Integer server) {
        numResponses++;
        notifyAll();
    }

	@Override
	public synchronized void sendError(Throwable t) {
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
