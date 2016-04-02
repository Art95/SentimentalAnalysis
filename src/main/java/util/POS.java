package util;

public enum POS {
    NOUN,
    VERB,
    ADJECTIVE,
    ADVERB,
    UNKNOWN;

    public static POS fromString(String pos) {
        if (pos.startsWith("NN")) {
            return NOUN;
        }
        else if (pos.startsWith("VB")) {
            return VERB;
        }
        else if (pos.startsWith("JJ")) {
            return ADJECTIVE;
        }
        else if (pos.startsWith("RB")) {
            return ADVERB;
        }

        return UNKNOWN;
    }

    @Override
    public String toString() {
        switch (this) {
            case NOUN:
                return "NN";
            case VERB:
                return "VB";
            case ADJECTIVE:
                return "JJ";
            case ADVERB:
                return "RB";
            default:
                return "UN";
        }
    }
}