package main;

import analyzer.SentimentAnalyzer;
import corpus.Corpus;;

import corpus.Document;
import dictionary.Dictionary;
import analyzer.WordMarker;
import util.Utils;
import util.Word;

import java.util.*;

/**
 * Created by artem on 28.02.16.
 */
public class Main {

    private static List<Word> words;

    public static void main(String[] args) {
        Corpus corpus = new Corpus();

        corpus.loadCorpus(Utils.proCorpusAddress, Utils.antiCorpusAddress);

        SentimentAnalyzer analyzer = new SentimentAnalyzer();
       /* analyzer.setPosTypes(Word.ADJECTIVE, Word.ADVERB);

        List<String> poss = corpus.getPositiveDocs();
        List<String> negs = corpus.getNegativeDocs();

        int spliterator = (int)(poss.size() * Utils.TRAINING_DATA_SIZE);

        poss = poss.subList(spliterator, poss.size());
        negs = negs.subList(spliterator, negs.size());

        double accuracy = analyzer.testClassifier(poss, negs);

        System.out.println(accuracy);*/

        corpus.removeSpecialWords();
        corpus.removeInsignificantWords();
        analyzer.createNewClassifier(corpus, Word.ADJECTIVE);
    }
}
