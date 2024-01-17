package dev.tr7zw.gradle_compose;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.fusesource.jansi.AnsiConsole;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import dev.tr7zw.gradle_compose.ComposeData.Project;
import dev.tr7zw.gradle_compose.provider.SourceProvider;
import dev.tr7zw.gradle_compose.util.ConfigUtil;
import dev.tr7zw.gradle_compose.util.FileProcessingUtil;
import dev.tr7zw.gradle_compose.util.FileUtil;
import dev.tr7zw.gradle_compose.util.GitUtil;

public class App {
	public final String version = "0.0.5";

	public static void main(String[] args) {
		new App().run(args);
	}

	public void run(String[] args) {
		setup();
		printWelcome();
		applyExecFlag();
		ComposeData data = ConfigUtil.loadLocalConfig();
		SourceProvider provider = FileUtil.getProvider(data);
		TemplateData template = ConfigUtil.getTemplateData(provider, data);
		addAutoReplacements(data, template);
		processComposition(data, template, provider);
		provider.markAsDone();
	}

	private void setup() {
		AnsiConsole.systemInstall();
	}

	private void printWelcome() {
		System.out.println("Loading gradle-compose V" + version + "...");
	}

	private void applyExecFlag() {
		if (GitUtil.gitAvailable()) {
			System.out.println("Update exec flag of gradlecw...");
			GitUtil.runGitCommand(new File("."), new String[] { "git", "add", "--chmod=+x", "./gradlecw" });
		}
	}

