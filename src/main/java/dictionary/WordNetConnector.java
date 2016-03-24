package dictionary;

import edu.mit.jwi.*;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.item.*;
import org.omg.PortableServer.LIFESPAN_POLICY_ID;
import util.Word;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by artem on 06.03.16.
 */
public class WordNetConnector {
    private final String WNAddress = "/home/artem/Documents/Additional/WordNet-3.0/dict";

    private IDictionary dictionary;

    public WordNetConnector() {
        URL url = null;

        try {
            url = new URL("file", null, WNAddress);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        if (url == null) return;

        // construct the dictionary object and open it
        dictionary = new edu.mit.jwi.Dictionary(url);

        try {
            dictionary.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getPeriphery(Word word) {
        POS pos = partOfSpeechConverter(word);
        IWord iWord;
        List<IWord> words;
        Set<String> periphery = new TreeSet<>();
        List<ISynsetID> synsetIDs;

        try {
            IIndexWord idxWord = dictionary.getIndexWord(word.getWord(), pos);
            IWordID wordID = idxWord.getWordIDs().get(0);

            iWord = dictionary.getWord(wordID);
            periphery.add(iWord.getLemma());

            words = iWord.getSynset().getWords();
            periphery.addAll(words.stream().map(IWord::getLemma).collect(Collectors.toList()));

            synsetIDs = iWord.getSynset().getRelatedSynsets();

            for (ISynsetID synsetID : synsetIDs) {
                words = dictionary.getSynset(synsetID).getWords();
                periphery.addAll(words.stream().map(IWord::getLemma).collect(Collectors.toList()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>(periphery);
    }

    private POS partOfSpeechConverter(Word word) {
        POS pos = null;

        if (word.isAdjective())
            pos = POS.ADJECTIVE;
        else if (word.isVerb())
            pos = POS.VERB;
        else if (word.isAdverb())
            pos = POS.ADVERB;
        else if (word.isNoun())
            pos = POS.NOUN;

        return pos;
    }

}
