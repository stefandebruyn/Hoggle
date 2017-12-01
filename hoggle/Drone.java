package hoggle;

import java.util.ArrayList;

public final class Drone extends Hoggle {
  private int startX, startY;

  // Create a drone in some maze at some dead-end; maze not cloned for efficiency purposes
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

  // Prompt the drone to backtrack from the dead-end until its entrance is reached, and return that entrance's position
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
      // Move in the current direction
      boolean entranceReached = false;

      while (!collision(dir)) {
        move(dir);

        // Check to make sure I'm not accidentally about to roadblock the maze's start or end
        if (startOrEnd(x, y))
          return null;

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

      // The last loop broke, so a wall was hit -- find what direction I should turn in to keep going
      for (int dx = -1; dx <= 1; dx += 1)
        for (int dy = -1; dy <= 1; dy += 1)
          if ((dx == 0 ^ dy == 0) && !visited(x+dx, y+dy) && !blocked(x+dx, y+dy)) {
            dir = dxdyDirection(dx, dy);
            break;
          }
    }

    // Done!
    return visitedCoords.size() >= 2 ? visitedCoords.get(visitedCoords.size()-2) : null;
  }

  // Step once in some direction
  private void move(Direction dir) {
    x += (dir == Direction.LEFT || dir == Direction.RIGHT) ? -1+2*(dir == Direction.RIGHT ? 1 : 0) : 0;
    y += (dir == Direction.UP || dir == Direction.DOWN) ? -1+2*(dir == Direction.DOWN ? 1 : 0) : 0;

    visitedCoords.add(new Coordinate(x, y));
  }

  // Return if a position is the maze's start or end
  private boolean startOrEnd(int x, int y) { return ((x == startX && y == startY) || (x == endX && y == endY)); }
}
