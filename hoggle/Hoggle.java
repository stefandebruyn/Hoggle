package hoggle;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.Stack;
import java.util.ArrayList;
import javax.imageio.ImageIO;

public class Hoggle {
  protected ArrayList<Coordinate> visitedCoords = new ArrayList<Coordinate>();
  protected ArrayList<Coordinate> roadblocks = new ArrayList<Coordinate>();
  protected boolean[][] maze;
  protected short[][] heuristic;
  protected int x, y, startX, startY, endX, endY;

  private Stack<PathElement> path = new Stack<PathElement>();
  private Direction direction;
  private BufferedImage map;
  private int moveCount = 0;
  private File solutionDirectory;
  private String name;
  private boolean ASCII_ANIM, PNG_ANIM, PNG_SOLUTION, STEP_BY_STEP, TELEMETRY;
  private final Color WALL_COLOR = Color.WHITE, PATH_COLOR = Color.BLUE, SPACE_COLOR = Color.LIGHT_GRAY, END_COLOR = Color.GREEN;
  private final int DELAY = 0, MAP_SCALE = 6;

  private enum Mode { PNG, ASCII, CUSTOM }
  private Mode runMode = Mode.PNG;

  // Give Hoggle a starting position, an ending position, and a maze
  public Hoggle(boolean[][] maze, Coordinate startPosition, Coordinate endPosition) {
    // Assert argument validity
    for (boolean[] row : maze)
      assert (row.length == maze[0].length);
    assert (startPosition.getX() >= 0 && startPosition.getX() < maze[0].length);
    assert (endPosition.getY() >= 0 && endPosition.getY() < maze.length);

    // State population
    this.maze = maze;
    this.heuristic = new short[maze.length][maze[0].length];
    this.x = this.startX = startPosition.getX();
    this.y = this.startY = startPosition.getY();
    this.endX = endPosition.getX();
    this.endY = endPosition.getY();

    // Run mode specifications
    if (runMode == Mode.PNG) {
      ASCII_ANIM = false;
      PNG_ANIM = true;
      PNG_SOLUTION = true;
      STEP_BY_STEP = false;
      TELEMETRY = true;
    } else if (runMode == Mode.ASCII) {
      ASCII_ANIM = true;
      PNG_ANIM = false;
      PNG_SOLUTION = true;
      STEP_BY_STEP = false;
      TELEMETRY = false;
    } else {
      ASCII_ANIM = true;
      PNG_ANIM = false;
      PNG_SOLUTION = true;
      STEP_BY_STEP = true;
      TELEMETRY = false;
    }
  }

  protected Hoggle(boolean[][] maze) { this.maze = maze; }

  /* * * SOLVING ALGORITHM * * */

