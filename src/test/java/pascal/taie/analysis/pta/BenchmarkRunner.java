package pascal.taie.analysis.pta;

import pascal.taie.Main;
import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@CommandLine.Command
public class BenchmarkRunner {

    private static final String BENCHMARK_HOME = "java-benchmarks";

    private static final String BENCHMARK_INFO = "java-benchmarks/benchmark-info.yml";

    private static final Map<String, BenchmarkInfo> benchmarkInfos =
            BenchmarkInfo.load(BENCHMARK_INFO);

    @CommandLine.Option(names = "-cs", defaultValue = "ci")
    private String cs;

    @CommandLine.Option(names = "-java", defaultValue = "-1")
    private int jdk;

    @CommandLine.Option(names = "-advanced", defaultValue = "null")
    private String advanced;

    @CommandLine.Parameters
    private List<String> benchmarks;

    public static void main(String[] args) {
        BenchmarkRunner runner = CommandLine.populateCommand(new BenchmarkRunner(), args);
        runner.runAll();
    }

    private void runAll() {
        if (benchmarks == null) {
            throw new IllegalArgumentException("benchmarks are not given");
        }
        benchmarks.forEach(this::run);
    }

    private void run(String benchmark) {
        System.out.println("\nAnalyzing " + benchmark);
        Main.main(composeArgs(benchmark));
    }

    private String[] composeArgs(String benchmark) {
        BenchmarkInfo info = benchmarkInfos.get(benchmark);
        List<String> args = new ArrayList<>();
        int jdkVersion = jdk != -1 ? jdk : info.jdk();
        Collections.addAll(args,
                "-java", Integer.toString(jdkVersion),
                "-cp", buildClassPath(info),
                "-m", info.main());
        if (info.allowPhantom()) {
            args.add("--allow-phantom");
        }
        Map<String, String> ptaArgs = Map.of(
                "distinguish-string-constants", "null",
                "merge-string-objects", "false",
                "cs", cs,
                "advanced", advanced,
                "reflection-inference", "null",
                "reflection-log", new File(BENCHMARK_HOME, info.reflectionLog()).toString());
        Collections.addAll(args,
                "-a", "pta=" + ptaArgs.entrySet()
                        .stream()
                        .map(e -> e.getKey() + ":" + e.getValue())
                        .collect(Collectors.joining(";")),
                "-a", "may-fail-cast",
                "-a", "poly-call");
        return args.toArray(new String[0]);
    }

    private String buildClassPath(BenchmarkInfo info) {
        List<String> cp = new ArrayList<>();
        info.apps().forEach(appPath -> cp.addAll(extendCP(appPath)));
        info.libs().forEach(libPath -> cp.addAll(extendCP(libPath)));
        return String.join(File.pathSeparator, cp);
    }

    private static boolean isJar(File file) {
        return file.getName().endsWith(".jar");
    }

    private List<String> extendCP(String path) {
        File file = new File(BENCHMARK_HOME, path);
        List<String> paths = new ArrayList<>();
        if (isJar(file)) {
            paths.add(file.toString());
        } else if (file.isDirectory()) {
            paths.add(file.toString());
            for (File item : Objects.requireNonNull(file.listFiles())) {
                if (isJar(item)) {
                    paths.add(item.toString());
                }
            }
        } else {
            throw new RuntimeException(path + " is neither a directory nor a JAR");
        }
        return paths;
    }
}
