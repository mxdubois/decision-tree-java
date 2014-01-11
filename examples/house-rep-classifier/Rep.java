import com.michaelxdubois.decisiontree.DataSet;

/**
 * Our model of a US House Representative.
 * @extends DataSet.ILabeledData<Character, Character>
 */
class Rep implements DataSet.ILabeledData<Character, Character> {

    private String mIdentifier;
    private Character mLabel;
    private Character[] mVotes;

    /**
     * Constructs a Rep.
     * @param identifier - String identifier of object
     * @param label - Character not char label of this Rep
     * @param votes - Character[] array of votes
     */
    public Rep(String identifier, Character label, Character[] votes) {
        mIdentifier = identifier;
        mLabel = label;
        mVotes = votes;
    }

    @Override
    public Character getLabel() {
        return mLabel;
    }

    @Override
    public Character getFeature(int i) {
        if(i < mVotes.length) {
            return mVotes[i];
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public int getFeatureVectorSize() {
        return mVotes.length;
    }
}
