package hoggle;

import java.util.ArrayList;

public final class Junction extends PathElement {
    private ArrayList<Direction> unexplored;

    public Junction(ArrayList<Direction> branches) { unexplored = branches; }

    public Direction getUnexplored(int ind) { return unexplored.get(ind); }

    public Direction[] getAllUnexplored() {
      Direction[] options = new Direction[unexplored.size()];

      for (int i = 0; i < unexplored.size(); i++)
        options[i] = unexplored.get(i);

      return options;
    }

    public int getUnexploredCount() { return unexplored.size(); }

    public void remove(Direction dir) { unexplored.remove(dir); }

    @Override
    public String toString() { return "JUNCTION" + unexplored.toString(); }
}
