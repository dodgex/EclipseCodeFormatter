package krasa.formatter.settings;/*
 * External Code Formatter Copyright (c) 2007-2009 Esko Luontola, www.orfjackal.net Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the License.
 */


import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import krasa.formatter.plugin.EclipseCodeStyleManager;
import krasa.formatter.plugin.ProjectCodeStyleInstaller;
import org.jetbrains.annotations.NotNull;

/**
 * Takes care of initializing a project's CodeFormatter and disposing of it when the project is closed. Updates the
 * formatter whenever the plugin stateComponent.getState() are changed.
 *
 * @author Esko Luontola
 * @since 4.12.2007
 */
public class ProjectComponent implements com.intellij.openapi.components.ProjectComponent {

	private static final Logger LOG = Logger.getInstance(ProjectComponent.class.getName());

	@NotNull
	public static NotificationGroup getNotificationGroupError() {
		return NotificationGroupManager.getInstance().getNotificationGroup("Adapter for Eclipse Code Formatter error");
	}

	@NotNull
	public static NotificationGroup getNotificationGroupInfo() {
		return NotificationGroupManager.getInstance().getNotificationGroup("Adapter for Eclipse Code Formatter info");

	}

	@NotNull
	private final ProjectCodeStyleInstaller projectCodeStyle;
	@NotNull
	protected Project project;
	@NotNull
	private ProjectSettings projectSettings;
	private EclipseCodeStyleManager eclipseCodeStyleManager;

	public ProjectComponent(@NotNull Project project) {
		this.projectCodeStyle = new ProjectCodeStyleInstaller(project);
		this.project = project;
		this.projectSettings = ProjectSettings.getInstance(project);
	}

	public static Settings getSettings(PsiFile psiFile) {
		return getInstance(psiFile.getProject()).getSelectedProfile();
	}

	public static Settings getSettings(Project project) {
		return getInstance(project).getSelectedProfile();
	}

	@Override
	public void initComponent() {
	}


	@Override
	public void disposeComponent() {
	}

	@Override
	@NotNull
	public String getComponentName() {
		return "ProjectSettingsComponent";
	}

	@Override
	public void projectOpened() {
		projectSettings.projectOpened();
		installOrUpdate(projectSettings.getSelectedProfile());
	}

	public void installOrUpdate(@NotNull Settings settings) {
		if (eclipseCodeStyleManager == null) {
			eclipseCodeStyleManager = projectCodeStyle.install(settings);
		} else {
			eclipseCodeStyleManager.updateSettings(settings);
		}
	}

	@Override
	public void projectClosed() {
	}

	@NotNull
	public Project getProject() {
		return project;
	}

	@NotNull
	public Settings getSelectedProfile() {
		return projectSettings.getSelectedProfile();
	}

	public void globalProfileUpdated(@NotNull Settings updatedGlobalProfile) {
		projectSettings.globalProfileUpdated(updatedGlobalProfile);
		installOrUpdate(projectSettings.getSelectedProfile());
	}

	@NotNull
	public ProjectSettings getProjectSettings() {
		return projectSettings;
	}

	public static ProjectComponent getInstance(Project project) {
		return project.getComponent(ProjectComponent.class);
	}
}
