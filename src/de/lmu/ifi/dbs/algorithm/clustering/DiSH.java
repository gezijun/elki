package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusterOrder;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusterOrderEntry;
import de.lmu.ifi.dbs.algorithm.result.clustering.HierarchicalAxesParallelCluster;
import de.lmu.ifi.dbs.algorithm.result.clustering.HierarchicalAxesParallelClusters;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.distance.distancefunction.DiSHDistanceFunction;
import de.lmu.ifi.dbs.logging.LogLevel;
import de.lmu.ifi.dbs.logging.ProgressLogRecord;
import de.lmu.ifi.dbs.preprocessing.DiSHPreprocessor;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.*;

/**
 * Algorithm for detecting supspace hierarchies.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DiSH extends AbstractAlgorithm<RealVector> {
  /**
   * The default value for epsilon.
   */
  public static final double DEFAULT_EPSILON = 0.001;

  /**
   * Option string for parameter epsilon.
   */
  public static final String EPSILON_P = "epsilon";

  /**
   * Description for parameter epsilon.
   */
  public static String EPSILON_D = "a double specifying the "
                                   + "maximum radius of the neighborhood to be "
                                   + "considered in each dimension for determination of "
                                   + "the preference vector " + "(default is " + DEFAULT_EPSILON
                                   + "in each dimension).";

  /**
   * The optics algorithm to determine the cluster order.
   */
  private OPTICS<RealVector, PreferenceVectorBasedCorrelationDistance> optics;

  /**
   * Holds the result;
   */
  private Result<RealVector> result;

  /**
   * Holds the value of epsilon parameter.
   */
  private double epsilon;

  /**
   * Provides a new algorithm for detecting supspace hierarchies.
   */
  public DiSH() {
    super();
    debug = true;

    // parameter epsilon
    DoubleParameter eps = new DoubleParameter(EPSILON_P, EPSILON_D, new GreaterEqualConstraint(0));
    eps.setDefaultValue(DEFAULT_EPSILON);
    optionHandler.put(EPSILON_P, eps);
  }

  /**
   * The run method encapsulated in measure of runtime. An extending class
   * needs not to take care of runtime itself.
   *
   * @param database the database to run the algorithm on
   * @throws IllegalStateException if the algorithm has not been initialized properly (e.g. the
   *                               setParameters(String[]) method has been failed to be called).
   */
  protected void runInTime(Database<RealVector> database) throws IllegalStateException {
    if (isVerbose()) {
      verbose("\nRun OPTICS algorithm.");
    }
    optics.run(database);

    if (isVerbose()) {
      verbose("\n\nCompute Clusters.");
    }
    computeClusters(database, (ClusterOrder<RealVector, PreferenceVectorBasedCorrelationDistance>) optics.getResult());
  }

  /**
   * Returns the result of the algorithm.
   *
   * @return the result of the algorithm
   */
  public Result<RealVector> getResult() {
    return result;
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description(
        "DiSH",
        "Detecting Subsapace Clusters",
        "Algorithm to find hierarchical correlation clusters in subspaces.",
        "unpublished :-(");
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // epsilon
    epsilon = (Double) optionHandler.getOptionValue(EPSILON_P);

    // parameters for optics
    List<String> opticsParameters = new ArrayList<String>();
    // epsilon for OPTICS
    opticsParameters.add(OptionHandler.OPTION_PREFIX + OPTICS.EPSILON_P);
    opticsParameters.add(DiSHDistanceFunction.INFINITY_PATTERN);
    // distance function
    opticsParameters.add(OptionHandler.OPTION_PREFIX + OPTICS.DISTANCE_FUNCTION_P);
    opticsParameters.add(DiSHDistanceFunction.class.getName());
    // epsilon for distance function
    opticsParameters.add(OptionHandler.OPTION_PREFIX + DiSHDistanceFunction.EPSILON_P);
    opticsParameters.add(Double.toString(epsilon));
    // omit flag
    opticsParameters.add(OptionHandler.OPTION_PREFIX + DiSHDistanceFunction.OMIT_PREPROCESSING_F);
    // preprocessor
    opticsParameters.add(OptionHandler.OPTION_PREFIX + DiSHDistanceFunction.PREPROCESSOR_CLASS_P);
    opticsParameters.add(DiSHPreprocessor.class.getName());
    // preprocessor epsilon
    opticsParameters.add(OptionHandler.OPTION_PREFIX + DiSHPreprocessor.EPSILON_P);
    opticsParameters.add(Double.toString(epsilon));
    // verbose
    if (isVerbose()) {
      opticsParameters.add(OptionHandler.OPTION_PREFIX + OPTICS.VERBOSE_F);
    }
    // time
    if (isTime()) {
      opticsParameters.add(OptionHandler.OPTION_PREFIX + OPTICS.TIME_F);
    }

    for (String parameter : remainingParameters) {
      opticsParameters.add(parameter);
    }
    optics = new OPTICS<RealVector, PreferenceVectorBasedCorrelationDistance>();
    remainingParameters = optics.setParameters(opticsParameters.toArray(new String[opticsParameters.size()]));

    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();

    AttributeSettings mySetting = settings.get(0);
    mySetting.addSetting(EPSILON_P, Double.toString(epsilon));

    settings.addAll(optics.getAttributeSettings());
    return settings;
  }

  /**
   * Computes the hierarchical clusters according to the cluster order.
   *
   * @param database     the database holding the objects
   * @param clusterOrder the cluster order
   */
  private void computeClusters(Database<RealVector> database, ClusterOrder<RealVector, PreferenceVectorBasedCorrelationDistance> clusterOrder) {
    int dimensionality = database.dimensionality();

    //noinspection unchecked
    DiSHDistanceFunction distanceFunction = (DiSHDistanceFunction) optics.getDistanceFunction();

    // extract clusters
    Map<BitSet, List<HierarchicalAxesParallelCluster>> clustersMap = extractClusters(database, distanceFunction, clusterOrder);
    if (this.debug) {
      StringBuffer msg = new StringBuffer("\nStep 1");
      for (List<HierarchicalAxesParallelCluster> clusterList : clustersMap.values()) {
        for (HierarchicalAxesParallelCluster c : clusterList) {
          msg.append("\n" + Util.format(dimensionality, c.getPreferenceVector()) + " ids " + c.getIDs().size());
        }
      }
      debugFine(msg.toString());
    }

    // check if there are clusters < minpts
    checkClusters(database, distanceFunction, clustersMap);
    if (this.debug) {
      StringBuffer msg = new StringBuffer("\n\nStep 2");
      for (List<HierarchicalAxesParallelCluster> clusterList : clustersMap.values()) {
        for (HierarchicalAxesParallelCluster c : clusterList) {
          msg.append("\n" + Util.format(dimensionality, c.getPreferenceVector()) + " ids " + c.getIDs().size());
        }
      }
      debugFine(msg.toString());
    }

    // actualize the levels and indices and sort the clusters
    List<HierarchicalAxesParallelCluster> clusters = sortClusters(clustersMap, dimensionality);
    if (this.debug) {
      StringBuffer msg = new StringBuffer("\n\nStep 3");
      for (HierarchicalAxesParallelCluster c : clusters) {
        msg.append("\n" + Util.format(dimensionality, c.getPreferenceVector()) + " ids " + c.getIDs().size());
      }
      debugFine(msg.toString());
    }

    // build the hierarchy
    buildHierarchy(database, distanceFunction, clusters, dimensionality);
    if (this.debug) {
      StringBuffer msg = new StringBuffer("\n\nStep 4");
      for (HierarchicalAxesParallelCluster c : clusters) {
        msg.append("\n" + Util.format(dimensionality, c.getPreferenceVector()) + " ids " + c.getIDs().size());
      }
      debugFine(msg.toString());
    }

    result = new HierarchicalAxesParallelClusters<RealVector, PreferenceVectorBasedCorrelationDistance>(clusters.get(clusters.size() - 1), clusterOrder, database);
  }

  /**
   * Extracts the clusters from the cluster order.
   *
   * @param database         the database storing the objects
   * @param distanceFunction the distance function
   * @param clusterOrder     the cluster order to extract the clusters from
   * @return the extracted clusters
   */
  private Map<BitSet, List<HierarchicalAxesParallelCluster>> extractClusters(Database<RealVector> database,
                                                                             DiSHDistanceFunction distanceFunction,
                                                                             ClusterOrder<RealVector, PreferenceVectorBasedCorrelationDistance> clusterOrder) {

    Progress progress = new Progress("Extract Clusters", database.size());
    int processed = 0;
    Map<BitSet, List<HierarchicalAxesParallelCluster>> clustersMap = new HashMap<BitSet, List<HierarchicalAxesParallelCluster>>();
    Map<Integer, ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance>> entryMap = new HashMap<Integer, ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance>>();
    Map<Integer, HierarchicalAxesParallelCluster> entryToClusterMap = new HashMap<Integer, HierarchicalAxesParallelCluster>();
    for (Iterator<ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance>> it = clusterOrder.iterator(); it.hasNext();)
    {
      ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance> entry = it.next();
      entryMap.put(entry.getID(), entry);

      RealVector object = database.get(entry.getID());
      BitSet preferenceVector = entry.getReachability().getCommonPreferenceVector();

      // get the list of (parallel) clusters for the preference vector
      List<HierarchicalAxesParallelCluster> parallelClusters = clustersMap.get(preferenceVector);
      if (parallelClusters == null) {
        parallelClusters = new ArrayList<HierarchicalAxesParallelCluster>();
        clustersMap.put(preferenceVector, parallelClusters);
      }

      // look for the proper cluster
      HierarchicalAxesParallelCluster cluster = null;
      for (HierarchicalAxesParallelCluster c : parallelClusters) {
        RealVector c_centroid = Util.centroid(database, c.getIDs(), c.getPreferenceVector());
        PreferenceVectorBasedCorrelationDistance dist = distanceFunction.correlationDistance(object, c_centroid, preferenceVector, preferenceVector);
        if (dist.getCorrelationValue() == entry.getReachability().getCorrelationValue()) {
          double d = distanceFunction.weightedDistance(object, c_centroid, dist.getCommonPreferenceVector());
          if (d <= 2 * epsilon) {
            cluster = c;
            break;
          }
        }
      }
      if (cluster == null) {
        cluster = new HierarchicalAxesParallelCluster(preferenceVector);
        parallelClusters.add(cluster);
      }
      cluster.addID(entry.getID());
      entryToClusterMap.put(entry.getID(), cluster);

      if (isVerbose()) {
        progress.setProcessed(++processed);
        progress(new ProgressLogRecord(LogLevel.PROGRESS, Util.status(progress), progress.getTask(), progress.status()));
      }
    }

    if (this.debug) {
      StringBuffer msg = new StringBuffer("\nStep 0");
      for (List<HierarchicalAxesParallelCluster> clusterList : clustersMap.values()) {
        for (HierarchicalAxesParallelCluster c : clusterList) {
          msg.append("\n" + Util.format(database.dimensionality(), c.getPreferenceVector()) + " ids " + c.getIDs().size());
        }
      }
      debugFine(msg.toString());
    }

    // add the predecessor to the cluster
    for (BitSet pv : clustersMap.keySet()) {
      List<HierarchicalAxesParallelCluster> parallelClusters = clustersMap.get(pv);
      for (HierarchicalAxesParallelCluster cluster : parallelClusters) {
        if (cluster.getIDs().isEmpty()) continue;
        Integer firstID = cluster.getIDs().get(0);
        ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance> entry = entryMap.get(firstID);
        Integer predecessorID = entry.getPredecessorID();
        if (predecessorID == null) continue;
        ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance> predecessor = entryMap.get(predecessorID);
        // parallel cluster
        if (predecessor.getReachability().getCommonPreferenceVector().equals(entry.getReachability().getCommonPreferenceVector()))
          continue;
        if (predecessor.getReachability().compareTo(entry.getReachability()) < 0)
          continue;

        HierarchicalAxesParallelCluster oldCluster = entryToClusterMap.get(predecessorID);
        oldCluster.removeID(predecessorID);
        cluster.addID(predecessorID);
        entryToClusterMap.remove(predecessorID);
        entryToClusterMap.put(predecessorID, cluster);
      }
    }

    return clustersMap;
  }

  /**
   * Sets the levels and indices in the clusters and returns a sorted list of the clusters.
   *
   * @param clustersMap    the mapping of bits sets to clusters
   * @param dimensionality the dimensionality of the data
   * @return a sorted list of the clusters
   */
  private List<HierarchicalAxesParallelCluster> sortClusters(Map<BitSet, List<HierarchicalAxesParallelCluster>> clustersMap, int dimensionality) {
    // actualize the levels and indices
    int[] clustersInLevel = new int[dimensionality + 1];
    List<HierarchicalAxesParallelCluster> clusters = new ArrayList<HierarchicalAxesParallelCluster>();
    for (BitSet pv : clustersMap.keySet()) {
      int level = pv.cardinality();
      List<HierarchicalAxesParallelCluster> parallelClusters = clustersMap.get(pv);
      for (int i = 0; i < parallelClusters.size(); i++) {
        HierarchicalAxesParallelCluster c = parallelClusters.get(i);
        c.setLevel(level);
        c.setLevelIndex(clustersInLevel[level]++);
        if (parallelClusters.size() > 1) {
          c.setLabel("[" + Util.format(dimensionality, pv) + "_" + i + "]");
        }
        else {
          c.setLabel("[" + Util.format(dimensionality, pv) + "]");
        }
        clusters.add(c);
      }
    }
    Collections.sort(clusters);
    return clusters;
  }

  /**
   * Removes the clusters with size < minpts from the cluster map and adds them to their parents.
   *
   * @param database         the database storing the objects
   * @param distanceFunction the distance function
   * @param clustersMap      the map containing the clusters
   */
  private void checkClusters(Database<RealVector> database,
                             DiSHDistanceFunction distanceFunction,
                             Map<BitSet, List<HierarchicalAxesParallelCluster>> clustersMap) {

    // check if there are clusters < minpts
    // and add them to not assigned
    int minpts = ((DiSHPreprocessor) distanceFunction.getPreprocessor()).getMinpts();
    List<HierarchicalAxesParallelCluster> notAssigned = new ArrayList<HierarchicalAxesParallelCluster>();
    Map<BitSet, List<HierarchicalAxesParallelCluster>> newClustersMap = new HashMap<BitSet, List<HierarchicalAxesParallelCluster>>();
    for (BitSet pv : clustersMap.keySet()) {
      List<HierarchicalAxesParallelCluster> parallelClusters = clustersMap.get(pv);
      List<HierarchicalAxesParallelCluster> newParallelClusters = new ArrayList<HierarchicalAxesParallelCluster>(parallelClusters.size());
      for (HierarchicalAxesParallelCluster c : parallelClusters) {
        if (!pv.equals(new BitSet()) && c.getIDs().size() < minpts) {
          notAssigned.add(c);
        }
        else {
          newParallelClusters.add(c);
        }
      }
      newClustersMap.put(pv, newParallelClusters);
    }

    clustersMap.clear();
    clustersMap.putAll(newClustersMap);

    HierarchicalAxesParallelCluster noise = new HierarchicalAxesParallelCluster(new BitSet());
    for (HierarchicalAxesParallelCluster c : notAssigned) {
      if (c.getIDs().isEmpty()) continue;
      HierarchicalAxesParallelCluster parent = findParent(database, distanceFunction, c, clustersMap);
      if (parent != null) {
        parent.addIDs(c.getIDs());
      }
      else {
        noise.addIDs(c.getIDs());
      }
    }

    List<HierarchicalAxesParallelCluster> noiseList = new ArrayList<HierarchicalAxesParallelCluster>(1);
    noiseList.add(noise);
    clustersMap.put(noise.getPreferenceVector(), noiseList);
  }

  /**
   * Returns the parent of the specified cluster
   *
   * @param database         the database storing the objects
   * @param distanceFunction the distance function
   * @param child            the child to search teh parent for
   * @param clustersMap      the map containing the clusters
   * @return the parent of the specified cluster
   */
  private HierarchicalAxesParallelCluster findParent(Database<RealVector> database,
                                                     DiSHDistanceFunction distanceFunction,
                                                     HierarchicalAxesParallelCluster child,
                                                     Map<BitSet, List<HierarchicalAxesParallelCluster>> clustersMap) {
    RealVector child_centroid = Util.centroid(database, child.getIDs(), child.getPreferenceVector());

    HierarchicalAxesParallelCluster result = null;
    int resultCardinality = -1;

    BitSet childPV = child.getPreferenceVector();
    int childCardinality = childPV.cardinality();
    for (BitSet parentPV : clustersMap.keySet()) {
      int parentCardinality = parentPV.cardinality();
      if (parentCardinality >= childCardinality) continue;
      if (resultCardinality != -1 && parentCardinality <= resultCardinality) continue;

      BitSet pv = (BitSet) childPV.clone();
      pv.and(parentPV);
      if (pv.equals(parentPV)) {
        List<HierarchicalAxesParallelCluster> parentList = clustersMap.get(parentPV);
        for (HierarchicalAxesParallelCluster parent : parentList) {
          RealVector parent_centroid = Util.centroid(database, parent.getIDs(), parentPV);
          double d = distanceFunction.weightedDistance(child_centroid, parent_centroid, parentPV);
          if (d <= 2 * epsilon) {
            result = parent;
            resultCardinality = parentCardinality;
            break;
          }
        }
      }
    }

    return result;
  }

  /**
   * Builds the cluster hierarchy
   *
   * @param distanceFunction the distance function
   * @param clusters         the sorted list of clusters
   * @param dimensionality   the dimensionality of the data
   * @param database         the fatabase containing the data objects
   */
  private void buildHierarchy(Database<RealVector> database,
                              DiSHDistanceFunction distanceFunction,
                              List<HierarchicalAxesParallelCluster> clusters, int dimensionality) {

    Map<HierarchicalAxesParallelCluster, Integer> parentLevels = new HashMap<HierarchicalAxesParallelCluster, Integer>();
    for (int i = 0; i < clusters.size() - 1; i++) {
      HierarchicalAxesParallelCluster c_i = clusters.get(i);
      int subspaceDim_i = dimensionality - c_i.getLevel();
      RealVector ci_centroid = Util.centroid(database, c_i.getIDs(), c_i.getPreferenceVector());

      for (int j = i + 1; j < clusters.size(); j++) {
        HierarchicalAxesParallelCluster c_j = clusters.get(j);
        int subspaceDim_j = dimensionality - c_j.getLevel();

        if (subspaceDim_i < subspaceDim_j) {
          if (c_j.getLevel() == 0) {
            Integer parentLevel = parentLevels.get(c_i);
            if (parentLevel == null) {
              parentLevels.put(c_i, c_j.getLevel());
              c_j.addChild(c_i);
              c_i.addParent(c_j);
            }
            else if (parentLevel == c_j.getLevel()) {
              c_j.addChild(c_i);
              c_i.addParent(c_j);
            }
          }
          else {
            RealVector cj_centroid = Util.centroid(database, c_j.getIDs(), c_j.getPreferenceVector());
            PreferenceVectorBasedCorrelationDistance distance = distanceFunction.correlationDistance(ci_centroid, cj_centroid, c_i.getPreferenceVector(), c_j.getPreferenceVector());
            double d = distanceFunction.weightedDistance(ci_centroid, cj_centroid, distance.getCommonPreferenceVector());

//            System.out.println("d2 " + distance.getEuklideanValue());
//            System.out.println("d " + d);

            if (distance.getCorrelationValue() <= subspaceDim_j) {
              if (d <= 2 * epsilon) {
                Integer parentLevel = parentLevels.get(c_i);
                if (parentLevel == null) {
                  parentLevels.put(c_i, c_j.getLevel());
                  c_j.addChild(c_i);
                  c_i.addParent(c_j);
                }
                else if (parentLevel == c_j.getLevel()) {
                  c_j.addChild(c_i);
                  c_i.addParent(c_j);
                }
              }
//              else {
//                System.out.println("ccccccccccccc d "+d);
//              }
            }
          }
        }
      }
    }

    System.out.println("");
    for (int i = 0; i < clusters.size(); i++) {
      HierarchicalAxesParallelCluster c_i = clusters.get(i);
      System.out.println("cluster " + c_i);
      for (int p = 0; p < c_i.getParents().size(); p++) {
        System.out.println("   parent " + c_i.getParents().get(p));
      }
    }
  }

}
