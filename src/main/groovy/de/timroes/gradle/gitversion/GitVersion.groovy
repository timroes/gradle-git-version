package de.timroes.gradle.gitversion

import org.gradle.api.GradleException

/**
 * Represents the current version determined from git
 */
class GitVersion {

	final int[] version;
	final boolean isSnapshot;
	final String commitHash;

	GitVersion() {
		// First check if we are on a tag commit currently
		def tagNameProc = "git describe --tags --dirty=^dirty --exact-match".execute()
		tagNameProc.waitFor()

		if (tagNameProc.exitValue() == 0) {
			// We are on a tag commit and maybe have some dirty files
			def tagName = tagNameProc.text.trim()
			if (tagName.endsWith('^dirty')) {
				version = checkVersionTag(tagName.replaceAll("\\^dirty", ""))
				isSnapshot = true
			} else {
				version = checkVersionTag(tagName)
				isSnapshot = false
			}
		} else {
			// We are not on a tag commit anymore, so find the latest tag and calculate from it.
			// Also we use 0.1.0-SNAPSHOT as a fallback if everything else goes wrong (e.g. we are
			// not in a git project, no tag yet, etc.)
			def gitDescribeProc = "git describe --tags --dirty".execute()
			gitDescribeProc.waitFor()
			if (gitDescribeProc.exitValue() == 0) {
				version = checkVersionTag(gitDescribeProc.text.tokenize('-')[0])
				isSnapshot = true
			} else {
				version = [0, 0, 0]
				isSnapshot = true
			}
		}

		// Read out commit hash of repository
		def gitCommitHash = "git rev-parse --short HEAD".execute()
		gitCommitHash.waitFor()

		commitHash = (gitCommitHash.exitValue() == 0) ? gitCommitHash.text.trim() : null;
	}

	private int[] checkVersionTag(String tag) {
		if(!(tag =~ /^\d+\.\d+\.\d+$/)) {
			throw new GradleException("This plugin only works on tags with semantic version formats (x.y.z). Found: ${tag}")
		}
		return tag.tokenize('.') as int[]
	}

	public boolean isSnapshot() {
		return isSnapshot
	}

	public String toString() {
		return toString(null)
	}

	public String toString(Map params) {
		def (major, minor, patch) = version
		def versionString = String.format(
				isSnapshot ? '%d.%d.%d-SNAPSHOT' : '%d.%d.%d',
				major,
				isSnapshot ? minor + 1 : minor,
				isSnapshot ? 0 : patch)

		// If withHash option was set or withHashIfSnapshot and we are on snapshot prepand commit hash
		if (params?.get('withHash') || (params?.get('withHashIfSnapshot') && isSnapshot)) {
			versionString += "-${commitHash}"
		}

		return versionString
	}
}
