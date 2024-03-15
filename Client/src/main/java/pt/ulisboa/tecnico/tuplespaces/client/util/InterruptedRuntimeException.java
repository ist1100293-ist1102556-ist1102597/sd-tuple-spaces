package pt.ulisboa.tecnico.tuplespaces.client.util;

// This class is only used to specify that the next() method on an
// iterator got an interrupted exception, since this is a subclass
// of RuntimeException, we don't have to change the method signature
// (which we can't on Iterators).
public class InterruptedRuntimeException extends RuntimeException {

}
