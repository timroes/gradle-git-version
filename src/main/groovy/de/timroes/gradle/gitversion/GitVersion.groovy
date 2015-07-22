package de.timroes.gradle.gitversion

import org.gradle.api.GradleException

/**
 * Represents the current version determined from git
 */
class GitVersion {

    final String version;

    GitVersion() {
        // First check if we are on a tag commit currently
        def tagNameProc = "git describe --tags --dirty=^dirty --exact-match".execute()
        tagNameProc.waitFor()

        if (tagNameProc.exitValue() == 0) {
            // We are on a tag commit and maybe have some dirty files
            def tagName = tagNameProc.text.trim()
            if (tagName.endsWith('^dirty')) {
                this.version = nextSnapshotVersion(tagName.replaceAll("\\^dirty", ""))
            } else {
                this.version = checkVersionTag(tagName)
            }
        } else {
            // We are not on a tag commit anymore, so find the latest tag and calculate from it.
            // Also we use 0.1.0-SNAPSHOT as a fallback if everything else goes wrong (e.g. we are
            // not in a git project, no tag yet, etc.)
            def gitDescribeProc = "git describe --tags --dirty".execute()
            gitDescribeProc.waitFor()
            if (gitDescribeProc.exitValue() == 0) {
                version = nextSnapshotVersion(gitDescribeProc.text.tokenize('-')[0])
            } else {
                version = '0.1.0-SNAPSHOT'
            }
        }
    }

    private String nextSnapshotVersion(String current) {
        def gitVersion = checkVersionTag(current)
        def (major, minor, patch) = gitVersion.tokenize('.')
        return String.format('%s.%s.%s-SNAPSHOT', major, minor.toInteger() + 1, 0)

    }

    private String checkVersionTag(String tag) {
        if(!(tag =~ /^\d+\.\d+\.\d+$/)) {
            throw new GradleException("This plugin only works on tags with semantic version formats (x.y.z). Found: ${tag}")
        }
        return tag
    }

    public boolean isSnapshot() {
        return version.endsWith('-SNAPSHOT')
    }

    public String toString() {
        return this.version
    }
}
