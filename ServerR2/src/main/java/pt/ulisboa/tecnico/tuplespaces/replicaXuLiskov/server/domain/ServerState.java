package pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.server.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServerState {

  private class Tuple {
    private String tuple;
    private boolean locked = false;
    private int owner;

    public Tuple(String tuple) {
      this.tuple = tuple;
    }

    public String getTuple() {
      return this.tuple;
    }

    public boolean isLocked() {
      return this.locked;
    }

    public int getOwner() {
      return this.owner;
    }

    public void lock(int owner) {
      if (this.locked) {
        return;
      }
      this.locked = true;
      this.owner = owner;
    }

    public void unlock(int owner) {
      if (!this.locked || this.owner != owner) {
        return;
      }

      this.locked = false;
    }
  }

  private List<Tuple> tuples;

  public ServerState() {
    this.tuples = new ArrayList<Tuple>();

  }

  public void put(String tuple) {
    tuples.add(new Tuple(tuple));
  }

  private Tuple getMatchingTuple(String pattern) {
    return this.tuples.stream().filter(tuple -> tuple.getTuple().matches(pattern)).findFirst().orElse(null);
  }

  public String read(String pattern) {
    Tuple tuple = getMatchingTuple(pattern);

    if (tuple == null) {
      return null;
    }

    return tuple.getTuple();
  }

  public Optional<List<String>> takePhase1(String pattern, int clientId) {
    List<String> matchingTuples = new ArrayList<>();

    if (!tuples.stream().filter(tuple -> tuple.getTuple().matches(pattern)).findAny().isPresent()) {
      return Optional.empty();
    }

    tuples.stream()
        .filter(tuple -> !tuple.isLocked() && tuple.getTuple().matches(pattern))
        .forEach(tuple -> {
          tuple.lock(clientId);
          matchingTuples.add(tuple.getTuple());
        });

    return Optional.of(matchingTuples);
  }

  public void takePhase1Release(int clientId) {
    tuples.stream()
        .filter(tuple -> tuple.isLocked() && tuple.getOwner() == clientId)
        .forEach(tuple -> tuple.unlock(clientId));
  }

  public void takePhase2(String tuple, int clientId) {
    Tuple matchingTuple = tuples.stream()
        .filter(t -> t.getTuple().equals(tuple) && t.isLocked() && t.getOwner() == clientId)
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No matching tuple found"));

    tuples.remove(matchingTuple);
  }

  public List<String> getTupleSpacesState() {
    List<String> resultingTuples = new ArrayList<>();

    tuples.stream().forEach(tuple -> resultingTuples.add(tuple.getTuple()));

    return resultingTuples;
  }
}
