
/**
 * @author Johannes Oehrlein, Institute of Geodesy and Geoinformation, University of Bonn, Germany
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

import gurobi.GRB;
import gurobi.GRB.DoubleAttr;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

public class Optimizer {
	private double[][] distances;
	private double[] scores;
	private int source;
	private int target;
	private double dMax;
	private int n;
	// number of constraints
	private int counter;

	public Optimizer(double[][] distances, double[] scores, int source, int target, double dMax) {
		this.distances = distances;
		this.scores = scores;
		this.source = source;
		this.target = target;
		this.dMax = dMax;
		n = distances.length;
		counter = 0;
	}

	public LinkedList<DirectedEdge> optimize() {

		try {

			GRBEnv env = new GRBEnv("OP.log");
			GRBModel model = new GRBModel(env);

			// variables for selection of edges
			DirectedEdge[][] edges = new DirectedEdge[n][n];
			GRBVar x[][] = new GRBVar[n][n];
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					if (i != j) {
						x[i][j] = model.addVar(0.0, 1.0, scores[i], GRB.BINARY, "x_" + i + "_" + j);
						edges[i][j] = new DirectedEdge(i, j, distances[i][j]);
					}
				}
			}
			model.update();
			model.setObjective(model.getObjective(), GRB.MAXIMIZE);
			model.update();

			// constraint: no arc enters the source
			if (source != target) {
				GRBLinExpr e0 = new GRBLinExpr();
				for (int i = 0; i < n; i++) {
					if (i != source)
						e0.addTerm(1.0, x[i][source]);
				}
				model.addConstr(e0, GRB.EQUAL, 0.0, "constr_" + (counter++));
			}

			// constraint: no arc leaves the target
			if (source != target) {
				GRBLinExpr e0 = new GRBLinExpr();
				for (int i = 0; i < n; i++) {
					if (i != target)
						e0.addTerm(1.0, x[target][i]);
				}
				model.addConstr(e0, GRB.EQUAL, 0.0, "constr_" + (counter++));
			}

			// constraint: exactly one arc leaves the source
			GRBLinExpr e1 = new GRBLinExpr();
			for (int i = 0; i < n; i++) {
				if (i != source)
					e1.addTerm(1.0, x[source][i]);
			}
			model.addConstr(e1, GRB.EQUAL, 1.0, "constr_" + (counter++));

			// constraint: exactly one arc enters the target
			GRBLinExpr e2 = new GRBLinExpr();
			for (int i = 0; i < n; i++) {
				if (i != target)
					e2.addTerm(1.0, x[i][target]);
			}
			model.addConstr(e2, GRB.EQUAL, 1.0, "constr_" + (counter++));

			// constraint: for each node, the flow is preserved
			for (int i = 0; i < n; i++) {
				if (i != source && i != target) {
					GRBLinExpr e3 = new GRBLinExpr();
					GRBLinExpr e4 = new GRBLinExpr();
					for (int j = 0; j < n; j++) {
						if (i != j) {
							e3.addTerm(1.0, x[i][j]);
							e4.addTerm(1.0, x[j][i]);
						}
					}
					model.addConstr(e3, GRB.EQUAL, e4, "constr_" + (counter++));
				}
			}

			// constraint: for each node, at most one outgoing arc
			for (int i = 0; i < n; i++) {
				GRBLinExpr e5 = new GRBLinExpr();
				for (int j = 0; j < n; j++) {
					if (i != j)
						e5.addTerm(1.0, x[i][j]);
				}
				model.addConstr(e5, GRB.LESS_EQUAL, 1.0, "constr_" + (counter++));
			}

			// constraint: do not exceed maximally allowed distance
			GRBLinExpr e6 = new GRBLinExpr();
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					if (i != j) {
						e6.addTerm(distances[i][j], x[i][j]);
					}
				}
			}
			model.addConstr(e6, GRB.LESS_EQUAL, dMax, "constr_" + (counter++));

			// constraint: node numbering increases along the path
			addEnumerationConstraint(model, x);

			model.write("model.lp");
			model.optimize();

			// construct optimal path
			LinkedList<DirectedEdge> path = new LinkedList<DirectedEdge>();
			int temp = source;
			do {
				for (int j = 0; j < n; j++) {
					if (j != temp) {
						double xval = Math.round(x[temp][j].get(DoubleAttr.X));
						if (xval == 1.0) {
							path.add(edges[temp][j]);
							temp = j;
							break;
						}
					}
				}
			} while (temp != target);
			return path;

		} catch (Exception e) {
			return null;
		}
	}

	public double getScore(LinkedList<DirectedEdge> path) {
		double scoreSum = 0;
		for (DirectedEdge e : path) {
			scoreSum += scores[e.getU()];
		}
		scoreSum += scores[path.getLast().getV()];
		return scoreSum;
	}

	public double getLength(LinkedList<DirectedEdge> path) {
		double dist = 0;
		for (DirectedEdge e : path) {
			dist += distances[e.getU()][e.getV()];
		}
		return dist;
	}

	private void addEnumerationConstraint(GRBModel model, GRBVar[][] x) throws GRBException {

		// variables defining numbering of nodes
		GRBVar[] u = new GRBVar[n];
		for (int i = 0; i < n; i++) {
			if (i != source) {
				u[i] = model.addVar(2.0, n, 0.0, GRB.INTEGER, "u_" + i);
			}
		}
		model.update();

		// constraint: node numbering increases along the path
		for (int i = 0; i < n; i++) {
			if (i != source) {
				for (int j = 0; j < n; j++) {
					if (i != j && j != source) {
						GRBLinExpr e = new GRBLinExpr();
						e.addTerm(1, u[i]);
						e.addTerm(-1, u[j]);
						e.addTerm(n - 1, x[i][j]);
						model.addConstr(e, GRB.LESS_EQUAL, n - 2, "constr_" + (counter++));
					}
				}
			}
		}

	}

	public void writeResult(LinkedList<DirectedEdge> res, String path) throws IOException {
		OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(new File(path)));
		for (DirectedEdge e : res) {
			os.write(e.getU() + ",");
		}
		os.write(res.getLast().getV() + "\n");
		os.close();
	}

	public LinkedList<DirectedEdge> readPath(String filepath) throws FileNotFoundException {
		LinkedList<DirectedEdge> path = new LinkedList<DirectedEdge>();
		Scanner s = new Scanner(new File(filepath));
		String line = s.nextLine();
		s.close();

		String[] split = line.split(",");
		int[] ids = new int[split.length];

		ids[0] = Integer.parseInt(split[0]);
		for (int i = 1; i < split.length; ++i) {
			ids[i] = Integer.parseInt(split[i]);
			path.add(new DirectedEdge(ids[i - 1], ids[i], distances[ids[i - 1]][ids[i]]));
		}
		return path;
	}

	public static class SubInstanceOptimizer {

		private int[] subIndices;
		private int[] indexMap = null;

		private SubInstanceOptimizer(int[] subIndices, int n) {
			this.subIndices = subIndices;
			System.out.println("Using sub instance with the following " + subIndices.length + " indices");
			for (int i : subIndices) {
				System.out.print(i + ",");
			}
			System.out.println();

			initIndexMap(n);
		}

		private SubInstanceOptimizer(int source, int target, double dMax, double[][] distances) throws Exception {
			ArrayList<Integer> aux = new ArrayList<Integer>();
			if (distances[source][target] > dMax) {
				throw new Exception("Distance between source and target (" + distances[source][target]
						+ ") > maximum distance (" + dMax + ")");
			}
			for (int i = 0; i < distances.length; ++i) {
				if (distances[source][i] + distances[i][target] <= dMax) {
					aux.add(i);
				}
			}
			this.subIndices = new int[aux.size()];
			int j = 0;
			for (Integer i : aux) {
				subIndices[j++] = i;
			}
			System.out.println("Using sub instance with the following " + subIndices.length + " indices");
			for (int i : subIndices) {
				System.out.print(i + ",");
			}
			System.out.println();

			initIndexMap(distances.length);
		}

		private void initIndexMap(int n) {
			indexMap = new int[n];
			for (int i = 0; i < n; ++i) {
				indexMap[i] = -1;
			}
			for (int i = 0; i < subIndices.length; ++i) {
				indexMap[subIndices[i]] = i;
			}
		}

		private double[] getScores(double[] scores) {
			double[] subScores = new double[subIndices.length];
			int i = 0;
			for (int s : subIndices) {
				subScores[i++] = scores[s];
			}
			return subScores;
		}

		private double[][] getDistances(double[][] distances) {
			double[][] subDistances = new double[subIndices.length][subIndices.length];
			int i = 0;
			int j = 0;
			for (int s : subIndices) {
				j = 0;
				for (int t : subIndices) {
					subDistances[i][j++] = distances[s][t];
				}
				++i;
			}
			return subDistances;
		}

		private int getOriginalIndex(int i) {
			return subIndices[i];
		}

		private int getSubIndex(int i) {
			return indexMap[i];
		}

		private LinkedList<DirectedEdge> retranslate(LinkedList<DirectedEdge> path) {
			LinkedList<DirectedEdge> newPath = new LinkedList<DirectedEdge>();
			for (DirectedEdge e : path) {
				newPath.add(new DirectedEdge(getOriginalIndex(e.getU()), getOriginalIndex(e.getV()), e.getW()));
			}
			return newPath;
		}

		public static LinkedList<DirectedEdge> solve(double[][] distances, double[] scores, int source, int target,
				double dMax) throws Exception {
			SubInstanceOptimizer sic = new SubInstanceOptimizer(source, target, dMax, distances);
			Optimizer opt = new Optimizer(sic.getDistances(distances), sic.getScores(scores), sic.getSubIndex(source),
					sic.getSubIndex(target), dMax);
			LinkedList<DirectedEdge> path = opt.optimize();
			return sic.retranslate(path);
		}

	}
}
