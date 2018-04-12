import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.ArrayList;
import java.util.List;

public class RetrofitJavaGenerator extends AnAction {


    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        PsiClass psiClass = getPsiClassFromContext(e, psiFile);
        e.getPresentation().setEnabled(
                psiClass != null && psiClass.isInterface()
        );
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        PsiClass psiClass = getPsiClassFromContext(e, psiFile);
        generateRetrofit(psiClass, getClassFields(psiClass.getMethods()));
    }

    private List<PsiMethod> getClassFields(PsiMethod[] psiMethods) {
        final List<PsiMethod> fields = new ArrayList<>();
        for (PsiMethod psiMethod : psiMethods) {
            fields.add(psiMethod);
        }
        return fields;
    }

    private PsiClass getPsiClassFromContext(AnActionEvent e, PsiFile psiFile) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (psiFile == null || editor == null) {
            return null;
        }
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = psiFile.findElementAt(offset);
        return PsiTreeUtil.getParentOfType(element, PsiClass.class);
    }

    private void generateRetrofit(final PsiClass psiClass, final List<PsiMethod> methods) {
        new WriteCommandAction.Simple(psiClass.getProject(), psiClass.getContainingFile()) {
            @Override
            protected void run() throws Throwable {
                new CodeGenerator(psiClass, methods).generate();
            }
        }.execute();
    }
}
