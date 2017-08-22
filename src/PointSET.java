import edu.princeton.cs.algs4.*;

import java.util.TreeSet;

/**
 * Created by lucila on 21/08/17.
 */
public class PointSET {

    private final TreeSet<Point2D> treeSet;

    public PointSET() {
        treeSet = new TreeSet<Point2D>();
    }                               // construct an empty set of points

    public boolean isEmpty() {
        return treeSet.isEmpty();
    }                      // is the set empty?

    public int size() {
        return treeSet.size();
    }                        // number of points in the set

    public void insert(Point2D p) {
        if (!treeSet.contains(p)) treeSet.add(p);
    }             // add the point to the set (if it is not already in the set)

    public boolean contains(Point2D p) {
        return treeSet.contains(p);
    }           // does the set contain point p?

    public void draw() {
        for (Point2D point : treeSet) {
            StdDraw.point(point.x(), point.y());
        }
    }                        // draw all points to standard draw

    public Iterable<Point2D> range(RectHV rect) {
        TreeSet<Point2D> result = new TreeSet<>();
        for (Point2D point : treeSet) {
            if (rect.contains(point)) {
                result.add(point);
            }
        }
        return result;
    }            // all points that are inside the rectangle (or on the boundary)

    public Point2D nearest(Point2D p) {
        if (isEmpty()) return null;
        Double bestDistance = null;
        Point2D nearest = null;
        for (Point2D point : treeSet) {
            if (bestDistance == null || bestDistance > p.distanceSquaredTo(point)) {
                nearest = point;
                bestDistance = p.distanceSquaredTo(point);
            }
        }
        return nearest;
    }            // a nearest neighbor in the set to point p; null if the set is empty

    public static void main(String[] args) {
        In in = new In(args[0]);
        KdTree kdTree = new KdTree();
        while (!in.isEmpty()) {
            double x = in.readDouble();
            double y = in.readDouble();
            kdTree.insert(new Point2D(x, y));
        }

        StdOut.println("lesto");
    }
}