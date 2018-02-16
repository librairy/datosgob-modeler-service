package cc.mallet.topics;

import cc.mallet.types.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class TopicTest {

    private static final Logger LOG = LoggerFactory.getLogger(TopicTest.class);

    Double alpha        = 0.1;
    Double beta         = 0.001;

    Integer numTopWords = 15;
    Integer numTopDocs  = 5;
    Integer showTopicsIntervalOption = 100;
    Integer numIterations = 1000;

    Double docTopicsThreshold   = 0.0;
    Integer docTopicsMax        = 10;

    String inputFilename        = "src/test/resources/input/data.csv";

    String topicKeysFilename    = "src/test/resources/output/topicKeys.txt";
    String outputModelFilename  = "src/test/resources/output/topicModel.txt";
    String labeledOutputModelFilename  = "src/test/resources/output/labeledLDA.ser";
    String diagnosticFilename   = "src/test/resources/output/topicDiagnostic.txt";
    String topicStateFilename   = "src/test/resources/output/topicState.txt";
    String topicDocsFilename    = "src/test/resources/output/topicDocs.txt";
    String docTopicsFilename    = "src/test/resources/output/docTopics.txt";
    String topicWordsFilename   = "src/test/resources/output/topicWords.txt";
    String wordTopicsFilename   = "src/test/resources/output/wordTopics.txt";
    String topicInfererFilename = "src/test/resources/output/topicInferencer.txt";


    @Test
    public void modelCreation() throws Exception {

        LabeledLDA labeledLDA = new LabeledLDA(alpha, beta);


        //labeledLDA.setRandomSeed(100);


        //
//        CSVCorpusReader corpusReader = new CSVCorpusReader("(.*)\"+separator+\"(.*)\"+separator+\"(.*)");
        CSVCorpusReader corpusReader = new CSVCorpusReader("(http.*),(.*),(http.*),(.*),(.*),([0-9].*),([0-9].*)");
        InstanceList topicModel = corpusReader.getInstances(inputFilename,7,5,1);

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
        PrintStream topicModel1 = new PrintStream(new File(topicKeysFilename));
        topicModel1.print(labeledLDA.topWords(numTopWords));
        topicModel1.close();

        labeledLDA.write(new File(labeledOutputModelFilename));

        LabeledLDA.read(new File(labeledOutputModelFilename));

        //
        ParallelTopicModel topicModel3 = new ParallelTopicModel(labeledLDA.getTopicAlphabet(), alpha * (double)labeledLDA.numTopics, beta);
        topicModel3.data = labeledLDA.data;
        topicModel3.alphabet = labeledLDA.alphabet;
        topicModel3.numTypes = labeledLDA.numTypes;
        topicModel3.betaSum = labeledLDA.betaSum;
        topicModel3.buildInitialTypeTopicCounts();

        topicModel3.write(new File(outputModelFilename));

        //
        PrintWriter e1 = new PrintWriter(diagnosticFilename);
        TopicModelDiagnostics diagnostics = new TopicModelDiagnostics(topicModel3, numTopWords);
        e1.println(diagnostics.toXML());
        e1.close();


        //
        topicModel3.printState(new File(topicStateFilename));

        //
        e1 = new PrintWriter(new FileWriter(new File(topicDocsFilename)));
        topicModel3.printTopicDocuments(e1, numTopDocs);
        e1.close();

        //
        e1 = new PrintWriter(new FileWriter(new File(docTopicsFilename)));
        if(docTopicsThreshold == 0.0D) {
            topicModel3.printDenseDocumentTopics(e1);
        } else {
            topicModel3.printDocumentTopics(e1, docTopicsThreshold, docTopicsMax);
        }
        e1.close();

        //
        topicModel3.printTopicWordWeights(new File(topicWordsFilename));

        //
        topicModel3.printTypeTopicCounts(new File(wordTopicsFilename));


        //
        ObjectOutputStream e2;
        try {
            e2 = new ObjectOutputStream(new FileOutputStream(topicInfererFilename));
            e2.writeObject(topicModel3.getInferencer());
            e2.close();
        } catch (Exception var6) {
            LOG.warn("Couldn\'t create inferencer: " + var6.getMessage());
        }

    }


    @Test
    public void inferenceTest() throws Exception {

        String data         = "decision means ruling nearly months regular season begins time sides work deal delaying season";
        String target       = "label1 label2";
        String source       = "";
        String name         = "d11";
        Instance rawInstance = new Instance(data,target,name,source);

        Instance instance = new CSVCorpusReader(";;").buildPipe().instanceFrom(rawInstance);


        TopicInferencer topicInferer = TopicInferencer.read(new File(topicInfererFilename));


        int thinning = 1;
        int burnIn = 0;
        double[] topicDistribution = topicInferer.getSampledDistribution(instance, numIterations, thinning, burnIn);
        LOG.info(Arrays.toString(topicDistribution));

    }

    @Test
    public void topicDetailsTest() throws Exception {


        ParallelTopicModel topicModel = ParallelTopicModel.read(new File(outputModelFilename));

        LabeledLDA lldaModel = LabeledLDA.read(new File(labeledOutputModelFilename));

        Alphabet alphabet = lldaModel.labelAlphabet;

        LOG.info(""+alphabet);


        IntStream.range(0,topicModel.getNumTopics()).forEach(id -> {

            LOG.info("Topic " + id + " - " + lldaModel.labelAlphabet.lookupObject(id));
            Map<String, Double> words = lldaModel.topWordsPerTopic(id, 5);
            words.entrySet().stream().sorted((a,b)-> -a.getValue().compareTo(b.getValue())).forEach(entry -> LOG.info("\t" + entry.getKey() + ":\t" + entry.getValue()));

        });


    }


    @Test
    public void regExTest(){

        final String regex = "(http.*),(.*),(http.*),(.*),(.*),([0-9].*),([0-9].*)";

        List<String> lines = Arrays.asList(new String[]{
                "\"http://datos.gob.es/catalogo/a16003011-conciliaciones-individuales-resueltas-en-la-c-a-de-euskadi1,Banakako adiskidetze bukatuak Euskal AEn .,http://datos.gob.es/recurso/sector-publico/org/Organismo/A16003011,Pais-Vasco,empleo,20170202,20170202, ,\"\"Este apartado ofrece información sobre el mercado laboral desde una perspectiva global, aportando información cuantificable tanto sobre los factores productivos  nivel de instrucción o de sindicación de la población ocupada,  tipo de sociedad mercantil o volumen de empleo por sector  de empresas de producción  como sobre las relaciones de producción  conflictividad laboral, conciliaciones laborales, expedientes de regulación de empleo, huelgas\u0085 . La información procede de diversas fuentes oficiales tales como departamentos del Gobierno Vasco y ministerios del Gobierno del Estado.\"\"\";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;",
                "\"http://datos.gob.es/catalogo/a02002834-recurso-educativo-primaria-comunes-primer-ciclo-ud12-mundnimal-un-reino-por-descubrir,Recurso Educativo  Primaria Comunes Primer Ciclo  UD12Mund@nimal.Un reino por descubrir.,http://datos.gob.es/recurso/sector-publico/org/Organismo/A02002834,Aragon,educacion,20161124,20170930,Educación Primaria Primer,\"\"Vivimos rodeados de animales. Respeta y te respetarán.Recueda que son seres vivos como nosotros.Para ello debemos conocerlos con profundidad. Además en esta unidad didáctica nos centraremos de manera globalizada sobre conocer e interactuar con: Matemáticas:Las horas y el reloj. Lengua: Palabras antónimas y sinónimas. Para ello nos adentraremos en el Planeta SABER, donde NAyMA, junto con su inseparable patrulla APRENDER CIENCIAMAN:Nuestros amigos animales nos ayudan y les ayudamos. Todos colaboramos. NUMBERMAN:Las horas y el reloj condicionan nustros día a día. andiquest\";Sabes mirar un reloj? LECTOWOMAN:Nos enseñará lo que son las palabras antónimas y sinónimas. Vivimos rodeados de animales. Respeta y te respetarán. Recueda que son seres vivos como nosotros. Para ello debemos conocerlos con profundidad.Además en esta unidad didáctica nos centraremos de manera globalizada sobre conocer e interactuar con:Matemáticas: Las horas y el reloj.Lengua:  Palabras antónimas y sinónimas.Para ello nos adentraremos en el Planeta SABER, donde NAyMA, junto con su inseparable patrulla “APRENDER”  CIENCIAMAN: Nuestros amigos animales nos ayudan y les ayudamos. Todos colaboramos.NUMBERMAN: Las horas y el reloj condicionan nustros día a día. ¿Sabes mirar un reloj?LECTOWOMAN: Nos enseñará lo que son las palabras antónimas y sinónimas.  type=applicationxshockwaveflash width=200 src=http:vhssd.oddcast.comvhss_editorsvoki_player.swf?doc=http%3A%2F%2Fvhssd.oddcast.com%2Fphp%2Fvhss_editors%2Fgetvoki%2Fchsm=b289922d9e89896bec3c289410144145%26sc=10044243 quality=high allowScriptAccess=always allowNetworking=all wmode=transparent allowFullScreen=true pluginspage=http:www.adobe.comshockwavedownloaddownload.cgi?P1_Prod_Version=ShockwaveFlash name=widget_nameandgt;\"",
                "\"http://datos.gob.es/catalogo/a16003011-conciliaciones-individuales-resueltas-en-la-c-a-de-euskadi1,Banakako adiskidetze bukatuak Euskal AEn .,http://datos.gob.es/recurso/sector-publico/org/Organismo/A16003011,Pais-Vasco,empleo,20170202,20170202, ,\"\"Este apartado ofrece información sobre el mercado laboral desde una perspectiva global, aportando información cuantificable tanto sobre los factores productivos  nivel de instrucción o de sindicación de la población ocupada,  tipo de sociedad mercantil o volumen de empleo por sector  de empresas de producción  como sobre las relaciones de producción  conflictividad laboral, conciliaciones laborales, expedientes de regulación de empleo, huelgas . La información procede de diversas fuentes oficiales tales como departamentos del Gobierno Vasco y ministerios del Gobierno del Estado.\"\"\";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;",
                "\"http://datos.gob.es/catalogo/a02002834-recurso-educativo-primaria-comunes-primer-ciclo-ud13-patrullando-el-barrio-el-poder-de-mis-pasos,Recurso Educativo  Primaria Comunes Primer Ciclo  UD13Patrullando el barrio.El poder de mis pasos.,http://datos.gob.es/recurso/sector-publico/org/Organismo/A02002834,Aragon,educacion,20161124,20170930,Educación Primaria Primer,Las buenas temperatiras hace que pasemos más tiempo fuera de casa, es hora de conocer con profundidad lo que me rodea. Mi localidad y barrio forman parte de mi día a día. Además en esta unidad didáctica nos centraremos de manera globalizada sobre conocer e interactuar con: Matemáticas:Las tablas de multiplicar del 1 al 5. Lengua: El verbo. Para ello nos adentraremos en el Planeta SABER, donde NAyMA, junto con su inseparable patrulla APRENDER CIENCIAMAN:Mi barrio y localidad necesito conocer para adaptarme al medio más cercano. NUMBERMAN:Las multiplicaciones no tendrán nungún misterio a partir de ahora. LECTOWOMAN:Con el verbo podremos explicar nuestras acciones diarias.\\\"\\\"\\\";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\""
        });

        lines.forEach(string -> {
            final Pattern pattern = Pattern.compile(regex);
            final Matcher matcher = pattern.matcher(string);

            while (matcher.find()) {
                System.out.println("Full match: " + matcher.group(0));
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    System.out.println("Group " + i + ": " + matcher.group(i));
                }
            }
        });
    }

    @Test
    public void validateFile() throws FileNotFoundException {
        //
//        CSVCorpusReader corpusReader = new CSVCorpusReader("(.*)\"+separator+\"(.*)\"+separator+\"(.*)");
        CSVCorpusReader corpusReader = new CSVCorpusReader("(http.*),(.*),(http.*),(.*),(.*),([0-9].*),([0-9].*)");
        InstanceList topicModel = corpusReader.validate(inputFilename,7,5,1);
    }

}
