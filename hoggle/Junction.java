package hoggle;

import java.util.ArrayList;

public final class Junction extends PathElement {
    private ArrayList<Direction> unexplored;

    /**
     * A Junction is a list of directions representing the unexplored branches of some position in a maze. The position is irrelevant
     * and absent from a Junction's state because Hoggle only ever pulls data from the Junction it currently stands in. In Hoggle's path
     * stack, Junctions are preceded by the sequence of moves taken to reach it from the previous Junction.
     *
     * @see Direction, Hoggle
     */
    public Junction(ArrayList<Direction> branches) { unexplored = branches; }

    /**
     * @param ind the index of the unexplored branch to get from the list (indices are not mapped to any specific direction)
     * @return the Direction at the index specified
     * @see Direction
     */
    public Direction getUnexplored(int ind) { return unexplored.get(ind); }

    public Direction[] getAllUnexplored() {
      Direction[] options = new Direction[unexplored.size()];

      for (int i = 0; i < unexplored.size(); i++)
        options[i] = unexplored.get(i);

      return options;
    }

    /**
     * @return the number of unexplored branches at this Junction
     */
    public int getUnexploredCount() { return unexplored.size(); }

    /**
     * @param dir the Direction to strike from this Junction's unexplored branches
     * @see Direction
     */
    public void remove(Direction dir) { unexplored.remove(dir); }

    // String conversion for debug purposes
    @Override
    public String toString() { return "JUNCTION" + unexplored.toString(); }
}
