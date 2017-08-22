import edu.princeton.cs.algs4.*;

import java.util.Comparator;

/**
 * Created by lucila on 21/08/17.
 */
public class KdTree {

    private static final Comparator<Point2D> COMPARATOR_HORIZONTAL_SPLIT = horizontalSplit();
    private static final Comparator<Point2D> COMPARATOR_VERTICAL_SPLIT = verticalSplit();

    private Node root;

    public KdTree() {
    }                             // construct an empty set of points

    public boolean isEmpty() {  
        return root == null;
    }            // is the set empty?

    public int size() {
        return root == null ? 0 : root.size;
    }                        // number of points in the set

    public void insert(Point2D p) {
        if (p == null) throw new IllegalArgumentException();
        root = put(root, p, false);
    }

    private Node put(Node x, Point2D point, boolean useHorizontal) {
        if (x == null) return new Node(point);
        Comparator<Point2D> comparator = comparator(useHorizontal);
        if (x.point.equals(point)) return x;
        int cmp = comparator.compare(point, x.point);
        if (cmp < 0) x.lb = put(x.lb, point, !useHorizontal);
        else if (cmp > 0) x.rt = put(x.rt, point, !useHorizontal);
        else {
            //look the other coordinate.
            cmp = comparator(!useHorizontal).compare(point, x.point);
            if (cmp > 0) x.rt = put(x.rt, point, !useHorizontal);
            else x.lb = put(x.lb, point, !useHorizontal);
        }
        resizeRectangle(x);
        x.size = 1 + (x.lb == null ? 0 : size(x.lb)) + (x.rt == null ? 0 : size(x.rt));
        return x;
    }

    private void resizeRectangle(Node x) {
        double xMin = x.rect.xmin();
        double yMin = x.rect.ymin();
        double xMax = x.rect.xmax();
        double maxY = x.rect.ymax();

        if (x.lb != null) {
            xMin = min(xMin, x.lb.rect.xmin());
            yMin = min(yMin, x.lb.rect.ymin());
            xMax = max(xMax, x.lb.rect.xmax());
            maxY = max(maxY, x.lb.rect.ymax());
        }
        if (x.rt != null) {
            xMin = min(xMin, x.rt.rect.xmin());
            yMin = min(yMin, x.rt.rect.ymin());
            xMax = max(xMax, x.rt.rect.xmax());
            maxY = max(maxY, x.rt.rect.ymax());
        }
        x.rect = new RectHV(xMin, yMin, xMax, maxY);
    }

    private double max(double a, double b) {
        return a > b ? a : b;
    }

    private double min(double a, double b) {
        return a > b ? b : a;
    }

    private int size(Node x) {
        return 1 + (x.lb == null ? 0 : size(x.lb)) + (x.rt == null ? 0 : size(x.rt));
    }

    private Comparator<Point2D> comparator(boolean horizontalSplit) {
        if (horizontalSplit) return COMPARATOR_HORIZONTAL_SPLIT;
        return COMPARATOR_VERTICAL_SPLIT;
    }

    private static Comparator<Point2D> horizontalSplit() {
        return new Comparator<Point2D>() {
            @Override
            public int compare(Point2D o1, Point2D o2) {
                return Double.valueOf(o1.y()).compareTo(Double.valueOf(o2.y()));
            }
        };
    }

    private static Comparator<Point2D> verticalSplit() {
        return new Comparator<Point2D>() {
            @Override
            public int compare(Point2D o1, Point2D o2) {
                return Double.valueOf(o1.x()).compareTo(Double.valueOf(o2.x()));
            }
        };
    }

    public boolean contains(Point2D p) {
        return get(root, p, false) != null;
    }

    private Point2D get(Node node, Point2D point, boolean horizontalSplit) {
        if (point == null) throw new IllegalArgumentException();
        if (node == null) return null;
        Comparator<Point2D> comparator = comparator(horizontalSplit);
        int cmp = comparator.compare(point, node.point);
        if (cmp < 0) return get(node.lb, point, !horizontalSplit);
        else if (cmp > 0) return get(node.rt, point, !horizontalSplit);
        else return node.point;
    }

