package com.github.ybroeker.maven.plugins.graphql_inspector;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractGraphqlInspectorMojo extends GraphqlInspectorArgs {

  @Parameter(defaultValue = "false")
  private boolean skip;

  protected abstract List<String> getCommand();

  protected abstract void handle(String out, String err, int status) throws MojoFailureException;

  @Override
  public final void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("Skipping plugin execution");
      return;
    }

    try {

      List<String> command = new ArrayList<>(baseCommand());
      command.addAll(getCommand());

      if (getLog().isDebugEnabled()) {
        getLog().debug("Running graphql-inspector with args " + command);
      }

      Process process = new ProcessBuilder(command.toArray(new String[0]))
        .directory(project.getBasedir())
        .start();
      try (
        InputStream stdout = process.getInputStream();
        InputStream stderr = process.getErrorStream()
      ) {

        String out = read(stdout);
        String err = read(stderr);
        int status = process.waitFor();

        handle(out, err, status);
      }
    } catch (IOException | InterruptedException e) {
      throw new MojoExecutionException("Error trying to run graphql-inspector", e);
    }
  }

  private String read(InputStream stream) throws IOException {
    InputStreamReader stdoutReader = new InputStreamReader(stream, StandardCharsets.UTF_8);
    BufferedReader reader = new BufferedReader(stdoutReader);

    StringBuilder stringBuilder = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      stringBuilder.append(line);
    }
    return stringBuilder.toString();
  }

  protected List<String> baseCommand() throws MojoExecutionException {
    Path nodeExecutable = resolveNodeExecutable();

    Path graphqlInspectorDirectory = extractGraphqlInspector();

    Path graphqlInspectorBin = graphqlInspectorDirectory
        .resolve("graphql-inspector")
        .resolve("node_modules")
        .resolve("@graphql-inspector/cli")
        .resolve("index.js");

    List<String> command = new ArrayList<>();
    command.add(toString(nodeExecutable));
    command.add(toString(graphqlInspectorBin));

    return command;
  }

  // Convert Windows Path to Unix style
  private String toString(Path path) {
    return path.toString().replace("\\", "/");
  }
}
