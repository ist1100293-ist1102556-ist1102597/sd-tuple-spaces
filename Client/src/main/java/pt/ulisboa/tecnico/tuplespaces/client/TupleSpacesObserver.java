package pt.ulisboa.tecnico.tuplespaces.client;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.client.collector.ResponseCollector;

public class TupleSpacesObserver<R> implements StreamObserver<R> {

    private ResponseCollector<R> responseCollector;
    private int server;

    public TupleSpacesObserver(ResponseCollector<R> responseCollector, Integer server) {
        this.responseCollector = responseCollector;
        this.server = server;
    }

    @Override
    public void onNext(R value) {
        responseCollector.addResponse(value, server);
    }

    @Override
    public void onError(Throwable t) {
        
    }

    @Override
    public void onCompleted() {
    }
    
    

}
