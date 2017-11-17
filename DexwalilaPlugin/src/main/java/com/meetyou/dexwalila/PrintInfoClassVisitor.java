package com.meetyou.dexwalila;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Created by Linhh on 2017/7/21.
 */
public class PrintInfoClassVisitor  extends ClassVisitor {

    public PrintInfoClassVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
//        AnnotationVisitor av0 = cv.visitAnnotation("Lcom/meetyou/anna/plugin/AntiAssassin;", true);
//        av0.visitEnd();

    }

    @Override
    public org.objectweb.asm.AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                     String[] exceptions) {
        MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);
        methodVisitor = new AdviceAdapter(Opcodes.ASM5, methodVisitor, access, name, desc) {

            @Override
            public void visitCode() {
                super.visitCode();

            }

            public void print(String msg){
                mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                mv.visitLdcInsn(msg);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                        "(Ljava/lang/String;)V", false);
            }

            @Override
            protected void onMethodEnter() {
                if(!name.equals("main")){
                    return;
                }
                print("dx-test");

                loadArgs();
                mv.visitMethodInsn(INVOKESTATIC, "com/meetyou/dex/walila/DexWalila", "maintest", "([Ljava/lang/String;)V", false);

            }

            @Override
            protected void onMethodExit(int i) {

            }
        };
        return methodVisitor;

    }
}
