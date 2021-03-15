package krasa.formatter.plugin;

import org.jetbrains.annotations.NotNull;

import com.intellij.lang.ImportOptimizer;
import com.intellij.lang.java.JavaImportOptimizer;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.psi.*;

import krasa.formatter.exception.FileDoesNotExistsException;
import krasa.formatter.exception.ParsingFailedException;
import krasa.formatter.settings.ProjectComponent;
import krasa.formatter.settings.Settings;
import krasa.formatter.settings.provider.ImportOrderProvider;
import krasa.formatter.utils.FileUtils;

/**
 * @author Vojtech Krasa
 */
public class EclipseImportOptimizer implements ImportOptimizer {

	private static final Logger LOG = Logger.getInstance("#krasa.formatter.plugin.processor.ImportOrderProcessor");

	private Notifier notifier = new Notifier();

	@NotNull
	@Override
	public Runnable processFile(final PsiFile file) {
		if (!(file instanceof PsiJavaFile)) {
			return EmptyRunnable.getInstance();
		}
		final PsiJavaFile dummyFile = (PsiJavaFile) file.copy();

		final Runnable intellijRunnable = new JavaImportOptimizer().processFile(dummyFile);
		if (!(file instanceof PsiJavaFile)) {
			return intellijRunnable;
		}

		if (!isEnabled(file)) {
			return intellijRunnable;
		}
		return new Runnable() {

			@Override
			public void run() {
				intellijRunnable.run();
				try {
					Settings settings = ProjectComponent.getSettings(file);
					if (isEnabled(settings)) {
						optimizeImportsByEclipse((PsiJavaFile) file, settings, dummyFile);
					}
				} catch (ParsingFailedException e) {
					notifier.configurationError(e, file.getProject());
					LOG.info("Eclipse Import Optimizer failed", e);
				} catch (FileDoesNotExistsException e) {
					notifier.configurationError(e, file.getProject());
					LOG.info("Eclipse Import Optimizer failed", e);
				} catch (IndexNotReadyException e) {
					throw e;
				} catch (ProcessCanceledException e) {
					throw e;
				} catch (Throwable e) {
					LOG.error("Eclipse Import Optimizer failed", e);
				}
			}
		};
	}

	private void optimizeImportsByEclipse(PsiJavaFile psiFile, Settings settings, PsiJavaFile dummy) {
		ImportSorterAdapter importSorter = null;
		try {
			importSorter = getImportSorter(settings);

			PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(psiFile.getProject());
			commitDocument(psiFile, psiDocumentManager);

			importSorter.sortImports(psiFile, dummy);
//			commitDocumentAndSave(psiFile, psiDocumentManager);
		} catch (ParsingFailedException e) {
			throw e;
		} catch (IndexNotReadyException e) {
			throw e;
		} catch (ProcessCanceledException e) {
			throw e;
		} catch (FileDoesNotExistsException e) {
			throw e;
		} catch (Throwable e) {
			final PsiImportList oldImportList = (psiFile).getImportList();
			StringBuilder stringBuilder = new StringBuilder();
			if (oldImportList != null) {
				PsiImportStatementBase[] allImportStatements = oldImportList.getAllImportStatements();
				for (PsiImportStatementBase allImportStatement : allImportStatements) {
					String text = allImportStatement.getText();
					stringBuilder.append(text);
				}
			}
			String message = "imports: " + stringBuilder.toString() + ", settings: "
					+ (importSorter != null ? importSorter.getImportsOrderAsString() : null);
			throw new ImportSorterException(message, e);
		}
	}

	/**
	 * very strange, https://github.com/krasa/EclipseCodeFormatter/issues/59
	 */
	private void commitDocument(PsiJavaFile psiFile, PsiDocumentManager psiDocumentManager) {
		Document document = psiDocumentManager.getDocument(psiFile);
		if (document != null) {
			psiDocumentManager.commitDocument(document);
		}
	}

	/**
	 * was needed for #87+#94 - saveDocument un-blues changed files - where content is equal, but now not changing PSI when imports are not changed (#179) makes it obsolete
	 */
	private void commitDocumentAndSave(PsiJavaFile psiFile, PsiDocumentManager psiDocumentManager) {
		Document document = psiDocumentManager.getDocument(psiFile);
		if (document != null) {
			psiDocumentManager.doPostponedOperationsAndUnblockDocument(document);
			psiDocumentManager.commitDocument(document);
			FileDocumentManager.getInstance().saveDocument(document);
		}
	}

	protected ImportSorterAdapter getImportSorter(Settings settings) {
		if (settings.isImportOrderFromFile()) {
			final ImportOrderProvider importOrderProviderFromFile = settings.getImportOrderProvider();
			return new ImportSorterAdapter(settings.getImportOrdering(), importOrderProviderFromFile.get());
		} else {
			return new ImportSorterAdapter(settings.getImportOrdering(), ImportOrderProvider.toList(settings.getImportOrder()));
		}
	}

	@Override
	public boolean supports(PsiFile file) {
		return FileUtils.isJava(file) && isEnabled(file);
	}

	private boolean isEnabled(Settings settings) {
		return settings.isEnabled() && settings.isEnableJavaFormatting() && settings.isOptimizeImports();
	}

	private boolean isEnabled(PsiFile file) {
		Settings settings = ProjectComponent.getSettings(file);
		return isEnabled(settings);
	}

}
