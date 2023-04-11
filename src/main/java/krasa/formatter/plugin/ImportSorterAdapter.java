package krasa.formatter.plugin;

import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.psi.*;
import krasa.formatter.settings.Settings;
import krasa.formatter.utils.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Vojtech Krasa
 */
public class ImportSorterAdapter {
	public static final String N = Settings.LINE_SEPARATOR;

	private Settings.ImportOrdering importOrdering;
	private List<String> importsOrder;

	public ImportSorterAdapter(Settings.ImportOrdering importOrdering, List<String> importsOrder) {
		this.importOrdering = importOrdering;
		this.importsOrder = new ArrayList<String>(importsOrder);
	}

	public String getImportsOrderAsString() {
		return Arrays.toString(importsOrder.toArray());
	}

	public void sortImports(PsiJavaFile file, PsiJavaFile dummy) {
		List<String> imports = new ArrayList<String>();
		List<PsiElement> nonImports = new ArrayList<PsiElement>();

		PsiImportList actualImportList = file.getImportList();
		if (actualImportList == null) {
			return;
		}
		PsiImportList dummyImportList = dummy.getImportList();
		if (dummyImportList == null) {
			return;
		}

		PsiElement[] children = dummyImportList.getChildren();
		for (int i = 0; i < children.length; i++) {
			PsiElement child = children[i];
			if (child instanceof PsiImportStatementBase) {
				imports.add(child.getText());
			} else if (!(child instanceof PsiWhiteSpace)) { //todo wild guess
				nonImports.add(child);
			}
		}

		List<String> sort = getImportsSorter(file).sort(StringUtils.trimImports(imports));

		StringBuilder text = new StringBuilder();
		for (int i = 0; i < sort.size(); i++) {
			text.append(sort.get(i));
		}
		for (int i = 0; i < nonImports.size(); i++) {
			PsiElement psiElement = nonImports.get(i);
			text.append("\n").append(psiElement.getText());
		}

		PsiFileFactory factory = PsiFileFactory.getInstance(file.getProject());
		String ext = StdFileTypes.JAVA.getDefaultExtension();
		final PsiJavaFile dummyFile = (PsiJavaFile) factory.createFileFromText("_Dummy_." + ext, StdFileTypes.JAVA,
				text);

		PsiImportList newImportList = dummyFile.getImportList();
		PsiImportList result = (PsiImportList) newImportList.copy();
		if (actualImportList.isReplaceEquivalent(result))
			return;
		if (!nonImports.isEmpty()) {
			PsiElement firstPrevious = newImportList.getPrevSibling();
			while (firstPrevious != null && firstPrevious.getPrevSibling() != null) {
				firstPrevious = firstPrevious.getPrevSibling();
			}
			for (PsiElement element = firstPrevious;
				 element != null && element != newImportList; element = element.getNextSibling()) {
				result.add(element.copy());
			}
			for (PsiElement element = newImportList.getNextSibling();
				 element != null; element = element.getNextSibling()) {
				result.add(element.copy());
			}
		}
		
		actualImportList.replace(result);
	}

	@NotNull
	private ImportsSorter getImportsSorter(PsiJavaFile file) {
		switch (importOrdering) {
		case ECLIPSE_44:
			return new ImportsSorter450(importsOrder);
			case ECLIPSE_452:
			return new ImportsSorter452(importsOrder, new ImportsComparator(file.getProject()));
		}
		throw new RuntimeException(String.valueOf(importOrdering));
	}

}
