/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package builders.loom.plugin.java;

import java.util.List;

import builders.loom.api.AbstractPlugin;
import builders.loom.api.CompileTarget;
import builders.loom.api.DependencyScope;
import builders.loom.util.SkipChecksumUtil;

@SuppressWarnings("checkstyle:classdataabstractioncoupling")
public class JavaPlugin extends AbstractPlugin<JavaPluginSettings> {

    public JavaPlugin() {
        super(new JavaPluginSettings());
    }

    @Override
    public void configure() {
        task("resolveCompileDependencies")
            .impl(() -> new DependencyResolverTask(DependencyScope.COMPILE))
            .provides("compileDependencies", true)
            .desc("Fetches dependencies needed for main class compilation.")
            .skipHints(List.of(SkipChecksumUtil.jvmVersion(),
                SkipChecksumUtil.collection(getModuleBuildConfig().getCompileDependencies())))
            .register();

        task("resolveCompileArtifacts")
            .impl(() -> new ArtifactResolverTask(DependencyScope.COMPILE))
            .provides("compileArtifacts", true)
            .desc("Fetches compile dependencies (incl. sources) needed for IDE import.")
            .skipHints(List.of(SkipChecksumUtil.jvmVersion(),
                SkipChecksumUtil.collection(getModuleBuildConfig().getCompileDependencies())))
            .register();

        task("resolveTestDependencies")
            .impl(() -> new DependencyResolverTask(DependencyScope.TEST))
            .provides("testDependencies", true)
            .desc("Fetches dependencies needed for test class compilation.")
            .skipHints(List.of(SkipChecksumUtil.jvmVersion(),
                SkipChecksumUtil.collection(getModuleBuildConfig().getCompileDependencies()),
                SkipChecksumUtil.collection(getModuleBuildConfig().getTestDependencies())))
            .register();

        task("resolveTestArtifacts")
            .impl(() -> new ArtifactResolverTask(DependencyScope.TEST))
            .provides("testArtifacts", true)
            .desc("Fetches test dependencies (incl. sources) needed for IDE import.")
            .skipHints(List.of(SkipChecksumUtil.jvmVersion(),
                SkipChecksumUtil.collection(getModuleBuildConfig().getCompileDependencies()),
                SkipChecksumUtil.collection(getModuleBuildConfig().getTestDependencies())))
            .register();

        task("provideSource")
            .impl(() -> new JavaProvideSourceDirTask(CompileTarget.MAIN))
            .provides("source", true)
            .desc("Provides main sources for other products.")
            .register();

        task("provideTestSource")
            .impl(() -> new JavaProvideSourceDirTask(CompileTarget.TEST))
            .provides("testSource", true)
            .desc("Provides test sources for other products.")
            .register();

        task("compileJava")
            .impl(() -> new JavaCompileTask(CompileTarget.MAIN))
            .provides("compilation")
            .uses("source", "compileDependencies")
            .importFromModules("compilation", "compileDependencies")
            .skipHints(List.of(SkipChecksumUtil.jvmVersion(), () -> "Module Java version "
                + getModuleBuildConfig().getBuildSettings().getJavaPlatformVersion()))
            .desc("Compiles main sources.")
            .register();

        task("compileTestJava")
            .impl(() -> new JavaCompileTask(CompileTarget.TEST))
            .provides("testCompilation")
            .uses("compilation", "testSource", "testDependencies")
            .importFromModules("compilation", "compileDependencies")
            .desc("Compiles test sources.")
            .skipHints(List.of(SkipChecksumUtil.jvmVersion(), () -> "Module Java version "
                + getModuleBuildConfig().getBuildSettings().getJavaPlatformVersion()))
            .register();

        task("assembleJar")
            .impl(() -> new JavaAssembleTask(getPluginSettings()))
            .provides("jar")
            .uses("processedResources", "compilation")
            .desc("Assembles .jar file from compiled classes.")
            .skipHints(List.of(SkipChecksumUtil.always()))
            .register();

        task("assembleSourcesJar")
            .impl(JavaAssembleSourcesJarTask::new)
            .provides("sourcesJar")
            .uses("source", "resources")
            .desc("Assembles .jar file from main sources and main resources.")
            .skipHints(List.of(SkipChecksumUtil.always()))
            .register();

        task("provideResources")
            .impl(() -> new JavaProvideResourcesDirTask(CompileTarget.MAIN))
            .provides("resources", true)
            .desc("Provides main resources for other products.")
            .register();

        task("provideTestResources")
            .impl(() -> new JavaProvideResourcesDirTask(CompileTarget.TEST))
            .provides("testResources", true)
            .desc("Provides test resources for other products.")
            .register();

        task("processResources")
            .impl(() -> new ResourcesTask(getPluginSettings(), CompileTarget.MAIN))
            .provides("processedResources")
            .uses("resources")
            .desc("Processes main resources (copy and replace variables if necessary).")
            .skipHints(List.of(
                SkipChecksumUtil.whenNull(getPluginSettings().getResourceFilterGlob())))
            .register();

        task("processTestResources")
            .impl(() -> new ResourcesTask(getPluginSettings(), CompileTarget.TEST))
            .provides("processedTestResources")
            .uses("testResources")
            .desc("Processes test resources (copy and replace variables if necessary).")
            .skipHints(List.of(
                SkipChecksumUtil.whenNull(getPluginSettings().getResourceFilterGlob())))
            .register();

        task("javadoc")
            .impl(JavadocTask::new)
            .provides("javadoc")
            .uses("source", "compileDependencies")
            .importFromModules("compilation")
            .desc("Creates Javadoc pages.")
            .skipHints(List.of(SkipChecksumUtil.jvmVersion()))
            .register();

        task("assembleJavadocJar")
            .impl(JavaAssembleJavadocJarTask::new)
            .provides("javadocJar")
            .uses("javadoc")
            .desc("Assembles .jar file from Javadocs.")
            .skipHints(List.of(SkipChecksumUtil.always()))
            .register();

        goal("assemble")
            .requires("jar", "sourcesJar")
            .register();

        goal("check")
            .register();

        goal("build")
            .requires("assemble", "check")
            .register();
    }

}
