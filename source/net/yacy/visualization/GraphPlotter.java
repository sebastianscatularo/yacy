// GraphPlotter.java
// (C) 2007 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
// first published 22.05.2007 on http://yacy.net
//
// This is a part of YaCy, a peer-to-peer based web search engine
//
// $LastChangedDate$
// $LastChangedRevision$
// $LastChangedBy$
//
// LICENSE
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package net.yacy.visualization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/* this class is a container for graph coordinates and it can draw such coordinates into a graph
 * all coordinates are given in a artificial coordinate system, in the range from
 * -1 to +1. The lower left point of the graph has the coordinate -1, -1 and the upper
 * right is 1,1
 * 0,0 is the center of the graph
 */

public class GraphPlotter implements Cloneable {

    // a ymageGraph is a set of points and borders between the points
    // to reference the points, they must all have a nickname

    private final Map<String, Point> nodes; // the interconnected objects
    private final Set<String> edges; // the links that connect pairs of vertices
    private double leftmost, rightmost, topmost, bottommost;

    public GraphPlotter() {
        this.nodes = new HashMap<String, Point>();
        this.edges = new HashSet<String>();
        this.leftmost = 1.0;
        this.rightmost = -1.0;
        this.topmost = -1.0;
        this.bottommost = 1.0;
    }

    @Override
    public Object clone() {
        GraphPlotter g = new GraphPlotter();
        g.nodes.putAll(this.nodes);
        g.edges.addAll(this.edges);
        g.leftmost = this.leftmost;
        g.rightmost = this.rightmost;
        g.topmost = this.topmost;
        g.bottommost = this.bottommost;
        return g;
    }

    public static class Ribbon {
        double length, attraction, repulsion;
        public Ribbon(double length, double attraction, double repulsion) {
            this.length = length;
            this.attraction = attraction;
            this.repulsion = repulsion;
        }
    }

    public static class Point implements Cloneable {
        public double x, y;
        public int layer;
        public Point(final double x, final double y, final int layer) {
            /*
            assert x >= -1;
            assert x <=  1;
            assert y >= -1;
            assert y <=  1;
            */
            this.x = x;
            this.y = y;
            this.layer = layer;
        }

        @Override
        public Object clone() {
            return new Point(this.x, this.y, this.layer);
        }
    }

    private final static double p2 = Math.PI / 2.0;
    private final static double p23 = p2 * 3.0;

    public static void force(Point calcPoint, Point currentPoint, Point otherPoint, Ribbon r) {
        double dx = otherPoint.x - currentPoint.x;
        double dy = otherPoint.y - currentPoint.y;
        double a = Math.atan(dy / dx); // the angle from this point to the other point
        if (a < 0) a += Math.PI * 2.0; // this makes it easier for the asserts
        double d = Math.sqrt(dx * dx + dy * dy); // the distance of the points
        boolean attraction = d > r.length; // if the distance is greater than the ribbon length, then they attract, otherwise they repulse
        double f = attraction ? r.attraction * (d - r.length) * (d - r.length) : - r.repulsion * (r.length - d) * (r.length - d); // the force
        double x1 = Math.cos(a) * f;
        double y1 = Math.sin(a) * f;
        // verify calculation
        assert !(attraction && a < Math.PI) || y1 >= 0 : "attraction = " + attraction + ", a = " + a + ", y1 = " + y1;
        assert !(!attraction && a < Math.PI) || y1 <= 0 : "attraction = " + attraction + ", a = " + a + ", y1 = " + y1;
        assert !(attraction && a > Math.PI) || y1 <= 0 : "attraction = " + attraction + ", a = " + a + ", y1 = " + y1;
        assert !(!attraction && a > Math.PI) || y1 >= 0 : "attraction = " + attraction + ", a = " + a + ", y1 = " + y1;
        assert !(attraction && (a < p2 || a > p23)) || x1 >= 0  : "attraction = " + attraction + ", a = " + a + ", x1 = " + x1;
        assert !(!attraction && (a < p2 || a > p23)) || x1 <= 0  : "attraction = " + attraction + ", a = " + a + ", x1 = " + x1;
        assert !(attraction && !(a < p2 || a > p23)) || x1 <= 0  : "attraction = " + attraction + ", a = " + a + ", x1 = " + x1;
        assert !(!attraction && !(a < p2 || a > p23)) || x1 >= 0  : "attraction = " + attraction + ", a = " + a + ", x1 = " + x1;
        calcPoint.x += x1;
        calcPoint.y += y1;
    }

    public GraphPlotter physics(Ribbon all, Ribbon edges) {
        GraphPlotter g = new GraphPlotter();
        // compute force for every node
        Point calc, current;
        for (Map.Entry<String, Point> node: this.nodes.entrySet()) {
            calc = (Point) node.getValue().clone();
            current = (Point) node.getValue().clone();
            for (Map.Entry<String, Point> p: this.nodes.entrySet()) {
                if (!node.getKey().equals(p.getKey())) {
                    //System.out.println("force all: " + node.getKey() + " - " + p.getKey());
                    force(calc, current, p.getValue(), all);
                }
            }
            for (String e: this.getEdges(node.getKey(), true)) {
                //System.out.println("force edge start: " + node.getKey() + " - " + e);
                force(calc, current, this.getNode(e), edges);
            }
            for (String e: this.getEdges(node.getKey(), false)) {
                //System.out.println("force edge stop: " + node.getKey() + " - " + e);
                force(calc, current, this.getNode(e), edges);
            }
            g.addNode(node.getKey(), calc);
        }
        g.edges.addAll(this.edges);
        return g;
    }

