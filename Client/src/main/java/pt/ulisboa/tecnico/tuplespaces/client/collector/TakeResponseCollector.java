package pt.ulisboa.tecnico.tuplespaces.client.collector;

import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.TakeResponse;

public class TakeResponseCollector implements ResponseCollector<TakeResponse> {

    private int numValidResponses = 0;
    private int numResponses = 0;
    private int numServers;
    private RuntimeException error;
    private TakeResponse response;

    public TakeResponseCollector(int numServers) {
        this.numServers = numServers;
    }

    @Override
	public synchronized void addResponse(TakeResponse r, Integer server) {
        numValidResponses++;
        numResponses++;
        if (response == null) {
            response = r;
        }
        notifyAll();
    }

	@Override
	public synchronized void sendError(RuntimeException t) {
        error = t;
        numResponses++;
        notifyAll();
	}

    public synchronized TakeResponse waitForResponses() {
        while (numResponses < numServers) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (numValidResponses == 0) { // In case all servers are "dead"
            throw error;
        } // Otherwise, we have at least one valid response
        return response;
    }
}
