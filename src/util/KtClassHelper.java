/*
 * Copyright (C) 2016 Nekocode (https://github.com/nekocode)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package util;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.asJava.elements.KtLightElement;
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.resolve.lazy.ResolveSession;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement;

import java.util.*;

/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
public class KtClassHelper {
//
//    public static List<ValueParameterDescriptor> findPMethods(KtClass ktClass) {
//        List<KtElement> list = new ArrayList<KtElement>();
//        list.add(ktClass);
//        ktClass.getChildren();
//        ResolveSession resolveSession = KotlinCacheService.Companion.getInstance(ktClass.getProject()).
//                getResolutionFacade(list).getFrontendService(ResolveSession.class);
//        ClassDescriptor classDescriptor = resolveSession.getClassDescriptor(ktClass, NoLookupLocation.FROM_IDE);
//
//        List<ValueParameterDescriptor> valueParameters = new ArrayList<ValueParameterDescriptor>();
////        if (classDescriptor.isData()) {
//        ConstructorDescriptor constructorDescriptor = classDescriptor.getUnsubstitutedPrimaryConstructor();
//
//        classDescriptor.getUnsubstitutedMemberScope().getFunctionNames()
//        if (constructorDescriptor != null) {
//            List<ValueParameterDescriptor> allParameters = constructorDescriptor.getValueParameters();
//
//            for (ValueParameterDescriptor parameter : allParameters) {
//                valueParameters.add(parameter);
//            }
//        }
////        }
//
//        return valueParameters;
//    }

    public static List<String> findConstructorParamNames(KtClass ktClass) {
        List<KtElement> list = new ArrayList();
        list.add(ktClass);
        ResolveSession resolveSession = KotlinCacheService.Companion.getInstance(ktClass.getProject()).
                getResolutionFacade(list).getFrontendService(ResolveSession.class);
        ClassDescriptor classDescriptor = resolveSession.getClassDescriptor(ktClass, NoLookupLocation.FROM_IDE);
        List<String> valueParameters = new ArrayList();
        ConstructorDescriptor constructorDescriptor = classDescriptor.getUnsubstitutedPrimaryConstructor();
        if (constructorDescriptor != null) {
            List<ValueParameterDescriptor> allParameters = constructorDescriptor.getValueParameters();

            for (ValueParameterDescriptor parameter : allParameters) {
                valueParameters.add(parameter.getName().toString());
            }
        }
        return valueParameters;
    }

    public static List<PropertyDescriptor> getFields(KtClass ktClass) {
        List<KtElement> list = new ArrayList<KtElement>();
        list.add(ktClass);
        ResolveSession resolveSession = KotlinCacheService.Companion.getInstance(ktClass.getProject()).
                getResolutionFacade(list).getFrontendService(ResolveSession.class);
        ClassDescriptor classDescriptor = resolveSession.getClassDescriptor(ktClass, NoLookupLocation.FROM_IDE);
        MemberScope memberScope = classDescriptor.getUnsubstitutedMemberScope();
        Set<Name> variableNames = memberScope.getVariableNames();
        Iterator<Name> nameIterator = variableNames.iterator();
        List<PropertyDescriptor> simpleFunctionDescriptors = new ArrayList<>();
        while (nameIterator.hasNext()) {
            Name name = nameIterator.next();
            Collection<PropertyDescriptor> collection = memberScope.getContributedVariables(name, NoLookupLocation.FROM_IDE);
            if (!collection.isEmpty()) {
                PropertyDescriptor propertyDescriptor = collection.iterator().next();
                KotlinSourceElement kotlinSourceElemen = (KotlinSourceElement) propertyDescriptor.getSource();
                if (kotlinSourceElemen.getPsi().toString().equals("PROPERTY")){
                    simpleFunctionDescriptors.add(propertyDescriptor);
                }
            }
        }
        return simpleFunctionDescriptors;
    }

    public static List<SimpleFunctionDescriptor> getMethods(KtClass ktClass) {
        List<KtElement> list = new ArrayList<KtElement>();
        list.add(ktClass);
        ResolveSession resolveSession = KotlinCacheService.Companion.getInstance(ktClass.getProject()).
                getResolutionFacade(list).getFrontendService(ResolveSession.class);
        ClassDescriptor classDescriptor = resolveSession.getClassDescriptor(ktClass, NoLookupLocation.FROM_IDE);
        MemberScope memberScope = classDescriptor.getUnsubstitutedMemberScope();
        Set<Name> functionNames = memberScope.getFunctionNames();
        Iterator<Name> nameIterator = functionNames.iterator();
        List<SimpleFunctionDescriptor> simpleFunctionDescriptors = new ArrayList<>();
        while (nameIterator.hasNext()) {
            Name name = nameIterator.next();
            Collection<SimpleFunctionDescriptor> collection = memberScope.getContributedFunctions(name, NoLookupLocation.FROM_IDE);
            if (!collection.isEmpty()) {
                simpleFunctionDescriptors.add(collection.iterator().next());
            }
        }
        return simpleFunctionDescriptors;
    }

    public static KtClass getKtClassForElement(@NotNull PsiElement psiElement) {
        if (psiElement instanceof KtLightElement) {
            PsiElement origin = ((KtLightElement) psiElement).getKotlinOrigin();
            if (origin != null) {
                return getKtClassForElement(origin);
            } else {
                return null;
            }

        } else if (psiElement instanceof KtClass) {
            return (KtClass) psiElement;
        } else {
            PsiElement parent = psiElement.getParent();
            if (parent == null) {
                return null;
            } else {
                return getKtClassForElement(parent);
            }
        }
    }

    public static KtClass getKtInterfaceForElement(@NotNull PsiElement psiElement) {
        if (psiElement instanceof KtLightElement) {
            PsiElement origin = ((KtLightElement) psiElement).getKotlinOrigin();
            if (origin != null) {
                return getKtInterfaceForElement(origin);
            } else {
                return null;
            }

        } else if (psiElement instanceof KtClass && ((KtClass) psiElement).isInterface()) {
            return (KtClass) psiElement;
        } else {
            PsiElement parent = psiElement.getParent();
            if (parent == null) {
                return null;
            } else {
                return getKtInterfaceForElement(parent);
            }
        }
    }
}
