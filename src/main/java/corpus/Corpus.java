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

    private StanfordCoreNLP pipeline;

    private List<Word> words;
    private Map<Word, Word> uniqueWords;

    private List<Document> positiveDocuments;
    private List<Document> negativeDocuments;

    private Set<String> specialWords;

    public Corpus() {
        Properties props = new Properties();
        //props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");
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

        positiveDocuments = new ArrayList<>();
        negativeDocuments = new ArrayList<>();

        positiveDocuments.addAll(loadDocuments(posFile, DocumentOpinion.POSITIVE));
        negativeDocuments.addAll(loadDocuments(negFile, DocumentOpinion.NEGATIVE));

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

    public List<Document> getDocuments() {
        List<Document> allDocuments = new ArrayList<>();
        allDocuments.addAll(positiveDocuments);
        allDocuments.addAll(negativeDocuments);

        return allDocuments;
    }

    public List<Document> getPositiveDocuments() {
        return this.positiveDocuments;
    }

    public List<Document> getNegativeDocuments() {
        return this.negativeDocuments;
    }

    public int getPositiveDocsNumber() { return this.positiveDocuments.size(); }

    public int getNegativeDocsNumber() { return this.negativeDocuments.size(); }

    public void removeSpecialWords(SpecialWordType... specialWordsType) {
        specialWords = new HashSet<>();

        if (specialWordsType.length == 0) {
            specialWords.addAll(loadAdditionalFile(Utils.stopWordsFileAddress));
            specialWords.addAll(loadAdditionalFile(Utils.kernelFileAddress));
            specialWords.addAll(loadAdditionalFile(Utils.peripheryFileAddress));
        } else {
            for (SpecialWordType type : specialWordsType) {
                if (type.equals(SpecialWordType.STOP_WORD))
                    specialWords.addAll(loadAdditionalFile(Utils.stopWordsFileAddress));
                else if (type.equals(SpecialWordType.KERNEL_WORD))
                    specialWords.addAll(loadAdditionalFile(Utils.kernelFileAddress));
                else if (type.equals(SpecialWordType.PERIPHERY_WORD))
                    specialWords.addAll(loadAdditionalFile(Utils.peripheryFileAddress));
            }
        }

        List<Word> result = new ArrayList<>();

        for (Document document : positiveDocuments) {
            document.setSpecialWords(specialWords);
            document.removeSpecialWords(specialWordsType);
        }

        for (Document document : negativeDocuments) {
            document.setSpecialWords(specialWords);
            document.removeSpecialWords(specialWordsType);
        }

        /* easier to remove special words here than recollecting all words from documents
           after cleaning each of them
         */
        for (Word word : words) {
            if (!specialWords.contains(word.getWord())) {
                result.add(word);
            }
        }

        words.clear();
        words = result;
    }

    public void removeInsignificantWords(int threshold) {
        List<Word> result = new ArrayList<>();
        Double averageFrequency = 0.0;

        for (Word word : words) {
            averageFrequency += word.getTotalFrequency();
        }

        averageFrequency /= words.size();

        for (Word word : words) {
            if (word.getTotalFrequency() > threshold)
                result.add(word);
        }

        words.clear();
        words = result;
    }

    public Set<String> getSpecialWords() { return this.specialWords; }

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

        positiveDocuments.forEach(this::extractWordsFromDoc);
        negativeDocuments.forEach(this::extractWordsFromDoc);

        words = new ArrayList<>(uniqueWords.values());
        Collections.sort(words);
        words = removeDuplicates(words);

        return words;
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

    private void extractWordsFromDoc(Document document) {
        for (Word word : document.getWords()) {
            if (uniqueWords.containsKey(word)) {
                Word temp = uniqueWords.get(word);

                if (temp.canMerge(word))
                    temp.merge(word);

                uniqueWords.put(word, temp);
            } else {
                uniqueWords.put(word, word);
            }

        }
    }

    private List<Document> loadDocuments(File file, DocumentOpinion opinion) {
        try {
            List<Document> documents = new ArrayList<>();
            InputStream in = new FileInputStream(file);
            Scanner scan = new Scanner(in);
            StringBuilder text = new StringBuilder();

            while (scan.hasNextLine()) {
                text.append(scan.nextLine());
                text.append("\n");
            }

            String sText = text.toString();
            String[] docsTexts = sText.split("Document Number: [0-9]+");

            for (String dText : docsTexts) {
                if (dText.trim().isEmpty())
                    continue;

                Document document = new Document(pipeline, opinion);
                document.setText(dText);

                documents.add(document);
            }

            return documents;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }
}
