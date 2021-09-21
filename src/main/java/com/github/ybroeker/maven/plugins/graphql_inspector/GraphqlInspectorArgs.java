package com.github.ybroeker.maven.plugins.graphql_inspector;

import java.nio.file.Path;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;


public abstract class GraphqlInspectorArgs extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = false)
    protected MavenProject project;

    @Parameter(defaultValue = "false")
    private boolean extractGraphqlInspectorToTargetDirectory;

    @Parameter(
            defaultValue = "${repositorySystemSession}",
            required = true,
            readonly = true
    )
    private RepositorySystemSession repositorySystemSession;

    @Component
    private PluginDescriptor pluginDescriptor;

    @Component
    private RepositorySystem repositorySystem;

    protected Path resolveNodeExecutable() throws MojoExecutionException {
        final ArtifactResolver artifactResolver = new ArtifactResolver(project,
                extractGraphqlInspectorToTargetDirectory,
                repositorySystemSession,
                pluginDescriptor,
                repositorySystem,
                getLog());
        return artifactResolver.resolveNodeExecutable();
    }

    protected Path extractGraphqlInspector() throws MojoExecutionException {
        final ArtifactResolver artifactResolver = new ArtifactResolver(project,
                extractGraphqlInspectorToTargetDirectory,
                repositorySystemSession,
                pluginDescriptor,
                repositorySystem,
                getLog());
        return artifactResolver.extractGraphqlInspector();
    }


}
