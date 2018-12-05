package com.nxiangbo.lib;

import com.google.auto.service.AutoService;
import com.nxiangbo.library.Factory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
public class FactoryProcessor extends AbstractProcessor {
	private Types typeUtils;
	private Elements elementUtils;
	private Filer filer;
	private Messager messager;
	private Map<String, FactoryGroupedClasses> factoryClasses =
			new LinkedHashMap<String, FactoryGroupedClasses>();

	@Override
	public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
		for (Element element : roundEnvironment.getElementsAnnotatedWith(Factory.class)) {
			if (element.getKind() != ElementKind.CLASS) {
				error(element, "Only classes can be annotated with @%s", Factory.class.getSimpleName());
				return true;
			}

			TypeElement typeElement = (TypeElement) element;
			FactoryAnnotatedClass annotatedClass = null;
			try {
				annotatedClass = new FactoryAnnotatedClass(typeElement);
				checkValidClass(annotatedClass);

				// Everything is fine, so try to add
				FactoryGroupedClasses factoryClass =
						factoryClasses.get(annotatedClass.getQualifiedFactoryGroupName());
				if (factoryClass == null) {
					String qualifiedGroupName = annotatedClass.getQualifiedFactoryGroupName();
					factoryClass = new FactoryGroupedClasses(qualifiedGroupName);
					factoryClasses.put(qualifiedGroupName, factoryClass);
				}

				// Checks if id is conflicting with another @Factory annotated class with the same id
				factoryClass.add(annotatedClass);


			} catch (ProcessingException e) {
				e.printStackTrace();
			}

		}
		// Generate code
		for (FactoryGroupedClasses factoryClass : factoryClasses.values()) {
			try {
				factoryClass.generateCode(elementUtils, filer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		factoryClasses.clear();
		return true;
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		Set<String> annotataions = new LinkedHashSet<String>();
		annotataions.add(Factory.class.getCanonicalName());
		return annotataions;
	}



	@Override
	public synchronized void init(ProcessingEnvironment processingEnvironment) {
		super.init(processingEnvironment);
		typeUtils = processingEnvironment.getTypeUtils();
		elementUtils = processingEnvironment.getElementUtils();
		filer = processingEnvironment.getFiler();
		messager = processingEnvironment.getMessager();
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}




	private void checkValidClass(FactoryAnnotatedClass item) throws ProcessingException {

		// Cast to TypeElement, has more type specific methods
		TypeElement classElement = item.getTypeElement();

		if (!classElement.getModifiers().contains(Modifier.PUBLIC)) {
			throw new ProcessingException(classElement, "The class %s is not public.",
					classElement.getQualifiedName().toString());
		}

		// Check if it's an abstract class
		if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
			throw new ProcessingException(classElement,
					"The class %s is abstract. You can't annotate abstract classes with @%",
					classElement.getQualifiedName().toString(), Factory.class.getSimpleName());
		}

		// Check inheritance: Class must be childclass as specified in @Factory.type();
		TypeElement superClassElement =
				elementUtils.getTypeElement(item.getQualifiedFactoryGroupName());
		if (superClassElement.getKind() == ElementKind.INTERFACE) {
			// Check interface implemented
			if (!classElement.getInterfaces().contains(superClassElement.asType())) {
				throw new ProcessingException(classElement,
						"The class %s annotated with @%s must implement the interface %s",
						classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
						item.getQualifiedFactoryGroupName());
			}
		} else {
			// Check subclassing
			TypeElement currentClass = classElement;
			while (true) {
				TypeMirror superClassType = currentClass.getSuperclass();

				if (superClassType.getKind() == TypeKind.NONE) {
					// Basis class (java.lang.Object) reached, so exit
					throw new ProcessingException(classElement,
							"The class %s annotated with @%s must inherit from %s",
							classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
							item.getQualifiedFactoryGroupName());
				}

				if (superClassType.toString().equals(item.getQualifiedFactoryGroupName())) {
					// Required super class found
					break;
				}

				// Moving up in inheritance tree
				currentClass = (TypeElement) typeUtils.asElement(superClassType);
			}
		}

		// Check if an empty public constructor is given
		for (Element enclosed : classElement.getEnclosedElements()) {
			if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
				ExecutableElement constructorElement = (ExecutableElement) enclosed;
				if (constructorElement.getParameters().size() == 0 && constructorElement.getModifiers()
						.contains(Modifier.PUBLIC)) {
					// Found an empty constructor
					return;
				}
			}
		}

		// No empty constructor found
		throw new ProcessingException(classElement,
				"The class %s must provide an public empty default constructor",
				classElement.getQualifiedName().toString());
	}

	private void error(Element e, String msg, Object... args) {
		messager.printMessage(
				Diagnostic.Kind.ERROR,
				String.format(msg, args),
				e);
	}
}
