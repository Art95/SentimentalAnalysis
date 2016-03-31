package util;

/**
 * Created by artem on 10.03.16.
 */
public class Utils {
    public static final String stopWordsFileAddress =
            "./Files/Additional/StopWords.txt";

    public static final String antiCorpusAddress =
            "./Files/Corpuses/anti_GMO.txt";

    public static final String proCorpusAddress =
            "./Files/Corpuses/pro_GMO.txt";

    public static final String kernelFileAddress =
            "./Files/Corpuses/Kernel.txt";

    public static final String peripheryFileAddress =
            "./Files/Corpuses/Periphery.txt";


    public static final String markedDictionaryAddress =
            "./Files/Dictionaries/MarkedDictionary.txt";

    public static final String markedAdjectives =
            "./Files/Dictionaries/markedAdjectives.txt";

    public static final String markedAdverbs =
            "./Files/Dictionaries/markedAdverbs.txt";

    public static final String markedNouns =
            "./Files/Dictionaries/markedNouns.txt";

    public static final String markedVerbs =
            "./Files/Dictionaries/markedVerbs.txt";



    public static final String weightsAddress =
            "./Files/Classifier/Weights.txt";

    public static final String indexesAddress =
            "./Files/Classifier/Indexes.txt";


    public static final double LEARNING_ACCURACY = 1.0;

    public static final double BIAS = 0.0;

    public static final int LOGGING_FREQUENCY = 10;


    public static final double TRAINING_DATA_SIZE = 0.75;

    public static final int TESTING_ITERATIONS = 200;
}
