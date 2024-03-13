package pt.ulisboa.tecnico.tuplespaces.client.collector;

import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.PutResponse;

public class PutResponseCollector implements ResponseCollector<PutResponse>{

    private int numValidResponses = 0;
    private int numResponses = 0;
    private int numServers;
    private RuntimeException error;

    public PutResponseCollector(int numServers) {
        this.numServers = numServers;
    }

    @Override
    public synchronized void addResponse(PutResponse r, Integer server) {
        numValidResponses++;
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
        if (numValidResponses == 0) { // In case all servers are "dead"
            throw error;
        } // Otherwise, we have at least one valid response
    }

    @Override
    public synchronized void sendError(RuntimeException t) {
        error = t;
        numResponses++;
        notifyAll();
    }
}
