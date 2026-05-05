///usr/bin/env java --source 17 "$0" "$@"; exit $?
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

public class temp_gen {
    private static final String BASE_PACKAGE  = "com.registry.verg";
    private static final String SOURCE_ROOT   = "src/main/java/com/registry/verg";
    private static final String RESOURCE_ROOT = "src/main/resources";
    private static final String TEMPLATE_DIR  = "scripts/registry_templates";

    private static final String[] SUB_DIRS = { "", "controller", "entity", "repository", "service", "service/impl" };

    private static int dirsCreated = 0, dirsSkipped = 0;
    private static int filesCreated = 0, filesSkipped = 0;
    private static int configsUpdated = 0, configsSkipped = 0;

    public static void main(String[] args) {
        if (args.length < 1 || args[0].isBlank()) {
            System.err.println("Usage: java scripts/temp_gen.java <registryName>");
            System.exit(1);
        }

        String rawInput  = args[0].trim();
        String name      = rawInput.toLowerCase();
        String nameUpper = capitalize(name);
        String nameSnake = name.toUpperCase();
        String nameId    = name + "Id";

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{{name}}", name);
        placeholders.put("{{Name}}", nameUpper);
        placeholders.put("{{NAME}}", nameSnake);
        placeholders.put("{{nameId}}", nameId);

        Path projectRoot  = Paths.get("").toAbsolutePath();
        Path domainRoot   = projectRoot.resolve(SOURCE_ROOT).resolve(name);
        Path templateDir  = projectRoot.resolve(TEMPLATE_DIR);
        Path resourceRoot = projectRoot.resolve(RESOURCE_ROOT);

        if (!Files.isDirectory(templateDir)) {
            System.err.println("[ERROR] Template directory not found: " + templateDir);
            System.exit(1);
        }

        System.out.println("Scaffolding registry: " + name + "\n");

        createDirectories(projectRoot, domainRoot);

        generateJavaFile(templateDir, domainRoot, "Controller.java.template", "controller", nameUpper + "Controller.java", placeholders, projectRoot);
        generateJavaFile(templateDir, domainRoot, "Entity.java.template", "entity", nameUpper + "Entity.java", placeholders, projectRoot);
        generateJavaFile(templateDir, domainRoot, "Repository.java.template", "repository", nameUpper + "Repository.java", placeholders, projectRoot);
        generateJavaFile(templateDir, domainRoot, "Service.java.template", "service", nameUpper + "Service.java", placeholders, projectRoot);
        generateJavaFile(templateDir, domainRoot, "ServiceImpl.java.template", "service/impl", nameUpper + "ServiceImpl.java", placeholders, projectRoot);

        generateResourceFile(templateDir, resourceRoot, "PayloadValidation.json.template", "payloadValidation", name + "PayloadValidation.json", placeholders, projectRoot);
        generateResourceFile(templateDir, resourceRoot, "EsFieldsMapping.json.template", "EsFieldsmapping", name + "EsFieldsMapping.json", placeholders, projectRoot);

        updateConstants(projectRoot, name, nameUpper, nameSnake, nameId);
        updateVergProperties(projectRoot, name, nameUpper);
        updateApplicationProperties(projectRoot, name);

        System.out.println("\nSummary:");
        System.out.printf("  Dirs: %d created, %d skipped\n", dirsCreated, dirsSkipped);
        System.out.printf("  Files: %d created, %d skipped\n", filesCreated, filesSkipped);
        System.out.printf("  Configs: %d updated, %d skipped\n", configsUpdated, configsSkipped);
        System.out.println("\n[DONE] Successfully scaffolded '" + name + "'.");
    }

    private static void createDirectories(Path projectRoot, Path domainRoot) {
        for (String sub : SUB_DIRS) {
            Path dirPath = sub.isEmpty() ? domainRoot : domainRoot.resolve(sub);
            try {
                if (Files.exists(dirPath)) {
                    dirsSkipped++;
                } else {
                    Files.createDirectories(dirPath);
                    System.out.println("[CREATED] " + rel(projectRoot, dirPath));
                    dirsCreated++;
                }
            } catch (Exception e) {
                System.err.println("[ERROR] " + dirPath + " - " + e.getMessage());
            }
        }
    }

    private static void generateJavaFile(Path templateDir, Path domainRoot, String templateName, String subDir, String outputName, Map<String, String> placeholders, Path projectRoot) {
        Path templatePath = templateDir.resolve(templateName);
        Path outputPath   = domainRoot.resolve(subDir).resolve(outputName);

        if (!Files.exists(templatePath)) return;
        if (Files.exists(outputPath)) {
            filesSkipped++;
            return;
        }

        try {
            String content = stripTemplateHeader(Files.readString(templatePath, StandardCharsets.UTF_8));
            writeFile(outputPath, applyPlaceholders(content, placeholders));
            System.out.println("[CREATED] " + rel(projectRoot, outputPath));
            filesCreated++;
        } catch (IOException e) {
            System.err.println("[ERROR] " + outputPath + " - " + e.getMessage());
        }
    }

