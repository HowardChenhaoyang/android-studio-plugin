import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.idea.internal.Location;
import org.jetbrains.kotlin.psi.*;
import util.KtClassHelper;

import java.util.List;

public class RetrofitKotlinGenerator extends AnAction {
    private KtClass ktClass;
    private PsiElement psiElement;
    private PsiFile psiFile;

    @Override
    public void update(AnActionEvent e) {
        ktClass = getPsiClassFromEvent(e);
        e.getPresentation().setEnabled(
                ktClass != null
        );
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        deleteExistClass(psiElement, ktClass);
        generateServiceImpl(ktClass, KtClassHelper.getMethods(ktClass));
    }

    private void generateServiceImpl(final KtClass ktClass, final List<SimpleFunctionDescriptor> methods) {
        new WriteCommandAction.Simple(ktClass.getProject(), ktClass.getContainingFile()) {
            @Override
            protected void run() throws Throwable {
                new KotlinCodeGenerator(ktClass, methods).generate();
            }
        }.execute();
    }

    private KtClass getPsiClassFromEvent(AnActionEvent e) {
        final Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor == null) return null;

        final Project project = editor.getProject();
        if (project == null) return null;
        final PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        if (psiFile == null || psiFile.getLanguage() != KotlinLanguage.INSTANCE) return null;
        this.psiFile = psiFile;
        final Location location = Location.fromEditor(editor, project);
        final PsiElement psiElement = psiFile.findElementAt(location.getStartOffset());
        if (psiElement == null) return null;
        this.psiElement = psiElement;
        KtClass ktClass = KtClassHelper.getKtInterfaceForElement(psiElement);
        return ktClass;
    }

    private void deleteExistClass(PsiElement focusedPsiElement, KtClass focusedKtClass) {
        PsiElement[] psiElements = psiFile.getChildren();
        if (psiElements == null || psiElements.length <= 1) {
            return;
        }
        for (PsiElement psiElement : psiElements) {
            KtClass ktClass = KtClassHelper.getKtClassForElement(psiElement);
            if (ktClass == null) continue;
            if (ktClass.getName().equals(focusedKtClass.getName() + "Impl")) {
                ktClass.delete();
                break;
            }
        }
    }
}
