package org.eclipse.m2e.tests.internal.refactoring;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.Exclusion;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.refactoring.exclude.ExcludeArtifactRefactoring;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.junit.AfterClass;

public class ExcludeArtifactRefactoringTest extends AbstractMavenProjectTestCase {

	private static final String EXCLUDE_PATH = "projects/refactoring/exclude";

	private static final ArtifactKey MISSING = new ArtifactKey("a-fake-artifact", "that-should-never-exist", "1.2.3", null);

	private static final ArtifactKey VALID = new ArtifactKey("commons-logging", "commons-logging", "1.1.1", null);

	private static final ArtifactKey VALID2 = new ArtifactKey("commons-lang", "commons-lang", "1.0", null);

	private static final ArtifactKey VALID3 = new ArtifactKey("commons-beanutils", "commons-beanutils", "1.6", null);

	private static final ArtifactKey ROOT = new ArtifactKey("commons-cli", "commons-cli", "1.0", null);

	private static final ArtifactKey ROOT2 = new ArtifactKey("commons-digester", "commons-digester", "1.6", null);

	private MavenPomEditor editor = null;

	public void tearDown() throws Exception {
		if (editor != null) {
			editor.close(false);
		}
		super.tearDown();
	}

	@AfterClass
	public void afterClass() throws Exception {
		waitForJobsToComplete();
	}

	/*
	 * A pom with no parent, told to exclude an artifact that does not exist
	 */
	public void testSingleArtifactMissing() throws Exception {
		IProject project = importProjects(EXCLUDE_PATH, new String[] { "noParent/pom.xml" }, new ResolverConfiguration())[0];
		waitForJobsToComplete();

		new FindEditorRunnable(project.getFile("pom.xml")).open();
		Refactoring refactoring = new ExcludeArtifactRefactoring( project.getFile("pom.xml"), new ArtifactKey[] { MISSING });
		RefactoringStatus status = refactoring.checkInitialConditions(monitor);
		assertEquals("Expected FATAL status from checkInitialConditions: ", RefactoringStatus.FATAL, status.getSeverity());
		assertMessage("Unexpected message", "No pom found for operation", status);
	}

	/*
	 * A pom without a parent and a valid exclusion
	 */
	public void testSingleArtifactNoParent() throws Exception {
		IProject project = importProjects(EXCLUDE_PATH, new String[] { "noParent/pom.xml" }, new ResolverConfiguration())[0];
		waitForJobsToComplete();

		new FindEditorRunnable(project.getFile("pom.xml")).open();
		Refactoring refactoring = new ExcludeArtifactRefactoring( project.getFile("pom.xml"), new ArtifactKey[] { VALID });
		RefactoringStatus status = refactoring.checkInitialConditions(monitor);
		assertTrue("Expected OK status from checkInitialConditions: " + status.toString(), status.isOK());

		status = refactoring.checkFinalConditions(monitor);
		assertTrue("Expected OK status from checkFinalConditions: " + status.toString(), status.isOK());

		Change change = refactoring.createChange(monitor);
		assertTrue(change.isEnabled());

		assertAffected("Expected change to affect pom.xml", project.getFile("pom.xml"), change);
		Change undo = change.perform(monitor);
		assertTrue("Editor is dirty", editor.isDirty());
		assertNotNull("Undo Operation", undo);

		assertExclusionSet("pom has exclusion set", editor, ROOT, VALID);
	}

	/*
	 * A pom with a workspace parent and a single valid exclude
	 */
	public void testSingleArtifactKeyWorkspaceParent() throws Exception {
		IProject[] projects = importProjects(EXCLUDE_PATH + "/workspaceParent", new String[] { "workspaceParentProject/pom.xml", "workspaceParentModule/pom.xml" }, new ResolverConfiguration());
		waitForJobsToComplete();
		IProject project = getProject(projects, "workspaceParentModule");

		new FindEditorRunnable(project.getFile("pom.xml")).open();
		Refactoring refactoring = new ExcludeArtifactRefactoring(project.getFile("pom.xml"), new ArtifactKey[] { VALID });
		RefactoringStatus status = refactoring.checkInitialConditions(monitor);
		assertTrue("Expected OK status from checkInitialConditions: " + status.toString(), status.isOK());

		status = refactoring.checkFinalConditions(monitor);
		assertTrue("Expected OK status from checkFinalConditions: " + status.toString(), status.isOK());

		Change change = refactoring.createChange(monitor);
		assertTrue(change.isEnabled());

		assertAffected("Expected change to affect module's pom.xml", project.getFile("pom.xml"), change);
		Change undo = change.perform(monitor);
		assertTrue("Editor is dirty", editor.isDirty());
		assertNotNull("Undo Operation", undo);

		assertExclusionSet("pom has exclusion set", editor, ROOT, VALID);
	}