    private static void generateResourceFile(Path templateDir, Path resourceRoot, String templateName, String subDir, String outputName, Map<String, String> placeholders, Path projectRoot) {
        Path templatePath = templateDir.resolve(templateName);
        Path outputPath   = resourceRoot.resolve(subDir).resolve(outputName);

        if (!Files.exists(templatePath)) return;
        if (Files.exists(outputPath)) {
            filesSkipped++;
            return;
        }

        try {
            String content = Files.readString(templatePath, StandardCharsets.UTF_8);
            writeFile(outputPath, applyPlaceholders(content, placeholders));
            System.out.println("[CREATED] " + rel(projectRoot, outputPath));
            filesCreated++;
        } catch (IOException e) {
            System.err.println("[ERROR] " + outputPath + " - " + e.getMessage());
        }
    }

    private static void updateConstants(Path projectRoot, String name, String nameUpper, String nameSnake, String nameId) {
        Path constantsPath = projectRoot.resolve(SOURCE_ROOT).resolve("core/util/Constants.java");
        if (!Files.exists(constantsPath)) return;

        try {
            String content = Files.readString(constantsPath, StandardCharsets.UTF_8);
            if (content.contains(nameSnake + "_VALIDATION_FILE_JSON")) {
                configsSkipped++;
                return;
            }

            String newBlock = "\n    // " + nameUpper + " Specific Constants\n"
                + "    public static final String " + nameSnake + "_VALIDATION_FILE_JSON = \"/payloadValidation/" + name + "PayloadValidation.json\";\n"
                + "    public static final String " + nameSnake + "_ID_RQST = \"" + nameId + "\";\n"
                + "    public static final String " + nameSnake + "_INDEX_NAME = \"" + name + "_index\";\n\n";

            String anchor = "    private Constants() {";
            content = content.replace(anchor, newBlock + anchor);
            Files.writeString(constantsPath, content, StandardCharsets.UTF_8);
            System.out.println("[UPDATED] " + rel(projectRoot, constantsPath));
            configsUpdated++;
        } catch (IOException e) {
            System.err.println("[ERROR] Constants.java - " + e.getMessage());
        }
    }

    private static void updateVergProperties(Path projectRoot, String name, String nameUpper) {
        Path propsPath = projectRoot.resolve(SOURCE_ROOT).resolve("core/util/VergProperties.java");
        if (!Files.exists(propsPath)) return;

        try {
            String content = Files.readString(propsPath, StandardCharsets.UTF_8);
            String fieldName = "elastic" + nameUpper + "JsonPath";
            if (content.contains(fieldName)) {
                configsSkipped++;
                return;
            }

            String newField = "\n        @Value(\"${elastic.required.field." + name + ".json.path}\")\n"
                + "        private String " + fieldName + ";\n";

            int lastBrace = content.lastIndexOf('}');
            content = content.substring(0, lastBrace) + newField + "\n" + content.substring(lastBrace);
            Files.writeString(propsPath, content, StandardCharsets.UTF_8);
            System.out.println("[UPDATED] " + rel(projectRoot, propsPath));
            configsUpdated++;
        } catch (IOException e) {
            System.err.println("[ERROR] VergProperties.java - " + e.getMessage());
        }
    }

    private static void updateApplicationProperties(Path projectRoot, String name) {
        Path appPropsPath = projectRoot.resolve(RESOURCE_ROOT).resolve("application.properties");
        if (!Files.exists(appPropsPath)) return;

        try {
            String content = Files.readString(appPropsPath, StandardCharsets.UTF_8);
            String propKey = "elastic.required.field." + name + ".json.path";
            if (content.contains(propKey)) {
                configsSkipped++;
                return;
            }

            String newProp = "\n" + propKey + "=/EsFieldsmapping/" + name + "EsFieldsMapping.json\n";
            Files.writeString(appPropsPath, newProp, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            System.out.println("[UPDATED] " + rel(projectRoot, appPropsPath));
            configsUpdated++;
        } catch (IOException e) {
            System.err.println("[ERROR] application.properties - " + e.getMessage());
        }
    }

    private static String stripTemplateHeader(String content) {
        String[] lines = content.split("\n", -1);
        int start = 0;
        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (trimmed.startsWith("//") || trimmed.isEmpty()) start = i + 1;
            else break;
        }
        if (start == 0) return content;
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < lines.length; i++) {
            sb.append(lines[i]);
            if (i < lines.length - 1) sb.append("\n");
        }
        return sb.toString();
    }

    private static String applyPlaceholders(String content, Map<String, String> placeholders) {
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            content = content.replace(entry.getKey(), entry.getValue());
        }
        return content;
    }

    private static void writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private static String capitalize(String s) {
        return (s == null || s.isEmpty()) ? s : s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private static String rel(Path base, Path target) {
        return base.relativize(target).toString();
    }
}
