package org.jetbrains.android.augment;

import com.android.builder.model.AaptOptions.Namespacing;
import com.android.resources.ResourceType;
import com.android.tools.idea.projectsystem.ProjectSystemUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.impl.source.PsiExtensibleClass;
import org.jetbrains.android.dom.converters.ResourceReferenceConverter;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.util.AndroidResourceUtil;
import org.jetbrains.android.util.AndroidUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Eugene.Kudelevsky
 */
public class AndroidPsiAugmentProvider extends PsiAugmentProvider {
  private static final Logger LOG = Logger.getInstance("#org.jetbrains.android.augment.AndroidPsiAugmentProvider");

  @SuppressWarnings("unchecked")
  @NotNull
  @Override
  public <Psi extends PsiElement> List<Psi> getAugments(@NotNull PsiElement element, @NotNull Class<Psi> type) {
    if (!ProjectSystemUtil.getProjectSystem(element.getProject()).getAugmentRClasses()) {
      return Collections.emptyList();
    }

    if ((type != PsiClass.class && type != PsiField.class) ||
        !(element instanceof PsiExtensibleClass)) {
      return Collections.emptyList();
    }
    final PsiExtensibleClass aClass = (PsiExtensibleClass)element;
    final String className = aClass.getName();
    final boolean rClassAugment = AndroidUtils.R_CLASS_NAME.equals(className)
                                  && type == PsiClass.class;

    if (DumbService.isDumb(element.getProject())) {
      if (rClassAugment) {
        LOG.debug("R_CLASS_AUGMENT: empty because of dumb mode");
      }
      return Collections.emptyList();
    }

    final AndroidFacet facet = AndroidFacet.getInstance(element);
    if (facet == null) {
      if (rClassAugment) {
        LOG.debug("R_CLASS_AUGMENT: empty because no facet");
      }
      return Collections.emptyList();
    }

    final PsiFile containingFile = element.getContainingFile();
    if (containingFile == null) {
      if (rClassAugment) {
        LOG.debug("R_CLASS_AUGMENT: empty because of no containing file");
      }
      return Collections.emptyList();
    }

    if (type == PsiClass.class) {
      if (AndroidResourceUtil.isRJavaClass(aClass)) {
        final Set<String> existingInnerClasses = getOwnInnerClasses(aClass);
        final Set<ResourceType> types = ResourceReferenceConverter.getResourceTypesInCurrentModule(facet);
        final List<Psi> result = new ArrayList<>();

        for (ResourceType resType : types) {
          if (!resType.getHasInnerClass()) {
            continue;
          }

          if (!existingInnerClasses.contains(resType.getName())) {
            final AndroidLightInnerClassBase resClass = new ModuleResourceTypeClass(facet, Namespacing.DISABLED, resType, aClass);
            result.add((Psi)resClass);
          }
        }
        if (rClassAugment) {
          LOG.debug("R_CLASS_AUGMENT: " + result.size() + " classes added");
        }
        return result;
      }
      else if (AndroidResourceUtil.isManifestClass(aClass)) {
        // aapt can generate Manifest files that include these permission groups
        return Arrays.asList((Psi)new PermissionClass(facet, aClass),
                             (Psi)new PermissionGroupClass(facet, aClass));
      }

      if (rClassAugment) {
        LOG.debug("R_CLASS_AUGMENT: empty because containing file is not actual R.java file");
      }
    }
    else if (!(aClass instanceof AndroidLightInnerClassBase)) {
      // extend existing inner classes, not provided by this augment (ex. they can be generated by maven)
      final PsiClass parentClass = aClass.getContainingClass();
      if (parentClass != null && AndroidResourceUtil.isRJavaClass(parentClass)) {
        final String resClassName = aClass.getName();

        if (resClassName != null) {
          ResourceType resourceType = ResourceType.fromClassName(resClassName);
          if (resourceType != null) {
            final Set<String> existingFields = getOwnFields(aClass);
            final PsiField[] newFields = ModuleResourceTypeClass.buildLocalResourceFields(facet, resourceType, Namespacing.DISABLED, aClass);
            final List<Psi> result = new ArrayList<>();

            for (PsiField field : newFields) {
              if (!existingFields.contains(field.getName())) {
                result.add((Psi)field);
              }
            }
            return result;
          }
        }
      }
    }
    return Collections.emptyList();
  }

  @NotNull
  private static Set<String> getOwnInnerClasses(@NotNull PsiExtensibleClass aClass) {
    final Set<String> result = new HashSet<>();

    for (PsiClass innerClass : aClass.getOwnInnerClasses()) {
      result.add(innerClass.getName());
    }
    return result;
  }

  @NotNull
  private static Set<String> getOwnFields(@NotNull PsiExtensibleClass aClass) {
    final Set<String> result = new HashSet<>();

    for (PsiField field : aClass.getOwnFields()) {
      result.add(field.getName());
    }
    return result;
  }
}
