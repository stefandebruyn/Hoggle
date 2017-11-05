package hoggle;

import java.util.Stack;
import java.util.ArrayList;

public class Hoggle {
  protected ArrayList<Coordinate> visitedCoords = new ArrayList<Coordinate>();
  protected ArrayList<Coordinate> roadblocks = new ArrayList<Coordinate>();
  protected boolean[][] maze;
  protected int[][] heuristic;
  protected int x, y, endX, endY;

  private Stack<PathElement> path = new Stack<PathElement>();
  private Direction direction;
  private final int DELAY = 100;

  /**
   * An A*-inspired 2D maze solver.
   *
   * @param maze a non-jagged 2D array of booleans representing which nodes may be passed and which ones may not
   * @param startPosition the coordinates to begin solving the maze from
   * @param endPosition the destination coordinates
   * @see Coordinate
   *
   */
  public Hoggle(boolean[][] maze, Coordinate startPosition, Coordinate endPosition) {
    for (boolean[] row : maze)
      assert (row.length == maze[0].length);
    assert (startPosition.getX() >= 0 && startPosition.getX() < maze[0].length);
    assert (endPosition.getY() >= 0 && endPosition.getY() < maze.length);

    this.maze = maze;
    this.heuristic = new int[maze.length][maze[0].length];
    this.x = startPosition.getX();
    this.y = startPosition.getY();
    this.endX = endPosition.getX();
    this.endY = endPosition.getY();
  }

  protected Hoggle(boolean[][] maze) { this.maze = maze; }

  /* * * SOLVING ALGORITHM * * */

  /**
   * Prompt Hoggle to solve the maze and return the path taken
   *
   * @return a list of the steps taken to solve the maze
   * @see Direction
   */
  public final ArrayList<Direction> solve() {
    // Begin by optimizing the maze
    optimize();

    // Use the adjacent positions' heuristics to determine which direction I should start in
    direction = Direction.UP;
    path.push((PathElement)makeJunction());

    boolean success = false;

    // While the end has not been reached
    while (!success) {
      animate();

      // Move once in the current direction. If it brings me somewhere I've visited previously, backtrack and skip this iteration
      if (!move(true)) {
        backtrack();
        continue;
      }

      // Check if I'm in sight of the end. If so, move straight towards it and conclude solving
      if ((x == endX || y == endY) && !collisionLine(x, y, endX, endY)) {
        direction = dxdyDirection(endX-x, endY-y);

        while (!(x == endX && y == endY)) {
          move(true);
          animate();
        }

        success = true;
        continue;
      }

      // The step I just took brought me to either a fork or a turn
      if (wallCount(x, y) < 2 || (wallCount(x, y) == 2 && collision(direction))) {
        // This is a fork, albeit one I've seen before
        if (path.peek() instanceof Junction) {
          Junction j = (Junction)path.peek();

          // If this fork still has unexplored branches, use pre-calculated heuristics to choose the cheapest one to explore
          if (j.getUnexploredCount() > 0) {
            direction = cheapestJunctionPath(j);
            continue;
          } else {
            path.pop();
            backtrack();
          }

          continue;
        // This is not a fork I've seen before; document it in my path
        } else {
          Junction j = makeJunction();
          path.push(j);

          // If this fork still has unexplored branches, use pre-calculated heuristics to choose the cheapest one to explore
          if (j.getUnexploredCount() > 0) {
            direction = cheapestJunctionPath(j);
            continue;
          } else backtrack();
        }
      // This is a dead-end; retrace my steps
      } else if (wallCount(x, y) == 3) backtrack();
    }

    // The last loop broke -- the end has been reached. Construct and return the final path
    ArrayList<Direction> finalPath = new ArrayList<Direction>();

    while (!path.isEmpty()) {
      PathElement p = path.pop();
      if (p instanceof Movement)
        finalPath.add(0, ((Movement)p).get());
    }

    return finalPath;
  }

