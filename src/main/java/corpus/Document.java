package corpus;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import util.Pair;
import util.Utils;
import util.Word;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created by artem95 on 24.03.16.
 */
public class Document {
    protected StanfordCoreNLP pipeline;

    private List<Word> words;
    private Map<Word, Word> uniqueWords;

    private String text;

    public Document() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        this.pipeline = new StanfordCoreNLP(props);
        words = new ArrayList<>();
    }

    public Document(String text) {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        this.pipeline = new StanfordCoreNLP(props);
        words = new ArrayList<>();

        this.text = text;
        words = extractWords();
    }

    public void setText(String text) {
        this.text = text;

        words.clear();
        words = extractWords();
    }

    public void loadDocument(String fileAddress) {
        File file = new File(fileAddress);
        text = loadText(file);

        words = extractWords();
    }

    public List<Word> getWords(Integer... wordsPOS) {
        if (wordsPOS == null || wordsPOS.length == 0)
            return this.words;

        HashSet<Integer> pos = new HashSet<>();
        Collections.addAll(pos, wordsPOS);

        if (pos.contains(Word.ALL_PARTS_OF_SPEECH))
            return this.words;

        List<Word> result = new ArrayList<>();

        for (Word word : words) {
            if (pos.contains(Word.ADJECTIVE) && word.isAdjective())
                result.add(word);
            else if (pos.contains(Word.ADVERB) && word.isAdverb())
                result.add(word);
            else if (pos.contains(Word.NOUN) && word.isNoun())
                result.add(word);
            else if (pos.contains(Word.VERB) && word.isVerb())
                result.add(word);
        }

        return result;
    }

    public void removeSpecialWords(Integer... specialWordsType) {
        Set<String> removeWords = new HashSet<>();

        if (specialWordsType.length == 0) {
            removeWords.addAll(loadAdditionalFile(Utils.stopWordsFileAddress));
            removeWords.addAll(loadAdditionalFile(Utils.kernelFileAddress));
            removeWords.addAll(loadAdditionalFile(Utils.peripheryFileAddress));
        } else {
            for (Integer type : specialWordsType) {
                if (type.equals(Texts.STOP_WORDS))
                    removeWords.addAll(loadAdditionalFile(Utils.stopWordsFileAddress));
                else if (type.equals(Texts.KERNEL))
                    removeWords.addAll(loadAdditionalFile(Utils.kernelFileAddress));
                else if (type.equals(Texts.PERIPHERY))
                    removeWords.addAll(loadAdditionalFile(Utils.peripheryFileAddress));
            }
        }

        List<Word> result = new ArrayList<>();

        for (Word word : words) {
            if (!removeWords.contains(word.getWord())) {
                result.add(word);
            }
        }

        words.clear();
        words = result;
    }

    private Set<String> loadAdditionalFile(String fileAddress) {
        Set<String> words = new HashSet<>();

        try {
            File file = new File(fileAddress);
            InputStream in = new FileInputStream(file);
            Scanner scan = new Scanner(in);

            while (scan.hasNextLine()) {
                words.add(scan.nextLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return words;
    }

    private List<Word> extractWords() {
        List<Word> words;
        uniqueWords = new HashMap<>();

        extractWordsFromText(text);

        words = new ArrayList<>(uniqueWords.values());
        Collections.sort(words);
        words = removeDuplicates(words);

        return words;
    }

    private void extractWordsFromText(String text) {
        Annotation document = new Annotation(text);
        Word word, temp;

        this.pipeline.annotate(document);

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        for (CoreMap sentence : sentences) {
            for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                word = new Word(token);

                if (!word.isWord())
                    continue;

                uniqueWords.putIfAbsent(word, word);

                temp = uniqueWords.get(word);
                temp.addNeutralOccurrence();

                uniqueWords.put(word, temp);
            }
        }
    }

    private List<Word> removeDuplicates(List<Word> words) {
        Word word1, word2;
        List<Word> cleanedList = new ArrayList<>();

        for (int i = 0; i < words.size(); ++i) {
            word1 = words.get(i);

            for (int j = i + 1; j < words.size(); ++j) {
                word2 = words.get(j);

                if (word1.canMerge(word2)) {
                    word1.merge(word2);
                } else {
                    i = j - 1;
                    break;
                }
            }

            cleanedList.add(word1);
        }

        return cleanedList;
    }

    private String loadText(File file) {
        try {
            InputStream in = new FileInputStream(file);
            Scanner scan = new Scanner(in);
            StringBuilder text = new StringBuilder();

            while (scan.hasNextLine()) {
                text.append(scan.nextLine());
                text.append("\n");
            }

            return text.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }
}