
/**
 * @author Johannes Oehrlein, Institute of Geodesy and Geoinformation, University of Bonn, Germany
 */
 
 import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.StringTokenizer;

public class BonnOrienteering {

	private static int IN_D = 0;
	private static int IN_S = 1;
	private static int OUT = 2;
	private static int IN_ID_S = 3;
	private static int IN_ID_T = 4;
	private static int IN_MAX_D = 5;

	public static void main(String[] args) throws Exception {

		if (args.length != 6 && args.length != 3) {
			System.out.println("Please provide the following arguments (in that order):");
			System.out.println("\t" + IN_D + ": path to distance matrix file [*.csv of an nxn-matrix]");
			System.out.println("\t" + IN_S + ": path to score vector file [*.csv of an 1xn-vector]");
			System.out.println("\t" + OUT + ": path to the output file [*.csv]");
			System.out.println();
			System.out.println(
					"\t" + IN_ID_S + ": id of the source [integer, 0--(n-1)], necessary for optimization only");
			System.out.println(
					"\t" + IN_ID_T + ": id of the target [integer, 0--(n-1)], necessary for optimization only");
			System.out.println("\t" + IN_MAX_D + ": max. distance in meters [float], necessary for optimization only");
			System.exit(1);
		}

		double[][] distances = readDistances(args[IN_D]);
		double[] scores = readScores(args[IN_S]);

		System.out.println("Instance has " + distances.length + " sites.");

		if (args.length == 3) {

			Optimizer opt = new Optimizer(distances, scores, -1, -1, 0.0);

			LinkedList<DirectedEdge> path = opt.readPath(args[OUT]);

			System.out.println("Path information");
			System.out.println("score:\t" + opt.getScore(path));
			System.out.println("distance:\t" + opt.getLength(path));

		} else {
			Optimizer opt = new Optimizer(distances, scores, Integer.parseInt(args[IN_ID_S]),
					Integer.parseInt(args[IN_ID_T]), Double.parseDouble(args[IN_MAX_D]));
//			LinkedList<DirectedEdge> path = opt.optimize();
			LinkedList<DirectedEdge> path = Optimizer.SubInstanceOptimizer.solve(distances, scores,
					Integer.parseInt(args[IN_ID_S]), Integer.parseInt(args[IN_ID_T]), 2750);
//					Double.parseDouble(args[IN_MAX_D]));

			System.out.println("Solution:");
			for (DirectedEdge e : path) {
				System.out.println(e);
			}

			System.out.println(opt.getScore(path));
			System.out.println(opt.getLength(path));

			opt.writeResult(path, args[OUT]);
		}
	}

	private static double[] readScores(String filename) throws FileNotFoundException {
		double[] scores = null;
		Scanner s = new Scanner(new File(filename));
		StringTokenizer st = new StringTokenizer(s.nextLine(), " ,");
		scores = new double[st.countTokens()];
		for (int i = 0; i < scores.length; i++) {
			scores[i] = Double.parseDouble(st.nextToken());
		}
		s.close();
		return scores;
	}

	private static double[][] readDistances(String filename) throws FileNotFoundException {
		double[][] distances = null;
		Scanner s = new Scanner(new File(filename));
		String line = null;
		StringTokenizer st;
		int i = 0;
		while (s.hasNextLine()) {
			line = s.nextLine();
			st = new StringTokenizer(line, " ,");
			if (distances == null) {
				distances = new double[st.countTokens()][st.countTokens()];
			}
			for (int j = 0; j < distances.length; j++) {
				distances[i][j] = Double.parseDouble(st.nextToken());
			}
			i++;
		}
		s.close();
		return distances;
	}

}