	/*
	 * A pom with a workspace parent and an exclude which takes place on the
	 * parent.
	 */
	public void testSingleArtifactKeyInWorkspaceParent() throws Exception {
		IProject[] projects = importProjects(EXCLUDE_PATH + "/workspaceParentWithDependency", new String[] { "workspaceParentWithDependencyProject/pom.xml",
				"workspaceParentWithDependencyModule/pom.xml" }, new ResolverConfiguration());
		waitForJobsToComplete();
		IProject project = getProject(projects, "workspaceParentWithDependencyModule");
		new FindEditorRunnable(project.getFile("pom.xml")).open();
		Refactoring refactoring = new ExcludeArtifactRefactoring(project.getFile("pom.xml"), new ArtifactKey[] { VALID });
		RefactoringStatus status = refactoring.checkInitialConditions(monitor);
		assertTrue("Expected OK status from checkInitialConditions: " + status.toString(), status.isOK());

		status = refactoring.checkFinalConditions(monitor);
		assertTrue("Expected OK status from checkFinalConditions: " + status.toString(), status.isOK());

		Change change = refactoring.createChange(monitor);
		assertTrue(change.isEnabled());
		assertAffected("Expected parent to be changed", getProject(projects, "workspaceParentWithDependencyProject").getFile("pom.xml"), change);

		Change undo = change.perform(monitor);
		assertNotNull("Undo Operation", undo);

		assertExclusionSet("pom has exclusion set", getProject(projects, "workspaceParentWithDependencyProject"), ROOT, VALID);
	}

	/*
	 * A pom with a remote parent and a valid exclude
	 */
	public void testSingleArtifactKeyRemoteParent() throws Exception {
		IProject project = importProjects(EXCLUDE_PATH, new String[] { "hasRemoteParent/pom.xml" }, new ResolverConfiguration())[0];
		waitForJobsToComplete();

		new FindEditorRunnable(project.getFile("pom.xml")).open();
		Refactoring refactoring = new ExcludeArtifactRefactoring(project.getFile("pom.xml"), new ArtifactKey[] { VALID });
		RefactoringStatus status = refactoring.checkInitialConditions(monitor);
		assertTrue("Expected OK status from checkInitialConditions: " + status.toString(), status.isOK());

		status = refactoring.checkFinalConditions(monitor);
		assertTrue("Expected OK status from checkFinalConditions: " + status.toString(), status.isOK());

		Change change = refactoring.createChange(monitor);
		assertTrue(change.isEnabled());

		assertAffected("Expected change to affect local pom.xml", project.getFile("pom.xml"), change);
		Change undo = change.perform(monitor);
		assertTrue("Editor is dirty", editor.isDirty());
		assertNotNull("Undo Operation", undo);

		assertExclusionSet("pom has exclusion set", editor, ROOT, VALID);
	}

	/*
	 * A pom without a parent, a missing dependency and a valid dependency
	 */
	public void testMultipleArtifactWithMissing() throws Exception {
		IProject project = importProjects(EXCLUDE_PATH, new String[] { "noParent/pom.xml" }, new ResolverConfiguration())[0];
		waitForJobsToComplete();

		new FindEditorRunnable(project.getFile("pom.xml")).open();
		Refactoring refactoring = new ExcludeArtifactRefactoring(project.getFile("pom.xml"), new ArtifactKey[] { VALID, MISSING });
		RefactoringStatus status = refactoring.checkInitialConditions(monitor);
		assertEquals("Expected FATAL status from checkInitialConditions: ", RefactoringStatus.FATAL, status.getSeverity());
		assertMessage("Missing Dependency Expected", "Failed to locate: a-fake-artifact:that-should-never-exist:1.2.3", status);
	}