	private void addAutoReplacements(ComposeData data, TemplateData template) {
		StringBuilder includes = new StringBuilder();
		for (String entry : data.subProjects.keySet()) {
			includes.append("include(\"" + entry + "\")\n");
		}
		data.replacements.put("autoincludes", includes.toString());
		StringBuilder githubWorkFlow = new StringBuilder();
		for (String entry : data.subProjects.keySet()) {
			githubWorkFlow.append("            " + entry + "/build/libs/*\n");
		}
		data.replacements.put("autoworkflowfiles", githubWorkFlow.toString());
		StringBuilder forkGithubWorkFlow = new StringBuilder();
		for (String entry : data.subProjects.keySet()) {
			forkGithubWorkFlow.append("            workspace/" + entry + "/build/libs/*\n");
		}
		data.replacements.put("githubautoworkflowfiles", forkGithubWorkFlow.toString());

		if (data.enabledFlags.contains("autopublish") && new File("settings.json").exists()) {
			Map<String, String> baseReplacements = FileProcessingUtil.mergeReplacements(data.replacements,
					template.defaultReplacements);

			StringBuilder releaseText = new StringBuilder();
			try {
				JsonObject settingsData = new Gson().fromJson(
						new InputStreamReader(new FileInputStream(new File("settings.json"))), JsonObject.class);
				for (JsonElement el : settingsData.get("versions").getAsJsonArray()) {
					String version = el.getAsString();
					if (data.enabledFlags.contains("curseforge")) {
						// Curseforge releases
						releaseText.append("      - name: Publish-" + version + "-Curseforge\n");
						releaseText.append("        uses: Kir-Antipov/mc-publish@v"
								+ baseReplacements.get("mcpublishVersion") + "\n");
						releaseText.append("        with:\n");
						releaseText.append("          curseforge-id: " + data.replacements.get("curseforgeid") + "\n");
						releaseText.append("          curseforge-token: ${{ secrets.CURSEFORGE_TOKEN }}\n");
						releaseText.append("          loaders: " + getModloaderName(version).toLowerCase() + "\n");
						releaseText.append("          name: ${{github.ref_name}}-" + getMCVersion(version) + " - "
								+ getModloaderName(version) + "\n");
						if (isForgelike(version)) {
							releaseText.append("          version-type: beta\n");
						}
						releaseText.append("          files: 'versions/" + version
								+ "/build/libs/!(*-@(dev|sources|javadoc|all)).jar'\n");
						releaseText.append("          game-versions: " + getMCVersion(version) + "\n");
					}
					if (data.enabledFlags.contains("modrinth")) {
						// Modrinth releases
						releaseText.append("      - name: Publish-" + version + "-Modrinth\n");
						releaseText.append("        uses: Kir-Antipov/mc-publish@v"
								+ baseReplacements.get("mcpublishVersion") + "\n");
						releaseText.append("        with:\n");
						releaseText.append("          modrinth-id: " + data.replacements.get("modrinthid") + "\n");
						releaseText.append("          modrinth-token: ${{ secrets.MODRINTH_TOKEN }}\n");
						releaseText.append("          loaders: " + getModloaderName(version).toLowerCase() + "\n");
						releaseText.append("          name: ${{github.ref_name}}-" + getMCVersion(version) + " - "
								+ getModloaderName(version) + "\n");
						releaseText.append("          files: 'versions/" + version
								+ "/build/libs/!(*-@(dev|sources|javadoc|all)).jar'\n");
						releaseText.append("          game-versions: " + getMCVersion(version) + "\n");
					}
				}
				data.replacements.put("autoreleasesteps", releaseText.toString());
			} catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	private String getMCVersion(String version) {
		return version.split("-")[0];
	}

	private String getModloaderName(String version) {
		return version.toLowerCase().contains("fabric") ? "Fabric"
				: version.toLowerCase().contains("neoforge") ? "NeoForge" : "Forge";
	}

	private boolean isForgelike(String version) {
		return version.toLowerCase().contains("forge");
	}

	private void processComposition(ComposeData data, TemplateData template, SourceProvider provider) {
		// copy in ./gradle files
		FileUtil.copyIfAvailable(provider, "gradle/wrapper/gradle-wrapper.properties");
		FileUtil.copyIfAvailable(provider, "gradle/wrapper/gradle-wrapper.jar");
		Map<String, String> baseReplacements = FileProcessingUtil.mergeReplacements(data.replacements,
				template.defaultReplacements);
		updateProject(new File("."), baseReplacements, provider, data.rootProject, template.availableFlags,
				data.enabledFlags);
		for (Entry<String, Project> entry : data.subProjects.entrySet()) {
			updateProject(new File(".", entry.getKey()), baseReplacements, provider, entry.getValue(),
					template.availableFlags, data.enabledFlags);
		}
		if (data.version.equals("0.0.1")) {
			Set<String> customEntries = ConfigUtil.readCustomList(provider, "custom.compose");
			for (String name : customEntries) {
				FileUtil.copyIfAvailableWithReplacments(provider, new File("."), data.rootProject.template, name,
						FileProcessingUtil.mergeReplacements(data.rootProject.replacements, baseReplacements),
						template.availableFlags, data.enabledFlags);
			}
		} else {
			for (String name : template.customEntries) {
				FileUtil.copyIfAvailableWithReplacments(provider, new File("."), data.rootProject.template, name,
						FileProcessingUtil.mergeReplacements(data.rootProject.replacements, baseReplacements),
						template.availableFlags, data.enabledFlags);
			}
		}
		processFork(baseReplacements, provider, data.rootProject, template.availableFlags, data.enabledFlags);
	}

	private void updateProject(File baseDir, Map<String, String> replacements, SourceProvider provider, Project project,
			Set<String> availableTags, Set<String> enabledTags) {
		Map<String, String> projectReplacements = FileProcessingUtil.mergeReplacements(project.replacements,
				replacements);
		FileUtil.copyIfAvailableWithReplacments(provider, baseDir, project.template, "github/workflows/build.yml",
				".github/workflows/build.yml", projectReplacements, availableTags, enabledTags);
		FileUtil.copyIfAvailableWithReplacments(provider, baseDir, project.template, "github/workflows/tag.yml",
				".github/workflows/tag.yml", projectReplacements, availableTags, enabledTags);
		FileUtil.copyIfAvailableWithReplacments(provider, baseDir, project.template, "github/FUNDING.yml",
				".github/FUNDING.yml", projectReplacements, availableTags, enabledTags);
		FileUtil.copyIfAvailableWithReplacments(provider, baseDir, project.template, "build.gradle",
				projectReplacements, availableTags, enabledTags);
		FileUtil.copyIfAvailableWithReplacments(provider, baseDir, project.template, "gradle.properties",
				projectReplacements, availableTags, enabledTags);
		FileUtil.copyIfAvailableWithReplacments(provider, baseDir, project.template, "settings.gradle",
				projectReplacements, availableTags, enabledTags);
	}

	private void processFork(Map<String, String> replacements, SourceProvider provider, Project project,
			Set<String> availableTags, Set<String> enabledTags) {
		File parentDir = new File("..");
		if (new File(parentDir, "patches").exists() && new File(parentDir, "upstream").exists()
				&& new File(parentDir, "workspace").exists() && new File(parentDir, "repo").exists()
				&& new File(parentDir, "sha").exists()) {
			System.out.println("Updating fork .github folder...");
			Map<String, String> projectReplacements = FileProcessingUtil.mergeReplacements(project.replacements,
					replacements);
			FileUtil.copyIfAvailableWithReplacments(provider, parentDir, project.template,
					"github/forkworkflows/build.yml", ".github/workflows/build.yml", projectReplacements, availableTags,
					enabledTags);
			FileUtil.copyIfAvailableWithReplacments(provider, parentDir, project.template,
					"github/forkworkflows/tag.yml", ".github/workflows/tag.yml", projectReplacements, availableTags,
					enabledTags);
			FileUtil.copyIfAvailableWithReplacments(provider, parentDir, project.template, "github/FUNDING.yml",
					".github/FUNDING.yml", projectReplacements, availableTags, enabledTags);
		}
	}

}
