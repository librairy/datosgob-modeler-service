package org.librairy.service.modeler.service;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Input2CharSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.Instance;
import cc.mallet.types.SingleInstanceIterator;
import cc.mallet.types.TokenSequence;
import cc.mallet.util.CharSequenceLexer;
import com.google.common.base.Strings;
import org.apache.avro.AvroRemoteException;
import org.librairy.service.nlp.facade.AvroClient;
import org.librairy.service.nlp.facade.model.Form;
import org.librairy.service.nlp.facade.model.PoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class Lemmatizer extends Pipe implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(Lemmatizer.class);
    private final AvroClient client;
    private final List<PoS> pos;
    private final AtomicInteger counter;


    public Lemmatizer(String host, Integer port, List<PoS> pos){
        this.client     = new AvroClient();
        this.pos        = pos;
        this.counter    = new AtomicInteger();
        try {
            client.open(host,port);
        } catch (IOException e) {
            throw new RuntimeException("Lemmatizer service not running!",e);
        }
    }

    public Instance pipe (Instance carrier)
    {
        String text = (String) carrier.getData();

        if (Strings.isNullOrEmpty(text)) return carrier;

        String description = text.length() > 25? text.substring(0,25) : text;

        LOG.info("["+this.counter.getAndIncrement()+"] retrieving lemmas from : " + description + " ..");

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
                    new Lemmatizer("localhost",65111, Arrays.asList(new PoS[]{PoS.NOUN, PoS.VERB, PoS.ADVERB, PoS.ADJECTIVE})),
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
