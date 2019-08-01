// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package qunar.tc.decompiler.main.rels;

import qunar.tc.decompiler.code.CodeConstants;
import qunar.tc.decompiler.code.Instruction;
import qunar.tc.decompiler.code.InstructionSequence;
import qunar.tc.decompiler.main.ClassesProcessor;
import qunar.tc.decompiler.main.DecompilerContext;
import qunar.tc.decompiler.struct.StructClass;
import qunar.tc.decompiler.struct.StructMethod;
import qunar.tc.decompiler.struct.attr.StructBootstrapMethodsAttribute;
import qunar.tc.decompiler.struct.attr.StructGeneralAttribute;
import qunar.tc.decompiler.struct.consts.LinkConstant;
import qunar.tc.decompiler.struct.consts.PooledConstant;
import qunar.tc.decompiler.struct.consts.PrimitiveConstant;
import qunar.tc.decompiler.struct.gen.MethodDescriptor;
import qunar.tc.decompiler.util.InterpreterUtil;

import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LambdaProcessor {
    @SuppressWarnings("SpellCheckingInspection")
    private static final String JAVAC_LAMBDA_CLASS = "java/lang/invoke/LambdaMetafactory";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String JAVAC_LAMBDA_METHOD = "metafactory";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String JAVAC_LAMBDA_ALT_METHOD = "altMetafactory";

    public void processClass(ClassesProcessor.ClassNode node) throws IOException {
        for (ClassesProcessor.ClassNode child : node.nested) {
            processClass(child);
        }

        ClassesProcessor clProcessor = DecompilerContext.getClassProcessor();
        StructClass cl = node.classStruct;

        if (cl.getBytecodeVersion() < CodeConstants.BYTECODE_JAVA_8) { // lambda beginning with Java 8
            return;
        }

        StructBootstrapMethodsAttribute bootstrap =
                (StructBootstrapMethodsAttribute) cl.getAttribute(StructGeneralAttribute.ATTRIBUTE_BOOTSTRAP_METHODS);
        if (bootstrap == null || bootstrap.getMethodsNumber() == 0) {
            return; // no bootstrap constants in pool
        }

        BitSet lambda_methods = new BitSet();

        // find lambda bootstrap constants
        for (int i = 0; i < bootstrap.getMethodsNumber(); ++i) {
            LinkConstant method_ref = bootstrap.getMethodReference(i); // method handle

            // FIXME: extend for Eclipse etc. at some point
            if (JAVAC_LAMBDA_CLASS.equals(method_ref.classname) &&
                    (JAVAC_LAMBDA_METHOD.equals(method_ref.elementname) || JAVAC_LAMBDA_ALT_METHOD.equals(method_ref.elementname))) {
                lambda_methods.set(i);
            }
        }

        if (lambda_methods.isEmpty()) {
            return; // no lambda bootstrap constant found
        }

        Map<String, String> mapMethodsLambda = new HashMap<>();

        // iterate over code and find invocations of bootstrap methods. Replace them with anonymous classes.
        for (StructMethod mt : cl.getMethods()) {
            mt.expandData();

            InstructionSequence seq = mt.getInstructionSequence();
            if (seq != null && seq.length() > 0) {
                int len = seq.length();

                for (int i = 0; i < len; ++i) {
                    Instruction instr = seq.getInstr(i);

                    if (instr.opcode == CodeConstants.opc_invokedynamic) {
                        LinkConstant invoke_dynamic = cl.getPool().getLinkConstant(instr.operand(0));

                        if (lambda_methods.get(invoke_dynamic.index1)) { // lambda invocation found

                            List<PooledConstant> bootstrap_arguments = bootstrap.getMethodArguments(invoke_dynamic.index1);
                            MethodDescriptor md = MethodDescriptor.parseDescriptor(invoke_dynamic.descriptor);

                            String lambda_class_name = md.ret.value;
                            String lambda_method_name = invoke_dynamic.elementname;
                            String lambda_method_descriptor = ((PrimitiveConstant) bootstrap_arguments.get(2)).getString(); // method type

                            LinkConstant content_method_handle = (LinkConstant) bootstrap_arguments.get(1);

                            ClassesProcessor.ClassNode node_lambda = new ClassesProcessor.ClassNode(content_method_handle.classname, content_method_handle.elementname,
                                    content_method_handle.descriptor, content_method_handle.index1,
                                    lambda_class_name, lambda_method_name, lambda_method_descriptor, cl);
                            node_lambda.simpleName = cl.qualifiedName + "##Lambda_" + invoke_dynamic.index1 + "_" + invoke_dynamic.index2;
                            node_lambda.enclosingMethod = InterpreterUtil.makeUniqueKey(mt.getName(), mt.getDescriptor());

                            node.nested.add(node_lambda);
                            node_lambda.parent = node;

                            clProcessor.getMapRootClasses().put(node_lambda.simpleName, node_lambda);
                            if (!node_lambda.lambdaInformation.is_method_reference) {
                                mapMethodsLambda.put(node_lambda.lambdaInformation.content_method_key, node_lambda.simpleName);
                            }
                        }
                    }
                }
            }

            mt.releaseResources();
        }

        // build class hierarchy on lambda
        for (ClassesProcessor.ClassNode nd : node.nested) {
            if (nd.type == ClassesProcessor.ClassNode.CLASS_LAMBDA) {
                String parent_class_name = mapMethodsLambda.get(nd.enclosingMethod);
                if (parent_class_name != null) {
                    ClassesProcessor.ClassNode parent_class = clProcessor.getMapRootClasses().get(parent_class_name);

                    parent_class.nested.add(nd);
                    nd.parent = parent_class;
                }
            }
        }

        // FIXME: mixed hierarchy?
    }
}