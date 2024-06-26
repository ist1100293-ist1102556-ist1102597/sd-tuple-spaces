package pt.ulisboa.tecnico.tuplespaces.client.collector;

import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.getTupleSpacesStateResponse;

public class GetTupleSpacesStateResponseCollector implements ResponseCollector<getTupleSpacesStateResponse> {

    getTupleSpacesStateResponse response;
    Throwable error;

    @Override
	public synchronized void addResponse(getTupleSpacesStateResponse r, Integer server) {
		response = r;
        notifyAll();
	}

	@Override
	public synchronized void sendError(RuntimeException t) {
        error = t;
        notifyAll();
	}

    public synchronized getTupleSpacesStateResponse getResponse() {
        while (response == null && error == null) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (error != null) {
            throw new RuntimeException(error);
        }

        return response;
    }
}
