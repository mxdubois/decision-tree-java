package com.michaelxdubois.decisiontree;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Set;
import java.util.Stack;

/**
 * A generic discrete decision tree.
 * This tree is designed to work with any number of discrete labels and any
 * number of discrete feature values so long as the full range of possible
 * values is uniformly-typed and included in your subclass of the 
 * accompanying generic DataSet class.
 *
 * Note: If you extend this class, you will have to reimplment/override the
 * static methods for your child class.
 * @param <D> class type of child class of DataSet (extends DataSet<D,E,L,K>)
 * @param <E> class type of data (must extend DecisionTree.ILabeledData<L,K>)
 * @param <L> class type of labels (must extend Comparable<L>).
 * @param <K> class type of feature values (must extend Comparable<K>) 
 */
public class DecisionTree< // begin generics
        D extends DataSet<D,E,L,K>,
        E extends DataSet.ILabeledData<L,K>, 
        L extends Comparable<L>, 
        K extends Comparable<K>> // end generics
{

    //--------------------------------------------------------------------------
    // STATIC METHODS/PROPERTIES
    //--------------------------------------------------------------------------

    public static final String TAG = "DecisionTree";
    // Tuning options
    public static final int TUNE_BY_NONE = 0;
    public static final int TUNE_BY_STRIDE = 1;
    public static final int TUNE_BY_SIZE = 2;

    /**
     * Builds and tunes a tree using the specified tuning method.
     * TUNING_BY_SIZE interprets `tuningOrSize` as the size of the tuning data 
     * set and divides the `dataSet` accordingly. 
     * TUNING_BY_STRIDE interprets `tuningOrSize` as the stride with which to 
     * pluck tuning data out of the training set
     * @param dataSet - a dataset
     * @param tuningMethod - the predefined method to split tuning data out by
     * @param tuningSizeOrStride - either the size of the tuning set or the stride
     *                             with which to pick out tuning data
     * @return a tuned DecisionTree
     */
    // static, so we have to specify generics all over again
    // Quite possibly the ugliest method statement ever...
    public static <D extends DataSet<D,E,L,K>, 
                   E extends DataSet.ILabeledData<L,K>, 
                   L extends Comparable<L>, 
                   K extends Comparable<K>> 
           DecisionTree<D,E,L,K> // Returns
           buildTunedTree(
               // Params
               D dataSet,
               int tuningMethod,
               int tuningSizeOrStride) 
    { // method body
        D trainingDataSet = dataSet;
        if(tuningMethod != TUNE_BY_NONE) {
            int stride = tuningSizeOrStride;
            if(tuningMethod == TUNE_BY_SIZE) {
                stride = dataSet.size() / tuningSizeOrStride;
            }
            List<D> subsets = dataSet.divideByStride(stride);
            trainingDataSet = subsets.get(1);
            D tuningDataSet = subsets.get(0);
            DecisionTree<D,E,L,K> tree = new DecisionTree(trainingDataSet);
            DecisionTree.tuneTree(tree, tuningDataSet);
            return tree;
        } else {
            DecisionTree<D,E,L,K> tree = 
                new DecisionTree<D,E,L,K>(trainingDataSet);
            return tree;
        }
    }


    /**
     * Cross-validates a tuned tree by creating `dataSet.size() - n`
     * similar trees using the supplied dataSet minus `n` elements and 
     * testing each tree against the remaining `n` elements.
     * 
     * @param <D> a subclass of DataSet
     * @param <E> classtype of data, a subclass of DataSet.ILabeledData<L,K>
     * @param <L> classtype of labels, a subclass of Comparable<L>
     * @param <K> classtype of feature values, subclass of Comparable<K>
     * 
     * @param dataSet - the full dataSet with which to run tests
     * @param n - the size of the folds
     * @param tuningMethod - the tuning method with which to tune test trees
     *                       see DecisionTree.tuneTree() for details
     * @param tuningSizeOrStride - the size or stride of tuning data set
     */
    public static <D extends DataSet<D,E,L,K>, 
                   E extends DataSet.ILabeledData<L,K>, 
                   L extends Comparable<L>, 
                   K extends Comparable<K>> 
           double // Returns
           nFoldCrossValidate(
                   // Params
                   D dataSet,
                   int n,
                   int tuningMethod,
                   int tuningSizeOrStride) 
    {
        // (We'll let Java handle out of bounds errors)
        int subsetSize = dataSet.size() - n;
        double summedScores = 0.0;
        int i = 0;
        for(i=0; i < subsetSize; i++) {
            D trainingSet = dataSet.subsetExcludingRange(i, i+n);
            D testingSet = dataSet.subsetFromRange(i, i+n);
            DecisionTree<D,E,L,K> tree = 
                buildTunedTree(trainingSet, tuningMethod, tuningSizeOrStride);
            double score = testTree(tree, testingSet);
            summedScores += score;
        }
        return (double)summedScores / (double)i;
    }

    /**
     * Cross-validates a tuned tree by creating `dataSet.size() - n`
     * similar trees using the supplied dataSet minus `n` elements and 
     * testing each tree against the remaining `n` elements.
     * 
     * @param <T> a subclass of DecisionTree
     * @param <D> a subclass of DataSet
     * @param <E> classtype of data, a subclass of DataSet.ILabeledData<L,K>
     * @param <L> classtype of labels, a subclass of Comparable<L>
     * @param <K> classtype of feature values, subclass of Comparable<K>
     * 
     * @param testTree - the tree to test
     * @param testingDataSet - the DataSet to test against
     */
    public static <T extends DecisionTree<D,E,L,K>,
                   D extends DataSet<D,E,L,K>, 
                   E extends DataSet.ILabeledData<L,K>, 
                   L extends Comparable<L>, 
                   K extends Comparable<K>> 
           double // Returns
           testTree(
                   // Params
                   T testTree, 
                   D testingDataSet) 
    {
        // (We'll let Java handle out of bounds errors)
        List<E> data = testingDataSet.getData();
        int correct = 0;
        for(int i=0; i < data.size(); i++) {
            E datum = data.get(i);
            L treeClassification = testTree.classify(datum);
            //Log.d(TAG, "testing. actual = " + 
            //        datum.getLabel() + " == " + 
            //        testTree.classify(datum) + "? " + 
            //        (treeClassification.compareTo(datum.getLabel()) == 0));
            if(treeClassification.compareTo(datum.getLabel()) == 0){
                correct++;
            }
        }
        return (double)correct / (double)testingDataSet.size();
    }

    /**
     * Tunes the given tree by pruning nodes and testing against 
     * the given `tuningDataSet`. 
     *
     * @param <T> a subclass of DecisionTree
     * @param <D> a subclass of DataSet
     * @param <E> classtype of data, a subclass of DataSet.ILabeledData<L,K>
     * @param <L> classtype of labels, a subclass of Comparable<L>
     * @param <K> classtype of feature values, subclass of Comparable<K>
     * 
     * @param tree - the tree to tune
     * @param tuningDataSet - the DataSet to tune against
     */
    public static <D extends DataSet<D,E,L,K>, 
                   E extends DataSet.ILabeledData<L,K>, 
                   L extends Comparable<L>, 
                   K extends Comparable<K>> 
           void // Returns
           tuneTree(DecisionTree<D,E,L,K> tree, D tuningDataSet) 
    {
        boolean progress = true;
        while(progress) {
            // Do post-order traversal without recursion
            Stack<DecisionTree<D,E,L,K>> nodeStack = 
                new Stack<DecisionTree<D,E,L,K>>();
            nodeStack.push(tree);
           
            DecisionTree<D,E,L,K> currNode = null;
            DecisionTree<D,E,L,K> prevNode = null;
            DecisionTree<D,E,L,K> bestPrune = null;
            double bestAccuracy = DecisionTree.testTree(tree, tuningDataSet);

            while(!nodeStack.isEmpty()) {
                currNode = nodeStack.peek();
                // If currNode is a leaf 
                if(currNode.getChildren() == null || currNode.isPruned()) {
                    // Visit node... but do nothing with it.
                    // We can only prune inner nodes.
                    prevNode = nodeStack.pop();
                } else if(prevNode != null && 
                        currNode == prevNode.getParent()) 
                {
                    // We already traversed currNode's children
                    // Visit node
                    prevNode = nodeStack.pop();

                    // Prune node and test
                    currNode.setPruned(true);
                    double accuracy = testTree(tree, tuningDataSet);

                    // Check if worth pruning
                    // We use >= initial/best here because
                    // a simpler tree is better (Occam's razor)
                    if(accuracy >= bestAccuracy ) {
                        bestPrune = currNode;
                    }

                    // Un-prune it for now
                    currNode.setPruned(false);
                } else { 
                    // Add children to traversal stack
                    HashMap<K, DecisionTree<D,E,L,K>> children = 
                        currNode.getChildren();
                    for(DecisionTree<D,E,L,K> child : children.values()) {
                        nodeStack.push(child);
                    }
                    prevNode = currNode;
                }
            }

            if(bestPrune != null) {
                bestPrune.setPruned(true);    
            } else {
                progress = false;
            }
        }
    }

    //--------------------------------------------------------------------------
    // INSTANCE METHODS / PROPERTIES
    // -------------------------------------------------------------------------

    private DecisionTree<D,E,L,K> mParent = null;
    private HashMap<K, DecisionTree<D,E,L,K>> mChildren = null;
    private int mSplitByFeat = -1;
    private K mFeatValue;
    private L mLabel;
    private boolean mPruned = false;

    /**
     * Constructs a DecisionTree from given Dataset.
     * @param dataSet - the dataset for this tree.
     */
    public DecisionTree(D dataSet) {
        this(null, dataSet);
    }

    /**
     * Constructs a subtree with given parent and subset Dataset.
     * @param parent - pass null if this DecisionTree is the root.
     * @param dataSet - the dataset of this node
     */
    private DecisionTree(DecisionTree<D,E,L,K> parent, D dataSet) {
        mParent = parent;

        // Compute labels for all nodes since some children inherit parent label 
        // we can use isLeaf() to determine if nodes are leaves
        mLabel = computeLabel(dataSet);
        
        // Try to split data into child nodes
        mSplitByFeat = procreate(dataSet);
    }

    /**
     * Make lil' tree babies!!!
     * This method will not make children if none of the possible 
     * feature divisions yields an entropic gain.
     * @param dataset - the dataset to subdivide into child nodes.
     */
    private int procreate(D dataSet) {
        //Log.d(TAG, "Procreating");
        List<E> data = dataSet.getData(); 
        K[] possibleFeatValues = dataSet.getFeatValues(); 
        L[] possibleLabels = dataSet.getLabels();

        // Exit early if data set is too small to split
        int splitByFeat = -1;
        //Log.d(TAG, "data.size(): " + data.size());
        if(data.size() <= 1) { 
            return splitByFeat;
        }

        double initialEntropy = dataSet.entropy(); // caclulate once
        // Exit early if entropy == 0, no need to split
        if(initialEntropy == 0.0) { 
            return splitByFeat;
        }

        double gainMax = 0;
        //List<D> optimalDataSubsets = null;
        HashMap<K,D> optimalSubsets = null;

        // Find featVal split resulting in greatest entropic gain
        for(int i=0; i<data.get(0).getFeatureVectorSize(); i++) {
            //List<D> dataSubsets = dataSet.splitByFeature(i);
            HashMap<K,D> subsetsByFeatVal = dataSet.subsetMapByFeatVal(i);
            Log.d(DataSet.TAG, "Split dataset into " + 
                    subsetsByFeatVal.values().size() + 
                    " subsets by feat " + (char)('A' + i));

            // Determine the weighted average entropy after splitting on this 
            // feature, SUM( S_i.size / S.size * Entropy(S_i) )
            double weightedEntropy = 0.0;
            int j = 0;
            for (D dataSubset : subsetsByFeatVal.values()) {
            //for (DataSet<D,E,L,K> dataSubset : dataSubsets) {
                //Log.d(TAG, "subset1 has size: " + dataSubset.getData().size());
                double ei = dataSubset.entropy();
                //Log.d(TAG, "entropy: " + ei);
                //Log.d(TAG, "dataSubset " + 
                //        (char)('A' + j) + " entropy: " + ei );
                weightedEntropy += 
                    ((double)dataSubset.size() / (double)dataSet.size()) * ei;
                j++;
            }
            // Determine if the gain from this split is king of the hill
            double featGain = initialEntropy - weightedEntropy;
            // Log.d(DataSet.TAG, "weightedEntropy: " + weightedEntropy);
            // Log.d(DataSet.TAG,"gain: " + featGain);
            // Log.d(DataSet.TAG,"gainMax: " + gainMax);
            // Log.d(DataSet.TAG,"===================================");
            if(featGain > gainMax) {
                splitByFeat = i;
                gainMax = featGain;
                //optimalDataSubsets = dataSubsets;
                optimalSubsets = subsetsByFeatVal;
            }
        }

        // If we found split with favorable entropic gain
        if(gainMax > 0) {
            Log.d(DataSet.TAG, "optimalSplitBy: " + (char)('A' + splitByFeat));
            // Create child trees containing subsets from optimal split
            mChildren = new HashMap<K, DecisionTree<D,E,L,K>>();
            for (Map.Entry<K, D> entry : optimalSubsets.entrySet()) {
                D dataSubset = (D) entry.getValue();
                DecisionTree<D,E,L,K> child = 
                    new DecisionTree<D,E,L,K>(this, dataSubset);
                mChildren.put(entry.getKey(), child);
                
            }
        } else {
            // we can't split this data
            splitByFeat = -1;
        }

        return splitByFeat;
    }

    /**
     * Determines and caches the label of this node as if it was a leaf.
     * Do not call this to ask the tree for it's label. Use instead 
     * `getLabel()`
     * @param dataSet - the current node's dataSet.
     * @return the best possible label for this node.
     */
    private L computeLabel(DataSet<D,E,L,K> dataSet) {
        List<E> data = dataSet.getData();

        // Some early exit cases
        if(data.size() == 1) {
            return data.get(0).getLabel();
        } else if(data.size() == 0) {
            if(mParent == null) {
                // No parent to inherit from? Can't do much with this.
                throw new IllegalStateException(
                        "Cannot compute the label of a root node" + 
                        " with an empty data set.");
            } else {
                return mParent.getLabel();
            }
        }

        // King of the hill search for majority label
        int majority = 0;
        L majorityLabel = dataSet.getDefaultLabel();
        boolean tie = false;
        HashMap<L, List<E>> labelMap = dataSet.mapDataByLabel();
        for (Map.Entry<L, List<E>> entry : labelMap.entrySet()) {
            int labelCount = entry.getValue().size(); 
            if(labelCount > majority) {
                majorityLabel = entry.getKey();
                majority = labelCount;
                tie = false;
            } else if(labelCount == majority) {
                tie = true;
            }
        }

        // Break ties if possible
        if(tie == true) {
            if(mParent != null) {
                majorityLabel = mParent.getLabel();
            }
            // Else, tie break is arbitrary (whichever we discovered last)
        }

        return majorityLabel;
    }

    /**
     * Returns this node's parent or null if there is none.
     * @return a DecisionTree<D,E,L,K>
     */
    public DecisionTree getParent() {
        return mParent;
    }

   /**
     * Get this node's children.
     * @return a List of DecisionTree subtrees. 
     */
    public HashMap<K, DecisionTree<D,E,L,K>> getChildren() {
        //return mChildren;
        return mChildren;
    }

    /**
     * Returns the feature index upon which this node's children were split.
     * @return int index of split-by-feature
     */
    public int getSplitByFeat() {
        return mSplitByFeat;
    }

    /**
     * Returns this node's label as if it was a leaf.
     * @return label of this node
     */
    public L getLabel() {
        return mLabel;
    }

    /**
     * Is this node a leaf?
     * @return boolean, true if node is leaf, false if not
     */
    public boolean isLeaf() {
        return mChildren == null || mChildren.values().size() == 0;
    }

    /**
     * Is this node pruned? ie forced to behave as a leaf?
     * @return boolean true if is pruned, false if not
     */
    public boolean isPruned() {
        return mPruned;
    }

    /**
     * Mark this node as pruned or not.
     * @param pruned - true if node should be pruned, false if not.
     */
    public void setPruned(boolean pruned) {
        mPruned = pruned;
    }

    /**
     * Is this node the root?
     * @return boolean, true if node is root, false if not.
     */
    public boolean isRoot() {
        return mParent == null;
    }

    /**
     * Returns the best possible classification for given LabeledData object.
     * @return best possible label for given data
     */
    public L classify(E datum) {
        if(isLeaf() || isPruned()) {
            // Return this node's label
            return getLabel();
        }

        DecisionTree<D,E,L,K> matchedChild = 
            mChildren.get(datum.getFeature(mSplitByFeat));
        if(matchedChild != null) {
            return matchedChild.classify(datum);
        } else {
            Log.d(TAG, "No matching child found for " + datum);
        }

        // We didn't find one... Our best guess is this node's label.
        // This could happen if someone called classify on a subtree
        // instead of the root, or if the datum has a feature value
        // outside of the datasets possible featvals
        return getLabel();
    }

    @Override
    public String toString() {
        return toStringHelper(0);
    }

    /**
     * A pre-order recursion helper for toString.
     * @param depth - the recursion depth
     */
    public String toStringHelper(int depth) {
        String indent = "";
        for(int i=0; i<depth+1; i++) {
            if(i > 0 && i<depth) {
                indent += "|";
            }
            indent += "    ";
        }
        String str = "";
        if(isLeaf() || isPruned()) {
            return str + getLabel();
        }
        str += "Feature " + (char)('A' + mSplitByFeat) + ":";
        for(Map.Entry<K, DecisionTree<D,E,L,K>> c : getChildren().entrySet()) {
           str += "\n" + 
               indent +  
               c.getKey() + " " + 
               c.getValue().toStringHelper(depth+1);
        }
        return str;
    }
}
