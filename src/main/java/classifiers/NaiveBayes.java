package classifiers;

import corpus.Corpus;
import corpus.Document;
import corpus.DocumentOpinion;
import corpus.SpecialWordType;
import util.Word;

import java.util.*;

/**
 * Created by artem95 on 19.05.16.
 */
public class NaiveBayes {
    private Map<Word, Double> positiveMultinomialProbabilities;
    private Map<Word, Double> negativeMultinomialProbabilities;

    private Map<Word, Double> positiveBinaryProbabilities;
    private Map<Word, Double> negativeBinaryProbabilities;

    private double positiveDocumentProbability;
    private double negativeDocumentProbability;

    private static final String UNKNOWN_WORD = "UNKNOWN_WORD";

    public NaiveBayes() {
        this.positiveMultinomialProbabilities = new HashMap<>();
        this.negativeMultinomialProbabilities = new HashMap<>();
        this.positiveBinaryProbabilities = new HashMap<>();
        this.negativeBinaryProbabilities = new HashMap<>();
    }

    public DocumentOpinion classify(Document document, NaiveBayesType type) {
        document.removeSpecialWords(SpecialWordType.STOP_WORD);
        List<Word> words = document.getWords();

        Map<Word, Double> positiveMap;
        Map<Word, Double> negativeMap;

        if (type == NaiveBayesType.MULTINOMIAL) {
            positiveMap = positiveMultinomialProbabilities;
            negativeMap = negativeMultinomialProbabilities;
        } else if (type == NaiveBayesType.BINARY) {
            positiveMap = positiveBinaryProbabilities;
            negativeMap = negativeBinaryProbabilities;
        } else {
            throw new IllegalArgumentException();
        }

        Word unknownWord = new Word();
        unknownWord.setWord(UNKNOWN_WORD);

        double positiveProbability = 0;
        double negativeProbability = 0;

        for (Word word : words) {
            double wordPositiveProbability = (positiveMap.containsKey(word)) ?
                    positiveMap.get(word) : positiveMap.get(unknownWord);
            double wordNegativeProbability = (negativeMap.containsKey(word)) ?
                    negativeMap.get(word) : negativeMap.get(unknownWord);

            positiveProbability += Math.log(wordPositiveProbability);
            negativeProbability += Math.log(wordNegativeProbability);
        }

        positiveProbability += Math.log(positiveDocumentProbability);
        negativeProbability += Math.log(negativeDocumentProbability);

        if (positiveProbability > negativeProbability) {
            return DocumentOpinion.POSITIVE;
        } else {
            return DocumentOpinion.NEGATIVE;
        }
    }

    public void learn(Corpus corpus, int crossValidationShift) {
        corpus.removeSpecialWords(SpecialWordType.STOP_WORD);
        //corpus.removeInsignificantWords();

        List<Document> positiveDocuments = new ArrayList<>(corpus.getPositiveDocuments());
        List<Document> negativeDocuments = new ArrayList<>(corpus.getNegativeDocuments());

        //List<Document> positiveTrainingSet = positiveDocuments.subList(0, (int)(0.8 * positiveDocuments.size()));
        //List<Document> negativeTrainingSet = negativeDocuments.subList(0, (int)(0.8 * negativeDocuments.size()));

        double testingSetStart = 0.2 * crossValidationShift;

        List<Document> positiveTrainingSet = new ArrayList<>();
        List<Document> negativeTrainingSet = new ArrayList<>();
        List<Document> positiveTestingSet = new ArrayList<>();
        List<Document> negativeTestingSet = new ArrayList<>();

        for (int i = 0; i < positiveDocuments.size(); ++i) {
            if (i >= testingSetStart * positiveDocuments.size() &&
                    i < testingSetStart * positiveDocuments.size() + 0.2 * positiveDocuments.size()) {
                positiveTestingSet.add(positiveDocuments.get(i));
            } else {
                positiveTrainingSet.add(positiveDocuments.get(i));
            }
        }

        for (int i = 0; i < negativeDocuments.size(); ++i) {
            if (i >= testingSetStart * negativeDocuments.size() &&
                    i < testingSetStart * negativeDocuments.size() + 0.2 * negativeDocuments.size()) {
                negativeTestingSet.add(negativeDocuments.get(i));
            } else {
                negativeTrainingSet.add(negativeDocuments.get(i));
            }
        }

        System.out.println("\nMultinomial classifier:");
        learn(positiveTrainingSet, negativeTrainingSet, NaiveBayesType.MULTINOMIAL);
        test(positiveTestingSet, negativeTestingSet, NaiveBayesType.MULTINOMIAL);

        System.out.println("\nBinary classifier:");
        learn(positiveTrainingSet, negativeTrainingSet, NaiveBayesType.BINARY);
        test(positiveTestingSet, negativeTestingSet, NaiveBayesType.BINARY);
    }

