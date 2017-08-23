import edu.princeton.cs.algs4.*;

/**
 * Created by lucila on 21/08/17
 */
public class KdTree {

    private int size;

    private static final RectHV UNIT_SQUARE = new RectHV(0, 0, 1, 1);
    private Node root;

    public KdTree() {
        size = 0;
    }                             // construct an empty set of points

    public boolean isEmpty() {
        return root == null;
    }            // is the set empty?

    public int size() {
        return size;
    }                        // number of points in the set

    public void insert(Point2D p) {
        if (p == null) throw new IllegalArgumentException();
        else root = put(root, p, false, UNIT_SQUARE);
    }

    private Node put(Node x, Point2D point, boolean horizontalSplit, RectHV referenceRect) {
        if (x == null) {
            size++;
            return new Node(point);
        }

        if (x.point.equals(point)) {
            return x;
        }
        if (!hasSameKey(x.point, point, horizontalSplit)) {
            RectHV rtRect = rtSplit(referenceRect, x.point, horizontalSplit);
            x.rt = put(x.rt, point, !horizontalSplit, rtRect);
        } else {
            RectHV lbRect = lbSplit(referenceRect, x.point, horizontalSplit);
            if (lbRect.contains(point)) {
                x.lb = put(x.lb, point, !horizontalSplit, lbRect);
            } else {
                RectHV rtRect = rtSplit(referenceRect, x.point, horizontalSplit);
                x.rt = put(x.rt, point, !horizontalSplit, rtRect);
            }
        }

        return x;
    }

    private RectHV lbSplit(RectHV referenceRect, Point2D point, boolean useHorizontal) {
        if (useHorizontal) return horizontalSplitOf(point, referenceRect, true);
        return verticalSplitOf(point, referenceRect, true);
    }

    private RectHV rtSplit(RectHV referenceRect, Point2D point, boolean useHorizontal) {
        if (useHorizontal) return horizontalSplitOf(point, referenceRect, false);
        return verticalSplitOf(point, referenceRect, false);
    }

    private RectHV verticalSplitOf(Point2D point, RectHV lastRect, boolean isLb) {
        if (!isLb) {
            return new RectHV(point.x(), lastRect.ymin(), lastRect.xmax(), lastRect.ymax());
        }

        return new RectHV(lastRect.xmin(), lastRect.ymin(), point.x(), lastRect.ymax());
    }

    private RectHV horizontalSplitOf(Point2D point, RectHV lastRect, boolean isLb) {
        if (!isLb) {
            return new RectHV(lastRect.xmin(), point.y(), lastRect.xmax(), lastRect.ymax());
        }

        return new RectHV(lastRect.xmin(), lastRect.ymin(), lastRect.xmax(), point.y());
    }

    public boolean contains(Point2D queryPoint) {
        if (queryPoint == null) throw new IllegalArgumentException();
        return get(root, queryPoint, false, UNIT_SQUARE) != null;
    }

    private Point2D get(Node node, Point2D queryPoint, boolean horizontalSplit, RectHV referenceRect) {
        if (node == null) return null;
        if (node.point.equals(queryPoint)) return node.point;

        RectHV lbRect = lbSplit(referenceRect, node.point, horizontalSplit);
        if (lbRect.contains(queryPoint) && !hasSameKey(node.point, queryPoint, horizontalSplit)) {
            return get(node.lb, queryPoint, !horizontalSplit, lbRect);
        }

        RectHV rtRect = rtSplit(referenceRect, node.point, horizontalSplit);
        return get(node.rt, queryPoint, !horizontalSplit, rtRect);
    }

    private boolean hasSameKey(Point2D point, Point2D anotherPoint, boolean horizontalSplit) {
        if (horizontalSplit) return point.y() == anotherPoint.y();
        return point.x() == anotherPoint.x();
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
        if (rect == null) throw new IllegalArgumentException();
        Queue<Point2D> result = new Queue<Point2D>();
        enqueueInRange(root, UNIT_SQUARE, rect, false, result);
        return result;
    }            // all points that are inside the rectangle (or on the boundary)

