package com.fourlastor.pickle

import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@SupportedAnnotationTypes("cucumber.api.java.*", "com.fourlastor.pickle.Pickle")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class PickleProcessor : AbstractProcessor() {

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (annotations.isEmpty()) {
            return false
        }

        try {
            options(roundEnv).run {

                val parser = FeatureParser()
                val classConverter = ClassConverter(
                        MethodsConverter(
                                MethodConverter(
                                        StatementConverter(roundEnv),
                                        StatementHooksCreator(roundEnv)
                                ),
                                strictMode
                        )
                )

                val generator = ClassGenerator()
                val writer = ClassWriter(processingEnv, packageName)

                parser.parse(featuresDirPath)
                        .map { classConverter.convert(it) }
                        .filter { it.methods.isNotEmpty() }
                        .map { generator.generate(it) }
                        .forEach { writer.write(it) }
            }
        } catch (exception: Exception) {
            processingEnv.messager.error(exception.message!!)
        } finally {
            return false
        }
    }

    private fun Messager.error(message: String) {
        printMessage(Diagnostic.Kind.ERROR, message)
    }
}
