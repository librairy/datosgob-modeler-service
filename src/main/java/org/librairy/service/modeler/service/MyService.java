package org.librairy.service.modeler.service;

import cc.mallet.pipe.Pipe;
import cc.mallet.topics.LabeledLDA;
import cc.mallet.topics.LabeledLDALauncher;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import com.google.common.base.Strings;
import com.google.common.primitives.Doubles;
import org.apache.avro.AvroRemoteException;
import org.librairy.service.modeler.facade.model.ModelerService;
import org.librairy.service.modeler.facade.model.Topic;
import org.librairy.service.modeler.facade.model.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class MyService implements ModelerService {

    private static final Logger LOG = LoggerFactory.getLogger(MyService.class);

    @Value("#{environment['RESOURCE_FOLDER']?:'${resource.folder}'}")
    private String resourceFolder;

    private Pipe pipe;
    private TopicInferencer topicInferer;
    private ArrayList topics;
    private HashMap<Integer, List<Word>> words;

    @PostConstruct
    public void setup() throws Exception {

        LabeledLDALauncher labeledLDALauncher = new LabeledLDALauncher();


        this.topicInferer               = labeledLDALauncher.getTopicInferencer(resourceFolder);

        ParallelTopicModel topicModel   = labeledLDALauncher.getTopicModel(resourceFolder);

        LabeledLDA lldaModel            = labeledLDALauncher.getLabeledLDA(resourceFolder);


        this.pipe   = new PipeBuilder().build();
        this.topics = new ArrayList<>();
        this.words  = new HashMap();

        IntStream.range(0,topicModel.getNumTopics()).forEach(id -> {

            Topic topic = new Topic();

            topic.setId(id);
            topic.setName((String)lldaModel.getLabelAlphabet().lookupObject(id));

            List<Word> topWords = lldaModel.topWordsPerTopic(id, 50).entrySet().stream().map(entry -> new Word(entry.getKey(), entry.getValue())).sorted((a,b)-> -a.getScore().compareTo(b.getScore())).collect(Collectors.toList());
            this.words.put(id,topWords);

            topic.setDescription(topWords.stream().limit(10).map(w->w.getValue()).collect(Collectors.joining(",")));
            topics.add(topic);

        });



        LOG.info("Service initialized");
    }

    @Override
    public List<Double> inference(String s) throws AvroRemoteException {

        if (Strings.isNullOrEmpty(s)) return Collections.emptyList();

        String data = s;
        String name = "";
        String source = "";
        String target = "";
        Integer numIterations = 100;

        Instance rawInstance = new Instance(data,target,name,source);

        Instance instance = this.pipe.instanceFrom(rawInstance);

        int thinning = 1;
        int burnIn = 0;
        double[] topicDistribution = topicInferer.getSampledDistribution(instance, numIterations, thinning, burnIn);
        LOG.info("Topic Distribution of: " + s.substring(0,10)+ ".. " + Arrays.toString(topicDistribution));

        return Doubles.asList(topicDistribution);
    }

    @Override
    public List<Topic> topics() throws AvroRemoteException {
        return topics;
    }

    @Override
    public List<Word> words(int topicId, int maxWords) throws AvroRemoteException {
        if (!words.containsKey(topicId)) return Collections.emptyList();
        return words.get(topicId).stream().limit(maxWords).collect(Collectors.toList());

    }
}
