package hoggle;

public class Movement extends PathElement {
    private Direction direction;

    public Movement(Direction dir) { direction = dir; }

    public Direction get() { return direction; }

    public boolean equals(Movement other) { return (direction == other.get()); }

    @Override
    public String toString() { return direction.toString(); }
}