    public Point getNode(final String node) {
        return this.nodes.get(node);
    }

    private Point[] getEdge(final String edge) {
        final int p = edge.indexOf('$',0);
        if (p < 0) return null;
        final Point from = getNode(edge.substring(0, p));
        final Point to = getNode(edge.substring(p + 1));
        if ((from == null) || (to == null)) return null;
        return new Point[] {from, to};
    }

    public Point addNode(final String node, Point p) {
        final Point p0 = this.nodes.put(node, p);
        assert p0 == null; // all add shall be unique
        if (p.x > this.rightmost) this.rightmost = p.x;
        if (p.x < this.leftmost) this.leftmost = p.x;
        if (p.y > this.topmost) this.topmost = p.y;
        if (p.y < this.bottommost) this.bottommost = p.y;
        return p;
    }

    public Point addNode(final String node, final double x, final double y, final int layer) {
        return addNode(node, new Point(x, y, layer));
    }

    public boolean hasEdge(final String fromNode, final String toNode) {
        return this.edges.contains(fromNode + "-" + toNode);
    }

    public void setEdge(final String fromNode, final String toNode) {
        final Point from = this.nodes.get(fromNode);
        final Point to = this.nodes.get(toNode);
        assert from != null;
        assert to != null;
        this.edges.add(fromNode + "$" + toNode);
    }

    public Collection<String> getEdges(final String node, boolean start) {
        Collection<String> c = new ArrayList<String>();
        if (start) {
            String s = node + "$";
            for (String e: this.edges) {
                if (e.startsWith(s)) c.add(e.substring(s.length()));
            }
        } else {
            String s = "$" + node;
            for (String e: this.edges) {
                if (e.endsWith(s)) c.add(e.substring(0, e.length() - s.length()));
            }
        }
        return c;
    }

    public void print() {
        // for debug purpose: print out all coordinates
        final Iterator<Map.Entry<String, Point>> i = this.nodes.entrySet().iterator();
        Map.Entry<String, Point> entry;
        String name;
        Point c;
        while (i.hasNext()) {
            entry = i.next();
            name = entry.getKey();
            c = entry.getValue();
            System.out.println("point(" + c.x + ", " + c.y + ", " + c.layer + ") [" + name + "]");
        }
        final Iterator<String> j = this.edges.iterator();
        while (j.hasNext()) {
            System.out.println("border(" + j.next() + ")");
        }
    }

    public RasterPlotter draw(
            final int width,
            final int height,
            final int leftborder,
            final int rightborder,
            final int topborder,
            final int bottomborder,
            final String color_back,
            final String color_dot,
            final String color_line,
            final String color_lineend,
            final String color_text
            ) {
        final RasterPlotter.DrawMode drawMode = (RasterPlotter.darkColor(color_back)) ? RasterPlotter.DrawMode.MODE_ADD : RasterPlotter.DrawMode.MODE_SUB;

        final RasterPlotter image = new RasterPlotter(width, height, drawMode, color_back);
        final double xfactor = ((this.rightmost - this.leftmost) == 0.0) ? 0.0 : (width - leftborder - rightborder) / (this.rightmost - this.leftmost);
        final double yfactor = ((this.topmost - this.bottommost) == 0.0) ? 0.0 : (height - topborder - bottomborder) / (this.topmost - this.bottommost);

        // draw dots and names
        final Iterator<Map.Entry<String, Point>> i = this.nodes.entrySet().iterator();
        Map.Entry<String, Point> entry;
        String name;
        Point c;
        int x, y;
        while (i.hasNext()) {
            entry = i.next();
            name = entry.getKey();
            c = entry.getValue();
            x = (xfactor == 0.0) ? width / 2 : (int) (leftborder + (c.x - this.leftmost) * xfactor);
            y = (yfactor == 0.0) ? height / 2 : (int) (height - bottomborder - (c.y - this.bottommost) * yfactor);
            image.setColor(color_dot);
            image.dot(x, y, 6, true, 100);
            image.setColor(color_text);
            PrintTool.print(image, x, y + 10, 0, name.toUpperCase(), 0);
        }

        // draw lines
        final Iterator<String> j = this.edges.iterator();
        Point[] border;
        image.setColor(color_line);
        int x0, x1, y0, y1;
        while (j.hasNext()) {
            border = getEdge(j.next());
            if (border == null) continue;
            if (xfactor == 0.0) {
                x0 = width / 2;
                x1 = width / 2;
            } else {
                x0 = (int) (leftborder + (border[0].x - this.leftmost) * xfactor);
                x1 = (int) (leftborder + (border[1].x - this.leftmost) * xfactor);
            }
            if (yfactor == 0.0) {
                y0 = height / 2;
                y1 = height / 2;
            } else {
                y0 = (int) (height - bottomborder - (border[0].y - this.bottommost) * yfactor);
                y1 = (int) (height - bottomborder - (border[1].y - this.bottommost) * yfactor);
            }
            // draw the line, with the dot at the beginning of the line
            image.lineDot(x1, y1, x0, y0, 3, 4, color_line, color_lineend);
        }
        return image;
    }

}
