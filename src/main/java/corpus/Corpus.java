package corpus;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import util.POS;
import util.Pair;
import util.Utils;
import util.Word;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created by artem on 28.02.16.
 */
public class Corpus {

    protected StanfordCoreNLP pipeline;

    private List<Word> words;
    private Map<Word, Word> uniqueWords;

    private List<String> positiveDocs;
    private List<String> negativeDocs;

    private int positiveDocsNumber;
    private  int negativeDocsNumber;


    public Corpus() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        this.pipeline = new StanfordCoreNLP(props);
        words = new ArrayList<>();
    }

    public List<Pair<Word, Word>> extractPatterns(String text) {
        Annotation document = new Annotation(text);
        List<Pair<Word, Word>> patterns = new ArrayList<>();
        Word word1, word2;

        this.pipeline.annotate(document);

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        List<CoreLabel> tokens;
        CoreLabel token1, token2;

        for(CoreMap sentence: sentences) {
            tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            for (int i = 0; i < tokens.size() - 1; ++i) {
                token1 = tokens.get(i);
                token2 = tokens.get(i + 1);

                if (!matchesPattern(token1, token2))
                    continue;

                word1 = new Word(token1);
                word2 = new Word(token2);

                patterns.add(new Pair<>(word1, word2));
            }
        }

        return patterns;
    }

    public void loadCorpus(String positiveFileAddress, String negativeFileAddress) {
        File posFile = new File(positiveFileAddress);
        File negFile = new File(negativeFileAddress);

        positiveDocs = loadDocuments(posFile);
        negativeDocs = loadDocuments(negFile);

        positiveDocsNumber = positiveDocs.size();
        negativeDocsNumber = negativeDocs.size();

        words = extractWords();
    }

    public List<Word> getWords(POS... wordsPOS) {
        if (wordsPOS.length == 0)
            return this.words;

        HashSet<POS> pos = new HashSet<>();
        Collections.addAll(pos, wordsPOS);

        List<Word> result = new ArrayList<>();

        for (Word word : words) {
            if (pos.contains(word.getPOS())) {
                result.add(word);
            }
        }

        return result;
    }

    public int getPositiveDocsNumber() { return positiveDocsNumber; }

    public int getNegativeDocsNumber() { return negativeDocsNumber; }

    public String getPositiveDoc(Integer index) { return this.positiveDocs.get(index); }

    public String getNegativeDoc(Integer index) { return this.negativeDocs.get(index); }

    public List<String> getPositiveDocs() {
        return positiveDocs;
    }

    public List<String> getNegativeDocs() {
        return negativeDocs;
    }

    public void markWords_TF_IDF() {
        Double pRate, nRate, temp;

        for (Word word : words) {
            temp = Math.log((negativeDocsNumber * Math.max(1.0, word.getPositiveDocsNumber())) /
                    (positiveDocsNumber * Math.max(1.0, word.getNegativeDocsNumber())));

            pRate = (word.getPositiveFrequency() / Math.max(1.0, positiveDocsNumber)) * temp;
            nRate = (word.getNegativeFrequency() / Math.max(1.0, negativeDocsNumber)) * temp;

            word.set_TF_IDF_Rate(new Pair<>(pRate, nRate));
        }
    }

    public void removeSpecialWords(SpecialWordType... specialWordsType) {
        Set<String> removeWords = new HashSet<>();

        if (specialWordsType.length == 0) {
            removeWords.addAll(loadAdditionalFile(Utils.stopWordsFileAddress));
            removeWords.addAll(loadAdditionalFile(Utils.kernelFileAddress));
            removeWords.addAll(loadAdditionalFile(Utils.peripheryFileAddress));
        } else {
            for (SpecialWordType type : specialWordsType) {
                if (type.equals(SpecialWordType.STOP_WORD))
                    removeWords.addAll(loadAdditionalFile(Utils.stopWordsFileAddress));
                else if (type.equals(SpecialWordType.KERNEL_WORD))
                    removeWords.addAll(loadAdditionalFile(Utils.kernelFileAddress));
                else if (type.equals(SpecialWordType.PERIPHERY_WORD))
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

    public void removeInsignificantWords() {
        List<Word> result = new ArrayList<>();
        Double averageFrequency = 0.0;

        for (Word word : words) {
            averageFrequency += word.getTotalFrequency();
        }

        averageFrequency /= words.size();

        for (Word word : words) {
            if (word.getTotalFrequency() >= averageFrequency)
                result.add(word);
        }

        words.clear();
        words = result;
    }

    private boolean matchesPattern(CoreLabel token1, CoreLabel token2) {
        String pos1, pos2;

        pos1 = token1.get(CoreAnnotations.PartOfSpeechAnnotation.class);
        pos2 = token2.get(CoreAnnotations.PartOfSpeechAnnotation.class);

        if (pos1.equals("JJ") && (pos2.equals("NN") || pos2.equals("NNS")))
            return true;
        else if ((pos1.equals("RB") || pos1.equals("RBR") || pos1.equals("RBS")) && pos2.equals("JJ"))
            return true;
        else if (pos1.equals("JJ") && pos2.equals("JJ"))
            return true;
        else if ((pos1.equals("NN") || pos1.equals("NNS")) && pos2.equals("JJ"))
            return true;
        else if ((pos1.equals("RB") || pos1.equals("RBR") || pos1.equals("RBS")) &&
                (pos2.equals("VB") || pos2.equals("VBD") || pos2.equals("VBN") || pos2.equals("VBG")))
            return true;
        else
            return false;
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

        for (int i = 0; i < positiveDocs.size(); ++i) {
            extractWordsFromDoc(positiveDocs.get(i), i, true);
        }

        for (int i = 0; i < negativeDocs.size(); ++i) {
            extractWordsFromDoc(negativeDocs.get(i), i + positiveDocs.size(), false);
        }

        words = new ArrayList<>(uniqueWords.values());
        Collections.sort(words);
        words = removeDuplicates(words);

        return words;
    }

    private void extractWordsFromDoc(String text, Integer docNumber, boolean positive) {
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

                if (positive)
                    temp.addPositiveOccurrence(docNumber);
                else
                    temp.addNegativeOccurrence(docNumber);

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

    private ArrayList<String> loadDocuments(File file) {
        try {
            InputStream in = new FileInputStream(file);
            Scanner scan = new Scanner(in);
            StringBuilder text = new StringBuilder();

            while (scan.hasNextLine()) {
                text.append(scan.nextLine());
                text.append("\n");
            }

            String sText = text.toString();
            String[] docs = sText.split("Document Number: [0-9]+");
            return new ArrayList<>(Arrays.asList(docs));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }
}
