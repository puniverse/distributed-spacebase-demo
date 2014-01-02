package co.paralleluniverse.spacebase.galaxy;

import co.paralleluniverse.spacebase.AABB;
import static co.paralleluniverse.spacebase.AABB.X;
import static co.paralleluniverse.spacebase.AABB.Y;
import static co.paralleluniverse.spacebase.AABB.Z;
import co.paralleluniverse.spacebase.SpatialToken;
import static java.lang.Math.*;

public class Thing implements java.io.Serializable {
    private final static int MIN_X = 0;
    private final static int MAX_X = 1;
    private final static int MIN_Y = 2;
    private final static int MAX_Y = 3;
    private final static int MIN_Z = 4;
    private final static int MAX_Z = 5;
    final int id;
    double[] location;
    double[] velocity;
    private SpatialToken spatialToken;

    public Thing(AABB aabb) {
        this(-1, aabb);
    }

    public Thing(int id, AABB aabb) {
        this.id = id;
        this.location = new double[aabb.dims() * 2];
        this.velocity = new double[aabb.dims()];
        for (int i = 0; i < aabb.dims(); i++) {
            location[i * 2] = aabb.min(i);
            location[i * 2 + 1] = aabb.max(i);
        }
        floatify(location);
    }

    public Thing(double minX, double maxX, double minY, double maxY) {
        this(-1, minX, maxX, minY, maxY);
    }

    public Thing(int id, double minX, double maxX, double minY, double maxY) {
        this.id = id;
        this.location = new double[4];
        this.velocity = new double[2];
        this.location[MIN_X] = minX;
        this.location[MAX_X] = maxX;
        this.location[MIN_Y] = minY;
        this.location[MAX_Y] = maxY;
        floatify(location);
    }

    public Thing(double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        this(-1, minX, maxX, minY, maxY, minZ, maxZ);
    }

    public Thing(int id, double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        this.id = id;
        this.location = new double[6];
        this.velocity = new double[3];
        this.location[MIN_X] = minX;
        this.location[MAX_X] = maxX;
        this.location[MIN_Y] = minY;
        this.location[MAX_Y] = maxY;
        this.location[MIN_Z] = minZ;
        this.location[MAX_Z] = maxZ;
        floatify(location);
    }

    public void setVelocityDir(double direction, double speed) {
        velocity[X] = speed * cos(direction);
        velocity[Y] = speed * sin(direction);
    }

    public void setVelocity(double vx, double vy) {
        velocity[X] = vx;
        velocity[Y] = vy;
    }

    public void setVelocity(double vx, double vy, double vz) {
        velocity[X] = vx;
        velocity[Y] = vy;
        velocity[Z] = vz;
    }

    public AABB getAABB() {
        return AABB.create(location);
    }

    public void setAABB(AABB aabb) {
        assert location.length == 2 * aabb.dims();
        this.velocity = new double[aabb.dims()];
        for (int i = 0; i < aabb.dims(); i++) {
            location[i * 2] = aabb.min(i);
            location[i * 2 + 1] = aabb.max(i);
        }
    }

    public void move(AABB bounds) {
        move(velocity, bounds);
    }

    public void move(double dx, double dy, AABB bounds) {
        move(new double[]{dx, dy}, bounds);
    }

    public void move(double dx, double dy, double dz, AABB bounds) {
        move(new double[]{dx, dy, dz}, bounds);
    }

    private synchronized void move(double velocity[], AABB bounds) {
        for (int i = 0; i < this.velocity.length; i++) {
            if (bounds != null) {
                if ((location[i * 2 + 1] + velocity[i]) > bounds.max(i) || (location[i * 2] + velocity[i]) < bounds.min(i))
                    velocity[i] = -velocity[i];
            }
            location[i * 2] += velocity[i];
            location[i * 2 + 1] += velocity[i];
        }
        floatify(location);
    }

    public SpatialToken getSpatialToken() {
        return spatialToken;
    }

    public void setSpatialToken(SpatialToken spatialToken) {
        if (this.spatialToken != null)
            throw new RuntimeException("token for " + this + " already set!");
        this.spatialToken = spatialToken;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Thing@");
        sb.append(id > 0 ? id : Integer.toHexString(System.identityHashCode(this)));
        sb.append('(');
        for (int i = 0; i < location.length; i += 2) {
            if (location[i] == location[i + 1])
                sb.append(location[i]);
            else
                sb.append('[').append(location[i]).append(", ").append(location[i + 1]).append(']');
            sb.append('x');
        }
        sb.delete(sb.length() - 1, sb.length());
        sb.append(')');
        return sb.toString();
    }

    private void floatify(double[] array) {
        for (int i = 0; i < array.length; i++)
            array[i] = (float) array[i];
    }
}
