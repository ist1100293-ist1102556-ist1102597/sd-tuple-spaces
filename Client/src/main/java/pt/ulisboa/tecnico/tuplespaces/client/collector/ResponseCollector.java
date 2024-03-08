package pt.ulisboa.tecnico.tuplespaces.client.collector;

public interface ResponseCollector<R> {
    
    public void addResponse(R r, String server);
    public void sendError(Throwable t);

}
