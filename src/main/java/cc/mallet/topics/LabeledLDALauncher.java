package cc.mallet.topics;

import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import org.librairy.service.modeler.service.PipeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class LabeledLDALauncher {

    private static final Logger LOG = LoggerFactory.getLogger(LabeledLDALauncher.class);

    public void train(String corpusFile, String outputDir, String regEx, int textIndex, int labelIndex, int idIndex) throws IOException {

        File outputDirFile = Paths.get(outputDir).toFile();
        if (!outputDirFile.exists()) outputDirFile.mkdirs();

        Double alpha        = 0.1;
        Double beta         = 0.001;
        Integer numTopWords = 50;
        Integer numTopDocs  = 5;
        Integer showTopicsIntervalOption = 100;
        Integer numIterations = 1000;


        LabeledLDA labeledLDA = new LabeledLDA(alpha, beta);

        //labeledLDA.setRandomSeed(100);

        InstanceList topicModel = new CSVReader().getInstances(corpusFile, regEx,textIndex, labelIndex, idIndex);

        LOG.info("Data loaded.");
        if(topicModel.size() > 0 && topicModel.get(0) != null) {
            Object e = ((Instance)topicModel.get(0)).getData();
            if(!(e instanceof FeatureSequence)) {
                LOG.warn("Topic modeling currently only supports feature sequences: use --keep-sequence option when importing data.");
                System.exit(1);
            }
        }

        labeledLDA.addInstances(topicModel);


        //
        labeledLDA.setTopicDisplay(showTopicsIntervalOption, numTopWords);


        //
        labeledLDA.setNumIterations(numIterations);
        labeledLDA.estimate();

        //
        PrintStream topicModel1 = new PrintStream(System.out);
        topicModel1.print(labeledLDA.topWords(numTopWords));
        topicModel1.close();

        labeledLDA.write(Paths.get(outputDir, "model-llda.bin").toFile());

        //
        ParallelTopicModel topicModel3 = new ParallelTopicModel(labeledLDA.getTopicAlphabet(), alpha * (double)labeledLDA.numTopics, beta);
        topicModel3.data = labeledLDA.data;
        topicModel3.alphabet = labeledLDA.alphabet;
        topicModel3.numTypes = labeledLDA.numTypes;
        topicModel3.betaSum = labeledLDA.betaSum;
        topicModel3.buildInitialTypeTopicCounts();

        topicModel3.write(Paths.get(outputDir, "model-parallel.bin").toFile());

        //
        PrintWriter e1 = new PrintWriter(Paths.get(outputDir, "diagnostic.txt").toFile());
        TopicModelDiagnostics diagnostics = new TopicModelDiagnostics(topicModel3, numTopWords);
        e1.println(diagnostics.toXML());
        e1.close();


        //
//        topicModel3.printState(new File(topicStateFilename));

        //
//        e1 = new PrintWriter(new FileWriter(new File(topicDocsFilename)));
//        topicModel3.printTopicDocuments(e1, numTopDocs);
//        e1.close();

        //
//        e1 = new PrintWriter(new FileWriter(new File(docTopicsFilename)));
//        if(docTopicsThreshold == 0.0D) {
//            topicModel3.printDenseDocumentTopics(e1);
//        } else {
//            topicModel3.printDocumentTopics(e1, docTopicsThreshold, docTopicsMax);
//        }
//        e1.close();

        //
//        topicModel3.printTopicWordWeights(new File(topicWordsFilename));

        //
//        topicModel3.printTypeTopicCounts(new File(wordTopicsFilename));


        //
        ObjectOutputStream e2;
        try {
            e2 = new ObjectOutputStream(new FileOutputStream(Paths.get(outputDir, "model-inferencer.bin").toFile()));
            e2.writeObject(topicModel3.getInferencer());
            e2.close();
        } catch (Exception var6) {
            LOG.warn("Couldn\'t create inferencer: " + var6.getMessage());
        }
    }

    public TopicInferencer getTopicInferencer(String baseDir) throws Exception {
        return TopicInferencer.read(Paths.get(baseDir,"model-inferencer.bin").toFile());
    }

    public LabeledLDA getLabeledLDA(String baseDir) throws Exception {
        return LabeledLDA.read(Paths.get(baseDir,"model-llda.bin").toFile());
    }

    public ParallelTopicModel getTopicModel(String baseDir) throws Exception {
        return ParallelTopicModel.read(Paths.get(baseDir,"model-parallel.bin").toFile());
    }

}
