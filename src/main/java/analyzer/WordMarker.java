package analyzer;

import corpus.Corpus;
import corpus.Document;
import corpus.DocumentOpinion;
import learning.TrainingExample;
import perceptron.Perceptron;
import perceptron.PerceptronTrainer;
import perceptron.RPPerceptron;
import util.POS;
import util.Utils;
import util.Word;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by artem95 on 24.03.16.
 */
public class WordMarker {
    private HashMap<Word, Integer> indexes;
    private PerceptronTrainer trainer;

    private int inputSize;

    private POS[] posTypes;

    private Corpus corpus;

    public WordMarker() {
        indexes = new HashMap<>();
        trainer = new PerceptronTrainer();
    }

    public List<Word> markWords(Corpus corpus, POS... wordsPOS) {
        this.corpus = corpus;
        posTypes = wordsPOS.clone();

        List<Word> words = corpus.getWords(wordsPOS);
        indexes = setIndexes(words);

        List<Double> marks = getWordsMarks();
        setMarks(words, marks);

        return words;
    }

    private void setMarks(List<Word> words, List<Double> marks) {
        Integer index;

        for (Word word : words) {
            index = indexes.get(word);
            word.setMark(marks.get(index));
        }
    }

    private List<Double> getWordsMarks() {
        List<Double> initialWeights = getInitialWeights();
        List<TrainingExample<List<Double>, Boolean>> trainingData = createTrainingData();

        trainer.enableLogging();
        trainer.setLoggingFrequency(Utils.LOGGING_FREQUENCY);

        trainer.getLearningRule().setMinAccuracy(Utils.LEARNING_ACCURACY);

        trainer.setLearningSet(trainingData);

        Perceptron p = new RPPerceptron(initialWeights, Utils.BIAS);

        trainer.train(p);

        return p.getWeightsVector();

        /*List<Double> initialWeights = getInitialWeights();
        List<TrainingExample<List<Double>, Boolean>> trainingData = createTrainingData(corpus);

        Perceptron perceptron = new EPPerceptron(initialWeights, Utils.BIAS);
        perceptron.learn(trainingData, Utils.LEARNING_ACCURACY);

        List<Double> marks = perceptron.getWeightsVector();

        return marks;*/
    }

    private HashMap<Word, Integer> setIndexes(List<Word> words) {
        HashMap<Word, Integer> indexes = new HashMap<>();
        Integer index = 0;

        for (Word word : words) {
            indexes.putIfAbsent(word, index++);
            ++inputSize;
        }

        return indexes;
    }

    private  List<Double> getInitialWeights() {
        List<Double> init_weights = zeroList(inputSize);
        Double weight;

        for (Word word : indexes.keySet()) {
            weight = (word.get_TF_IDF_Rate().getFirst() + word.get_TF_IDF_Rate().getSecond()) / 2;
            init_weights.set(indexes.get(word), weight);
        }

        return init_weights;
    }

    private List<TrainingExample<List<Double>, Boolean>> createTrainingData() {
        List<TrainingExample<List<Double>, Boolean>> trainingData = new ArrayList<>();
        List<Double> input;
        TrainingExample<List<Double>, Boolean> example;
        boolean positive;

        List<Double> zeros = zeroList(inputSize);

        for (Document document : corpus.getDocuments()) {

            input = getInputs(document);

            if (input == null || input.equals(zeros))
                continue;

            positive = document.getOpinion().equals(DocumentOpinion.POSITIVE);

            example = new TrainingExample<>(input, positive);

            trainingData.add(example);
        }

        return trainingData;
    }

    private List<Double> getInputs(Document document) {
        List<Word> words = document.getWords(posTypes);

        if (words.size() == 0)
            return null;

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
}