  /**
   * Pathfinding optimization via elimination of dead-end branches and A*-inspired node cost heuristics. Like in classic A*, heuristics
   * are calculated by summing a node's Manhattan form and absolute distance from the end. Though not ideal for solving mazes, these
   * heuristics are better than nothing and, in most cases, do quicken the time to solve.
   *
   * @see Drone
   */
  private void optimize() {
    for (int y = 0; y < maze.length; y++) {
      for (int x = 0; x < maze[0].length; x++) {
        // Dead-end elimination
        if (!blocked(x, y) && wallCount(x, y) == 3) {
          // This position is a dead-end -- deploy a drone to find and block its entrance
          Drone d = new Drone(maze, new Coordinate(x, y), new Coordinate(this.x, this.y), new Coordinate(endX, endY), roadblocks);
          Coordinate entrance = d.backtrack();

          if (entrance != null)
            roadblocks.add(entrance.clone());
        }

        // Cost heuristic calculations
        int manhattan = (int)(Math.abs(endX-x) + Math.abs(endY-y));
        int absDist = (int)(Math.sqrt(Math.pow(endX-x, 2) + Math.pow(endY-y, 2)));
        heuristic[y][x] = manhattan+absDist;
      }
    }
  }

  /* * * MAZE NAVIGATION UTIL SHARED WITH DRONE * * */

  /**
   * @param x the x-coordinate to check
   * @param y the y-coordinate to check
   * @return the number of impassable nodes adjacent to the specified position
   */
  protected final int wallCount(int x, int y) {
    int sum = 0;
    for (int dx = -1; dx <= 1; dx++)
      for (int dy = -1; dy <= 1; dy++)
        if ((dx == 0 ^ dy == 0) && blocked(x+dx, y+dy))
          sum++;
    return sum;
  }

  /**
   * @param dir the direction to check for a collision in
   * @return whether or not the node one position in the specified direction is impassable
   * @see Direction
   */
  protected final boolean collision(Direction dir) {
    int cx = x + ((dir == Direction.LEFT || dir == Direction.RIGHT) ? -1+2*(dir == Direction.RIGHT ? 1 : 0) : 0);
    int cy = y + ((dir == Direction.UP || dir == Direction.DOWN) ? -1+2*(dir == Direction.DOWN ? 1 : 0) : 0);
    return blocked(cx, cy);
  }

  /**
   * @param x1 the x-coordinate of the line's tail
   * @param y2 the y-coordinate of the line's tail
   * @param x2 the x-coordinate of the line's head
   * @param y2 the y-coordinate of the line's head
   * @return whether or not an impassable node lies on an orthagonal line between the two specified points
   */
  protected final boolean collisionLine(int x1, int y1, int x2, int y2) {
    assert ((x1 == x2) ^ (y1 == y2));

    Direction dir = dxdyDirection(x2-x1, y2-y1);

    for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
      if (inBounds(x, y1) && blocked(x, y1))
        return true;

    for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
      if (inBounds(x1, y) && blocked(x1, y))
        return true;

    return false;
  }

  /**
   * @param x the x-coordinate to check
   * @param y the y-coordinate to check
   * @return whether or not the specified coordinates are inside of the maze's bounds
   */
  protected final boolean inBounds(int x, int y) { return (x >= 0 && x < maze[0].length && y >= 0 && y < maze.length); }

  /**
   * @param dx change in x
   * @param dy change in y
   * @return the direction associated with some dx and dy sign (does not support diagonal motion)
   * @see Direction
   */
  protected final Direction dxdyDirection(int dx, int dy) {
    assert (dx == 0 ^ dy == 0);

    if (dx != 0)
      return dx < 0 ? Direction.LEFT : Direction.RIGHT;

    return dy < 0 ? Direction.UP : Direction.DOWN;
  }

  /**
   * @param dir the direction to check in
   * @return the coordinate one step in the specified direction
   * @see Coordinate
   */
  protected final Coordinate dirCoordinate(Direction dir) {
    int dx = 0, dy = 0;

    switch (dir) {
      case UP: dy = -1; break;
      case DOWN: dy = 1; break;
      case LEFT: dx = -1; break;
      case RIGHT: dx = 1; break;
    }

    return new Coordinate(x+dx, y+dy);
  }

  /**
   * @param x the x-coordinate to check
   * @param y the y-coordinate to check
   * @return whether or not I've previously visited the specified coordinates
   */
  protected final boolean visited(int x, int y) {
    for (Coordinate c : visitedCoords)
      if (c.getX() == x && c.getY() == y)
        return true;
    return false;
  }

