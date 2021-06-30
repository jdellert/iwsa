package de.jdellert.iwsa.cluster;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FlatClustering {
	public static boolean VERBOSE = false;
	public static boolean SHOW_MATRICES = false;
	public static Map<Integer, String> pointIdentifiers = null;

	// implemented linkage criteria
	public static final int SINGLE_LINKAGE = 0;
	public static final int COMPLETE_LINKAGE = 1;
	public static final int AVERAGE_LINKAGE = 2;
	public static final int ENERGY_DIFFERENCE = 3;
	
	public static Set<Set<Integer>> singleLinkageClustering(double[][] distMtx, double threshold) {
		return linkageClustering(distMtx, threshold, SINGLE_LINKAGE);
	}

	public static Set<Set<Integer>> completeLinkageClustering(double[][] distMtx, double threshold) {
		return linkageClustering(distMtx, threshold, COMPLETE_LINKAGE);
	}

	public static Set<Set<Integer>> upgma(double[][] distMtx, double threshold) {
		return linkageClustering(distMtx, threshold, AVERAGE_LINKAGE);
	}

	public static Set<Set<Integer>> minimumEnergyClustering(double[][] distMtx, double threshold) {
		return linkageClustering(distMtx, threshold, ENERGY_DIFFERENCE);
	}

	private static Set<Set<Integer>> linkageClustering(double[][] distMtx, double threshold, int linkageCriterion) {
		Set<Set<Integer>> sets = new HashSet<Set<Integer>>();
		for (int i = 0; i < distMtx.length; i++) {
			HashSet<Integer> unarySet = new HashSet<Integer>();
			unarySet.add(i);
			sets.add(unarySet);
		}

		double lastDistance = Double.NEGATIVE_INFINITY;
		while (true) {
			lastDistance = Double.POSITIVE_INFINITY;
			Set<Integer> minCluster1 = null;
			Set<Integer> minCluster2 = null;
			for (Set<Integer> set1 : sets) {
				for (Set<Integer> set2 : sets) {
					if (set1 == set2)
						continue;
					double distance = 1.0;
					if (linkageCriterion == SINGLE_LINKAGE)
						distance = minimumClusterDistance(set1, set2, distMtx);
					else if (linkageCriterion == COMPLETE_LINKAGE)
						distance = maximumClusterDistance(set1, set2, distMtx);
					else if (linkageCriterion == AVERAGE_LINKAGE)
						distance = averageClusterDistance(set1, set2, distMtx);
					else if (linkageCriterion == ENERGY_DIFFERENCE)
						distance = energyIncreaseDuringFusion(set1, set2, distMtx, threshold);
					if (distance < lastDistance) {
						minCluster1 = set1;
						minCluster2 = set2;
						lastDistance = distance;

						if (SHOW_MATRICES) {
							System.err.print("     ");
							for (Integer j : set2) {
								String label = pointIdentifiers.get(j);
								if (label == null)
									label = j + "    ";
								else {
									while (label.length() < 5) {
										label += " ";
									}
									if (label.length() > 5) {
										label = label.substring(0, 5);
									}
								}
								System.err.print("  " + label);
							}
							System.err.println();
							for (Integer i : set1) {
								String label = pointIdentifiers.get(i);
								if (label == null)
									label = i + "    ";
								else {
									while (label.length() < 5) {
										label += " ";
									}
									if (label.length() > 5) {
										label = label.substring(0, 5);
									}
								}
								System.err.print(label);
								for (Integer j : set2) {
									System.err.print("  " + String.format(Locale.ENGLISH, "%.3f", distMtx[i][j]));
								}
								System.err.println();
							}
						}
						if (VERBOSE)
							System.err.println("  " + String.format(Locale.ENGLISH, "%.3f", distance) + " "
									+ strRep(set1) + " " + strRep(set2));
					}
				}
			}
			if (lastDistance > threshold)
				break;

			if (VERBOSE)
				System.err.println("Fusing: " + strRep(minCluster1) + " " + strRep(minCluster2));
			sets.remove(minCluster1);
			sets.remove(minCluster2);

			Set<Integer> fusedCluster = new HashSet<Integer>(minCluster1);
			fusedCluster.addAll(minCluster2);
			sets.add(fusedCluster);
		}

		return sets;
	}

	private static double minimumClusterDistance(Set<Integer> cluster1, Set<Integer> cluster2, double[][] distMtx) {
		double minDistance = Double.MAX_VALUE;
		for (int i : cluster1) {
			for (int j : cluster2) {
				if (distMtx[i][j] < minDistance) {
					minDistance = distMtx[i][j];
				}
			}
		}
		return minDistance;
	}

	private static double maximumClusterDistance(Set<Integer> cluster1, Set<Integer> cluster2, double[][] distMtx) {
		double maxDistance = 0.0;
		for (int i : cluster1) {
			for (int j : cluster2) {
				if (distMtx[i][j] > maxDistance) {
					maxDistance = distMtx[i][j];
				}
			}
		}
		return maxDistance;
	}

	private static double averageClusterDistance(Set<Integer> cluster1, Set<Integer> cluster2, double[][] distMtx) {
		double sum = 0.0;
		for (int i : cluster1) {
			for (int j : cluster2) {
				sum += distMtx[i][j];
			}
		}
		return sum / (cluster1.size() * cluster2.size());
	}

	private static double energyIncreaseDuringFusion(Set<Integer> cluster1, Set<Integer> cluster2, double[][] distMtx,
			double maxEnergyThreshold) {
		double term1 = 0.0;
		for (int i : cluster1) {
			for (int j : cluster2) {
				term1 += distMtx[i][j];
			}
		}

		double term2 = 0.0;
		for (int i : cluster1) {
			for (int j : cluster1) {
				term2 += distMtx[i][j];
			}
		}

		double term3 = 0.0;
		for (int i : cluster2) {
			for (int j : cluster2) {
				term3 += distMtx[i][j];
			}
		}

		term1 = 2 * term1 / (cluster1.size() * cluster2.size());
		term2 /= (cluster1.size() * cluster1.size());
		term3 /= (cluster2.size() * cluster2.size());

		return ((double) cluster1.size() * cluster2.size() / (cluster1.size() + cluster2.size()))
				* (term1 + term2 + term3);
	}
	
	public static String strRep(Set<Integer> cluster)
	{
		StringBuilder str = new StringBuilder("[");
		for (int point : cluster)
		{
			if (pointIdentifiers != null)
			{
				String id = pointIdentifiers.get(point);
				if (id == null)
				{
					id = point + "";
				}
				str.append(id + ", ");
			}
			else
			{
				str.append(point + ", ");
			}
		}
		str.delete(str.length() - 2, str.length());
		str.append("]");
		return str.toString();
	}
}
