package hoggle;

import java.util.ArrayList;

public final class Drone extends Hoggle {
  private int startX, startY;

  /**
   * A Drone is an object used by Hoggle to optimize a maze before attempting to solve it. One Drone is deployed to every
   * dead-end in the maze, its goal being to find the entrance to that dead-end and register it with Hoggle as an impassable node.
   *
   * @param maze a non-jagged 2D array of booleans representing which nodes may be passed and which ones may not; not cloned for effiency purposes
   * @param pos the Drone's starting position within the maze
   * @param mazeStart Hoggle's starting position within the maze (must be specified so the Drone does not accidentally block it)
   * @param mazeEnd Hoggle's ending position within the maze (must be specified so the Drone does not accidentally block it)
   * @param rbs roadblocks; coordinates already registered impassable by previous Drone sweeps
   * @see Coordinate, Hoggle
   */
  public Drone(boolean[][] maze, Coordinate pos, Coordinate mazeStart, Coordinate mazeEnd, ArrayList<Coordinate> rbs) {
    super(maze);

    x = pos.getX();
    y = pos.getY();
    startX = mazeStart.getX();
    startY = mazeStart.getY();
    endX = mazeEnd.getX();
    endY = mazeEnd.getY();

    for (Coordinate c : rbs)
      roadblocks.add(c);

    assert (wallCount(x, y) == 3);
  }

  /**
   * Prompt the drone to backtrack from the dead-end until its entrance is reached, and return that entrance's position
   *
   * @return the coordinates of the dead-end's entrance, or null if either a) this dead-end is isolated or b) this dead-end includes the maze's start or end
   * @see Coordinate
   */
  protected Coordinate backtrack() {
    if (startOrEnd(x, y))
      return null;

    // Determine the direction opposite the dead-end
    Direction dir = Direction.UP;
    if (!blocked(x, y+1)) dir = Direction.DOWN;
    if (!blocked(x+1, y)) dir = Direction.RIGHT;
    if (!blocked(x-1, y)) dir = Direction.LEFT;

    // Backtrack
    while (wallCount(x, y) >= 2) {
      boolean entranceReached = false;

      // Move in the current direction
      while (!collision(dir)) {
        move(dir);

        // Make sure I'm not accidentally about to roadblock the maze's start or end
        if (startOrEnd(x, y))
          return null;

        // If I'm only touching two walls, I've officially left the dead-end hallway and should break the search loop
        if (wallCount(x, y) < 2) {
          entranceReached = true;
          break;
        }
      }

      if (entranceReached)
        break;

      // If another dead-end is hit, this drone was deployed in an isolated section of the maze. It can do no more
      if (wallCount(x, y) == 3)
        return null;

      // The last loop broke, so a turn was encountered -- find what direction I should turn in to keep going
      for (int dx = -1; dx <= 1; dx += 1)
        for (int dy = -1; dy <= 1; dy += 1)
          if ((dx == 0 ^ dy == 0) && !visited(x+dx, y+dy) && !blocked(x+dx, y+dy)) {
            dir = dxdyDirection(dx, dy);
            break;
          }
    }

    // The search loop broke, so I left the dead-end hallway. Return my second to last coordinate (the entrance proper)
    return visitedCoords.size() >= 2 ? visitedCoords.get(visitedCoords.size()-2) : null;
  }

  /**
   * Step once in some direction
   *
   * @param dir the direction to step in
   * @see Direction
   */
  private void move(Direction dir) {
    x += (dir == Direction.LEFT || dir == Direction.RIGHT) ? -1+2*(dir == Direction.RIGHT ? 1 : 0) : 0;
    y += (dir == Direction.UP || dir == Direction.DOWN) ? -1+2*(dir == Direction.DOWN ? 1 : 0) : 0;

    visitedCoords.add(new Coordinate(x, y));
  }

  /**
   * @param x the x-coordinate to check
   * @param y the y-coordinate to check
   * @return whether or not the specified coordinates are the maze's start or end
   */
  private boolean startOrEnd(int x, int y) { return ((x == startX && y == startY) || (x == endX && y == endY)); }
}
