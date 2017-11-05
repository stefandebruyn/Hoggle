package hoggle;

public final class Movement extends PathElement {
    private Direction direction;

    /**
     * Object encapsulation of a single step taken by Hoggle in some direction.
     *
     * @param dir the Direction of the Movement
     * @see Direction, Hoggle
     */
    public Movement(Direction dir) { direction = dir; }

    /**
     * @return this Movement's Direction
     * @see Direction
     */
    public Direction get() { return direction; }

    /**
     * @param other the Movement to test equivalency to
     * @return whether or not this Movement and the specified other have the same Direction
     */
    public boolean equals(Movement other) { return (direction == other.get()); }

    // String conversion for debug purposes
    @Override
    public String toString() { return direction.toString(); }
}
