package corpus;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;
import util.POS;
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
    private StanfordCoreNLP pipeline;

    private List<Word> words;
    private Map<Word, Word> uniqueWords;

    private Set<String> removeWords;

    private String text;

    private DocumentOpinion opinion;

    private List<SemanticGraph> semanticGraphs;

    public Document() {
        Properties props = new Properties();
        //props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        this.pipeline = new StanfordCoreNLP(props);

        words = new ArrayList<>();
        semanticGraphs = new ArrayList<>();
        uniqueWords = new HashMap<>();

        opinion = DocumentOpinion.UNKNOWN;
    }

    public Document(StanfordCoreNLP pipeline) {
        this.pipeline = pipeline;

        words = new ArrayList<>();
        semanticGraphs = new ArrayList<>();
        uniqueWords = new HashMap<>();

        opinion = DocumentOpinion.UNKNOWN;
    }

    public Document(StanfordCoreNLP pipeline, DocumentOpinion opinion) {
        this.pipeline = pipeline;

        words = new ArrayList<>();
        semanticGraphs = new ArrayList<>();
        uniqueWords = new HashMap<>();

        this.opinion = opinion;
    }

    public void setText(String text) {
        this.text = text;

        words.clear();
        semanticGraphs.clear();
        uniqueWords.clear();

        parseText(text);

        words = extractWords();
    }

    public void setOpinion(DocumentOpinion opinion) { this.opinion = opinion; }

    public DocumentOpinion getOpinion() { return this.opinion; }

    public void loadDocument(String fileAddress) {
        File file = new File(fileAddress);
        text = loadText(file);

        parseText(text);

        words = extractWords();
    }

    public List<Word> getWords(POS... wordsPOS) {
        if (wordsPOS == null || wordsPOS.length == 0)
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

    public void removeSpecialWords(SpecialWordType... specialWordsType) {
        if (removeWords == null || removeWords.size() == 0) {

            removeWords = new HashSet<>();

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

    public void setSpecialWords(Set<String> specialWords) { this.removeWords = specialWords; }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Document)) return false;

        Document document = (Document) o;

        if (words != null ? !words.equals(document.words) : document.words != null) return false;
        if (text != null ? !text.equals(document.text) : document.text != null) return false;
        return opinion == document.opinion;

    }

    @Override
    public int hashCode() {
        int result = text != null ? text.hashCode() : 0;
        return result;
    }

    public Map<String, List<Word>> getConnectedWords(SpecialWordType type) {
        Map<String, List<Word>> result = new HashMap<>();
        Set<String> kernel;

        if (type == SpecialWordType.KERNEL_WORD)
            kernel = loadAdditionalFile(Utils.kernelFileAddress);
        else if  (type == SpecialWordType.PERIPHERY_WORD)
            kernel = loadAdditionalFile(Utils.peripheryFileAddress);
        else
            kernel = new HashSet<>();

        List<Word> words;

        for (SemanticGraph graph : semanticGraphs) {
            for (String kernelWord : kernel) {
                words = new ArrayList<>();
                List<IndexedWord> kernelIndexedWords = graph.getAllNodesByWordPattern(kernelWord);

                for (IndexedWord iWord: kernelIndexedWords) {
                    List<SemanticGraphEdge> inEdges = graph.getIncomingEdgesSorted(iWord);
                    List<SemanticGraphEdge> outEdges = graph.getOutEdgesSorted(iWord);

                    for (SemanticGraphEdge edge : inEdges) {
                        Word connectedWord = new Word(edge.getSource().backingLabel());

                        if (!kernel.contains(connectedWord.getWord()))
                            words.add(connectedWord);
                    }

                    for (SemanticGraphEdge edge : outEdges) {
                        Word connectedWord = new Word(edge.getTarget().backingLabel());

                        if (!kernel.contains(connectedWord.getWord()))
                            words.add(connectedWord);
                    }

                }

                if (!words.isEmpty()) {
                    if (result.containsKey(kernelWord))
                        result.get(kernelWord).addAll(words);
                    else
                        result.put(kernelWord, words);
                }
            }
        }

        return result;
    }

    private List<Word> extractWords() {
        List<Word> words = new ArrayList<>(uniqueWords.values());
        Collections.sort(words);
        words = removeDuplicates(words);

        return words;
    }

    private void parseText(String text) {
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
                temp.addOccurrence(this);

                uniqueWords.put(word, temp);
            }

            /*SemanticGraph dependencies =
                    sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);

            semanticGraphs.add(dependencies);*/
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
