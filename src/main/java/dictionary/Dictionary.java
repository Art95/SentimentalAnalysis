package dictionary;

import util.Word;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by artem on 29.02.16.
 */
public class Dictionary {
    private HashMap<Word, Word> dictionary;

    public Dictionary() {
        dictionary = new HashMap<>();
    }

    public void loadDictionary(String dictionaryAddress) {
        Word word;

        try {
            File file = new File(dictionaryAddress);

            InputStream in = new FileInputStream(file);
            Scanner scanner = new Scanner(in);
            String line;

            while (scanner.hasNextLine()) {
                line = scanner.nextLine();
                word = Word.parseWord(line);
                dictionary.put(word, word);
            }

        } catch(IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
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

    public void clear() {
        dictionary.clear();
    }

    public void add(Word word) {
        dictionary.put(word, word);
    }

    private List<String> toStringLines() {
        List<String> lines = dictionary.values().stream().map(Word::toString).collect(Collectors.toList());
        Collections.sort(lines);

        return lines;
    }
}
