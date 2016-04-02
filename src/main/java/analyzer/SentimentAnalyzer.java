package analyzer;

import corpus.Corpus;
import corpus.Document;
import learning.TrainingExample;
import perceptron.Perceptron;
import perceptron.PerceptronTrainer;
import perceptron.RPPerceptron;
import util.POS;
import util.Pair;
import util.Utils;
import util.Word;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by artem95 on 30.03.16.
 */
public class SentimentAnalyzer {
    private Perceptron perceptron;
    private List<Double> weights;
    private Map<Word, Integer> indexes;

    private int inputSize;

    private POS[] posTypes;

    public SentimentAnalyzer() {
        weights = loadWeights();
        indexes = loadIndexes();

        inputSize = indexes.size();

        if (weights.size() == 0) {
            perceptron = null;
        } else {
            perceptron = new RPPerceptron(weights, Utils.BIAS);
        }
    }

    public void setPosTypes(POS... posTypes) {
        this.posTypes = posTypes.clone();
    }

    public boolean getOpinion(Document document) {
        if (perceptron == null || weights.size() == 0 || indexes.size() == 0)
            throw new NullPointerException("You should create classifier first");

        List<Double> input = getInputs(document);
        return perceptron.classify(input);
    }

    public void createNewClassifier(Corpus corpus, POS... wordsPOS) {
        indexes.clear();
        weights.clear();
        inputSize = 0;

        posTypes = wordsPOS.clone();

        List<Word> words = corpus.getWords(wordsPOS);

        indexes = setIndexes(words);

        inputSize = indexes.size();

        Pair<List<String>, List<String>> posDocs = splitData(corpus.getPositiveDocs());
        Pair<List<String>, List<String>> negDocs = splitData(corpus.getNegativeDocs());

        weights = trainClassifier(posDocs.getFirst(), negDocs.getFirst());
        //weights = trainClassifier(corpus.getPositiveDocs(), corpus.getNegativeDocs());

        saveWeights();
        saveIndexes();

        System.out.println("Data saved");

        double accuracy = testClassifier(posDocs.getSecond(), negDocs.getSecond());

        System.out.println("Classifier gives " + accuracy + " accuracy");
    }

    private List<Double> trainClassifier(List<String> posDocs, List<String> negDocs) {

        List<TrainingExample<List<Double>, Boolean>> trainingData =
                createTrainingData(posDocs, negDocs);

        inputSize = indexes.size();

        /*perceptron = new EPPerceptron(inputSize);
        perceptron.learn(trainingData, Utils.LEARNING_ACCURACY);

        return perceptron.getWeightsVector();*/

        PerceptronTrainer trainer = new PerceptronTrainer();

        trainer.enableLogging();
        trainer.setLoggingFrequency(Utils.LOGGING_FREQUENCY);
        trainer.getLearningRule().setMinAccuracy(Utils.LEARNING_ACCURACY);
        trainer.setLearningSet(trainingData);

        Perceptron p = new RPPerceptron(inputSize);

        trainer.train(p);

        return p.getWeightsVector();
    }

    public double testClassifier(List<String> posDocs, List<String> negDocs) {
        Random random = new Random();
        boolean positivity;
        int correctAnswers = 0;
        int index;
        Document document = new Document();

        for (int i = 0; i < Utils.TESTING_ITERATIONS; ++i) {
            positivity = random.nextBoolean();

            if (positivity) {
                index = random.nextInt(posDocs.size());
                document.setText(posDocs.get(index));
            } else {
                index = random.nextInt(negDocs.size());
                document.setText(negDocs.get(index));
            }

            if (getOpinion(document) == positivity)
                ++correctAnswers;
        }

        return (double)correctAnswers / Utils.TESTING_ITERATIONS;
    }

    private HashMap<Word, Integer> setIndexes(List<Word> words) {
        HashMap<Word, Integer> indexes = new HashMap<>();
        Integer index = 0;

        for (Word word : words) {
            indexes.putIfAbsent(word, index++);
        }

        return indexes;
    }

    private List<TrainingExample<List<Double>, Boolean>> createTrainingData(List<String> posDocs, List<String> negDocs) {
        List<TrainingExample<List<Double>, Boolean>> trainingData = new ArrayList<>();

        trainingData.addAll(extractData(posDocs, true));
        trainingData.addAll(extractData(negDocs, false));

        return trainingData;
    }

    private List<TrainingExample<List<Double>, Boolean>> extractData(List<String> documents, Boolean positive) {
        List<TrainingExample<List<Double>, Boolean>> trainingData = new ArrayList<>();
        Document document = new Document();
        TrainingExample<List<Double>, Boolean> example;

        List<Double> zeros = zeroList(inputSize);

        for (String text : documents) {
            document.setText(text);
            document.removeSpecialWords();

            List<Double> input = getInputs(document);

            if (input == null || input.equals(zeros))
                continue;

            example = new TrainingExample<>(input, positive);

            trainingData.add(example);
        }

        return trainingData;
    }

    private List<Double> getInputs(Document document) {
        List<Word> words = document.getWords(posTypes);

        List<Double> input = zeroList(inputSize);
        Integer index;

        for (Word word : words) {
            if (indexes.containsKey(word)) {
                index = indexes.get(word);
                input.set(index, word.getTotalFrequency().doubleValue());
            }
        }

        return input;
    }

    private List<Double> zeroList(int capacity) {
        List<Double> zeros = new ArrayList<>(capacity);

        for (int i = 0; i < capacity; ++i)
            zeros.add(0.0);

        return zeros;
    }

    private List<Double> loadWeights() {
        try {
            List<Double> weights = new ArrayList<>();

            File file = new File(Utils.weightsAddress);
            InputStream inStream = new FileInputStream(file);
            Scanner scan = new Scanner(inStream);

            while (scan.hasNextDouble()) {
                weights.add(scan.nextDouble());
            }

            return weights;

        } catch (IOException ex) {
            ex.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void saveWeights() {
        try {
            List<String> lines = weights.stream().map(Object::toString).collect(Collectors.toList());
            Path file = Paths.get(Utils.weightsAddress);
            Files.write(file, lines, Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<Word, Integer> loadIndexes() {
        try {
            Map<Word, Integer> indexes = new HashMap<>();

            File file = new File(Utils.indexesAddress);
            InputStream inStream = new FileInputStream(file);
            Scanner scan = new Scanner(inStream);

            while (scan.hasNextLine()) {
                String line = scan.nextLine();
                String[] info = line.split("#");

                if (info.length == 2) {
                    Word word = Word.parseWord(info[0]);
                    Integer index = Integer.parseInt(info[1]);

                    indexes.put(word, index);
                }
            }

            return indexes;

        } catch (IOException ex) {
            ex.printStackTrace();
            return new HashMap<>();
        }
    }

    private void saveIndexes() {
        try {
            List<String> lines = indexes.keySet().stream().map(word -> word + " #" +
                    indexes.get(word)).collect(Collectors.toList());
            Path file = Paths.get(Utils.indexesAddress);
            Files.write(file, lines, Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Pair<List<String>, List<String>> splitData(List<String> documents) {
        List<String> trainingData = new ArrayList<>();
        List<String> testingData = new ArrayList<>();

        int spliterator = (int)(Utils.TRAINING_DATA_SIZE * documents.size());

        for (int i = 0; i < spliterator; ++i) {
            trainingData.add(documents.get(i));
        }

        for (int i = spliterator; i < documents.size(); ++i) {
            testingData.add(documents.get(i));
        }

        return new Pair<>(trainingData, testingData);
    }
}
