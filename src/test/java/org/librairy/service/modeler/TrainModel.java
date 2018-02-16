package org.librairy.service.modeler;

import cc.mallet.topics.LabeledLDALauncher;
import org.junit.Test;

import java.io.IOException;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class TrainModel {

    @Test
    public void buildFromDatosGob() throws IOException {

        LabeledLDALauncher launcher = new LabeledLDALauncher();

        String file         = "src/test/resources/input/datasets-gob-es.csv";
        String outputDir    = "target/output";
        String regEx        = "(.*);;(.*);;(.*)";
        Integer textIndex   = 3;
        Integer labelIndex  = 2;
        Integer idIndex     = 1;

        launcher.train(file,outputDir,regEx,textIndex,labelIndex,idIndex);

    }

    @Test
    public void buildSample() throws IOException {

        LabeledLDALauncher launcher = new LabeledLDALauncher();

        String file         = "src/test/resources/input/sample.txt";
        String outputDir    = "target/output";
        String regEx        = "(.*);;(.*);;(.*)";
        Integer textIndex   = 3;
        Integer labelIndex  = 2;
        Integer idIndex     = 1;

        launcher.train(file,outputDir,regEx,textIndex,labelIndex,idIndex);

    }
}
