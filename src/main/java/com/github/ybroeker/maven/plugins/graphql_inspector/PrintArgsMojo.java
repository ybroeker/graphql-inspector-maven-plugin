package com.github.ybroeker.maven.plugins.graphql_inspector;

import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "print-args", requiresProject = false)
public class PrintArgsMojo extends GraphqlInspectorArgs {

  @Override
  public final void execute() throws MojoExecutionException {
    Path nodeExecutable = resolveNodeExecutable();

    Path graphqlInspectorDirectory = extractGraphqlInspector();

    Path graphqlInspectorBin = graphqlInspectorDirectory
            .resolve("graphql-inspector")
            .resolve("node_modules")
            .resolve("@graphql-inspector/cli")
            .resolve("index.js");

    /*
    Use System.out rather than getLog() because we don't want any leading characters
     */
    System.out.println("nodeExecutable=" + nodeExecutable);
    System.out.println("graphqlInspectorBin=" + graphqlInspectorBin);

  }
}
