package com.github.ybroeker.maven.plugins.graphql_inspector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


@Mojo(name = "validate", threadSafe = true)
public class ValidateMojo extends AbstractGraphqlInspectorMojo {

    private static final String COMMAND = "diff";

    @Parameter(required = true, defaultValue = "${oldSchema}")
    private String oldSchema;

    @Parameter(required = true, defaultValue = "${newSchema}")
    private String newSchema;

    @Parameter(defaultValue = "true")
    private boolean fail;


    @Override
    protected List<String> getCommand() {
        return Arrays.asList(COMMAND, resolveSchema(oldSchema), resolveSchema(newSchema));
    }

    public String resolveSchema(String schema) {
        if (isHttp(schema) && isSchemaFile(schema)) {
            //Download static schema file
            try {
                URL url = new URL(schema);

                final Path tempFile = Files.createTempFile("schema.", ".graphql");
                tempFile.toFile().deleteOnExit();

                try (final InputStream in = url.openStream();
                     final OutputStream out = Files.newOutputStream(tempFile);
                ) {
                    transferTo(in, out);
                }

                return tempFile.toAbsolutePath().toString();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return schema;
    }

    private boolean isHttp(String path) {
        return path.startsWith("http://") || path.startsWith("https://");
    }

    private boolean isSchemaFile(String path) {
        return path.endsWith(".graphql")
               || path.startsWith(".graphqls")
               || path.startsWith(".gql");
    }

    private void transferTo(InputStream in, OutputStream out) throws IOException {
        int DEFAULT_BUFFER_SIZE = 8096;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer, 0, DEFAULT_BUFFER_SIZE)) >= 0) {
            out.write(buffer, 0, read);
        }
    }

    @Override
    protected void handle(final String out, String err, int status) throws MojoFailureException {
        for (final String line : out.split("\\R+")) {
            handleLine(line);
        }
        if (status!=0) {
            handleNonZeroExit(status);
        }
    }

    private void handleLine(String logLine) {
        String line = stripLogLevel(logLine);

        if (line.trim().isEmpty()) {
            return;
        }
        if (line.matches("Detected the following changes \\(\\d+\\) between schemas:")) {
            return;
        }
        if (line.matches("Detected \\d+ breaking changes")) {
            getLog().error(line);
            return;
        }
        if (line.matches("No breaking changes detected")) {
            getLog().info(line);
            return;
        }

        if (line.startsWith("✖")) {
            getLog().error(line);
        } else if (line.startsWith("✔")) {
            getLog().info(line);
        } else {
            getLog().error(line);
        }
    }

    private String stripLogLevel(String line) {
        List<String> logLevels = Arrays.asList(
                "[log] ",
                "[error] ",
                "[warn] ",
                "[success] ",
                "[info] ");

        for (final String logLevel : logLevels) {
            if (line.startsWith(logLevel)) {
                return line.replaceFirst(Pattern.quote(logLevel), "");
            }
        }
        return line;
    }

    protected void handleNonZeroExit(final int status) throws MojoFailureException {
        getLog().debug("graphql-inspector exit status: " + status);
        if (fail) {
            throw new MojoFailureException("Breaking Changes in graphql schema found!");
        }
    }
}
