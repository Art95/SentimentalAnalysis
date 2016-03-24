package analyzer;

import corpus.CorpusAnalyzer;;

import util.Utils;
import util.Word;

import java.util.*;

/**
 * Created by artem on 28.02.16.
 */
public class SentimentalAnalyzer {

    private static List<Word> words;

    public static void main(String[] args) {
        CorpusAnalyzer corpus = new CorpusAnalyzer();

        corpus.loadCorpus(Utils.proCorpusAddress, Utils.antiCorpusAddress);

        corpus.removeStopWords();
        corpus.removeInsignificantWords();
        corpus.markWords_TF_IDF();

        words = corpus.getWords();

        words.sort((o1, o2) -> o2.getTotalFrequency() - o1.getTotalFrequency());

        for (Word word : words) {
            System.out.println(word + "\t" + word.getTotalFrequency());
        }

    }
}
