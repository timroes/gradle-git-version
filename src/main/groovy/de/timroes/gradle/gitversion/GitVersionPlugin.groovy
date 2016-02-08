package de.timroes.gradle.gitversion

import org.gradle.api.Plugin
import org.gradle.api.Project

public class GitVersionPlugin implements Plugin<Project> {

	void apply(Project project) {
		project.ext.gitVersion = new GitVersion()
	}

}
