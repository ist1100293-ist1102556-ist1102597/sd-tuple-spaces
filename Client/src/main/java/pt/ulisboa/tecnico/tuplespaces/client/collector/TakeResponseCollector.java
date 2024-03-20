package pt.ulisboa.tecnico.tuplespaces.client.collector;

import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.TakeResponse;

public class TakeResponseCollector implements ResponseCollector<TakeResponse> {

    public TakeResponseCollector(int numServers) {
        // TODO: implement this constructor
    }

    @Override
	public synchronized void addResponse(TakeResponse r, Integer server) {
        // TODO: implement this method
        notifyAll();
    }

	@Override
	public synchronized void sendError(RuntimeException t) {
        // TODO: implement this method
        notifyAll();
	}

    public synchronized void waitForResponses() {
        // TODO: Implement this method
    }
}
