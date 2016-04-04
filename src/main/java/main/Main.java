package main;

import analyzer.SentimentAnalyzer;
import analyzer.WordMarker;
import corpus.Corpus;
import dictionary.Dictionary;
import util.POS;
import util.Utils;
import util.Word;

import java.util.List;

/**
 * Created by artem on 28.02.16.
 */
public class Main {

    private static List<Word> words;

    public static void main(String[] args) {
        Corpus corpus = new Corpus();

        corpus.loadCorpus(Utils.proCorpusAddress, Utils.antiCorpusAddress);

        SentimentAnalyzer analyzer = new SentimentAnalyzer();

        corpus.removeSpecialWords();
        corpus.removeInsignificantWords();
        analyzer.createNewClassifier(corpus, POS.ADJECTIVE);

        /*WordMarker wm = new WordMarker();
        words = wm.markWords(corpus, POS.ADJECTIVE);

        Dictionary dict = new Dictionary(words);
        dict.saveDictionary(Utils.markedAdjectives);*/
    }
}