  // Prompt Hoggle to solve the maze and return the path taken
  public final ArrayList<Direction> solve() {
    prepareGraphics();
    optimize();

    direction = Direction.UP;
    path.push((PathElement)makeJunction());

    boolean success = false;

    telemln("Solving maze...");

    while (!success) {
      animate();

      if (!move(true)) {
        backtrack();
        continue;
      }

      // Check if I'm in sight of the end
      if ((x == endX || y == endY) && !collisionLine(x, y, endX, endY)) {
        direction = dxdyDirection(endX-x, endY-y);
        while (!(x == endX && y == endY)) {
          move(true);
          animate();
        }
        success = true;
        continue;
      }

      // That step brought me to either a fork or a turn
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

          // If this fork still has unexplored branches, choose one to explore
          if (j.getUnexploredCount() > 0) {
            direction = cheapestJunctionPath(j);
            continue;
          } else backtrack();
        }
      // This is a dead-end; retrace my steps
      } else if (wallCount(x, y) == 3) backtrack();
    }

    // The last loop broke -- the end has been reached. Construct the final path
    telemln("Maze solved.");

    ArrayList<Direction> finalPath = new ArrayList<Direction>();
    ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>();
    BufferedImage img = new BufferedImage(maze[0].length, maze.length, BufferedImage.TYPE_INT_RGB);
    int simX = endX, simY = endY, moveCount = path.size();
    double moves = 0;

    while (!path.isEmpty()) {
      PathElement p = path.pop();

      if (p instanceof Movement) {
        if (PNG_SOLUTION) {
          int red = (int)((moves/moveCount)*255);
          Color col = new Color(255-red, 0, red);
          img.setRGB(simX, simY, col.getRGB());
          coordinates.add(new Coordinate(simX, simY));
        }

        Movement m = (Movement)p;

        switch (oppositeDirection(m.get())) {
          case UP: simY--; break;
          case DOWN: simY++; break;
          case LEFT: simX--; break;
          case RIGHT: simX++; break;
        }

        finalPath.add(0, ((Movement)p).get());
        moves++;
      }
    }

    // Create solution file
    if (PNG_SOLUTION) {
      for (int i = 0; i < maze.length; i++)
        for (int j = 0; j < maze[0].length; j++)
          if (!maze[i][j])
            img.setRGB(j, i, Color.WHITE.getRGB());
          else if (!coordinates.contains(new Coordinate(j, i)))
            img.setRGB(j, i, Color.LIGHT_GRAY.getRGB());

      try {
        BufferedImage scaled = new BufferedImage(img.getWidth()*MAP_SCALE, img.getHeight()*MAP_SCALE, BufferedImage.TYPE_INT_ARGB);
        AffineTransform trans = new AffineTransform();
        trans.scale(MAP_SCALE*1.0, MAP_SCALE*1.0);
        AffineTransformOp scaleOp = new AffineTransformOp(trans, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        scaled = scaleOp.filter(img, scaled);
        File out = new File(solutionDirectory.getAbsolutePath() + "/solution.png");
        ImageIO.write(scaled, "png", out);
      } catch (IOException e) {
        telemln("(!) Failed to create solution image");
      } catch (NullPointerException e) {
        telemln("(!) Failed to create solution image");
      }
    }

    return finalPath;
  }

  // Pathfinding optimization via elimination of dead-end branches & A*-esque node cost heuristics
  private void optimize() {
    telemln("Beginning optimization phase... ");

    for (int y = 0; y < maze.length; y++) {
      for (int x = 0; x < maze[0].length; x++) {
        // Dead-end elimination
        if (!blocked(x, y) && wallCount(x, y) == 3) {
          // This position is a dead-end -- deploy a drone to find and block its entrance
          telemln("Deploying drone to (" + x + ", " + y + ")... ");

          Drone d = new Drone(maze, new Coordinate(x, y), new Coordinate(this.x, this.y), new Coordinate(endX, endY), roadblocks);
          Coordinate entrance = d.backtrack();
          if (entrance != null)
            roadblocks.add(entrance.clone());

          telem("Done.");
        }

        // Cost heuristic calculations
        telemln("Calculating node (" + x + ", " + y + ") heuristic... ");

        short manhattan = (short)(Math.abs(endX-x) + Math.abs(endY-y));
        short absDist = (short)(Math.sqrt(Math.pow(endX-x, 2) + Math.pow(endY-y, 2)));
        heuristic[y][x] = (short)(manhattan+absDist);

        telem("Done.");
      }
    }

    telemln("Optimization finished.");
  }

  // Handle the initialization of solution directories and bitmaps if such was specified
  private void prepareGraphics() {
    // Create solution directory
    if (PNG_ANIM || PNG_SOLUTION) {
      telemln("Creating solution directory... ");

      solutionDirectory = new File((name == null ? this.hashCode() : name) + "-solution");

      if (!solutionDirectory.exists()) {
        try {
          solutionDirectory.mkdir();
        } catch (SecurityException e) {
          System.out.println("(!) A security exception occurred");
          return;
        }
      }

      telem("Done.");
    }

    // Create scratch bitmap for animated solving
    if (PNG_ANIM) {
      telemln("Preparing bitmap... ");

      map = new BufferedImage(maze[0].length, maze.length, BufferedImage.TYPE_INT_RGB);

      for (int i = 0; i < maze.length; i++)
        for (int j = 0; j < maze[0].length; j++)
          if (!maze[i][j])
            map.setRGB(j, i, WALL_COLOR.getRGB());
          else
            map.setRGB(j, i, SPACE_COLOR.getRGB());

      map.setRGB(x, y, PATH_COLOR.getRGB());
      map.setRGB(endX, endY, END_COLOR.getRGB());
      saveMapPng();
    }
  }

  /* * * MAZE NAVIGATION UTIL SHARED WITH DRONE * * */

  // Return the number of walls adjacent to some position
  protected final int wallCount(int x, int y) {
    int sum = 0;
    for (int dx = -1; dx <= 1; dx++)
      for (int dy = -1; dy <= 1; dy++)
        if ((dx == 0 ^ dy == 0) && blocked(x+dx, y+dy))
          sum++;
    return sum;
  }

  // Return whether or not there is a wall one position in some direction
  protected final boolean collision(Direction dir) {
    int cx = x + ((dir == Direction.LEFT || dir == Direction.RIGHT) ? -1+2*(dir == Direction.RIGHT ? 1 : 0) : 0);
    int cy = y + ((dir == Direction.UP || dir == Direction.DOWN) ? -1+2*(dir == Direction.DOWN ? 1 : 0) : 0);
    return blocked(cx, cy);
  }

  // Return whether or not a wall lies on a cardinal line between two points
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

  // Return whether or not a coordinate is within the maze's bounds
  protected final boolean inBounds(int x, int y) { return (x >= 0 && x < maze[0].length && y >= 0 && y < maze.length); }

  // Get the direction associated with some dx sign and dy sign -- does NOT support diagonal motion
  protected final Direction dxdyDirection(int dx, int dy) {
    assert (dx == 0 || dy == 0);
    assert !(dx == 0 && dy == 0);

    if (dx != 0)
      return dx < 0 ? Direction.LEFT : Direction.RIGHT;
    return dy < 0 ? Direction.UP : Direction.DOWN;
  }

  // Get the coordinate one space in some cardinal direction
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

  // Return whether or not I've visited some coordinate
  protected final boolean visited(int x, int y) {
    for (Coordinate c : visitedCoords)
      if (c.getX() == x && c.getY() == y)
        return true;
    return false;
  }

  // Get the direction opposite some other direction
  protected final Direction oppositeDirection(Direction dir) {
    switch (dir) {
      case UP: return Direction.DOWN;
      case DOWN: return Direction.UP;
      case LEFT: return Direction.RIGHT;
      case RIGHT: return Direction.LEFT;
      default: return null;
    }
  }

  /* * * PRIVATE UTIL * * */

  // Step in the current direction and return whether or not I've visited this spot before
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

    if (PNG_ANIM) {
      moveCount++;
      map.setRGB(oldX, oldY, SPACE_COLOR.getRGB());
      map.setRGB(x, y, PATH_COLOR.getRGB());
      saveMapPng();
    }

    return pioneer;
  }

  // Pull movements off the path stack until the previous junction is reached
  private void backtrack() {
    while (path.peek() instanceof Movement) {
      direction = oppositeDirection(((Movement)path.pop()).get());
      move(false);
    }

    // Arriving at the old junction, determine if it has unexplored branches, or if it doesn't and I need to backtrack again
    Junction j = (Junction)path.peek();
    j.remove(oppositeDirection(direction));

    if (j.getUnexploredCount() > 0)
      direction = j.getUnexplored(0);
    else {
      path.pop();
      backtrack();
    }
  }

  // Return a junction object containing branch data from the current coordintes
  private Junction makeJunction() {
    ArrayList<Direction> branches = new ArrayList<Direction>();

    for (int dx = -1; dx <= 1; dx++)
      for (int dy = -1; dy <= 1; dy++)
        if ((dx == 0 ^ dy == 0) && inBounds(x+dx, y+dy) && oppositeDirection(direction) != dxdyDirection(dx, dy) && !blocked(x+dx, y+dy))
          branches.add(dxdyDirection(dx, dy));

    if (branches.size() == 1) branches.add(oppositeDirection(direction));

    return new Junction(branches);
  }

  // Returns whether or not some coordinate is a roadblock
  private boolean isRoadblock(int x, int y) {
    for (Coordinate c : roadblocks)
      if (c.getX() == x && c.getY() == y)
        return true;

    return false;
  }

  // Use the pre-calculated heuristics to calculate the cheapest path of a junction
  private Direction cheapestJunctionPath(Junction j) {
    Direction[] options = j.getAllUnexplored();
    Direction recordHolder = j.getUnexplored(0);
    short record = Short.MAX_VALUE;

    for (Direction d : options) {
      Coordinate c = dirCoordinate(d);
      if (heuristic[c.getY()][c.getX()] < record) {
        record = heuristic[c.getY()][c.getX()];
        recordHolder = d;
      }
    }

    return recordHolder;
  }

  // Print the maze, add a gutter, and delay; used to animate the solving process
  private void animate() {
    if (!ASCII_ANIM)
      return;

    print();
    System.out.print("\n");

    if (STEP_BY_STEP) {
      Scanner pause = new Scanner(System.in);
      pause.nextLine();
    } else
      try { Thread.sleep(DELAY); } catch (InterruptedException e) { e.printStackTrace(); }
  }

  // Return whether or not some position is a wall or a roadblock
  protected boolean blocked(int x, int y) { return (!inBounds(x, y) || !maze[y][x] || isRoadblock(x, y)); }

  // Print telemetry for debug purposes
  private void telemln(String str) { if (TELEMETRY) System.out.print("\n" + str); }

  private void telem(String str) { if (TELEMETRY) System.out.print(str); }

  // Save a picture of the current maze state if png animation was specified
  private void saveMapPng() {
    if (!PNG_ANIM)
      return;

    telemln("Generating bitmap for step " + moveCount + "... ");

    try {
      BufferedImage scaled = new BufferedImage(map.getWidth()*MAP_SCALE, map.getHeight()*MAP_SCALE, BufferedImage.TYPE_INT_ARGB);
      AffineTransform trans = new AffineTransform();
      trans.scale(MAP_SCALE*1.0, MAP_SCALE*1.0);
      AffineTransformOp scaleOp = new AffineTransformOp(trans, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
      scaled = scaleOp.filter(map, scaled);
      File out = new File(solutionDirectory.getAbsolutePath() + "/step" + moveCount + ".png");
      ImageIO.write(scaled, "png", out);
    } catch (IOException e) {
      telemln("(!) Failed to save map png");
    } catch (NullPointerException e) {
      telemln("(!) Failed to save map png");
    }

    telem("Done.");
  }

  // Specify a name -- used to name solution files. If no name is specified, Hoggle's hash is used
  public void setName(String str) { name = str; }

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

    System.out.print(str);
  }
}
