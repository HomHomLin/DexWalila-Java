package com.meetyou.dexwalila;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * Created by Linhh on 2017/7/20.
 */
public class Test {
    public static void main(File file, File tmpFile,String maindexlist_walila){
        try {
            //替换为dx位置
            JarFile jarFile = new JarFile(file);
            Enumeration enumeration = jarFile.entries();
//            File tmpFile = new File(file.getParent() + File.separator + "dx.jar");
            //避免上次的缓存被重复插入
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
//            System.out.println("dexwalila is calling anna, input:" + file.getAbsolutePath() + ",output:" + tmpFile.getAbsolutePath());
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpFile));
            //用于保存
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement();
                String entryName = jarEntry.getName();
                ZipEntry zipEntry = new ZipEntry(entryName);

                InputStream inputStream = jarFile.getInputStream(jarEntry);
                //如果是dx的Main就处理他
                if (entryName.endsWith("com/android/dx/command/Main.class")) {
                    //class文件处理
//                    if(minidex) {
//                        System.out.println("entryName anna:" + entryName);
                        jarOutputStream.putNextEntry(zipEntry);

                        ClassReader classReader = new ClassReader(IOUtils.toByteArray(inputStream));
                        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
                        ClassVisitor cv = new DexWalilaClassVisitor(Opcodes.ASM5, classWriter);
                        classReader.accept(cv, ClassReader.EXPAND_FRAMES);
                        byte[] code = classWriter.toByteArray();
                        jarOutputStream.write(code);
//                    }else{
////                        System.out.println("entryName anna:" + entryName);
//                        jarOutputStream.putNextEntry(zipEntry);
//                        jarOutputStream.write(IOUtils.toByteArray(inputStream));
//                    }
                } else if(entryName.endsWith("com/android/multidex/MainDexListBuilder.class")){
                    //class文件处理
//                    if(keepannotations){
////                        System.out.println("entryName anna:" + entryName);
//                        jarOutputStream.putNextEntry(zipEntry);
//                        jarOutputStream.write(IOUtils.toByteArray(inputStream));
//                    }else {
//                        System.out.println("entryName anna:" + entryName);
                        jarOutputStream.putNextEntry(zipEntry);

                        ClassReader classReader = new ClassReader(IOUtils.toByteArray(inputStream));
                        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
                        ClassVisitor cv = new MainDexListClassVisitor(Opcodes.ASM5, classWriter);
                        classReader.accept(cv, ClassReader.EXPAND_FRAMES);
                        byte[] code = classWriter.toByteArray();
                        jarOutputStream.write(code);
//                    }
                }else {
//                    System.out.println("entryName anna:" + entryName);
                    jarOutputStream.putNextEntry(zipEntry);
                    jarOutputStream.write(IOUtils.toByteArray(inputStream));
                }
                jarOutputStream.closeEntry();
            }
            //写入inject注解

//            File dexWalilaClazz = new File("/Users/Linhh/AndroidStudioProjects/Walila/DexWalila.class");
//            InputStream inputStream = new FileInputStream("/Users/Linhh/AndroidStudioProjects/Walila/DexWalila.class");


            //写入inject文件
            try {
                ZipEntry addEntry = new ZipEntry("com/meetyou/dex/walila/DexWalila.class");
                jarOutputStream.putNextEntry(addEntry);
                jarOutputStream.write(DexWalilaWriter.makeWalila(maindexlist_walila));
                jarOutputStream.closeEntry();
            }catch (Exception e){
                e.printStackTrace();
            }
//
//                    clazzindex++ ;
            //结束
            jarOutputStream.close();
            jarFile.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
