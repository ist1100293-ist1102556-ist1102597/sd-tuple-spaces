package pt.ulisboa.tecnico.tuplespaces.client.collector;

import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.PutResponse;

public class PutResponseCollector implements ResponseCollector<PutResponse>{

    private int numValidResponses = 0;
    private int numResponses = 0;
    private int numServers;

    public PutResponseCollector(int numServers) {
        this.numServers = numServers;
    }

    @Override
    public synchronized void addResponse(PutResponse r, String server) {
        numValidResponses++;
        numResponses++;
        notifyAll();
    }
    
    public synchronized void waitForResponses(int numServers) {
        while (numResponses < numServers) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (numValidResponses == 0) { // In case all servers are "dead"
            throw new RuntimeException("No response received");
        } // Otherwise, we have at least one valid response
    }

    @Override
    public synchronized void sendError(Throwable t) {
        numResponses++;
        notifyAll();
    }
}
