import com.intellij.lang.jvm.JvmParameter;
import com.intellij.psi.*;

import java.util.List;

public class CodeGenerator {
    private PsiClass psiClass;
    private List<PsiMethod> psiMethods;

    public CodeGenerator(PsiClass psiClass, List<PsiMethod> psiMethods) {
        this.psiClass = psiClass;
        this.psiMethods = psiMethods;
    }

    public void generate() {
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        PsiClass psiClass = elementFactory.createClass(this.psiClass.getName() + "Impl");
        for (PsiMethod psiMethod : psiMethods) {
            psiClass.add(elementFactory.createMethodFromText(generateJAVAMethodText(psiMethod), psiClass));
        }
        this.psiClass.add(psiClass);
    }

    private String generateKotlinMethodText(PsiMethod psiMethod) {
        StringBuilder sb = new StringBuilder("fun " + psiMethod.getName() + "(activity: android.app.Activity,context: android.content.Context,activityLifecycleProvider: com.trello.rxlifecycle.ActivityLifecycleProvider," + getKotlinParameters(psiMethod) + "): " + getReturnJAVAType(psiMethod) + "{");
        sb.append("return ");
        sb.append("com.mooyoo.r2.net.RetrofitManager.getInstance().retrofit.create(");
        sb.append(psiClass.getName() + "::class.java)\n.");
        sb.append(psiMethod.getName() + "(" + getKotlinParameters(psiMethod) + ")\n");
        sb.append(".compose(this.applySchedulers(activity, activityLifecycleProvider, true));");
        sb.append("}");
        return sb.toString();
    }

    private String getKotlinParameters(PsiMethod psiMethod) {
        StringBuilder sb = new StringBuilder();
        String[] parameters = getJAVAParameters(psiMethod).split(",");
        for (int index = 0; index < parameters.length; index++) {
            String parameter = parameters[index];
            String[] typeAndName = parameter.split(" ");
            sb.append(typeAndName[1]);
            sb.append(" ");
            sb.append(typeAndName[0]);
            if (index != parameters.length - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    private String getReturnJAVAType(PsiMethod psiMethod) {
        String returnType = psiMethod.getReturnType().getPresentableText();
        return returnType.substring(returnType.indexOf("<") + 1, returnType.length() - 1);
    }

    private String getJAVAParameters(PsiMethod psiMethod) {
        JvmParameter[] jvmParameters = psiMethod.getParameters();
        if (jvmParameters == null || jvmParameters.length == 0) {
            return "";
        }
        String parameters = jvmParameters[0].getSourceElement().getContext().toString().replace("PsiParameterList:", "");
        return parameters.substring(1, parameters.length() - 1);
    }

    private String generateJAVAMethodText(PsiMethod psiMethod) {
        StringBuilder sb = new StringBuilder("public final " + getReturnJAVAType(psiMethod) + " " + psiMethod.getName() + "(android.app.Activity activity,android.content.Context context,com.trello.rxlifecycle.ActivityLifecycleProvider activityLifecycleProvider," + getJAVAParameters(psiMethod) + "){");
        sb.append("return ");
        sb.append("com.mooyoo.r2.net.RetrofitManager.getInstance().retrofit.create(");
        sb.append(psiClass.getName() + ".class)\n.");
        sb.append(psiMethod.getName() + "(" + getJAVAParameterNames(psiMethod) + ")\n");
        sb.append(".compose(this.applySchedulers(activity, activityLifecycleProvider, true));");
        sb.append("}");
        return sb.toString();
    }

    private String getJAVAParameterNames(PsiMethod psiMethod) {
        JvmParameter[] jvmParameters = psiMethod.getParameters();
        if (jvmParameters == null || jvmParameters.length == 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int index = 0; index < jvmParameters.length; index++) {
            stringBuilder.append(jvmParameters[index].getName());
            if (index != jvmParameters.length - 1) {
                stringBuilder.append(", ");
            }
        }
        return stringBuilder.toString();
    }
}
