import edu.princeton.cs.algs4.*;

import java.util.Comparator;

/**
 * Created by lucila on 21/08/17
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
        root = put(root, p, false, 0, null);
    }

    private Node put(Node x, Point2D point, boolean useHorizontal, int lastCmp, Node father) {
        if (x == null)
            return new Node(point, createRectangle(father, !useHorizontal, lastCmp));
        Comparator<Point2D> comparator = comparator(useHorizontal);
        if (x.point.equals(point)) return x;
        int cmp = comparator.compare(point, x.point);
        if (cmp < 0) x.lb = put(x.lb, point, !useHorizontal, cmp, x);
        else if (cmp > 0) x.rt = put(x.rt, point, !useHorizontal, cmp, x);

        x.size = 1 + size(x.lb) + size(x.rt);
        return x;
    }

    private RectHV createRectangle(Node father, boolean horizontalSplit, int lastCmp) {
        if (father == null) return new RectHV(0, 0, 1, 1);
        if (horizontalSplit)
            return horizontalSplitOf(father.point, father.rect, lastCmp);
        return verticalSplitOf(father.point, father.rect, lastCmp);
    }

    private RectHV verticalSplitOf(Point2D point, RectHV lastRect, int lastCmp) {
        if (lastCmp > 0) {
            return new RectHV(point.x(), lastRect.ymin(), lastRect.xmax(), lastRect.ymax());
        }

        return new RectHV(lastRect.xmin(), lastRect.ymin(), point.x(), lastRect.ymax());
    }

    private RectHV horizontalSplitOf(Point2D point, RectHV lastRect, int lastCmp) {
        if (lastCmp > 0) {
            return new RectHV(lastRect.xmin(), point.y(), lastRect.xmax(), lastRect.ymax());
        }

        return new RectHV(lastRect.xmin(), lastRect.ymin(), lastRect.xmax(), point.y());
    }

    private int size(Node x) {
        if (x == null) return 0;
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
                return Double.compare(o1.y(), o2.y());
            }
        };
    }

    private static Comparator<Point2D> verticalSplit() {
        return new Comparator<Point2D>() {
            @Override
            public int compare(Point2D o1, Point2D o2) {
                return Double.compare(o1.x(), o2.x());
            }
        };
    }

    public boolean contains(Point2D p) {
        return get(root, p, false) != null;
    }

    private Point2D get(Node node, Point2D point, boolean horizontalSplit) {
        if (point == null) throw new IllegalArgumentException();
        if (node == null) return null;
        if (node.point.equals(point)) return node.point;
        Comparator<Point2D> comparator = comparator(horizontalSplit);
        int cmp = comparator.compare(point, node.point);
        if (cmp == 0) {
            Point2D lb = get(node.lb, point, !horizontalSplit);
            Point2D rt = get(node.rt, point, !horizontalSplit);
            return lb != null ? lb : rt;
        }
        if (cmp < 0) return get(node.lb, point, !horizontalSplit);
        else return get(node.rt, point, !horizontalSplit);
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

    private void enqueueInRange(Node node, RectHV queryRect, boolean horizontalSplit, Queue<Point2D> queue) {
        if (node != null && pruningRule(node, queryRect)) {
            if (queryRect.contains(node.point)) queue.enqueue(node.point);
            if (node.lb != null) {
                enqueueInRange(node.lb, queryRect, !horizontalSplit, queue);
            }
            if (node.rt != null) {
                enqueueInRange(node.rt, queryRect, !horizontalSplit, queue);
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

    private boolean pruningRule(Node node, RectHV queryRect) {
        return queryRect.intersects(node.rect);
    }

    private boolean pruningRule(Node node, Point2D queryPoint, Point2D champion) {
        return node.rect.distanceSquaredTo(queryPoint) < queryPoint.distanceSquaredTo(champion);
    }

    private boolean isLB(Point2D nodePoint, Point2D queryPoint, boolean horizontalSplit) {
        Comparator<Point2D> comparator = comparator(horizontalSplit);
        return comparator.compare(queryPoint, nodePoint) < 0;
    }

    private static boolean isDegenerate(Point2D point, Point2D[] applied, int amount) {
        for (int i = 0; i < amount; i++) {
            Point2D appliedPoint = applied[i];
            if (appliedPoint.x() == point.x() || appliedPoint.y() == point.y()) return true;
        }
        return false;
    }

    public static void main(String[] args) {
        In in = new In(args[0]);
        KdTree kdTree = new KdTree();

        //10 random non-degenerate points in a 16-by-16 grid

        int gridAmount = 1024;
        int amountToInsert = 100000;

        Point2D[] applied = new Point2D[amountToInsert];
        int amountInserted = 0;

        double step = 1.0 / gridAmount;
        while (amountInserted < amountToInsert) {
            int a = StdRandom.uniform(gridAmount);
            int b = StdRandom.uniform(gridAmount);
            double x = a * step;
            double y = b * step;
            Point2D point = new Point2D(x, y);
            if (!isDegenerate(point, applied, amountInserted)) {
                StdOut.println("Trying to insert: " + point);
                kdTree.insert(new Point2D(x, y));
                applied[amountInserted] = point;
                amountInserted++;
            }
        }

        //StdOut.println("busco (0.11, 0.51), debería dar false");
        //boolean contains = kdTree.contains(new Point2D(0.11, 0.51));
        //StdOut.println(contains);


        StdOut.println("lesto");
    }                 // unit testing of the methods (optional)


    private static class Node {

        private Point2D point;
        private Node lb;
        private Node rt;
        private int size;
        private RectHV rect;

        public Node(Point2D point, RectHV rect) {
            this.point = point;
            this.size = 1;
            this.rect = rect;
        }
    }
}