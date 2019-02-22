package com.ftxz.loon.processor;


import com.ftxz.loon.annotation.Serializable;
import com.ftxz.loon.common.Constants;
import com.google.auto.service.AutoService;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

/**
 * process {@link com.ftxz.loon.annotation.Serializable} annotation
 *
 * @author ftxz
 *
 * @since 1.0.0
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("*")
public class AnnotationProcessor extends AbstractProcessor {

    private Messager messager;

    private JavacTrees javacTrees;

    private TreeMaker treeMaker;

    private Names names;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        javacTrees = JavacTrees.instance(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        Set<? extends Element> serializableElements = roundEnv.getElementsAnnotatedWith(Serializable.class);
        if(serializableElements == null || serializableElements.isEmpty()){
            return true;
        }
        for (Element element: serializableElements) {
            JCTree tree = javacTrees.getTree(element);
            tree.accept(new TreeTranslator(){
                @Override
                public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                    List<JCTree.JCExpression> implementings = jcClassDecl.implementing;
                    boolean flag = false;
                    if(implementings != null || !implementings.isEmpty()){
                        implementings.
                                stream()
                                .forEach(jcExpression -> {
                                    //messager.printMessage(Diagnostic.Kind.NOTE, jcExpression.toString());
                                    //messager.printMessage(Diagnostic.Kind.NOTE, jcExpression.getClass().getName());
                                    JCTree.JCExpression jcExpression1 = jcExpression;
                                    JCTree.JCIdent ident = null;
                                    do{
                                        messager.printMessage(Diagnostic.Kind.NOTE, jcExpression1.toString());
                                        messager.printMessage(Diagnostic.Kind.NOTE, jcExpression.getClass().getName());
                                        messager.printMessage(Diagnostic.Kind.NOTE, jcExpression.getKind().name());
                                        try{
                                            jcExpression1 = ((JCTree.JCFieldAccess)jcExpression1).getExpression();
                                        } catch (Exception ex){
                                            ident = ((JCTree.JCIdent)jcExpression1);
                                            jcExpression1 = null;
                                        }
                                    } while(jcExpression1 != null);
                                    messager.printMessage(Diagnostic.Kind.NOTE, ident.name);
                                    messager.printMessage(Diagnostic.Kind.NOTE, ident.sym.name);
                                });
                        flag = implementings.stream()
                                .anyMatch(jcExpression -> Constants.SERIAL_INTERFACE_NAME.equals(jcExpression.toString()));
                    }
                    if(!flag){
                        /**
                         * 增加 {@link java.io.Serializable}接口
                         */
                        messager.printMessage(Diagnostic.Kind.NOTE, "missing serializable, add it");
                        jcClassDecl.implementing = jcClassDecl.implementing.prepend(makeJCFieldAccess());
                    } //else {
                       // 判断是否存在serialVersionUID
                    List<JCTree> defs = jcClassDecl.defs;
                    if(defs != null && !defs.isEmpty()){
                        defs.stream().forEach(jcTree -> {
                            if(jcTree instanceof JCTree.JCVariableDecl){
                                JCTree.JCVariableDecl jcTree_ = (JCTree.JCVariableDecl)jcTree;
                                JCTree.JCExpression init = jcTree_.init;
                                messager.printMessage(Diagnostic.Kind.NOTE, init.toString());
                                if(init instanceof JCTree.JCLiteral){
                                    messager.printMessage(Diagnostic.Kind.NOTE, ((JCTree.JCLiteral)init).value.toString());
                                    messager.printMessage(Diagnostic.Kind.NOTE, ((JCTree.JCLiteral)init).typetag.getKindLiteral().toString());
                                }
                            }
                        });
                    }
                    jcClassDecl.defs = jcClassDecl.defs.prepend(makeVariableCl());

                    super.visitClassDef(jcClassDecl);
                }


            });
        }
        return true;
    }


    private JCTree makeVariableCl() {
        return treeMaker.VarDef(treeMaker.Modifiers(Flags.PRIVATE | Flags.FINAL | Flags.STATIC),
                names.fromString(Constants.SERIAL_VERSION_UID),
                treeMaker.TypeIdent(TypeTag.LONG),
                treeMaker.Literal(1L));
    }

    private JCTree.JCExpression makeJCFieldAccess() {
        JCTree.JCIdent ident = treeMaker.Ident(names.fromString("java"));
        JCTree.JCFieldAccess io = treeMaker.Select(ident, names.fromString("io"));
        JCTree.JCFieldAccess serial = treeMaker.Select(io, names.fromString("Serializable"));
        return serial;
    }
}
