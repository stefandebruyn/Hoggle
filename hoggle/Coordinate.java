package hoggle;

public final class Coordinate {
  private int x, y;

  public Coordinate(int x, int y) { this.x = x; this.y = y; }

  public int getX() { return x; }

  public int getY() { return y; }

  public void setX(int x) { this.x = x; }

  public void setY(int y) { this.y = y; }

  public boolean equals(Coordinate other) { return (other.getX() == x && other.getY() == y); }

  @Override
  public String toString() { return "(" + x + ", " + y + ")"; }

  @Override
  public Coordinate clone() { return new Coordinate(x, y); }
}
