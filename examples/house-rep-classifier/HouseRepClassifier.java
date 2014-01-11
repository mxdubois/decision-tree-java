
import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Scanner;
import java.text.ParseException;

import com.michaelxdubois.decisiontree.DecisionTree;

/**
 * A program to classify US House Representatives based on voting records.
 * @author Michael DuBois
 * @version 2013.11.15
 */
class HouseRepClassifier {
    private static final int CONSOLE_WIDTH = 80;

    public static void main (String[] args) {
        System.out.println("");
        if(args.length != 1) {
            System.out.println(" usage: HouseRepClassifier path/to/mydata.tsv");
            return;
        }
         try {
            // Get data from file
            RepDataSet repsDataSet = parseDataFile(args[0]);

            System.out.println("Hello. " +
                "Let me classify some US House Representatives for you...\n");

            // Build induced tree.
            DecisionTree<RepDataSet,Rep,Character,Character> tree = 
                DecisionTree.buildTunedTree(
                    repsDataSet, 
                    DecisionTree.TUNE_BY_STRIDE, 
                    4);

            // Estimate accuracy
            double estAccuracy = 
                DecisionTree.nFoldCrossValidate(
                        repsDataSet, 
                        1, 
                        DecisionTree.TUNE_BY_STRIDE, 
                        4);

            // Print tree
            System.out.println("I have induced the following tree from" + 
                    " your data:");
            System.out.print(formatLine(CONSOLE_WIDTH));
            System.out.println(tree.toString());
            System.out.print(formatLine(CONSOLE_WIDTH));
            System.out.println("with estimated accuracy: " + estAccuracy + "\n");

        } catch(FileNotFoundException e) {
            System.out.println( "The file you provided could not be found.");
            return;
        } catch(ParseException e) { 
            System.out.println("There was an error" + 
                    " parsing the supplied data file: \n" + e.getMessage());
        }catch(IllegalStateException e) {
            System.out.println("You broke me :( \n" + e.getMessage());
        }
    }


    /**
     * Constructs a HouseRepClassifier... but don't bother.
     */
    private HouseRepClassifier() {
        throw new IllegalStateException("You suck.");
    }

    /**
     * Parses US Representatives from a tsv voting record.
     * The file should be formatted as follows:
     * Each line is one case, "columns" are separated by tabs
     * Column 1: a string identifier for the case.
     * Column 2: a single character label.
     * Column 3: a string of chars representing vals for individual features.
     * @param pathToFile - the path to the tsv file containg the voting records
     * @return a RepDataSet containing Rep objects for all the cases found.
     */
    public static RepDataSet parseDataFile(String pathToFile) 
        throws FileNotFoundException,
               ParseException
    {
        // Track all possible featVals and labels
        HashSet<Character> featValSet = new HashSet<Character>();
        HashSet<Character> labelSet = new HashSet<Character>();

        List<Rep> repList = new ArrayList<Rep>();
        
        // Track elements per line and features per rep
        int lastLineTabbedTokens = -1;
        int lastLineFeatures = -1;

        // Iterate through lines
        Scanner lineScanner = 
            new Scanner(new BufferedReader(new FileReader(pathToFile)));
        while(lineScanner.hasNextLine()) {
            // Each line is a representative
            String identifier = "";
            Character label = RepDataSet.DEFAULT_LABEL;
            List<Character> features = new ArrayList<Character>();
            
            // Iterate through tab-delimited sections of this line
            Scanner tabScanner = new Scanner(lineScanner.nextLine());
            tabScanner.useDelimiter("\\t"); // split tokens by tabs
            int tabbedTokens = 0;
            int lineFeatures = 0;
            while(tabScanner.hasNext()) {
                String next = tabScanner.next();
                if(tabbedTokens==0) {
                    // First tabbed token should be string identifier

                } if(tabbedTokens == 1) {
                    // Second is label. We'll just take first char.
                    if(next.length() > 0) {
                        label = new Character(next.charAt(0));
                        labelSet.add(label); // Track labels we've seen
                    } else {
                        throw new ParseException(
                                "In `" + pathToFile + 
                                "`, a label was empty.", 0);
                    }
                } else if(tabbedTokens == 2){
                    // Assume single char feature values
                    int i = 0;
                    for(i=0; i < next.length(); i++) {
                        Character featVal = new Character(next.charAt(i));
                        features.add(featVal);
                        featValSet.add(featVal); // Track features we've seen
                    }
                    lineFeatures = i;
                }
                tabbedTokens++;
            } // End tab scan loop

            if(lastLineTabbedTokens >=0 && tabbedTokens != lastLineTabbedTokens) {
                throw new ParseException("PC LOAD LETTER", 0);
            } else {
                lastLineTabbedTokens = tabbedTokens;
            }
            if(lastLineFeatures >= 0 && lineFeatures != lastLineFeatures) {
                throw new ParseException("The items do not all have the " + 
                        "same number of features.", 0 );
            } else {
                lastLineFeatures = lineFeatures;
            }

            // Collect data for this representative
            Rep rep = 
                new Rep(identifier, 
                        label, 
                        features.toArray(new Character[features.size()]));
            repList.add(rep);
        } // End line scan loop

        Character[] labels = new Character[labelSet.size()];
        Character[] featVals = new Character[featValSet.size()];
        labels = labelSet.toArray(labels);
        featVals = featValSet.toArray(featVals);
        
        System.out.println("I found the following labels in the dataset: ");
        System.out.println(Arrays.toString(labels) + "\n");
        System.out.println("I found the following feature values in the" + 
                " dataset: ");
        System.out.println(Arrays.toString(featVals) + "\n");

        return new RepDataSet(repList, labels, featVals);
    }
    
    /**
     * Returns a string containing an ascii line charLength characters wide
     * @param charLength - the length of the line measured in char widths
     * @param character - the character with which to compose the line
     */
    public static String formatLine(int charLength, char character) {
        String ret = "";
        // TODO is this stupidly inefficient with immutable strings?
        for(int i=0; i<=charLength; i++) {
            ret += character;
        }
        return ret += "\n";
    }

    /**
     * Returns a string containing an ascii line charLength characters wide
     * @param charLength - the length of the line measured in char widths
     * @param character - the character with which to compose the line
     */
    public static String formatLine(int charLength) {
        return formatLine(charLength, '-');
    }
}
