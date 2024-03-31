package mt.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.persistence.Table;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * @Author Martin
 * @Date 2024/3/31
 */
@SupportedAnnotationTypes({"javax.persistence.Table"})
public class TestProcessor extends AbstractProcessor {
	
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}
	
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		Set<? extends Element> elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(Table.class);
		for (Element element : elementsAnnotatedWith) {
			System.out.println(element);
		}
		System.out.println("annotations:" + annotations);
		System.out.println("roundEnv:" + roundEnv);
		return false;
	}
}
