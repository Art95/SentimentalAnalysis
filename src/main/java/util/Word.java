package util;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;

import java.util.*;

/**
 * Created by artem on 28.02.16.
 */
public class Word implements Comparable<Word> {
    private String word;
    private String pos;

    private Integer positiveFrequency;
    private Integer negativeFrequency;

    private HashMap<Integer, Integer> frequencyPerDoc;

    private Set<Integer> positiveDocs;
    private Set<Integer> negativeDocs;

    private Pair<Double, Double> rate_TF_IDF;


    public Word() {
        word = null;
        pos = null;
        positiveFrequency = 0;
        negativeFrequency = 0;
        frequencyPerDoc = new HashMap<>();
        positiveDocs = new HashSet<>();
        negativeDocs = new HashSet<>();

        rate_TF_IDF = new Pair<>(0.0, 0.0);
    }

    public Word (CoreLabel token) {
        word = token.get(CoreAnnotations.LemmaAnnotation.class).trim().toLowerCase();
        pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);

        positiveFrequency = 0;
        negativeFrequency = 0;
        frequencyPerDoc = new HashMap<>();
        positiveDocs = new HashSet<>();
        negativeDocs = new HashSet<>();

        rate_TF_IDF = new Pair<>(0.0, 0.0);
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

    public String getPOS() {
        return this.pos;
    }

    public void setPOS(String pos) {
        this.pos = pos;
    }

    public Integer getTotalFrequency() { return this.positiveFrequency + this.negativeFrequency; }

    public Integer getPositiveFrequency() { return this.positiveFrequency; }

    public Integer getNegativeFrequency() { return this.negativeFrequency; }

    public void addPositiveOccurrence(Integer docNumber) {
        addOccurrence(docNumber);
        positiveDocs.add(docNumber);
        ++positiveFrequency;
    }

    public void addNegativeOccurrence(Integer docNumber) {
        addOccurrence(docNumber);
        negativeDocs.add(docNumber);
        ++negativeFrequency;
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

        format.format("%1$-25s\t%2$4s\t%3$25s\t%4$25s", word, pos, rate_TF_IDF.getFirst(),
                rate_TF_IDF.getSecond());

        return format.toString();
    }

    public static Word parseWord(String string) {
        String[] values = string.split("\t");

        if (values.length < 4)
            return null;

        try {
            String _word = values[0].trim();
            String _pos = values[1].trim();

            Double _positiveRate = Double.parseDouble(values[2].trim());
            Double _negativeRate = Double.parseDouble(values[3].trim());

            Pair<Double, Double> _rate = new Pair<>(_positiveRate, _negativeRate);

            Word word = new Word();
            word.setWord(_word);
            word.setPOS(_pos);
            word.set_TF_IDF_Rate(_rate);

            return word;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isAdjective() {
        return pos.equals("JJ") || pos.equals("JJR") || pos.equals("JJS");
    }

    public boolean isAdverb() {
        return pos.equals("RB") || pos.equals("RBR") || pos.equals("RBS");
    }

    public boolean isVerb() {
        return pos.equals("VB") || pos.equals("VBD") || pos.equals("VBG") ||
                pos.equals("VBN") || pos.equals("VBP") || pos.equals("VBZ");
    }

    public boolean isNoun() {
        return pos.equals("NN") || pos.equals("NNS") || pos.equals("NNP");
    }

    public boolean isDigit() { return word.matches("-?\\d+(\\.\\d+)?"); }

    public boolean isWord() {
        return (!isDigit() && (isAdjective() || isNoun() || isAdverb() || isVerb()));
    }

    public boolean hasSamePOS(Word anotherWord) {
        return (this.isAdjective() && anotherWord.isAdjective()) || (this.isNoun() && anotherWord.isNoun()) ||
                (this.isAdverb() && anotherWord.isAdverb()) || (this.isVerb() && anotherWord.isVerb());
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