    private void enqueueInRange(Node node, RectHV nodeRect, RectHV queryRect, boolean horizontalSplit, Queue<Point2D> queue) {
        if (node != null && pruningRule(nodeRect, queryRect)) {
            if (queryRect.contains(node.point)) queue.enqueue(node.point);

            RectHV lbRect = lbSplit(nodeRect, node.point, horizontalSplit);

            if (intersectsLine(queryRect, node.point, horizontalSplit)) {
                RectHV rtRect = rtSplit(nodeRect, node.point, horizontalSplit);
                enqueueInRange(node.lb, lbRect, queryRect, !horizontalSplit, queue);
                enqueueInRange(node.rt, rtRect, queryRect, !horizontalSplit, queue);
            } else if (lbRect.intersects(queryRect)) {
                enqueueInRange(node.lb, lbRect, queryRect, !horizontalSplit, queue);
            } else {
                RectHV rtRect = rtSplit(nodeRect, node.point, horizontalSplit);
                enqueueInRange(node.rt, rtRect, queryRect, !horizontalSplit, queue);
            }
        }
    }

    private boolean intersectsLine(RectHV queryRect, Point2D point, boolean horizontalSplit) {
        if (horizontalSplit) return queryRect.ymax() >= point.y() && point.y() >= queryRect.ymin();
        return queryRect.xmax() >= point.x() && point.x() >= queryRect.xmin();
    }

    public Point2D nearest(Point2D queryPoint) {
        if (queryPoint == null) throw new IllegalArgumentException();
        return nearestIn(root, UNIT_SQUARE, queryPoint, false, null);
    }            // a nearest neighbor in the set to point p; null if the set is empty

    private boolean isNearer(Point2D referencePoint, Point2D oldPoint, Point2D newPoint) {
        return referencePoint.distanceSquaredTo(oldPoint) > referencePoint.distanceSquaredTo(newPoint);
    }

    private Point2D nearestIn(Node node, RectHV rectHV, Point2D queryPoint, boolean horizontalSplit, Point2D champion) {
        if (champion != null && queryPoint.distanceSquaredTo(champion) == 0) return champion;
        if (node == null || !pruningRule(rectHV, queryPoint, champion)) return champion;
        if (node.point == queryPoint) return node.point;
        if (champion == null || isNearer(queryPoint, champion, node.point)) {
            champion = node.point;
        }
        RectHV lbRect = lbSplit(rectHV, node.point, horizontalSplit);
        RectHV rtRect = rtSplit(rectHV, node.point, horizontalSplit);
        if (isLB(node.point, queryPoint, horizontalSplit)) {
            champion = nearestIn(node.lb, lbRect, queryPoint, !horizontalSplit, champion);
            champion = nearestIn(node.rt, rtRect, queryPoint, !horizontalSplit, champion);
        } else {
            champion = nearestIn(node.rt, rtRect, queryPoint, !horizontalSplit, champion);
            champion = nearestIn(node.lb, lbRect, queryPoint, !horizontalSplit, champion);
        }

        return champion;
    }

    private boolean pruningRule(RectHV nodeRect, RectHV queryRect) {
        return queryRect.intersects(nodeRect);
    }

    private boolean pruningRule(RectHV nodeRect, Point2D queryPoint, Point2D champion) {
        return champion == null || nodeRect.distanceSquaredTo(queryPoint) < queryPoint.distanceSquaredTo(champion);
    }

    private boolean isLB(Point2D nodePoint, Point2D queryPoint, boolean horizontalSplit) {
        if (horizontalSplit) return queryPoint.y() < nodePoint.y();
        return queryPoint.x() < nodePoint.x();
    }

    public static void main(String[] args) {
        In in = new In(args[0]);
        long start = System.currentTimeMillis();
        KdTree kdTree = new KdTree();

        while (!in.isEmpty()) {
            double x = in.readDouble();
            double y = in.readDouble();
            Point2D point = new Point2D(x, y);
            System.out.println(point);
            kdTree.insert(point);
        }


        StdOut.println("busco (0.9, 0.6), debería dar true");
        boolean contains = kdTree.contains(new Point2D(0.9, 0.6));
        StdOut.println(contains);


        StdOut.println("lesto");
    }                 // unit testing of the methods (optional)


    private static class Node {

        private Point2D point;
        private Node lb;
        private Node rt;

        public Node(Point2D point) {
            this.point = point;
        }
    }
}