package com.github.ybroeker.maven.plugins.graphql_inspector;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;


public class ArtifactResolver {

    private static final String NODE_VERSION = "12.13.0";

    private static final String GRAPHQL_INSPECTOR_VERSION = "2.9.0";

    private static final Object RESOLUTION_LOCK = new Object();
    private static final Object EXTRACTION_LOCK = new Object();

    private final MavenProject project;

    private final boolean extractToTargetDirectory;

    private final RepositorySystemSession repositorySystemSession;

    private final PluginDescriptor pluginDescriptor;

    private final RepositorySystem repositorySystem;

    private final Log log;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ArtifactResolver(final MavenProject project, final boolean extractToTargetDirectory, final RepositorySystemSession repositorySystemSession, final PluginDescriptor pluginDescriptor, final RepositorySystem repositorySystem, final Log log) {
        this.project = project;
        this.extractToTargetDirectory = extractToTargetDirectory;
        this.repositorySystemSession = repositorySystemSession;
        this.pluginDescriptor = pluginDescriptor;
        this.repositorySystem = repositorySystem;
        this.log = log;
    }

    protected Path resolveNodeExecutable() throws MojoExecutionException {
        Artifact nodeArtifact = new DefaultArtifact(
                pluginDescriptor.getGroupId(),
                pluginDescriptor.getArtifactId(),
                getNodeClassifier(),
                "exe",
                pluginDescriptor.getVersion()
        );

        getLog().debug("Resolving node artifact " + nodeArtifact);

        Path path = resolveArtifact(nodeArtifact).getFile().toPath();

        if (!path.toFile().setExecutable(true, false)) {
            throw new MojoExecutionException("Unable to make file executable " + path);
        }

        getLog().debug("Resolved node artifact to " + path);

        return path;
    }

    protected Path extractGraphqlInspector() throws MojoExecutionException {
        Artifact artifact = new DefaultArtifact(
                pluginDescriptor.getGroupId(),
                pluginDescriptor.getArtifactId(),
                getGraphqlInspectorClassifier(),
                "zip",
                pluginDescriptor.getVersion()
        );

        getLog().debug("Resolving graphql-inspector artifact " + artifact);

        artifact = resolveArtifact(artifact);
        Path extractionPath = determineExtractionPath(resolveArtifact(artifact));

        synchronized (EXTRACTION_LOCK) {
            if (Files.isDirectory(extractionPath)) {
                getLog().debug("Reusing cached graphql-inspector at " + extractionPath);
                return extractionPath;
            }

            Path tempDir = extractionPath.resolveSibling(UUID.randomUUID().toString());
            try {
                Files.createDirectories(
                        tempDir,
                        OperatingSystemFamily.get().getGlobalPermissions()
                );
            } catch (IOException e) {
                throw new MojoExecutionException("Error creating temp directory: " + tempDir, e);
            }

            getLog().debug("Extracting graphql-inspector to " + tempDir);

            try {
                new ZipFile(artifact.getFile()).extractAll(tempDir.toString());
            } catch (ZipException e) {
                throw new MojoExecutionException("Error extracting graphql-inspector " + artifact.getFile(), e);
            }

            getLog().debug("Copying graphql-inspector to " + extractionPath);
            try {
                Files.move(tempDir, extractionPath, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                if (isIgnorableMoveError(e)) {
                    // should be a harmless race condition
                    getLog().debug("Directory already created at: " + extractionPath);
                } else {
                    String message = String.format(
                            "Error moving directory from %s to %s",
                            tempDir,
                            extractionPath
                    );

                    throw new MojoExecutionException(message, e);
                }
            }

            return extractionPath;
        }
    }

    private Path determineExtractionPath(Artifact artifact) {
        String directoryName = String.join(
                "-",
                artifact.getArtifactId(),
                artifact.getVersion(),
                artifact.getClassifier()
        );

        if (shouldExtractToTargetDir(artifact)) {
            return Paths.get(project.getBuild().getDirectory()).resolve(directoryName);
        } else {
            return artifact.getFile().toPath().resolveSibling(directoryName);
        }
    }

    private boolean shouldExtractToTargetDir(Artifact artifact) {
        return extractToTargetDirectory || isSnapshotArtifact(artifact);
    }

    private Artifact resolveArtifact(Artifact artifact) throws MojoExecutionException {
        ArtifactRequest artifactRequest = new ArtifactRequest()
                .setArtifact(artifact)
                .setRepositories(project.getRemoteProjectRepositories());

        try {
            synchronized (RESOLUTION_LOCK) {
                ArtifactResult result = repositorySystem.resolveArtifact(repositorySystemSession, artifactRequest);
                return result.getArtifact();
            }
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException(String.format("Error resolving artifact %s:%s", artifact.getGroupId(), artifact.getArtifactId()), e);
        }
    }

    private String getGraphqlInspectorClassifier() {
        return "graphql-inspector-" + GRAPHQL_INSPECTOR_VERSION;
    }

    private String getNodeClassifier() throws MojoExecutionException {
        OperatingSystemFamily osFamily = OperatingSystemFamily.get();
        return "node-" + NODE_VERSION + "-" + osFamily.getShortName();
    }

    private static boolean isIgnorableMoveError(IOException e) {
        return (
                e instanceof FileAlreadyExistsException ||
                e instanceof DirectoryNotEmptyException ||
                (e instanceof FileSystemException && e.getMessage().contains("Directory not empty"))
        );
    }

    private static boolean isSnapshotArtifact(Artifact artifact) {
        return artifact.isSnapshot() && artifact.getVersion().endsWith("-SNAPSHOT");
    }

    private Log getLog() {
        return log;
    }

}
