import java.util.List;

import com.michaelxdubois.decisiontree.DataSet;

/**
 * Dataset class for US House Representatives
 * @extends DataSet<RepDataSet,Rep,Character,Character>
 */
class RepDataSet extends DataSet<RepDataSet, Rep, Character, Character> {
    // E for error.
    public static final Character DEFAULT_LABEL = new Character('E');

    List<Rep> mReps;
    Character[] mLabels;
    Character[] mFeatVals;

    /**
     * Constructs a RepDataSet.
     * @param reps - a List<Rep> because arrays don't play well with generics
     * @param labels - the array of all possible labels for the full dataset
     * @param featVals - the array of all possible featVals for full dataset.
     */
    public RepDataSet(List<Rep> repsList, 
                      Character[] labels, 
                      Character[] featVals) 
    {
        mReps = repsList;
        mLabels = labels;
        mFeatVals = featVals;
    }

    @Override
    public RepDataSet spawnSubset(List<Rep> reps, 
                                  Character[] labels, 
                                  Character[] featVals) 
    {
        return new RepDataSet(reps, labels, featVals);
    }

    @Override
    public Character[] getLabels() {
        return mLabels;
    }

    @Override
    public Character getDefaultLabel() {
        return DEFAULT_LABEL;
    }

    @Override
    public Character[] getFeatValues() {
        return mFeatVals;
    }

    @Override
    public List<Rep> getData() {
        return mReps;
    }

    @Override
    public int size() {
        return mReps.size();
    }
}