    private void learn(List<Document> positiveTrainingSet, List<Document> negativeTrainingSet, NaiveBayesType type) {
        System.out.println("Learning...");

        if (type == NaiveBayesType.MULTINOMIAL) {
            this.positiveMultinomialProbabilities.clear();
            this.negativeMultinomialProbabilities.clear();
        } else if (type == NaiveBayesType.BINARY) {
            this.negativeBinaryProbabilities.clear();
            this.positiveBinaryProbabilities.clear();
        }

        double totalNumberOfDocuments = positiveTrainingSet.size() + negativeTrainingSet.size();
        positiveDocumentProbability = positiveTrainingSet.size() / totalNumberOfDocuments;
        negativeDocumentProbability = negativeTrainingSet.size() / totalNumberOfDocuments;

        Map<Word, Integer> positiveWordsFrequencies = getWordsFrequencies(positiveTrainingSet, type);
        Map<Word, Integer> negativeWordsFrequencies = getWordsFrequencies(negativeTrainingSet, type);

        Set<Word> allWords = new HashSet<>();
        allWords.addAll(positiveWordsFrequencies.keySet());
        allWords.addAll(negativeWordsFrequencies.keySet());

        int positiveVocabularySize = positiveWordsFrequencies.size() + 1; // 1 is for UNKNOWN_WORD
        int negativeVocabularySize = negativeWordsFrequencies.size() + 1; // 1 is for UNKNOWN_WORD
        int totalPositiveWordsCount = getTotalNumberOfWordsPerClass(positiveWordsFrequencies);
        int totalNegativeWordsCount = getTotalNumberOfWordsPerClass(negativeWordsFrequencies);

        for (Word word : allWords) {
            double positiveFrequency = (positiveWordsFrequencies.containsKey(word)) ?
                    positiveWordsFrequencies.get(word) : 1;
            double negativeFrequency = (negativeWordsFrequencies.containsKey(word)) ?
                    negativeWordsFrequencies.get(word) : 1;

            double positiveProbability = positiveFrequency / (totalPositiveWordsCount + positiveVocabularySize);
            double negativeProbability = negativeFrequency / (totalNegativeWordsCount + negativeVocabularySize);

            if (type == NaiveBayesType.MULTINOMIAL) {
                positiveMultinomialProbabilities.put(word, positiveProbability);
                negativeMultinomialProbabilities.put(word, negativeProbability);
            } else if (type == NaiveBayesType.BINARY) {
                positiveBinaryProbabilities.put(word, positiveProbability);
                negativeBinaryProbabilities.put(word, negativeProbability);
            }
        }

        Word unknownWord = new Word();
        unknownWord.setWord(UNKNOWN_WORD);

        if (type == NaiveBayesType.MULTINOMIAL) {
            positiveMultinomialProbabilities.put(unknownWord, 1.0 / (totalPositiveWordsCount + positiveVocabularySize));
            negativeMultinomialProbabilities.put(unknownWord, 1.0 / (totalNegativeWordsCount + negativeVocabularySize));
        } else if (type == NaiveBayesType.BINARY) {
            positiveBinaryProbabilities.put(unknownWord, 1.0 / (totalPositiveWordsCount + positiveVocabularySize));
            negativeBinaryProbabilities.put(unknownWord, 1.0 / (totalNegativeWordsCount + negativeVocabularySize));
        }

        System.out.println("Learning finished.");
    }

    public void test(List<Document> positiveTestingSet, List<Document> negativeTestingSet, NaiveBayesType type) {
        System.out.println("Testing...");

        double truePositive = 0;
        double falsePositive = 0;
        double trueNegative = 0;
        double falseNegative = 0;

        for (Document document : positiveTestingSet) {
            DocumentOpinion opinion = classify(document, type);

            if (opinion == DocumentOpinion.POSITIVE) {
                ++truePositive;
            } else {
                ++falseNegative;
            }
        }

        for (Document document : negativeTestingSet) {
            DocumentOpinion opinion = classify(document, type);

            if (opinion == DocumentOpinion.NEGATIVE) {
                ++trueNegative;
            } else {
                ++falsePositive;
            }
        }

        double accuracy = (truePositive + trueNegative) / (truePositive + trueNegative + falsePositive + falseNegative);
        double precision = truePositive / (truePositive + falsePositive);
        double recall = truePositive / (truePositive + falseNegative);
        double f_measure = 2 * precision * recall / (precision + recall);

        System.out.println("\nTesting results:");
        System.out.println("Accuracy: " + accuracy);
        System.out.println("Precision: " + precision);
        System.out.println("Recall: " + recall);
        System.out.println("F-measure: " + f_measure);
    }

    private Map<Word, Integer> getWordsFrequencies(List<Document> documents, NaiveBayesType type) {
        Map<Word, Integer> wordsFrequencies = new HashMap<>();

        for (Document doc : documents) {
            List<Word> words = doc.getWords();

            for (Word word : words) {
                wordsFrequencies.putIfAbsent(word, 0);

                int oldFrequency = wordsFrequencies.get(word);
                int newFrequency;

                if (type == NaiveBayesType.MULTINOMIAL) {
                    newFrequency = oldFrequency + word.getFrequencyInDoc(doc);
                } else if (type == NaiveBayesType.BINARY) {
                    newFrequency = oldFrequency + 1;
                } else {
                    throw new IllegalArgumentException();
                }

                wordsFrequencies.replace(word, newFrequency);
            }
        }

        return wordsFrequencies;
    }

    private int getTotalNumberOfWordsPerClass(Map<Word, Integer> wordsFrequencies) {
        int total = 0;

        for (Word word : wordsFrequencies.keySet()) {
            total += wordsFrequencies.get(word);
        }

        return total;
    }
}