	/*
	 * A pom with a workspace parent with exclusions in both
	 */
	public void testArtifactsInMultiplePom() throws Exception {
		IProject[] projects = importProjects(EXCLUDE_PATH + "/workspaceParent2", new String[] { "workspaceParent2Module/pom.xml", "workspaceParent2Project/pom.xml" }, new ResolverConfiguration());
		waitForJobsToComplete();

		IProject module = getProject(projects, "workspaceParent2Module");
		IProject project = getProject(projects, "workspaceParent2Project");
		new FindEditorRunnable(module.getFile("pom.xml")).open();
		Refactoring refactoring = new ExcludeArtifactRefactoring(module.getFile("pom.xml"), new ArtifactKey[] { VALID, VALID3 });
		RefactoringStatus status = refactoring.checkInitialConditions(monitor);
		assertTrue("Expected OK status from checkInitialConditions: " + status.toString(), status.isOK());

		status = refactoring.checkFinalConditions(monitor);
		assertTrue("Expected OK status from checkFinalConditions: " + status.toString(), status.isOK());

		Change change = refactoring.createChange(monitor);
		assertTrue(change.isEnabled());

		assertAffected("Expected change to affect pom.xml", module.getFile("pom.xml"), change);
		assertAffected("Expected change to affect pom.xml", project.getFile("pom.xml"), change);
		Change undo = change.perform(monitor);
		assertTrue("Editor is dirty", editor.isDirty());
		assertNotNull("Undo Operation", undo);

		assertExclusionSet("pom has exclusion set", editor, ROOT, VALID);
		assertExclusionSet("pom has exclusion set", project, ROOT2, VALID3);
	}

	/*
	 * A single pom with multiple exclusions
	 */
	public void testMultipleArtifactKeySinglePom() throws Exception {
		IProject project = importProjects(EXCLUDE_PATH, new String[] { "noParent/pom.xml" }, new ResolverConfiguration())[0];
		waitForJobsToComplete();

		new FindEditorRunnable(project.getFile("pom.xml")).open();
		Refactoring refactoring = new ExcludeArtifactRefactoring(
				project.getFile("pom.xml"), new ArtifactKey[] { VALID, VALID2 });
		RefactoringStatus status = refactoring.checkInitialConditions(monitor);
		assertTrue("Expected OK status from checkInitialConditions: " + status.toString(), status.isOK());

		status = refactoring.checkFinalConditions(monitor);
		assertTrue("Expected OK status from checkFinalConditions: " + status.toString(), status.isOK());

		Change change = refactoring.createChange(monitor);
		assertTrue(change.isEnabled());

		assertAffected("Expected change to affect pom.xml", project.getFile("pom.xml"), change);
		Change undo = change.perform(monitor);
		assertTrue("Editor is dirty", editor.isDirty());
		assertNotNull("Undo Operation", undo);

		assertExclusionSet("pom has exclusion set", editor, ROOT, VALID);
		assertExclusionSet("pom has exclusion set", editor, ROOT, VALID2);
	}

	/*
	 * A pom with strange name
	 */
	public void testStrangePomName() throws Exception {
		IProject project = importProjects(EXCLUDE_PATH, new String[] { "noParent/test-pom.xml" }, new ResolverConfiguration())[0];
		waitForJobsToComplete();

		new FindEditorRunnable(project.getFile("pom.xml")).open();
		Refactoring refactoring = new ExcludeArtifactRefactoring( project.getFile("pom.xml"), new ArtifactKey[] { VALID });
		RefactoringStatus status = refactoring.checkInitialConditions(monitor);
		assertTrue("Expected OK status from checkInitialConditions: " + status.toString(), status.isOK());

		status = refactoring.checkFinalConditions(monitor);
		assertTrue("Expected OK status from checkFinalConditions: " + status.toString(), status.isOK());

		Change change = refactoring.createChange(monitor);
		assertTrue(change.isEnabled());

		assertAffected("Expected change to affect test-pom.xml", project.getFile("pom.xml"), change);
		Change undo = change.perform(monitor);
		assertTrue("Editor is dirty", editor.isDirty());
		assertNotNull("Undo Operation", undo);

		assertExclusionSet("pom has exclusion set", editor, ROOT, VALID);
	}

	private class FindEditorRunnable implements Runnable {
		private IEditorInput editorInput;
		private PartInitException exception;

