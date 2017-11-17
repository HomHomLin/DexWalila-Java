package com.meetyou.dexwalila.plugin

import com.android.annotations.NonNull
import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.dsl.DexOptions
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.transforms.BaseProguardAction
import com.android.build.gradle.internal.transforms.MultiDexTransform
import com.android.build.gradle.internal.transforms.NewShrinkerTransform
import com.meetyou.dexwalila.Test
import org.apache.commons.io.FileUtils
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.objectweb.asm.Opcodes

import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Created by Linhh on 17/5/31.
 */

public class PluginImpl extends Transform implements Plugin<Project> ,Opcodes{

    class DexWalilaTransform extends MultiDexTransform {

        public DexWalilaTransform(VariantScope variantScope, DexOptions dexOptions, File includeInMainDexJarFile) {
            super(variantScope, dexOptions, includeInMainDexJarFile)
        }

        @Override
        public void transform(@NonNull TransformInvocation invocation)
                throws IOException, TransformException, InterruptedException {
            super.transform(invocation);
        }

        @Override
        void keep(@NonNull String keep) {
            //super.keep(keep)
            println "remove" + keep
//            throw new RuntimeException(keep);
        }
    }

    public DexWalilaTransform create(MultiDexTransform multiDexTransform) {
        Field t = MultiDexTransform.getDeclaredField("variantScope");
        t.setAccessible(true);
        VariantScope scope = t.get(multiDexTransform)
        DexOptions options = new DexOptions(null)
        options.setKeepRuntimeAnnotatedClasses(false)
        new DexWalilaTransform(scope, options, null)
    }

    void apply(Project project) {

        project.extensions.create("dexwalila", DexWalilaExt, project)
        project.afterEvaluate {
            println '==============Dexwalila start=================='

            def android = project.extensions.getByType(AppExtension);
            //瓦莉拉
            boolean enable = true;


            def extension = project.extensions.findByName("dexwalila") as DexWalilaExt
            if(extension != null){
                enable = extension.enable;
//                    minidex = extension.miniDex;
//                    keepannotations = extension.keepAnnotations;
            }

            println "dexwalila enable status:" + enable

            File sdkDirectory = android.getSdkDirectory()
            if(sdkDirectory != null && sdkDirectory.exists()) {
//            println 'sdk:' + sdkDirectory.absolutePath
                String buildtools = sdkDirectory.absolutePath + File.separator + "build-tools"
                File buildtoolsDirectory = new File(buildtools);
                if (buildtoolsDirectory.exists()) {
                    for (String tool_path: buildtoolsDirectory.list()){
//                            println "buildtools:" +
                        //当前buildtools才进行处理
//                            if(tool_path.equals(android.getBuildToolsVersion())) {
//                            println tool_path
//                                println "enable:" + enable + ",minidex:" + minidex + ",ka:" + keepannotations
                        String lib_path = buildtools + File.separator + tool_path + File.separator + "lib";
                        String dx_jar_path = lib_path + File.separator + "dx.jar";
                        String old_jar_path = lib_path + File.separator + "dx_old.jar";
                        File dx_jar = new File(dx_jar_path);
                        File old_jar = new File(old_jar_path);

                        //先还原
                        if (old_jar.exists() && dx_jar.exists()) {
                            dx_jar.delete()
                            FileUtils.copyFile(old_jar, new File(dx_jar_path));
                            old_jar.delete()
                        }
                        old_jar = new File(old_jar_path)
                        dx_jar = new File(dx_jar_path)
                        if (enable) {
                            if (!old_jar.exists() && dx_jar.exists()) {
                                //如果旧的jar不存在说明还没有被瓦莉拉弄过
                                //先复制一份原本的dx到dx_old
                                FileUtils.copyFile(dx_jar, old_jar);
                                String maindexlist_walila = project.getRootDir().getAbsolutePath() + File.separator + "dexwalila_log.txt";
//                                        println "log:" + maindexlist_walila
                                Test.main(new File(old_jar_path), new File(dx_jar_path),maindexlist_walila)
                            }
                        }
                    }
                }

            }

            println '==============Dexwalila end=================='

            project.android.applicationVariants.each { variant ->
                println variant.name
                //替换keep文件
                def collectTask = project.getTasks().findByName("collect${variant.name.capitalize()}MultiDexComponents")//collectZroTestDebugMultiDexComponents
                if (collectTask != null) {
                    TransformTask dexTask = project.getTasks().findByName("transformClassesWithMultidexlistFor${variant.name.capitalize()}")
                    if (dexTask != null) {
                        //MultiDexTransform
                        Transform trans = dexTask.getTransform()
                        //println '0000000000000 ' + trans.class.getName()
                        if (trans.class.getSimpleName().equals("MultiDexTransform")) {
                            Field t = TransformTask.getDeclaredField("transform");
                            t.setAccessible(true);
                            def myMultiDexTransform = create((MultiDexTransform) trans)
                            t.set(dexTask, myMultiDexTransform)
                        }
                    }
//                    TransformTask dexTask = project.getTasks().findByName("transformClassesWithMultidexlistFor${variant.name.capitalize()}")
//                    if (dexTask != null) {
//                        //MultiDexTransform
//                        Transform trans = dexTask.getTransform()
//                        //println '0000000000000 ' + trans.class.getName()
//                        if (trans.class.getSimpleName().equals("MultiDexTransform")) {
//                            println '0000000000000 find MultiDexTransform'
//                            trans.metaClass.keep = {
//                                String str ->
//                                    println '0000000000 ' + str
//                            }
//                        }
//                    }

                    List<Action<? super Task>> list = new ArrayList<>()
                    list.add(new Action<Task>() {
                        @Override
                        void execute(Task task) {
                            def backManifestListFile = new File(project.getProjectDir().absolutePath+ "/manifest_keep.txt")
                            if(backManifestListFile.exists()) {
                                println "collect${variant.name.capitalize()}MultiDexComponents action execute!mini main dex生效了!!!!" + project.getProjectDir()
                                def dir = new File(project.getProjectDir().absolutePath+ "/build/intermediates/multi-dex/${variant.dirName}");
                                if (!dir.exists()) {
                                    println "$dir 不存在,进行创建"
                                    dir.mkdirs()
                                }
                                def manifestkeep = new File(dir.getAbsolutePath() + "/manifest_keep.txt")
                                manifestkeep.delete()
                                manifestkeep.createNewFile()
                                println "先删除,后创建manifest_keep"

                                backManifestListFile.eachLine {
                                    line ->
                                        manifestkeep << line << '\n'
                                }
                            }else{
                                println "不存在自定义mainifest_keep文件"
                            }
                        }
                    })
                    collectTask.setActions(list)
                }

//                println '==============Dexwalila end=================='
            }
        }


//        println '==============Dexwalila apply end=================='
    }


    @Override
    public String getName() {
        return "Dexwalila";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }



}