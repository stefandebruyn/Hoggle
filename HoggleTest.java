import hoggle.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class HoggleTest {
  public static void main(String[] args) throws IOException {
    // Maze files to be loaded for solving
    String[] mazes = {"test mazes/maze1.txt", "test mazes/maze2.txt", "test mazes/maze3.txt", "test mazes/maze4.txt"};
    Scanner kb = new Scanner(System.in);
    int pos = 0;

    // Solve each maze and print the solution. Press enter to begin solving the next maze
    for (String str : mazes) {
      Hoggle hoggle = createSolver(new File(str));
      ArrayList<Direction> solution = hoggle.solve();

      System.out.print("Maze solved in " + solution.size() + " steps: " + solution + "\n\n" + (pos != mazes.length-1 ? "Press any key to solve next maze..." : "All mazes solved."));

      pos++;
      kb.nextLine();
    }
  }

  // Create a maze from some file, parse for its start & end points, and construct a Hoggle from that data & return it
  private static Hoggle createSolver(File f) throws FileNotFoundException {
    Scanner reader = new Scanner(f);
    int cols = reader.nextInt()*2+1, rows = reader.nextInt()*2+1;
    boolean[][] maze = new boolean[rows][cols];
    int row = 0, startX = -1, startY = -1, endX = -1, endY = -1;

    reader.nextLine();

    while (reader.hasNextLine()) {
      String line = reader.nextLine().trim();

      for (int col = 0; col < line.length(); col++) {
        char c = line.charAt(col);
        maze[row][col] = (c == ' ' || c == '*' || c == 'O') ? true: false;

        if (c == '*') {
          assert (startX == -1 && startY == -1);
          startX = col;
          startY = row;
        } else if (c == 'O') {
          assert (endX == -1 && endY == -1);
          endX = col;
          endY = col;
        }
      }

      row++;
    }

    return new Hoggle(maze, new Coordinate(startX, startY), new Coordinate(endX, endY));
  }
}
