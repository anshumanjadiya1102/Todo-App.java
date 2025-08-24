import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Simple CLI To‑Do App in one file, no external libraries.
 *
 * Features
 * - Add / list / edit / complete / delete tasks
 * - Optional fields: due date, priority (LOW|MEDIUM|HIGH), tags
 * - Search & filter by text, status, priority, or due date range
 * - Persists to a TSV file ("tasks.tsv") in the working directory
 * - Works on any Java 11+ environment (no GUI, no Maven needed)
 *
 * Compile:   javac TodoApp.java
 * Run:       java TodoApp
 */
public class TodoApp {
    private static final String STORAGE_FILE = "tasks.tsv";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) {
        TaskStore store = new TaskStore(STORAGE_FILE);
        store.load();
        new TodoApp().run(store);
    }

    private final Scanner in = new Scanner(System.in);

    private void run(TaskStore store) {
        println("\n===== Simple CLI To‑Do =====");
        println("Storage: " + STORAGE_FILE);
        String cmd;
        help();
        while (true) {
            System.out.print("\n> ");
            cmd = in.nextLine().trim();
            if (cmd.isEmpty()) continue;
            String[] parts = cmd.split("\\s+", 2);
            String op = parts[0].toLowerCase(Locale.ROOT);
            String rest = parts.length > 1 ? parts[1] : "";
            try {
                switch (op) {
                    case "help":
                    case "?":
                        help();
                        break;
                    case "add":
                        add(store, rest);
                        break;
                    case "list":
                    case "ls":
                        list(store, rest);
                        break;
                    case "done":
                        toggleDone(store, rest, true);
                        break;
                    case "undone":
                        toggleDone(store, rest, false);
                        break;
                    case "edit":
                        edit(store, rest);
                        break;
                    case "del":
                    case "rm":
                        delete(store, rest);
                        break;
                    case "search":
                        search(store, rest);
                        break;
                    case "clear":
                        clearCompleted(store);
                        break;
                    case "save":
                        store.save();
                        println("Saved.");
                        break;
                    case "quit":
                    case "exit":
                        store.save();
                        println("Saved. Bye!");
                        return;
                    default:
                        println("Unknown command. Type 'help' for commands.");
                }
            } catch (Exception e) {
                println("Error: " + e.getMessage());
            }
        }
    }

    private void help() {
        println("\nCommands:");
        println("  add <title> [/due yyyy-mm-dd] [/p low|med|high] [/tags tag1,tag2]");
        println("  list [all|open|done] [/sort id|due|prio] [/rev]");
        println("  done <id>      — mark complete");
        println("  undone <id>    — mark incomplete");
        println("  edit <id> [/t new title] [/due yyyy-mm-dd|none] [/p low|med|high] [/tags list|none]");
        println("  del <id>       — delete task");
        println("  search <text>  — find in titles or tags");
        println("  clear          — remove all completed tasks");
        println("  save           — persist now (auto-saves on exit)");
        println("  exit           — save & quit");
    }

    private void add(TaskStore store, String args) {
        Map<String,String> flags = parseFlags(args);
        String title = leadingText(args);
        if (title.isBlank()) throw new IllegalArgumentException("Title is required. Try: add Buy milk /due 2025-08-31 /p high");
        LocalDate due = parseDate(flags.get("/due"));
        Priority p = parsePriority(flags.get("/p"));
        Set<String> tags = parseTags(flags.get("/tags"));
        Task t = store.add(title.trim(), due, p, tags);
        println("Added #" + t.id + ": " + t.title);
        store.save();
    }

    private void list(TaskStore store, String args) {
        Map<String,String> flags = parseFlags(args);
        String scope = leadingText(args).toLowerCase(Locale.ROOT);
        String sort = flags.getOrDefault("/sort", "id").toLowerCase(Locale.ROOT);
        boolean rev = flags.containsKey("/rev");
        List<Task> tasks = new ArrayList<>(store.all());
        switch (scope) {
            case "all":
                break;
            case "done":
                tasks.removeIf(t -> !t.done);
                break;
            case "open":
            case "":
                tasks.removeIf(t -> t.done);
                break;
            default:
                println("Unknown list scope '" + scope + "', showing open tasks.");
                tasks.removeIf(t -> t.done);
        }
        Comparator<Task> cmp;
        switch (sort) {
            case "due": cmp = Comparator.comparing((Task t) -> t.due, Comparator.nullsLast(Comparator.naturalOrder())); break;
            case "prio": cmp = Comparator.comparing((Task t) -> t.priority); break; // Priority enum order HIGH<MEDIUM<LOW? We'll invert below
            case "id": default: cmp = Comparator.comparingInt(t -> t.id);
        }
        tasks.sort(cmp);
        if (sort.equals("prio")) Collections.reverse(tasks); // HIGH first
        if (rev) Collections.reverse(tasks);
        renderTable(tasks);
    }

    private void toggleDone(TaskStore store, String arg, boolean to) {
        int id = parseId(arg);
        Task t = store.get(id);
        if (t == null) throw new IllegalArgumentException("No task with id " + id);
        t.done = to;
        store.save();
        println((to ? "Completed" : "Reopened") + " #" + id + ".");
    }

    private void edit(TaskStore store, String args) {
        String[] parts = args.trim().split("\\s+", 2);
        if (parts.length == 0 || parts[0].isBlank()) throw new IllegalArgumentException("Usage: edit <id> [flags]");
        int id = parseId(parts[0]);
        Task t = store.get(id);
        if (t == null) throw new IllegalArgumentException("No task with id " + id);
        String flagsStr = parts.length > 1 ? parts[1] : "";
        Map<String,String> flags = parseFlags(flagsStr);
        if (flags.containsKey("/t")) t.title = flags.get("/t").trim();
        if (flags.containsKey("/due")) t.due = parseDate(flags.get("/due"));
        if (flags.containsKey("/p")) t.priority = parsePriority(flags.get("/p"));
        if (flags.containsKey("/tags")) t.tags = parseTags(flags.get("/tags"));
        store.save();
        println("Edited #" + id + ".");
    }

    private void delete(TaskStore store, String arg) {
        int id = parseId(arg);
        boolean ok = store.delete(id);
        if (!ok) throw new IllegalArgumentException("No task with id " + id);
        store.save();
        println("Deleted #" + id + ".");
    }

    private void search(TaskStore store, String text) {
        String q = text.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) { println("Usage: search <text>"); return; }
        List<Task> matches = new ArrayList<>();
        for (Task t : store.all()) {
            if ((t.title != null && t.title.toLowerCase(Locale.ROOT).contains(q)) ||
                t.tags.stream().anyMatch(tag -> tag.toLowerCase(Locale.ROOT).contains(q))) {
                matches.add(t);
            }
        }
        renderTable(matches);
    }

    private void clearCompleted(TaskStore store) {
        int before = store.size();
        store.clearCompleted();
        int removed = before - store.size();
        store.save();
        println("Removed " + removed + " completed task(s).");
    }

    // ===== Helpers =====

    private static void renderTable(List<Task> tasks) {
        if (tasks.isEmpty()) { System.out.println("(no tasks)"); return; }
        String header = String.format("%-4s %-1s %-10s %-6s %-40s %-20s", "ID", "✔", "Due", "Prio", "Title", "Tags");
        System.out.println(header);
        System.out.println("-".repeat(header.length()));
        for (Task t : tasks) {
            String dueStr = t.due == null ? "" : DATE_FMT.format(t.due);
            String pr = t.priority == null ? "" : t.priority.name();
            String title = truncate(t.title, 40);
            String tags = truncate(String.join(",", t.tags), 20);
            System.out.printf("%-4d %-1s %-10s %-6s %-40s %-20s%n", t.id, t.done ? "✔" : "", dueStr, pr, title, tags);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 1)) + "…";
    }

    private static Map<String,String> parseFlags(String input) {
        Map<String,String> out = new LinkedHashMap<>();
        if (input == null) return out;
        String[] tokens = input.split("\\s+");
        String current = null;
        StringBuilder val = new StringBuilder();
        for (String tok : tokens) {
            if (tok.startsWith("/")) {
                if (current != null) out.put(current, val.toString().trim());
                current = tok.toLowerCase(Locale.ROOT);
                val.setLength(0);
            } else if (current != null) {
                if (val.length() > 0) val.append(' ');
                val.append(tok);
            }
        }
        if (current != null) out.put(current, val.toString().trim());
        return out;
    }

    private static String leadingText(String input) {
        if (input == null) return "";
        int idx = input.indexOf(" /");
        return (idx >= 0 ? input.substring(0, idx) : input).trim();
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank() || s.equalsIgnoreCase("none")) return null;
        return LocalDate.parse(s.trim(), DATE_FMT);
    }

    private static Priority parsePriority(String s) {
        if (s == null || s.isBlank()) return Priority.MEDIUM;
        s = s.toLowerCase(Locale.ROOT);
        switch (s) {
            case "h": case "hi": case "high": return Priority.HIGH;
            case "l": case "lo": case "low": return Priority.LOW;
            default: return Priority.MEDIUM;
        }
    }

    private static Set<String> parseTags(String s) {
        if (s == null || s.isBlank()) return new LinkedHashSet<>();
        if (s.equalsIgnoreCase("none")) return new LinkedHashSet<>();
        String[] arr = s.split(",");
        Set<String> tags = new LinkedHashSet<>();
        for (String a : arr) {
            String t = a.trim();
            if (!t.isEmpty()) tags.add(t);
        }
        return tags;
    }

    private static int parseId(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { throw new IllegalArgumentException("Provide a numeric id"); }
    }

    private static void println(String s) { System.out.println(s); }

    // ===== Data Model & Storage =====

    enum Priority { LOW, MEDIUM, HIGH }

    static class Task {
        int id;
        String title;
        LocalDate due; // nullable
        Priority priority = Priority.MEDIUM;
        boolean done = false;
        Set<String> tags = new LinkedHashSet<>();

        Task() {}
        Task(int id) { this.id = id; }

        String toTsv() {
            // id \t done \t priority \t due \t title \t tags(comma)
            return id + "\t" + (done ? 1 : 0) + "\t" + priority.name() + "\t" + (due == null ? "" : DATE_FMT.format(due))
                    + "\t" + escape(title) + "\t" + escape(String.join(",", tags));
        }

        static Task fromTsv(String line) {
            String[] f = splitTsv(line, 6);
            Task t = new Task(Integer.parseInt(f[0]));
            t.done = Objects.equals(f[1], "1");
            t.priority = Priority.valueOf(f[2]);
            t.due = f[3].isEmpty() ? null : LocalDate.parse(f[3], DATE_FMT);
            t.title = unescape(f[4]);
            String tagsStr = unescape(f[5]);
            if (!tagsStr.isEmpty()) t.tags.addAll(Arrays.asList(tagsStr.split(",")));
            return t;
        }

        private static String escape(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n");
        }
        private static String unescape(String s) {
            StringBuilder out = new StringBuilder();
            boolean esc = false;
            for (char c : s.toCharArray()) {
                if (!esc && c == '\\') { esc = true; continue; }
                if (esc) {
                    if (c == 't') out.append('\t');
                    else if (c == 'n') out.append('\n');
                    else out.append(c);
                    esc = false;
                } else out.append(c);
            }
            return out.toString();
        }

        private static String[] splitTsv(String line, int expected) {
            List<String> parts = new ArrayList<>(expected);
            StringBuilder cur = new StringBuilder();
            boolean esc = false;
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (!esc && c == '\\') { esc = true; continue; }
                if (esc) {
                    cur.append('\\').append(c); // keep escapes, handled in unescape
                    esc = false;
                    continue;
                }
                if (c == '\t') { parts.add(cur.toString()); cur.setLength(0); }
                else cur.append(c);
            }
            parts.add(cur.toString());
            while (parts.size() < expected) parts.add("");
            return parts.toArray(new String[0]);
        }
    }

    static class TaskStore {
        private final Path path;
        private final Map<Integer, Task> tasks = new LinkedHashMap<>();
        private int nextId = 1;

        TaskStore(String file) { this.path = Paths.get(file); }

        synchronized Task add(String title, LocalDate due, Priority p, Set<String> tags) {
            Task t = new Task(nextId++);
            t.title = title;
            t.due = due;
            t.priority = p == null ? Priority.MEDIUM : p;
            if (tags != null) t.tags.addAll(tags);
            tasks.put(t.id, t);
            return t;
        }

        synchronized boolean delete(int id) { return tasks.remove(id) != null; }
        synchronized Task get(int id) { return tasks.get(id); }
        synchronized Collection<Task> all() { return tasks.values(); }
        synchronized int size() { return tasks.size(); }

        synchronized void clearCompleted() { tasks.values().removeIf(t -> t.done); }

        synchronized void save() {
            List<String> lines = new ArrayList<>();
            lines.add("# Simple To‑Do TSV v1");
            lines.add("# id\tdone\tpriority\tdue\ttitle\ttags");
            for (Task t : tasks.values()) lines.add(t.toTsv());
            try {
                Files.write(path, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            } catch (IOException e) {
                throw new RuntimeException("Failed to save: " + e.getMessage(), e);
            }
            // also store nextId
            try {
                Files.writeString(Paths.get(path.toString() + ".meta"), Integer.toString(nextId), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException ignored) {}
        }

        synchronized void load() {
            tasks.clear();
            nextId = 1;
            if (!Files.exists(path)) return;
            try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("#") || line.isBlank()) continue;
                    Task t = Task.fromTsv(line);
                    tasks.put(t.id, t);
                    nextId = Math.max(nextId, t.id + 1);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load tasks: " + e.getMessage(), e);
            }
            // restore nextId if meta exists
            Path meta = Paths.get(path.toString() + ".meta");
            if (Files.exists(meta)) {
                try {
                    String s = Files.readString(meta, StandardCharsets.UTF_8).trim();
                    nextId = Math.max(nextId, Integer.parseInt(s));
                } catch (Exception ignored) {}
            }
        }
    }
}
