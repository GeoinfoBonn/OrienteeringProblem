
/**
 * @author Johannes Oehrlein, Institute of Geodesy and Geoinformation, University of Bonn, Germany
 */

public class DirectedEdge {
	
	private int u;
	private int v;
	private double w;
	
	public DirectedEdge(int u, int v, double w) {
		this.u = u;
		this.v = v;
		this.w = w;
	}
	
	public int getU() {
		return u;
	}
	public int getV() {
		return v;
	}
	public double getW() {
		return w;
	}
	
	public String toString() {
		return "" + u + "->" + v + "(" + w + ")";
	}
}
