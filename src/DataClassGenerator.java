import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.idea.internal.Location;
import org.jetbrains.kotlin.psi.*;
import util.KtClassHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DataClassGenerator extends AnAction {
    private KtClass ktClass;
    private PsiFile psiFile;

    @Override
    public void update(AnActionEvent e) {
        ktClass = getPsiClassFromEvent(e);
        e.getPresentation().setEnabled(
                ktClass != null
        );
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        new WriteCommandAction.Simple(ktClass.getProject(), ktClass.getContainingFile()) {
            @Override
            protected void run() {
                generateDataClass();
            }
        }.execute();
    }

    private void generateDataClass() {
        KtPsiFactory elementFactory = new KtPsiFactory(ktClass.getProject());
        List<PropertyDescriptor> fields = KtClassHelper.getFields(ktClass);
//        List<String> params = KtClassHelper.findConstructorParamNames(ktClass);
//        Iterator<PropertyDescriptor> fieldIterator = fields.iterator();
//        while (fieldIterator.hasNext()) {
//            fieldIterator.next().getSource();
//            if (params.contains(fieldIterator.next().getName().toString())) {
//                fieldIterator.remove();
//            }
//        }
        String primaryConstructor = createPrimaryConstructor(fields);
        System.out.println(primaryConstructor);
        String className = ktClass.getName();
        KtClass createdClass = elementFactory.createClass("data class " + className + " " + primaryConstructor);
        psiFile.add(createdClass);
        ktClass.getBody().delete();
        ktClass.delete();
//        formatCode(ktClass);
    }

    private String createPrimaryConstructor(List<PropertyDescriptor> fields) {
        StringBuilder stringBuilder = new StringBuilder("(");
        int size = fields.size();
        for (int index = 0; index < size; index++) {
            PropertyDescriptor propertyDescriptor = fields.get(index);
            stringBuilder.append("var " + propertyDescriptor.getName() + ":" + propertyDescriptor.getType().toString());
            if (index != size - 1) {
                stringBuilder.append(",");
            }
        }
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    private void formatCode(KtClass ktClass) {
        CodeStyleManager.getInstance(ktClass.getProject()).reformatText(ktClass.getContainingFile(),
                ContainerUtil.newArrayList(ktClass.getTextRange()));
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
        KtClass ktClass = KtClassHelper.getKtClassForElement(psiElement);
        return ktClass;
    }
}
