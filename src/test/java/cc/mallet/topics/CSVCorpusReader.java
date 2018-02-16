package cc.mallet.topics;

import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import org.librairy.service.nlp.facade.model.PoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class CSVCorpusReader {

    private static final Logger LOG= LoggerFactory.getLogger(CSVCorpusReader.class);

    private final String regex;

    public CSVCorpusReader(String regEx){

        //regex = "(.*)"+separator+"(.*)"+separator+"(.*)";
        this.regex = regEx;

    }

    public InstanceList getInstances(String filePath, int textIndex, int labelIndex, int idIndex) throws FileNotFoundException {
        // Construct a new instance list, passing it the pipe
        //  we want to use to process instances.
        Pipe pipe = buildPipe();
        InstanceList instances = new InstanceList(pipe);

        int dataGroup           = textIndex;
        int targetGroup         = labelIndex;
        int uriGroup            = idIndex;


        CsvIterator iterator = new CsvIterator(filePath, regex, dataGroup, targetGroup, uriGroup);

        // Now process each instance provided by the iterator.
        //instances.addThruPipe(iterator);

        Iterator<Instance> pipedInstanceIterator = pipe.newIteratorFrom(iterator);
        while (pipedInstanceIterator.hasNext())
        {
            try{
                Instance newInstance = pipedInstanceIterator.next();
                instances.add(newInstance);
            }catch (IllegalStateException e){
                LOG.warn("Line not processed",e);
            }
            //System.out.println("Add instance " + pipedInstanceIterator.next().getName());
        }
        return instances;
    }

    public InstanceList validate(String filePath, int textIndex, int labelIndex, int idIndex) throws FileNotFoundException {
        // Construct a new instance list, passing it the pipe
        //  we want to use to process instances.
        Pipe pipe = minimalPipe();
        InstanceList instances = new InstanceList(pipe);

        int dataGroup           = textIndex;
        int targetGroup         = labelIndex;
        int uriGroup            = idIndex;


        CsvIterator iterator = new CsvIterator(filePath, regex, dataGroup, targetGroup, uriGroup);

        // Now process each instance provided by the iterator.
        //instances.addThruPipe(iterator);

        Iterator<Instance> pipedInstanceIterator = pipe.newIteratorFrom(iterator);
        while (pipedInstanceIterator.hasNext())
        {
            try{
                Instance newInstance = pipedInstanceIterator.next();
                instances.add(newInstance);
            }catch (IllegalStateException e){
                LOG.warn("Line not processed",e);

//                final String regex = "(http.*),(.*),(http.*),(.*),([0-9].*),([0-9].*),\"\"(.*)\"\"\"";
//                final Pattern pattern = Pattern.compile(regex);
//                final Matcher matcher = pattern.matcher(iterator.);
//
//                while (matcher.find()) {
//                    System.out.println("Full match: " + matcher.group(0));
//                    for (int i = 1; i <= matcher.groupCount(); i++) {
//                        System.out.println("Group " + i + ": " + matcher.group(i));
//                    }
//                }


            }
            //System.out.println("Add instance " + pipedInstanceIterator.next().getName());
        }
        return instances;
    }

    public Pipe buildPipe() {
        ArrayList pipeList = new ArrayList();

        // Read data from File objects

        pipeList.add(new Input2CharSequence("UTF-8"));

        pipeList.add(new CharSequence2Lemma("localhost",65111, Arrays.asList(new PoS[]{PoS.NOUN, PoS.VERB, PoS.ADVERB, PoS.ADJECTIVE})));

        // Regular expression for what constitutes a token.
        //  This pattern includes Unicode letters, Unicode numbers,
        //   and the underscore character. Alternatives:
        //    "\\S+"   (anything not whitespace)
        //    "\\w+"    ( A-Z, a-z, 0-9, _ )
        //    "[\\p{L}\\p{N}_]+|[\\p{P}]+"   (a group of only letters and numbers OR
        //                                    a group of only punctuation marks)
        Pattern tokenPattern =
                Pattern.compile("[\\p{L}\\p{N}_]+");

        // Tokenize raw strings
        pipeList.add(new CharSequence2TokenSequence(tokenPattern));

        // Normalize all tokens to all lowercase
        pipeList.add(new TokenSequenceLowercase());

        // Remove stopwords from a standard English stoplist.
        //  options: [case sensitive] [mark deletions]
        pipeList.add(new TokenSequenceRemoveStopwords(false, false));

        // Rather than storing tokens as strings, convert
        //  them to integers by looking them up in an alphabet.
        pipeList.add(new TokenSequence2FeatureSequence());

        // Do the same thing for the "target" field:
        //  convert a class label string to a Label object,
        //  which has an index in a Label alphabet.
//        pipeList.add(new Target2Label());

        pipeList.add(new TargetStringToFeatures());


        // Now convert the sequence of features to a sparse vector,
        //  mapping feature IDs to counts.
//        pipeList.add(new FeatureSequence2FeatureVector());




        // Print out the features and the label
//        pipeList.add(new PrintInputAndTarget());

        return new SerialPipes(pipeList);
    }

    public Pipe minimalPipe() {
        ArrayList pipeList = new ArrayList();

        // Read data from File objects

        pipeList.add(new Input2CharSequence("ISO8859_1"));

        // Regular expression for what constitutes a token.
        //  This pattern includes Unicode letters, Unicode numbers,
        //   and the underscore character. Alternatives:
        //    "\\S+"   (anything not whitespace)
        //    "\\w+"    ( A-Z, a-z, 0-9, _ )
        //    "[\\p{L}\\p{N}_]+|[\\p{P}]+"   (a group of only letters and numbers OR
        //                                    a group of only punctuation marks)
        Pattern tokenPattern =
                Pattern.compile("[\\p{L}\\p{N}_]+");

        // Tokenize raw strings
        pipeList.add(new CharSequence2TokenSequence(tokenPattern));
        // Print out the features and the label
//        pipeList.add(new PrintInputAndTarget());

        return new SerialPipes(pipeList);
    }

}
