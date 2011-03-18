package org.eclipse.m2e.tests.internal.refactoring;

import java.util.HashMap;

import org.junit.AfterClass;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.editor.composites.ParentGatherer;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.Exclusion;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.util.PomResourceFactoryImpl;
import org.eclipse.m2e.model.edit.pom.util.PomResourceImpl;
import org.eclipse.m2e.refactoring.exclude.ExcludeArtifactRefactoring;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
		ExcludeArtifactRefactoring refactoring = createRefactoring(project.getFile("pom.xml"), new ArtifactKey[] { MISSING });
		RefactoringStatus status = refactoring.checkInitialConditions(monitor);
		assertEquals("Expected OK status from checkInitialConditions: ", RefactoringStatus.OK, status.getSeverity());

		status = refactoring.checkFinalConditions(monitor);
		assertEquals("Expected FATAL status from checkFinalConditions: ", RefactoringStatus.FATAL, status.getSeverity());
    assertTrue("Refactoring Message", hasMessage("Unable to locate source for dependency.", status));
	}

	/*
	 * A pom without a parent and a valid exclusion
	 */
	public void testSingleArtifactNoParent() throws Exception {
		IProject project = importProjects(EXCLUDE_PATH, new String[] { "noParent/pom.xml" }, new ResolverConfiguration())[0];
		waitForJobsToComplete();

		new FindEditorRunnable(project.getFile("pom.xml")).open();
		ExcludeArtifactRefactoring refactoring = createRefactoring(project.getFile("pom.xml"), new ArtifactKey[] { VALID });
		RefactoringStatus status = refactoring.checkInitialConditions(monitor);
		assertTrue("Expected OK status from checkInitialConditions: " + status.toString(), status.isOK());

		status = refactoring.checkFinalConditions(monitor);
		assertTrue("Expected OK status from checkFinalConditions: " + status.toString(), status.isOK());

		Change change = refactoring.createChange(monitor);
		assertTrue(change.isEnabled());

		assertTrue("Expected change to affect pom.xml", isAffected(project.getFile("pom.xml"), change));
		Change undo = change.perform(monitor);
		assertTrue("Editor is dirty", editor.isDirty());
		assertNotNull("Undo Operation", undo);

		assertTrue("pom has exclusion set", hasExclusionSet(editor, ROOT, VALID));
	}

	/*
	 * A pom with a workspace parent and a single valid exclude
	 */
	public void testSingleArtifactKeyWorkspaceParent() throws Exception {
		IProject[] projects = importProjects(EXCLUDE_PATH + "/workspaceParent", new String[] { "workspaceParentProject/pom.xml", "workspaceParentModule/pom.xml" }, new ResolverConfiguration());
		waitForJobsToComplete();
		IProject project = getProject(projects, "workspaceParentModule");

		new FindEditorRunnable(project.getFile("pom.xml")).open();
		ExcludeArtifactRefactoring refactoring = createRefactoring(project.getFile("pom.xml"), new ArtifactKey[] { VALID });
		RefactoringStatus status = refactoring.checkInitialConditions(monitor);
		assertTrue("Expected OK status from checkInitialConditions: " + status.toString(), status.isOK());

		status = refactoring.checkFinalConditions(monitor);
		assertTrue("Expected OK status from checkFinalConditions: " + status.toString(), status.isOK());

		Change change = refactoring.createChange(monitor);
		assertTrue(change.isEnabled());

		assertTrue("Expected change to affect module's pom.xml", isAffected(project.getFile("pom.xml"), change));
		Change undo = change.perform(monitor);
		assertTrue("Editor is dirty", editor.isDirty());
		assertNotNull("Undo Operation", undo);

		assertTrue("pom has exclusion set", hasExclusionSet(editor, ROOT, VALID));
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
		ExcludeArtifactRefactoring refactoring = createRefactoring(project.getFile("pom.xml"), new ArtifactKey[] { VALID });
		RefactoringStatus status = refactoring.checkInitialConditions(monitor);
		assertTrue("Expected OK status from checkInitialConditions: " + status.toString(), status.isOK());

		status = refactoring.checkFinalConditions(monitor);
		assertTrue("Expected OK status from checkFinalConditions: " + status.toString(), status.isOK());

		Change change = refactoring.createChange(monitor);
		assertTrue(change.isEnabled());
		assertTrue("Expected parent to be changed", isAffected(getProject(projects, "workspaceParentWithDependencyModule").getFile("pom.xml"), change));

		Change undo = change.perform(monitor);
		assertNotNull("Undo Operation", undo);

		assertTrue("pom has exclusion set", hasExclusionSet(getProject(projects, "workspaceParentWithDependencyModule"), ROOT, VALID));
	}

	/*
	 * A pom with a remote parent and a valid exclude
	 */
	public void testSingleArtifactKeyRemoteParent() throws Exception {
		IProject project = importProjects(EXCLUDE_PATH, new String[] { "hasRemoteParent/pom.xml" }, new ResolverConfiguration())[0];
		waitForJobsToComplete();

		new FindEditorRunnable(project.getFile("pom.xml")).open();
		ExcludeArtifactRefactoring refactoring = createRefactoring(project.getFile("pom.xml"), new ArtifactKey[] { VALID });
		RefactoringStatus status = refactoring.checkInitialConditions(monitor);
		assertTrue("Expected OK status from checkInitialConditions: " + status.toString(), status.isOK());

		status = refactoring.checkFinalConditions(monitor);
		assertTrue("Expected OK status from checkFinalConditions: " + status.toString(), status.isOK());

		Change change = refactoring.createChange(monitor);
		assertTrue(change.isEnabled());

		assertTrue("Expected change to affect local pom.xml", isAffected(project.getFile("pom.xml"), change));
		Change undo = change.perform(monitor);
		assertTrue("Editor is dirty", editor.isDirty());
		assertNotNull("Undo Operation", undo);

		assertTrue("pom has exclusion set", hasExclusionSet(editor, ROOT, VALID));
	}

	/*
	 * A pom without a parent, a missing dependency and a valid dependency
	 */
	public void testMultipleArtifactWithMissing() throws Exception {
		IProject project = importProjects(EXCLUDE_PATH, new String[] { "noParent/pom.xml" }, new ResolverConfiguration())[0];
		waitForJobsToComplete();

		new FindEditorRunnable(project.getFile("pom.xml")).open();
		ExcludeArtifactRefactoring refactoring = createRefactoring(project.getFile("pom.xml"), new ArtifactKey[] { VALID, MISSING });
		RefactoringStatus status = refactoring.checkInitialConditions(monitor);
		assertEquals("Expected OK status from checkInitialConditions: ", RefactoringStatus.OK, status.getSeverity());

		status = refactoring.checkFinalConditions(monitor);
		assertEquals("Expected FATAL status from checkFinalConditions: ", RefactoringStatus.ERROR, status.getSeverity());
		assertTrue("Missing Dependency Expected", hasMessage("Unable to locate source for dependency a-fake-artifact:that-should-never-exist:1.2.3 in the workspace.", status));

		Change change = refactoring.createChange(monitor);
		assertTrue(change.isEnabled());

		assertTrue("Expected change to affect pom.xml", isAffected(project.getFile("pom.xml"), change));
		Change undo = change.perform(monitor);
		assertTrue("Editor is dirty", editor.isDirty());
		assertNotNull("Undo Operation", undo);

		assertTrue("pom has exclusion set", hasExclusionSet(editor, ROOT, VALID));
	}

	/*
	 * A pom with a workspace parent with exclusions in both
	 */
	public void testArtifactsInMultiplePom() throws Exception {
		IProject[] projects = importProjects(EXCLUDE_PATH + "/workspaceParent2", new String[] { "workspaceParent2Module/pom.xml", "workspaceParent2Project/pom.xml" }, new ResolverConfiguration());
		waitForJobsToComplete();

		IProject module = getProject(projects, "workspaceParent2Module");
		new FindEditorRunnable(module.getFile("pom.xml")).open();
		ExcludeArtifactRefactoring refactoring = createRefactoring(module.getFile("pom.xml"), new ArtifactKey[] { VALID, VALID3 });
		RefactoringStatus status = refactoring.checkInitialConditions(monitor);
		assertTrue("Expected OK status from checkInitialConditions: " + status.toString(), status.isOK());

		status = refactoring.checkFinalConditions(monitor);
		assertTrue("Expected OK status from checkFinalConditions: " + status.toString(), status.isOK());

		Change change = refactoring.createChange(monitor);
		assertTrue(change.isEnabled());

		assertTrue("Expected change to affect pom.xml", isAffected(module.getFile("pom.xml"), change));
		Change undo = change.perform(monitor);
		assertTrue("Editor is dirty", editor.isDirty());
		assertNotNull("Undo Operation", undo);

		assertTrue("pom has exclusion set", hasExclusionSet(editor, ROOT, VALID));
		assertTrue("pom has exclusion set", hasExclusionSet(editor, ROOT2, VALID3));
	}

	/*
	 * A single pom with multiple exclusions
	 */
	public void testMultipleArtifactKeySinglePom() throws Exception {
		IProject project = importProjects(EXCLUDE_PATH, new String[] { "noParent/pom.xml" }, new ResolverConfiguration())[0];
		waitForJobsToComplete();

		new FindEditorRunnable(project.getFile("pom.xml")).open();
		ExcludeArtifactRefactoring refactoring = createRefactoring(project.getFile("pom.xml"), new ArtifactKey[] { VALID, VALID2 });
		RefactoringStatus status = refactoring.checkInitialConditions(monitor);
		assertTrue("Expected OK status from checkInitialConditions: " + status.toString(), status.isOK());

		status = refactoring.checkFinalConditions(monitor);
		assertTrue("Expected OK status from checkFinalConditions: " + status.toString(), status.isOK());

		Change change = refactoring.createChange(monitor);
		assertTrue(change.isEnabled());

		assertTrue("Expected change to affect pom.xml", isAffected(project.getFile("pom.xml"), change));
		Change undo = change.perform(monitor);
		assertTrue("Editor is dirty", editor.isDirty());
		assertNotNull("Undo Operation", undo);

		assertTrue("pom has exclusion set", hasExclusionSet(editor, ROOT, VALID));
		assertTrue("pom has exclusion set", hasExclusionSet(editor, ROOT, VALID2));
	}

	/*
	 * A pom with strange name
	 */
	public void testStrangePomName() throws Exception {
		IProject project = importProjects(EXCLUDE_PATH, new String[] { "noParent/test-pom.xml" }, new ResolverConfiguration())[0];
		waitForJobsToComplete();

		new FindEditorRunnable(project.getFile("pom.xml")).open();
		ExcludeArtifactRefactoring refactoring = createRefactoring(project.getFile("pom.xml"), new ArtifactKey[] { VALID });
		RefactoringStatus status = refactoring.checkInitialConditions(monitor);
		assertTrue("Expected OK status from checkInitialConditions: " + status.toString(), status.isOK());

		status = refactoring.checkFinalConditions(monitor);
		assertTrue("Expected OK status from checkFinalConditions: " + status.toString(), status.isOK());

		Change change = refactoring.createChange(monitor);
		assertTrue(change.isEnabled());

		assertTrue("Expected change to affect test-pom.xml", isAffected(project.getFile("pom.xml"), change));
		Change undo = change.perform(monitor);
		assertTrue("Editor is dirty", editor.isDirty());
		assertNotNull("Undo Operation", undo);

		assertTrue("pom has exclusion set", hasExclusionSet(editor, ROOT, VALID));
	}

	public void testCopyDown() throws Exception {
		IProject[] projects = importProjects(EXCLUDE_PATH + "/workspaceParentWithDependency", new String[] { "workspaceParentWithDependencyProject/pom.xml",
				"workspaceParentWithDependencyModule/pom.xml" }, new ResolverConfiguration());
		waitForJobsToComplete();
		IProject module = getProject(projects, "workspaceParentWithDependencyModule");
		new FindEditorRunnable(module.getFile("pom.xml")).open();
		ExcludeArtifactRefactoring refactoring = createRefactoring(module.getFile("pom.xml"), new ArtifactKey[] { VALID });
		RefactoringStatus status = refactoring.checkInitialConditions(monitor);
		assertTrue("Expected OK status from checkInitialConditions: " + status.toString(), status.isOK());

		status = refactoring.checkFinalConditions(monitor);
		assertTrue("Expected OK status from checkFinalConditions: " + status.toString(), status.isOK());

		Change change = refactoring.createChange(monitor);
		assertTrue(change.isEnabled());
		assertTrue("Expected module to be changed", isAffected(module.getFile("pom.xml"), change));

		Change undo = change.perform(monitor);
		assertNotNull("Undo Operation", undo);

		assertTrue("project has exclusion set", hasExclusionSet(module, ROOT, VALID));
	}

	public void testPullUp() throws Exception {
		IProject[] projects = importProjects(EXCLUDE_PATH + "/workspaceParent", new String[] { "workspaceParentProject/pom.xml", "workspaceParentModule/pom.xml" }, new ResolverConfiguration());
		waitForJobsToComplete();
		IProject module = getProject(projects, "workspaceParentModule");
		IProject project = getProject(projects, "workspaceParentProject");
		new FindEditorRunnable(module.getFile("pom.xml")).open();
		ExcludeArtifactRefactoring refactoring = createRefactoring(module.getFile("pom.xml"), new ArtifactKey[] { VALID }, project.getFile("pom.xml"));
		RefactoringStatus status = refactoring.checkFinalConditions(monitor);
		assertTrue("Expected OK status from checkFinalConditions: " + status.toString(), status.isOK());

		Change change = refactoring.createChange(monitor);
		assertTrue(change.isEnabled());
		assertTrue("Expected module to be changed", isAffected(module.getFile("pom.xml"), change));
		assertTrue("Expected module to be changed", isAffected(project.getFile("pom.xml"), change));

		Change undo = change.perform(monitor);
		assertNotNull("Undo Operation", undo);

		assertTrue("project has exclusion set", hasExclusionSet(project, ROOT, VALID));

	}

	private static ExcludeArtifactRefactoring createRefactoring(IFile pomFile, ArtifactKey[] keys) throws CoreException {
		return createRefactoring(pomFile, keys, pomFile);
	}

	private static ExcludeArtifactRefactoring createRefactoring(IFile pomFile, ArtifactKey[] keys, IFile exclusionPoint) throws CoreException {
		ExcludeArtifactRefactoring refactoring = new ExcludeArtifactRefactoring(pomFile, keys);
		IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManager().create(pomFile, true, monitor);
		ParentGatherer g = new ParentGatherer(facade.getMavenProject(monitor), facade);
		refactoring.setHierarchy(g.getParentHierarchy(monitor));

		IMavenProjectFacade excFacade = MavenPlugin.getDefault().getMavenProjectManager().create(exclusionPoint, true, monitor);
		refactoring.setExclusionPoint(excFacade.getMavenProject(monitor));
		return refactoring;
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

	/*
	 * Assert that a Change affects the given file
	 */
	protected static boolean isAffected(IFile file, Change change) {
		if (change instanceof CompositeChange) {
			IDocument open = getOpenDocument(file);
			for (Change chg : ((CompositeChange) change).getChildren()) {
				if (chg instanceof TextFileChange && file.equals(((TextFileChange) chg).getModifiedElement())) {
					return true;
				} else if (chg instanceof DocumentChange && chg.getModifiedElement().equals(open)) {
					return true;
				}
			}
		}
		return false;
	}

	private static IDocument getOpenDocument(IFile file) {
		IStructuredModel model = StructuredModelManager.getModelManager().getExistingModelForEdit(file);
		if (model != null) {
			return model.getStructuredDocument();
		}
		return null;
	}

	/*
	 * Assert the expected message on RefactoringStatus entry.
	 */
	protected static boolean hasMessage(String msg, RefactoringStatus status) {
		for (RefactoringStatusEntry entry : status.getEntries()) {
			if (entry.getMessage().equals(msg)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * The editor has the given exclusion set
	 */
	protected static boolean hasExclusionSet(MavenPomEditor editor, final ArtifactKey dependencyKey, final ArtifactKey excluded) throws Exception {
		final boolean[] found = new boolean[1];
		found[0] = false;
		performOnDOMDocument(new OperationTuple(editor.getDocument(), new Operation() {
			public void process(Document document) {
				Element dep = findChild(findChild(document.getDocumentElement(), DEPENDENCIES), DEPENDENCY, 
						childEquals(GROUP_ID, dependencyKey.getGroupId()), 
						childEquals(ARTIFACT_ID, dependencyKey.getArtifactId()), 
						childEquals(VERSION, dependencyKey.getVersion()) 
						);
				if (dep != null) {
					Element exclusion = findChild(findChild(dep, EXCLUSIONS), EXCLUSION,
							childEquals(GROUP_ID, excluded.getGroupId()), 
							childEquals(ARTIFACT_ID, excluded.getArtifactId()) 
							);
					found[0] = exclusion != null;
					
				}
			}
		}, true));
		
		return found[0];
	}
	
	 public static PomResourceImpl loadResource(IFile pomFile) throws CoreException {
	    String path = pomFile.getFullPath().toOSString();
	    URI uri = URI.createPlatformResourceURI(path, true);

	    try {
	      Resource resource = new PomResourceFactoryImpl().createResource(uri);
	      resource.load(new HashMap());
	      return (PomResourceImpl)resource;

	    } catch(Exception ex) {
	      String msg = NLS.bind("Can't load model {0}", pomFile);
	      throw new CoreException(new Status(IStatus.ERROR, "test", -1, msg, ex));
	    }
	  }  	

	/*
	 * The editor has the given exclusion set
	 */
	protected static boolean hasExclusionSet(IProject project, ArtifactKey dependencyKey, ArtifactKey excluded) throws Exception {
		Model model = loadResource(project.getFile("pom.xml")).getModel();
		Dependency d = null;
		for (Dependency dep : model.getDependencies()) {
			if (dep.getArtifactId().equals(dependencyKey.getArtifactId()) && dep.getGroupId().equals(dependencyKey.getGroupId()) && dep.getVersion().equals(dependencyKey.getVersion())) {
				d = dep;
				break;
			}
		}
		if (d == null) {
			return false;
		}
		for (Exclusion ex : d.getExclusions()) {
			if (ex.getArtifactId().equals(excluded.getArtifactId()) && ex.getGroupId().equals(excluded.getGroupId())) {
				return true;
			}
		}
		return false;
	}
}
