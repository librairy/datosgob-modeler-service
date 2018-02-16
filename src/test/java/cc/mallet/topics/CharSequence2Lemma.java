package cc.mallet.topics;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Input2CharSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.Instance;
import cc.mallet.types.SingleInstanceIterator;
import cc.mallet.types.TokenSequence;
import cc.mallet.util.CharSequenceLexer;
import org.apache.avro.AvroRemoteException;
import org.assertj.core.util.Strings;
import org.librairy.service.nlp.facade.AvroClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.librairy.service.nlp.facade.model.PoS;
import org.librairy.service.nlp.facade.model.Form;

import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class CharSequence2Lemma extends Pipe implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(CharSequence2Lemma.class);
    private final AvroClient client;
    private final List<PoS> pos;


    public CharSequence2Lemma(String nlpServiceHost, Integer nlpServicePort, List<PoS> pos) {
        this.client = new AvroClient();
        this.pos    = pos;

        try {
            client.open(nlpServiceHost,nlpServicePort);
        } catch (IOException e) {
            throw new RuntimeException("Lemmatizer service not running!",e);
        }
    }




    public Instance pipe (Instance carrier)
    {
        String text = (String) carrier.getData();

        if (Strings.isNullOrEmpty(text)) return carrier;

        LOG.debug("parsing lemmas from : " + (text).substring(0,25) + " ..");

        CharSequence rawData = (CharSequence) carrier.getData();
        CharSequence processedData = rawData;
        try {
            processedData = client.process(rawData.toString(), this.pos, Form.LEMMA);
        } catch (AvroRemoteException e) {
            LOG.warn("Lemmatizer service is down!",e);
        }
        carrier.setData(processedData);
        return carrier;
    }

    public static void main (String[] args)
    {
        try {
            Instance carrier = new Instance (new File("src/test/resources/input/sample.txt"), null, null, null);
            SerialPipes p = new SerialPipes (new Pipe[] {
                    new Input2CharSequence(),
                    new CharSequence2Lemma("localhost",65111,Arrays.asList(new PoS[]{PoS.NOUN, PoS.VERB, PoS.ADVERB, PoS.ADJECTIVE})),
                    new CharSequence2TokenSequence(new CharSequenceLexer())
            });
            carrier = p.newIteratorFrom (new SingleInstanceIterator(carrier)).next();
            TokenSequence ts = (TokenSequence) carrier.getData();
            System.out.println ("===");
            System.out.println (ts.toString());
        } catch (Exception e) {
            System.out.println (e);
            e.printStackTrace();
        }
    }

    // Serialization

    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 0;

    private void writeObject (ObjectOutputStream out) throws IOException {
        out.writeInt(CURRENT_SERIAL_VERSION);
        //out.writeObject(lexer);
    }

    private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt ();
        //lexer = (CharSequenceLexer) in.readObject();
    }

}
