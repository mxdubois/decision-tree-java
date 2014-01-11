package com.michaelxdubois.decisiontree;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

/**
 * A wrapper class for a set of ILabeledData intended for use with DecisionTree.
 * This class provides various essential mechanisms for splitting and analyzing
 * labelled data. It's motivating purpose, however, is to encapsulate the
 * related attributes of a generic DecisionTree's dataset -- namely, the
 * possible labels, the possible feature values, and the data itself. The 
 * DecisionTree would be unable to classify certain unseen feature combinations
 * if it tried to determine these possible values on the fly. 
 * @param <D> class type of child class of DataSet (extends DataSet<D,E,L,K>)
 * @param <E> class type of data (must extend DecisionTree.ILabeledData<L,K>)
 * @param <L> class type of labels (must extend Comparable<L>).
 * @param <K> class type of feature values (must extend Comparable<K>) 
 */
public abstract class DataSet<D extends DataSet<D,E,L,K>,
                       E extends DataSet.ILabeledData<L, K>, 
                       L extends Comparable<L>, 
                       K extends Comparable<K>> 
                       // Generics are the worst thing ever invented by man. 
{
    public static final String TAG = "DataSet";

    //--------------------------------------------------------------------------
    // ABSTRACT METHODS
    // -------------------------------------------------------------------------
    
    /**
     * Returns the full array of labels associated with this dataset
     */
    public abstract L[] getLabels();

    /**
     * Returns the full array of possible feature values for this dataset.
     */
    public abstract K[] getFeatValues();
    
    /**
     * Returns an array of the data contained in this dataset.
     * @return a List<E> because arrays don't play well with generics
     */
    public abstract List<E> getData();
    
    /**
     * Returns the size of this dataset.
     */
    public abstract int size();
    
    /**
     * returns the default label (one that won't be in data).
     */
    public abstract L getDefaultLabel();
    
    /**
     * Spawns a new DataSet from the given subset.
     * This is abstract because we cannot call generic type E's constructor
     * from here. It must be done by a class that know's E's type.
     * @param subsetList - a List<E> because "mutable" arrays don't 
     *                     play well with generics
     * @param labels - array of possible labels (won't be changing this one)
     * @param featVals - array of possible featVals (won't be chaning this one)
     */
    public abstract D spawnSubset(
            List<E> subsetList, 
            L[] labels, 
            K[] featVals);

    //--------------------------------------------------------------------------
    // INTERFACES
    // -------------------------------------------------------------------------

    /**
     * Items stored in this dataset must implement this interface.
     * @param <L> the class type of labels (must extend Comparable<L>).
     * @param <K> the class type of feature values (must extend Comparable<K>)
     */
    static interface 
        ILabeledData<L extends Comparable<L>, K extends Comparable<K>> {
        
        public L getLabel();
        public K getFeature(int i);
        public int getFeatureVectorSize();
    
    }

    //--------------------------------------------------------------------------
    // EVERYTHING ELSE
    // -------------------------------------------------------------------------

    /**
     * Returns a List of DataSets, each containing data unified by a featVal
     * for the given feature index.
     * @param i - the feature index to split upon
     * @return a List of datasets split by that feature
     */
    public List<D> splitByFeature(int i) {

        HashMap<K, List<E>> featValMap = new HashMap<K, List<E>>();
        // We can't just determine the possibleFeatVals on the fly
        // b/c we want to split on all possible and this node's dataset
        // might not have all possible. Thus, the possibleFeatValues param.        
        for(K key : getFeatValues()) {
            List<E> list = new ArrayList<E>();
            featValMap.put(key, list);
        }
        for(E d : getData()) {
            K featVal = d.getFeature(i);
            List<E> list = (List<E>)featValMap.get(featVal);
            if(list != null) {
                list.add(d);
            } else {
                throw new IllegalStateException(
                        "A LabeledData item contained a feature value" + 
                        " not specified in the possibleFeatValues Set." +
                        " Shame on you.");
            }
        }

        List<D> dataSubsetList = new ArrayList<D>();
        for (List<E> subset : featValMap.values()) {
            if(subset.size() > 0) {
                Log.d(TAG, subset.size() + " reps voted " + subset.get(0).getFeature(i));
            }
            D dataSubset = 
                spawnSubset(subset, getLabels(), getFeatValues());
            dataSubsetList.add(dataSubset);
        }
        return dataSubsetList;
    }

    /**
     * Maps this dataset's data by feature values for the given feature index.
     * @param i - int index of feature to map by
     * @return HashMap<K,D> of datasets by feature value key.
     */
    public HashMap<K,D> subsetMapByFeatVal(int i) {

        HashMap<K, List<E>> featValMap = new HashMap<K, List<E>>();
        HashMap<K, D> subsetsByFeatVal = new HashMap<K, D>();
        
        // Create buckets for all possible feature values   
        // Any empty lists this creates are desirable.
        for(K key : getFeatValues()) {
            List<E> list = new ArrayList<E>();
            featValMap.put(key, list);
        }

        // Iterate through data and sort according to featVal for i
        for(E d : getData()) {
            K featVal = d.getFeature(i);
            List<E> list = (List<E>)featValMap.get(featVal);
            if(list != null) {
                list.add(d);
            } else {
                throw new IllegalStateException(
                        "A LabeledData item contained a feature value" + 
                        " not specified in the possibleFeatValues Set." +
                        " Shame on you.");
            }
        }

        // Spawn DataSets containing each subset and add them to the dataset map
        for (Map.Entry<K, List<E>> entry : featValMap.entrySet()) {
            List<E> subset = (List<E>) entry.getValue(); 
            D dataSubset = spawnSubset(subset, getLabels(), getFeatValues());
            subsetsByFeatVal.put(entry.getKey(), dataSubset);
        }

        return subsetsByFeatVal;
    }


    /**
     * Returns a list of DataSet subsets of data on the stride (mod stride) 
     * and off the stride. The on-stride subset is placed first in the list.
     * @param stride - int the stride on which to divide
     * @return List<D>, 0th element is stride subset, 1st is remainder
     */
    public List<D> divideByStride(int stride) 
    {
        List<E> data = getData();
        List<E> strideList = new ArrayList<E>();
        List<E> remainderList = new ArrayList<E>();

        // Walk data and divide by on-stride and off-stride
        for(int i=0; i < data.size(); i++) {
            if(i%stride == 0) {
                strideList.add(data.get(i));
            } else {
                remainderList.add(data.get(i));
            }
        }

        // Collect data in to DataSets
        D strideSubset =
            spawnSubset(strideList, getLabels(), getFeatValues());
        D remainderSubset =
            spawnSubset(remainderList, getLabels(), getFeatValues());

        // Collect DataSets into List<T>
        List<D> returnList = new ArrayList<D>();
        returnList.add(strideSubset);
        returnList.add(remainderSubset);
        return returnList;

    }

    /**
     * Returns a subset DataSet excluding the range [lower, upper).
     * @param lower - int, the lower bound, inclusive, of range to exclude
     * @param upper - int, the upper bound, exclusive, of range to exclude
     * @return subset DataSet
     */
    public D subsetExcludingRange(int lower, int upper) {
        List<E> data = getData();
        ArrayList<E> subset = new ArrayList<E>(data);
        subset.subList(lower, upper).clear();
        return spawnSubset(subset, getLabels(), getFeatValues());
    }

    /**
     * Returns a subset DataSet with data in range [lower, upper).
     * @param lower - int, the lower bound, inclusive, of subset range
     * @param upper - int, the upper bound, exclusive, of subset range
     */
    public D subsetFromRange(int lower, int upper) {
        List<E> data = getData();
        // new ArrayList from subset b/c we don't want to return a DataSet
        // backed by a list that is itself backed by the full data list here.
        ArrayList<E> subset = new ArrayList<E> (data.subList(lower, upper));
        return spawnSubset(new ArrayList<E>(subset), getLabels(), getFeatValues());
    }


    /**
     * Maps data into Lists by label.
     * Note this does not return Lists of DataSets but instead Lists of 
     * the data typed E.
     * @return HashMap<L, List<E>> of data by label.
     */
    public HashMap<L, List<E>> mapDataByLabel() {
        // Otherwise, sort all the objects by label
        HashMap<L, List<E>> labelMap = new HashMap<L, List<E>>();
        for(E d : getData()) {
            L label = d.getLabel();
            List<E> labelList = (List<E>)labelMap.get(label);
            if(labelList == null) { 
                labelList = new ArrayList<E>();
                labelMap.put(label, labelList);
            }
            labelList.add(d);
        }
        return labelMap;
    }

    /**
     * Returns the entropy of this dataset.
     * @return double entropy of data contained in this dataset
     */
    public double entropy() {
        double entropy = 0.0;
        List<E> data = getData();
        // Early exit (also avoids division by zero later)
        if(data.size() == 0) {
           Log.d(TAG, "entropy() exit early. data size == 0."); 
            return entropy; 
        }

        L[] possibleLabels = getLabels();
        //Log.d(TAG, "There are " + possibleLabels.length + " possible labels");
        HashMap<L, List<E>> labelMap = new HashMap<L, List<E>>();

        // Add list for every valid label
        for(L label : possibleLabels) {
            List<E> list = new ArrayList<E>();
            labelMap.put(label, list);
        }

        // Sort data into label lists
        for(E d : data) {
            List<E> list = (List<E>) labelMap.get(d.getLabel());
            if(list != null) {
                list.add(d);
            } else {
                throw new IllegalStateException(
                        "A LabeledData item was labeled with a label" + 
                        " that was not included in the possibleLabels Set.");
            }
        }

        // Compute entropy
        // Entropy(S) = -SUM(p_i * log(p_i))
        for(L label : possibleLabels) {
            List<E> list = (List<E>) labelMap.get(label);
            Log.d(TAG, "There are " + list.size() + " labeled " + label);
            double p = (double)list.size() / (double)data.size();
            Log.d(TAG, "p: " + p);
            // let 0*log2(0) = 0 rather than undefined
            if(p != 0.0) {
                Log.d(TAG, "log2(" + p + "): " + log2(p)); 
                entropy += -1*(p * log2(p));
            }
        }
        Log.d(TAG, "------------------------------");
        Log.d(TAG, "entropy of subset: " + entropy);
        return entropy;
    }

    /**
     * Log base 2
     * @param a - the value to take the log2 of
     * @return double log base 2 of a
     */ 
    private static double log2(double a) {
        return Math.log(a) / Math.log(2);
    }



}
