package com.meetyou.dexwalila;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Created by Linhh on 2017/7/24.
 */
public class MainDexListClassVisitor extends ClassVisitor {

    public MainDexListClassVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                     String[] exceptions) {
        MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);
        methodVisitor = new AdviceAdapter(Opcodes.ASM5, methodVisitor, access, name, desc) {

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

                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESTATIC, "com/meetyou/dex/walila/DexWalila", "maindex", "([Ljava/lang/String;)[Ljava/lang/String;", false);
                mv.visitVarInsn(ASTORE, 0);
//                mv.visitMethodInsn(INVOKESTATIC, "com/meetyou/dex/walila/DexWalila", "print", "()V", false);

//                mv.visitVarInsn(ALOAD, 0);
//                mv.visitMethodInsn(INVOKESTATIC, "com/meetyou/dex/walila/DexWalila", "printArgs", "([Ljava/lang/String;)V", false);
            }

            @Override
            protected void onMethodExit(int i) {

            }
        };
        return methodVisitor;

    }
}
