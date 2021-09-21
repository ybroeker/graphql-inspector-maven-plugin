package com.github.ybroeker.maven.plugins.graphql_inspector;

import java.lang.invoke.MethodHandles;
import java.nio.file.attribute.*;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.maven.plugin.MojoExecutionException;

enum OperatingSystemFamily {
    LINUX("linux"), MAC_OS_X("mac_os_x"), WINDOWS("windows");


    private static final Set<PosixFilePermission> GLOBAL_PERMISSIONS = PosixFilePermissions.fromString(
            "rwxrwxrwx"
    );

    private final String shortName;

    OperatingSystemFamily(String shortName) {
        this.shortName = shortName;
    }

    public String getShortName() {
        return shortName;
    }

    public FileAttribute<?>[] getGlobalPermissions() {
        if (this == WINDOWS) {
            return new FileAttribute<?>[0];
        } else {
            return new FileAttribute<?>[] {
                    PosixFilePermissions.asFileAttribute(GLOBAL_PERMISSIONS)
            };
        }
    }

    public static OperatingSystemFamily get() throws MojoExecutionException {
        String osFullName = System.getProperty("os.name");
        if (osFullName == null) {
            throw new MojoExecutionException("No os.name system property set");
        } else {
            osFullName = osFullName.toLowerCase();
        }

        if (osFullName.startsWith("linux")) {
            return OperatingSystemFamily.LINUX;
        } else if (osFullName.startsWith("mac os x")) {
            return OperatingSystemFamily.MAC_OS_X;
        } else if (osFullName.startsWith("windows")) {
            return OperatingSystemFamily.WINDOWS;
        } else {
            throw new MojoExecutionException("Unknown os.name " + osFullName);
        }
    }

}