		FindEditorRunnable(IFile file) {
			editorInput = new FileEditorInput(file);
		}

		MavenPomEditor open() throws CoreException, InterruptedException {
			if (Display.getDefault().getThread() == Thread.currentThread()) {
				editor = (MavenPomEditor) getActivePage().openEditor(editorInput, "org.eclipse.m2e.editor.MavenPomEditor", true);
			} else {
				Display.getDefault().syncExec(this);
				if (exception != null) {
					throw new CoreException(exception.getStatus());
				}
			}
			waitForJobsToComplete();
			return editor;
		}

		public void run() {
			try {
				editor = (MavenPomEditor) getActivePage().openEditor(editorInput, "org.eclipse.m2e.editor.MavenPomEditor", true);
			} catch (PartInitException e) {
				this.exception = e;
			}
		}
	}

	protected static IWorkbenchPage getActivePage() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		return workbench.getWorkbenchWindows()[0].getActivePage();
	}

	protected static IProject getProject(IProject[] projects, String name) {
		for (IProject project : projects) {
			if (project.getName().equals(name)) {
				return project;
			}
		}
		fail("Failed to locate project " + name);
		return null;
	}

	private Model getModelFromEditor() throws Exception {
		if (editor != null) {
			return editor.readProjectDocument();
		}
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorReference ref : page.getEditorReferences()) {
					IEditorPart part = ref.getEditor(true);
					if (part != null && part instanceof MavenPomEditor) {
						return ((MavenPomEditor) part).readProjectDocument();
					}
				}
			}
		}
		fail("Failed to get Model");
		return null;
	}

	/*
	 * Assert that a Change affects the given file
	 */
	protected static void assertAffected(String message, IFile file, Change change) {
		if (change instanceof CompositeChange) {
			for (Change chg : ((CompositeChange) change).getChildren()) {
				if (chg instanceof TextFileChange && file.equals(((TextFileChange) chg).getModifiedElement())) {
					return;
				}
			}
		}
		fail(message);
	}

	/*
	 * Assert the expected message on RefactoringStatus entry.
	 */
	protected static void assertMessage(String failMessage, String msg, RefactoringStatus status) {
		for (RefactoringStatusEntry entry : status.getEntries()) {
			if (entry.getMessage().equals(msg)) {
				return;
			}
		}
		fail(failMessage);
	}

	/*
	 * Assert the editor has the given exclusion set
	 */
	protected static void assertExclusionSet(String msg, MavenPomEditor editor, ArtifactKey dependencyKey, ArtifactKey excluded) throws Exception {
		Model model = editor.readProjectDocument();
		Dependency d = null;
		for (Dependency dep : model.getDependencies()) {
			if (dep.getArtifactId().equals(dependencyKey.getArtifactId()) && dep.getGroupId().equals(dependencyKey.getGroupId()) && dep.getVersion().equals(dependencyKey.getVersion())) {
				d = dep;
				break;
			}
		}
		assertNotNull("Model does not have dependency: " + dependencyKey, d);
		for (Exclusion ex : d.getExclusions()) {
			if (ex.getArtifactId().equals(excluded.getArtifactId()) && ex.getGroupId().equals(excluded.getGroupId())) {
				return;
			}
		}
		fail(msg);
	}

	/*
	 * Assert the editor has the given exclusion set
	 */
	protected static void assertExclusionSet(String msg, IProject project, ArtifactKey dependencyKey, ArtifactKey excluded) throws Exception {
		Model model = MavenPlugin.getDefault().getMavenModelManager().loadResource(project.getFile("pom.xml")).getModel();
		Dependency d = null;
		for (Dependency dep : model.getDependencies()) {
			if (dep.getArtifactId().equals(dependencyKey.getArtifactId()) && dep.getGroupId().equals(dependencyKey.getGroupId()) && dep.getVersion().equals(dependencyKey.getVersion())) {
				d = dep;
				break;
			}
		}
		assertNotNull("Model does not have dependency: " + dependencyKey, d);
		for (Exclusion ex : d.getExclusions()) {
			if (ex.getArtifactId().equals(excluded.getArtifactId()) && ex.getGroupId().equals(excluded.getGroupId())) {
				return;
			}
		}
		fail(msg);
	}
}
