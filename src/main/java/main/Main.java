package main;

import analyzer.SentimentAnalyzer;
import corpus.Corpus;
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
       /* analyzer.setPosTypes(POS.ADJECTIVE, POS.ADVERB);

        List<String> poss = corpus.getPositiveDocs();
        List<String> negs = corpus.getNegativeDocs();

        int spliterator = (int)(poss.size() * Utils.TRAINING_DATA_SIZE);

        poss = poss.subList(spliterator, poss.size());
        negs = negs.subList(spliterator, negs.size());

        double accuracy = analyzer.testClassifier(poss, negs);

        System.out.println(accuracy);*/

        corpus.removeSpecialWords();
        corpus.removeInsignificantWords();
        analyzer.createNewClassifier(corpus, POS.ADJECTIVE);
    }
}
