package hoggle;

public class Coordinate {
  private int x, y;

  public Coordinate(int x, int y) { this.x = x; this.y = y; }

  public int getX() { return x; }

  public int getY() { return y; }

  public void setX(int x) { this.x = x; }

  public void setY(int y) { this.y = y; }

  @Override
  public boolean equals(Object other) {
    if (other instanceof Coordinate && ((Coordinate)other).getX() == x && ((Coordinate)other).getY() == y)
      return true;
    return false;
  }

  @Override
  public String toString() { return "(" + x + ", " + y + ")"; }

  @Override
  public Coordinate clone() { return new Coordinate(x, y); }
}
