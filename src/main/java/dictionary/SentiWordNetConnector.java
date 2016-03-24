package dictionary;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/**
 * Created by artem on 06.03.16.
 */
public class SentiWordNetConnector {
    private HashMap<String, ArrayList<Double>> nouns;
    private HashMap<String, ArrayList<Double>> verbs;
    private HashMap<String, ArrayList<Double>> adjectives;
    private HashMap<String, ArrayList<Double>> adverbs;

    private Long id;

    private int lineSize = 6;

    public SentiWordNetConnector() {
        nouns = new HashMap<>();
        verbs = new HashMap<>();
        adjectives = new HashMap<>();
        adverbs = new HashMap<>();
    }

    public void loadDictionary(String dictionaryAddress) {
        try {
            File file = new File(dictionaryAddress);

            InputStream in = new FileInputStream(file);
            Scanner scanner = new Scanner(in);
            String line;

            while (scanner.hasNextLine()) {
                line = scanner.nextLine();
                parseLine(line);
            }

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void saveDictionary(String fileAddress) {
        try {
            List<String> lines = toStringLines();
            Path file = Paths.get(fileAddress);
            Files.write(file, lines, Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Double> getAdjectiveOpinionMarks(String adjective) {
        return adjectives.get(adjective);
    }

    public ArrayList<Double> getAdverbOpinionMarks(String adverb) {
        return adverbs.get(adverb);
    }

    public ArrayList<Double> getNounOpinionMarks(String noun) {
        return nouns.get(noun);
    }

    public ArrayList<Double> getVerbOpinionMarks(String verb) {
        return verbs.get(verb);
    }

    public void putAdjectiveOpinionMarks(String adjective, ArrayList<Double> opinionMarks) {
        adjectives.put(adjective, opinionMarks);
    }

    public void putAdverbOpinionMarks(String adverb, ArrayList<Double> opinionMarks) {
        adverbs.put(adverb, opinionMarks);
    }

    public void putNounOpinionMarks(String noun, ArrayList<Double> opinionMarks) {
        nouns.put(noun, opinionMarks);
    }

    public void putVerbOpinionMarks(String verb, ArrayList<Double> opinionMarks) {
        verbs.put(verb, opinionMarks);
    }

    public void clear() {
        nouns.clear();
        verbs.clear();
        adjectives.clear();
        adverbs.clear();
    }

    public List<String> toStringLines() {
        List<String> dictionary = new ArrayList<>();
        List<String> nounWords, verbWords, adjectiveWords, adverbsWords;

        nounWords = formStringLines(nouns, "n");
        verbWords = formStringLines(verbs, "v");
        adjectiveWords = formStringLines(adjectives, "a");
        adverbsWords = formStringLines(adverbs, "r");

        dictionary.addAll(nounWords);
        dictionary.addAll(verbWords);
        dictionary.addAll(adjectiveWords);
        dictionary.addAll(adverbsWords);

        return dictionary;
    }

    private void parseLine(String line) {
        if (line.startsWith("#"))
            return;

        String[] values = line.split("\t");

        String pos;
        String[] words;
        Double positive, negative, neutral;
        Long id;
        ArrayList<Double> opinionMeasure = new ArrayList<>();
        String gloss;

        try {
            pos = values[0];

            id = Long.parseLong(values[1]);

            positive = Double.parseDouble(values[2]);
            negative = Double.parseDouble(values[3]);
            neutral = 1 - (positive + negative);

            words = values[4].split("#[0-9]+");

            opinionMeasure.add(positive);
            opinionMeasure.add(neutral);
            opinionMeasure.add(negative);

            gloss = values[5];

            for (String word : words)
                updateDictionary(pos, word.trim(), opinionMeasure);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void updateDictionary(String pos, String word, ArrayList<Double> opinion) {
        switch(pos) {
            case "a":
                adjectives.putIfAbsent(word, opinion);
                break;
            case "n":
                nouns.putIfAbsent(word, opinion);
                break;
            case "r":
                adverbs.putIfAbsent(word, opinion);
                break;
            case "v":
                verbs.putIfAbsent(word, opinion);
                break;
        }
    }

    private List<String> formStringLines(HashMap<String, ArrayList<Double>> dictionary, String dictionaryType) {
        List<String> lines = new ArrayList<>();
        String[] line;

        for (String key : dictionary.keySet()) {
            line = new String[lineSize];

            line[0] = dictionaryType;
            line[1] = (++id).toString();
            line[2] = dictionary.get(key).get(0).toString();
            line[3] = dictionary.get(key).get(2).toString();
            line[4] = key;

            lines.add(formLine(line));
        }

        return lines;
    }

    private String formLine (String[] values) {
        StringBuilder line = new StringBuilder();

        for (String value : values) {
            line.append(value);
            line.append("\t");
        }

        line.append("\n");

        return line.toString();
    }
}