  /**
   * @param dir the direction to be reversed
   * @return the direction opposite of the one specified
   * @see Direction
   */
  protected final Direction oppositeDirection(Direction dir) {
    switch (dir) {
      case UP: return Direction.DOWN;
      case DOWN: return Direction.UP;
      case LEFT: return Direction.RIGHT;
      case RIGHT: return Direction.LEFT;
      default: return null;
    }
  }

  /**
   * @param x the x-coordinate to check
   * @param y the y-coordinate to check
   * @return whether or not the specified coordinates are impassable
   */
  protected boolean blocked(int x, int y) { return (!inBounds(x, y) || !maze[y][x] || isRoadblock(x, y)); }

  /* * * PRIVATE UTIL * * */

  /**
   * @param addToPath whether or not to document this movement in my solution path
   * @return whether or not I've visited my new coordinates
   */
  private boolean move(boolean addToPath) {
    int oldX = x, oldY = y;
    boolean pioneer = true;

    if (!collision(direction)) {
      switch (direction) {
        case UP: y -= 1; break;
        case DOWN: y += 1; break;
        case LEFT: x -= 1; break;
        case RIGHT: x += 1; break;
      }
    }

    if (visited(x, y) && addToPath) pioneer = false;
    if (pioneer) visitedCoords.add(new Coordinate(x, y));
    if (addToPath && !(oldX == x && oldY == y)) path.push(new Movement(direction));

    return pioneer;
  }

  // Pull movements off the path stack and perform their opposites until the last Junction is reached
  private void backtrack() {
    while (path.peek() instanceof Movement) {
      direction = oppositeDirection(((Movement)path.pop()).get());
      move(false);
    }

    // Arriving at the old Junction, determine if it has unexplored branches, or if it doesn't and I need to backtrack again
    Junction j = (Junction)path.peek();
    j.remove(oppositeDirection(direction));

    if (j.getUnexploredCount() > 0)
      direction = j.getUnexplored(0);
    else {
      path.pop();
      backtrack();
    }
  }

  /**
   * @return a new Junction object containing information about my current position
   * @see Junction
   */
  private Junction makeJunction() {
    ArrayList<Direction> branches = new ArrayList<Direction>();

    for (int dx = -1; dx <= 1; dx++)
      for (int dy = -1; dy <= 1; dy++)
        if ((dx == 0 ^ dy == 0) && inBounds(x+dx, y+dy) && oppositeDirection(direction) != dxdyDirection(dx, dy) && !blocked(x+dx, y+dy))
          branches.add(dxdyDirection(dx, dy));

    if (branches.size() == 1) branches.add(oppositeDirection(direction));

    return new Junction(branches);
  }

  /**
   * @param x the x-coordinate to check
   * @param y the y-coordinate to check
   * @return whether or not the specified coordinates are specifically a roadblock (not a wall)
   */
  private boolean isRoadblock(int x, int y) {
    for (Coordinate c : roadblocks)
      if (c.getX() == x && c.getY() == y)
        return true;

    return false;
  }

  /**
   * Use the pre-calculated heuristics to determine the cheapest branch of a Junction
   *
   * @param j the Junction to analyze
   * @return the direction of the estimated cheapest path to the maze's end
   * @see Direction, Junction
   */
  private Direction cheapestJunctionPath(Junction j) {
    Direction[] options = j.getAllUnexplored();
    Direction recordHolder = j.getUnexplored(0);
    int record = Integer.MAX_VALUE;

    for (Direction d : options) {
      Coordinate c = dirCoordinate(d);
      if (heuristic[c.getY()][c.getX()] < record) {
        record = heuristic[c.getY()][c.getX()];
        recordHolder = d;
      }
    }

    return recordHolder;
  }

  // Print the maze, add a gutter, and sleep for a moment. Used to animate the solving process
  private void animate() {
    print();
    try { Thread.sleep(DELAY); } catch (InterruptedException e) { e.printStackTrace(); }
  }

  // Print the maze for debug purposes
  private void print() {
    String str = "";

    for (int y = 0; y < maze.length; y++) {
      for (int x = 0; x < maze[0].length; x++) {
        char c = ' ';

        if (!maze[y][x])
          c = 'X';
        else if (x == this.x && y == this.y)
          c = 'H';
        else if (x == endX && y == endY)
          c = 'E';
        else if (isRoadblock(x, y))
          c = 'R';

        str += c;
      }

      str += "\n";
    }

    System.out.print(str + "\n");
  }
}
