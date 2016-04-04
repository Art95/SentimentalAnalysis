package util;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;

import java.util.*;

/**
 * Created by artem on 28.02.16.
 */
public class Word implements Comparable<Word> {
    private static final int meaningful_information_length = 3;

    private String word;
    private POS pos;

    private Integer positiveFrequency;
    private Integer negativeFrequency;
    private Integer totalFrequency;

    private HashMap<Integer, Integer> frequencyPerDoc;

    private Set<Integer> positiveDocs;
    private Set<Integer> negativeDocs;

    private Pair<Double, Double> rate_TF_IDF;

    private Double mark;


    public Word() {
        word = null;
        pos = null;

        positiveFrequency = 0;
        negativeFrequency = 0;
        totalFrequency = 0;

        frequencyPerDoc = new HashMap<>();
        positiveDocs = new HashSet<>();
        negativeDocs = new HashSet<>();

        rate_TF_IDF = new Pair<>(0.0, 0.0);
        mark = 0.0;
    }

    public Word (CoreLabel token) {
        word = token.get(CoreAnnotations.LemmaAnnotation.class).trim().toLowerCase();
        pos = POS.fromString(token.get(CoreAnnotations.PartOfSpeechAnnotation.class));

        positiveFrequency = 0;
        negativeFrequency = 0;
        totalFrequency = 0;

        frequencyPerDoc = new HashMap<>();
        positiveDocs = new HashSet<>();
        negativeDocs = new HashSet<>();

        rate_TF_IDF = new Pair<>(0.0, 0.0);
        mark = 0.0;
    }

    @Override
    public int hashCode() {
        String temp = word + pos;
        return temp.hashCode();
    }

    public int compareTo(Word anotherWord) {
        return this.word.compareTo(anotherWord.getWord());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Word word1 = (Word) o;

        if (word != null ? !word.equals(word1.word) : word1.word != null) return false;
        return pos != null ? pos.equals(word1.pos) : word1.pos == null;

    }

    public int getPositiveDocsNumber() { return this.positiveDocs.size(); }

    public int getNegativeDocsNumber() { return this.negativeDocs.size(); }

    public String getWord() {
        return this.word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public POS getPOS() {
        return this.pos;
    }

    public Integer getFrequencyInDoc(Integer docIndex) {
        return frequencyPerDoc.get(docIndex);
    }

    public Double getMark() { return mark; }

    public void setMark(Double mark) { this.mark = mark; }

    public void setPOS(POS pos) {
        this.pos = pos;
    }

    public Integer getTotalFrequency() { return this.totalFrequency; }

    public Integer getPositiveFrequency() {
        if (positiveFrequency == 0 && totalFrequency != 0)
            return (int)Math.ceil(totalFrequency / 2);

        return this.positiveFrequency;
    }

    public Integer getNegativeFrequency() {
        if (negativeFrequency == 0 && totalFrequency != 0)
            return totalFrequency / 2;

        return this.negativeFrequency;
    }

    public void addPositiveOccurrence(Integer docNumber) {
        addOccurrence(docNumber);
        positiveDocs.add(docNumber);
        ++positiveFrequency;
        ++totalFrequency;
    }

    public void addNegativeOccurrence(Integer docNumber) {
        addOccurrence(docNumber);
        negativeDocs.add(docNumber);
        ++negativeFrequency;
        ++totalFrequency;
    }

    public void addNeutralOccurrence() {
        ++totalFrequency;
    }

    public void set_TF_IDF_Rate(Pair<Double, Double> rate) {
        this.rate_TF_IDF = rate;
    }

    public Pair<Double, Double> get_TF_IDF_Rate() {
        return this.rate_TF_IDF;
    }

    public String toString() {
        StringBuilder text = new StringBuilder();
        Formatter format = new Formatter(text, Locale.US);

        format.format("%1$-25s\t%2$4s\t%3$25s", word, pos, mark);

        return format.toString();
    }

    public static Word parseWord(String string) {
        String[] values = string.split("\t");

        if (values.length < meaningful_information_length)
            return null;

        try {
            String _word = values[0].trim();
            String _pos = values[1].trim();

            Double mark = Double.parseDouble(values[2].trim());

            Word word = new Word();
            word.setWord(_word);
            word.setPOS(POS.fromString(_pos));
            word.setMark(mark);

            return word;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isAdjective() {
        return pos == POS.ADJECTIVE;
    }

    public boolean isAdverb() {
        return pos == POS.ADVERB;
    }

    public boolean isVerb() {
        return pos == POS.VERB;
    }

    public boolean isNoun() {
        return pos == POS.NOUN;
    }

    public boolean isWord() {
        return pos != POS.UNKNOWN;
    }

    public boolean hasSamePOS(Word anotherWord) {
        return this.getPOS() == anotherWord.getPOS();
    }

    public boolean canMerge(Word anotherWord) {
        return this.word.equals(anotherWord.word) && this.hasSamePOS(anotherWord);
    }

    public void merge(Word anotherWord) {
        this.positiveFrequency += anotherWord.positiveFrequency;
        this.negativeFrequency += anotherWord.negativeFrequency;

        for (Integer key : anotherWord.frequencyPerDoc.keySet()) {
            if (this.frequencyPerDoc.containsKey(key)) {
                Integer freq = this.frequencyPerDoc.get(key);
                this.frequencyPerDoc.put(key, freq + anotherWord.frequencyPerDoc.get(key));
            } else
                this.frequencyPerDoc.put(key, anotherWord.frequencyPerDoc.get(key));
        }

        this.positiveDocs.addAll(anotherWord.positiveDocs);
        this.negativeDocs.addAll(anotherWord.negativeDocs);
    }

    private void addOccurrence(Integer docNumber) {
        this.frequencyPerDoc.putIfAbsent(docNumber, 0);
        Integer freq = this.frequencyPerDoc.get(docNumber);
        this.frequencyPerDoc.put(docNumber, ++freq);
    }
}
