package main;

import classifiers.NaiveBayes;
import corpus.Corpus;
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

        NaiveBayes nb;

        for (int i = 0; i < 5; ++i) {
            System.out.println("\nCross validation. Classifier #" + i + '\n');
            nb = new NaiveBayes();
            nb.learn(corpus, i);
        }
    }
}
