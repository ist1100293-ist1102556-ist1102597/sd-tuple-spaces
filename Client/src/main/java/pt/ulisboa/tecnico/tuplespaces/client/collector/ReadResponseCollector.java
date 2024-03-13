package pt.ulisboa.tecnico.tuplespaces.client.collector;

import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.ReadResponse;

public class ReadResponseCollector implements ResponseCollector<ReadResponse> {

    private boolean hasResponse = false;
    private ReadResponse response;
    private int numResponses = 0;
    private int numServers;
    private RuntimeException error;

    public ReadResponseCollector(int numServers) {
        this.numServers = numServers;
    }

    @Override
    public synchronized void addResponse(ReadResponse r, Integer server) {
        numResponses++;
        if (!hasResponse) {
            this.response = r;
            hasResponse = true;
        }
        notifyAll();
    }

    public synchronized ReadResponse getResponse() {
        while (!hasResponse) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (numResponses == numServers && !hasResponse) {
                throw error;
            }
        }
        return response;
    }

    @Override
    public synchronized void sendError(RuntimeException t) {
        error = t;
        numResponses++;
        notifyAll();
    }
}