    private Iterable<Point2D> levelOrder() {
        Queue<Point2D> keys = new Queue<Point2D>();
        Queue<Node> queue = new Queue<Node>();
        queue.enqueue(root);
        while (!queue.isEmpty()) {
            Node x = queue.dequeue();
            if (x == null) continue;
            keys.enqueue(x.point);
            queue.enqueue(x.lb);
            queue.enqueue(x.rt);
        }
        return keys;
    }

    public void draw() {
        for (Point2D point : levelOrder()) {
            StdDraw.point(point.x(), point.y());
        }
    }                        // draw all points to standard draw

    public Iterable<Point2D> range(RectHV rect) {
        Queue<Point2D> result = new Queue<Point2D>();
        enqueueInRange(root, rect, false, result);
        return result;
    }            // all points that are inside the rectangle (or on the boundary)

    private void enqueueInRange(Node node, RectHV rect, boolean horizontalSplit, Queue<Point2D> queue) {
        if (root != null) {
            if (rect.contains(node.point)) queue.enqueue(node.point);
            if (node.lb != null && !rect.intersects(node.lb.rect)) {
                enqueueInRange(node.lb, rect, !horizontalSplit, queue);
            }

            if (node.rt != null && !rect.intersects(node.rt.rect)) {
                enqueueInRange(node.rt, rect, !horizontalSplit, queue);
            }
        }
    }

    public Point2D nearest(Point2D queryPoint) {
        if (queryPoint == null) throw new IllegalArgumentException();
        return nearestIn(root, queryPoint, false, null);
    }            // a nearest neighbor in the set to point p; null if the set is empty

    private boolean isNearer(Point2D referencePoint, Point2D oldPoint, Point2D newPoint) {
        return referencePoint.distanceSquaredTo(oldPoint) > referencePoint.distanceSquaredTo(newPoint);
    }

    private Point2D nearestIn(Node node, Point2D queryPoint, boolean horizontalSplit, Point2D champion) {
        if (champion != null && queryPoint.distanceSquaredTo(champion) == 0) return champion;
        if (node == null || !pruningRule(node, queryPoint, champion)) return champion;
        if (node.point == queryPoint) return node.point;
        if (champion == null || isNearer(queryPoint, champion, node.point)) {
            champion = node.point;
        }

        if (isLB(node.point, queryPoint, horizontalSplit)) {
            champion = nearestIn(node.lb, queryPoint, !horizontalSplit, champion);
            champion = nearestIn(node.rt, queryPoint, !horizontalSplit, champion);
        } else {
            champion = nearestIn(node.rt, queryPoint, !horizontalSplit, champion);
            champion = nearestIn(node.lb, queryPoint, !horizontalSplit, champion);
        }

        return champion;
    }

    private boolean pruningRule(Node node, Point2D queryPoint, Point2D champion) {
        return node.rect.distanceSquaredTo(queryPoint) < queryPoint.distanceSquaredTo(champion);
    }

    private boolean isLB(Point2D nodePoint, Point2D queryPoint, boolean horizontalSplit) {
        Comparator<Point2D> comparator = comparator(horizontalSplit);
        return comparator.compare(queryPoint, nodePoint) < 0;
    }

    public static void main(String[] args) {
        In in = new In(args[0]);
        KdTree kdTree = new KdTree();
        while (!in.isEmpty()) {
            double x = in.readDouble();
            double y = in.readDouble();
            kdTree.insert(new Point2D(x, y));
            StdOut.println("inserto " + x + " " + y);
            StdOut.println(kdTree.size());
        }

        StdOut.println("lesto");
    }                 // unit testing of the methods (optional)


    private static class Node {

        private Point2D point;
        private Node lb;
        private Node rt;
        private int size;
        private RectHV rect;

        public Node(Point2D point) {
            this.point = point;
            this.size = 1;
            this.rect = new RectHV(point.x(), point.y(), point.x(), point.y());
        }
    }
}