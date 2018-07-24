import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.util.ImportInsertHelper;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtImportDirective;
import org.jetbrains.kotlin.psi.KtPsiFactory;
import org.jetbrains.kotlin.resolve.ImportPath;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class KotlinCodeGenerator {
    private final KtClass mClass;
    private final List<SimpleFunctionDescriptor> methods;

    public KotlinCodeGenerator(KtClass ktClass, List<SimpleFunctionDescriptor> methods) {
        mClass = ktClass;
        this.methods = methods;
    }

    private void insertImports(KtFile ktFile) {
        List<String> imports = getImports();
        List<KtImportDirective> importList = ktFile.getImportDirectives();
        for (KtImportDirective importDirective : importList) {
            ImportPath importPath = importDirective.getImportPath();
            if (importPath != null) {
                String pathStr = importPath.getPathStr();
                if (imports.contains(pathStr)) {
                    imports.remove(pathStr);
                }
            }
        }
        for (String importPackage : imports) {
            insertImport(ktFile, importPackage);
        }
    }

    private List<String> getImports() {
        List<String> imports = new ArrayList<>();
        imports.add("com.example.code.net.RetrofitManager");
        return imports;
    }

    private void insertImport(KtFile ktFile, String fqName) {
        final Collection<DeclarationDescriptor> descriptors =
                ResolutionUtils.resolveImportReference(ktFile, new FqName(fqName));

        if (!descriptors.isEmpty()) {
            ImportInsertHelper.getInstance(ktFile.getProject())
                    .importDescriptor(ktFile, descriptors.iterator().next(), false);
        }
    }

    public void generate() {
        KtPsiFactory elementFactory = new KtPsiFactory(mClass.getProject());
        insertImports(mClass.getContainingKtFile());
        KtClass createdClass = elementFactory.createClass("class " + mClass.getName() + "Impl");
        createdClass.add(elementFactory.createBlock(getBlock()));
        mClass.add(elementFactory.createNewLine());
        mClass.add(createdClass);
        formatCode(mClass);
    }

    private void formatCode(KtClass ktClass) {
        CodeStyleManager.getInstance(ktClass.getProject()).reformatText(ktClass.getContainingFile(),
                ContainerUtil.newArrayList(ktClass.getTextRange()));
    }

    private String getBlock() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("companion object{");
        for (SimpleFunctionDescriptor simpleFunctionDescriptor : methods) {
            stringBuilder.append(generateFun(simpleFunctionDescriptor));
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    private String generateFun(SimpleFunctionDescriptor simpleFunctionDescriptor) {
        List<String> unAvalable = new ArrayList<>();
        unAvalable.add("equals");
        unAvalable.add("hashCode");
        unAvalable.add("toString");
        if (unAvalable.contains(simpleFunctionDescriptor.getName().toString())) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("fun ");
        stringBuilder.append(simpleFunctionDescriptor.getName());
        stringBuilder.append("(");
        List<ValueParameterDescriptor> valueParameterDescriptors = simpleFunctionDescriptor.getValueParameters();
        StringBuilder paramStringBuilder = new StringBuilder();
        for (int index = 0; index < valueParameterDescriptors.size(); index++) {
            if (index == 0) {
                stringBuilder.append(",");
            }
            ValueParameterDescriptor valueParameterDescriptor = valueParameterDescriptors.get(index);
            stringBuilder.append(valueParameterDescriptor.getName());
            stringBuilder.append(": ");
            stringBuilder.append(valueParameterDescriptor.getType().toString());
            paramStringBuilder.append(valueParameterDescriptor.getName());
            if (index != valueParameterDescriptors.size() - 1) {
                stringBuilder.append(",");
                paramStringBuilder.append(",");
            }
        }
        stringBuilder.append(")");
        String returnType = simpleFunctionDescriptor.getReturnType().toString();
        if (!"Unit".equals(returnType)) {
            stringBuilder.append(": " + returnType);
        }
        stringBuilder.append("{\n");
        stringBuilder.append("return ");
        stringBuilder.append("RetrofitManager.retrofit.create(");
        stringBuilder.append(mClass.getName() + "::class.java)\n.");
        stringBuilder.append(simpleFunctionDescriptor.getName() + "(" + paramStringBuilder.toString() + ")\n");
        stringBuilder.append(".compose(RetrofitManager.threadSwitch())");
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
