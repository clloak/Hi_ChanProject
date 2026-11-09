import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small dependency-free JSON parser for the preprocessor's input. */
public final class JSONParser {
    private final String text;
    private int index;

    private JSONParser(String text) { this.text = text; }

    public static Object parse(String text) throws IOException {
        JSONParser parser = new JSONParser(text);
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (parser.index != text.length()) throw parser.error("Unexpected text after JSON value");
        return value;
    }

    private Object readValue() throws IOException {
        skipWhitespace();
        if (index >= text.length()) throw error("Unexpected end of JSON input");
        char ch = text.charAt(index);
        if (ch == '{') return readObject();
        if (ch == '[') return readArray();
        if (ch == '"') return readString();
        if (ch == 't' || ch == 'f') return readBoolean();
        if (ch == 'n') return readNull();
        if (ch == '-' || Character.isDigit(ch)) return readNumber();
        throw error("Unexpected character '" + ch + "'");
    }

    private Map<String, Object> readObject() throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        index++;
        skipWhitespace();
        if (take('}')) return result;
        while (true) {
            skipWhitespace();
            if (index >= text.length() || text.charAt(index) != '"') throw error("Expected object key");
            String key = readString();
            skipWhitespace();
            expect(':');
            result.put(key, readValue());
            skipWhitespace();
            if (take('}')) break;
            expect(',');
        }
        return result;
    }

    private List<Object> readArray() throws IOException {
        List<Object> result = new ArrayList<>();
        index++;
        skipWhitespace();
        if (take(']')) return result;
        while (true) {
            result.add(readValue());
            skipWhitespace();
            if (take(']')) break;
            expect(',');
        }
        return result;
    }

    private String readString() throws IOException {
        expect('"');
        StringBuilder result = new StringBuilder();
        while (index < text.length()) {
            char ch = text.charAt(index++);
            if (ch == '"') return result.toString();
            if (ch == '\\') {
                if (index >= text.length()) throw error("Unfinished escape sequence");
                char escaped = text.charAt(index++);
                switch (escaped) {
                    case '"': result.append('"'); break;
                    case '\\': result.append('\\'); break;
                    case '/': result.append('/'); break;
                    case 'b': result.append('\b'); break;
                    case 'f': result.append('\f'); break;
                    case 'n': result.append('\n'); break;
                    case 'r': result.append('\r'); break;
                    case 't': result.append('\t'); break;
                    case 'u':
                        if (index + 4 > text.length()) throw error("Invalid Unicode escape");
                        try {
                            result.append((char) Integer.parseInt(text.substring(index, index + 4), 16));
                        } catch (NumberFormatException ex) {
                            throw error("Invalid Unicode escape");
                        }
                        index += 4;
                        break;
                    default: throw error("Invalid escape sequence");
                }
            } else {
                result.append(ch);
            }
        }
        throw error("Unterminated string");
    }

    private Object readNumber() throws IOException {
        int start = index;
        if (take('-')) { /* sign consumed */ }
        while (index < text.length() && Character.isDigit(text.charAt(index))) index++;
        if (take('.')) while (index < text.length() && Character.isDigit(text.charAt(index))) index++;
        if (index < text.length() && (text.charAt(index) == 'e' || text.charAt(index) == 'E')) {
            index++;
            if (index < text.length() && (text.charAt(index) == '+' || text.charAt(index) == '-')) index++;
            while (index < text.length() && Character.isDigit(text.charAt(index))) index++;
        }
        try { return Double.parseDouble(text.substring(start, index)); }
        catch (NumberFormatException ex) { throw error("Invalid number"); }
    }

    private Boolean readBoolean() throws IOException {
        if (text.startsWith("true", index)) { index += 4; return Boolean.TRUE; }
        if (text.startsWith("false", index)) { index += 5; return Boolean.FALSE; }
        throw error("Invalid boolean");
    }

    private Object readNull() throws IOException {
        if (text.startsWith("null", index)) { index += 4; return null; }
        throw error("Invalid null value");
    }

    private void skipWhitespace() {
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) index++;
    }
    private boolean take(char expected) {
        if (index < text.length() && text.charAt(index) == expected) { index++; return true; }
        return false;
    }
    private void expect(char expected) throws IOException {
        if (!take(expected)) throw error("Expected '" + expected + "'");
    }
    private IOException error(String message) { return new IOException(message + " at character " + index); }
}
