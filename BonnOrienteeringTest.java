
/**
 * @author Johannes Oehrlein, Institute of Geodesy and Geoinformation, University of Bonn, Germany
 */
 
import java.util.LinkedList;

import org.junit.Assert;
import org.junit.Test;

public class BonnOrienteeringTest {
	double PREC = Double.MIN_VALUE;

	@Test
	public void testA() {
		double[][] distances = { { 0, 2, 4, 4 }, { 2, 0, 3, 5 }, { 4, 3, 0, 2 }, { 4, 5, 2, 0 } };
		double[] scores = { 1, 1, 1, 1 };
		Optimizer opt = new Optimizer(distances, scores, 0, 3, 7.0);
		LinkedList<DirectedEdge> path = opt.optimize();

		System.out.println("Solution:");
		for (DirectedEdge e : path) {
			System.out.println(e);
		}

		Assert.assertEquals(4, opt.getScore(path), PREC);
		Assert.assertEquals(7, opt.getLength(path), PREC);

	}

	@Test
	public void testB() {
		double[][] distances = { { 0, 2, 4, 4 }, { 2, 0, 3, 5 }, { 4, 3, 0, 2 }, { 4, 5, 2, 0 } };
		double[] scores = { 1, 1, 1, 1 };
		Optimizer opt = new Optimizer(distances, scores, 0, 3, 4.0);
		LinkedList<DirectedEdge> path = opt.optimize();

		System.out.println("Solution:");
		for (DirectedEdge e : path) {
			System.out.println(e);
		}

		Assert.assertEquals(2, opt.getScore(path), PREC);
		Assert.assertEquals(4, opt.getLength(path), PREC);

	}

	@Test
	public void testC() {
		double[][] distances = { { 0, 2, 4, 4 }, { 2, 0, 3, 5 }, { 4, 3, 0, 2 }, { 4, 5, 2, 0 } };
		double[] scores = { 1, 1, 1, 1 };
		Optimizer opt = new Optimizer(distances, scores, 0, 3, 6.0);
		LinkedList<DirectedEdge> path = opt.optimize();

		System.out.println("Solution:");
		for (DirectedEdge e : path) {
			System.out.println(e);
		}

		Assert.assertEquals(3, opt.getScore(path), PREC);
		Assert.assertEquals(6, opt.getLength(path), PREC);

	}

	@Test
	public void testD() {
		double[][] distances = { { 0, 2, 4, 4 }, { 2, 0, 3, 5 }, { 4, 3, 0, 2 }, { 4, 5, 2, 0 } };
		double[] scores = { 1, 1, 1, 1 };
		Optimizer opt = new Optimizer(distances, scores, 0, 3, 3.0);
		LinkedList<DirectedEdge> path = opt.optimize();

		Assert.assertEquals(null, path);
	}
}
