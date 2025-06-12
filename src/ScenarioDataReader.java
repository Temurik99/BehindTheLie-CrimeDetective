import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ScenarioDataReader {
    private static final String CSV_FILE = "src/Master Scenarios FINALLLL REAL.csv";
    private List<Scenario> scenarios;
    private Random random;

    public ScenarioDataReader() {
        this.scenarios = new ArrayList<>();
        this.random = new Random();
        loadScenarios();
    }

    private void loadScenarios() {
        try (BufferedReader br = new BufferedReader(new FileReader(CSV_FILE))) {
            String line;
            String[] headers = null;
            Map<String, Scenario> scenarioMap = new HashMap<>();

            while ((line = br.readLine()) != null) {
                String[] values = parseCSVLine(line);

                if (headers == null) {
                    headers = values;
                    continue;
                }

                String scenarioId = values[0];
                Scenario scenario = scenarioMap.computeIfAbsent(scenarioId, id -> {
                    Scenario s = new Scenario();
                    s.id = id;
                    s.difficulty = values[1];
                    s.description = cleanString(values[2]);
                    s.questions = new ArrayList<>();
                    return s;
                });

                String questionId = values[3];
                Question question = scenario.questions.stream()
                        .filter(q -> q.id.equals(questionId))
                        .findFirst()
                        .orElseGet(() -> {
                            Question q = new Question();
                            q.id = questionId;
                            q.text = cleanString(values[4]);
                            q.answers = new ArrayList<>();
                            scenario.questions.add(q);
                            return q;
                        });

                Answer answer = new Answer();
                answer.character = values[5];
                answer.innocentResponse = cleanString(values[6]);
                answer.guiltyResponse = cleanString(values[7]);
                question.answers.add(answer);
            }

            scenarios = new ArrayList<>(scenarioMap.values());
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String cleanString(String input) {
        return input.replace("\"", "").trim();
    }

    private String[] parseCSVLine(String line) {
        List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentValue = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }
        values.add(currentValue.toString());
        return values.toArray(new String[0]);
    }

    public Scenario getRandomScenario(String difficulty) {
        if (difficulty == null || difficulty.isEmpty()) {
            return null;
        }

        List<Scenario> filtered = new ArrayList<>();
        for (Scenario s : scenarios) {
            if (s.difficulty.equalsIgnoreCase(difficulty)) {
                filtered.add(s);
            }
        }

        return filtered.isEmpty() ? null : filtered.get(random.nextInt(filtered.size()));
    }

    public static class Scenario {
        public String id;
        public String difficulty;
        public String description;
        public List<Question> questions = new ArrayList<>();
    }

    public static class Question {
        public String id;
        public String text;
        public List<Answer> answers = new ArrayList<>();
    }

    public static class Answer {
        public String character;
        public String innocentResponse;
        public String guiltyResponse;
    }
}
