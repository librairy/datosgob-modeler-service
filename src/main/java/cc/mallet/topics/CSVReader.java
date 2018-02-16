package cc.mallet.topics;

import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.types.InstanceList;
import org.librairy.service.modeler.service.PipeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class CSVReader {

    private static final Logger LOG = LoggerFactory.getLogger(CSVReader.class);

    public InstanceList getInstances(String filePath, String regEx, int textIndex, int labelIndex, int idIndex) throws FileNotFoundException {

        // Construct a new instance list, passing it the pipe we want to use to process instances.
        Pipe pipe = new PipeBuilder().build();
        InstanceList instances = new InstanceList(pipe);

        int dataGroup           = textIndex;
        int targetGroup         = labelIndex;
        int uriGroup            = idIndex;

        CsvIterator iterator = new CsvIterator(filePath, regEx, dataGroup, targetGroup, uriGroup);

        // Now process each instance provided by the iterator.
        instances.addThruPipe(iterator);

        return instances;
    }
}
