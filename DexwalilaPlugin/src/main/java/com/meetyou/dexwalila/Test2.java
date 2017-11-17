package com.meetyou.dexwalila;

import com.android.multidex.MainDexListBuilder;

/**
 * Created by Linhh on 2017/7/24.
 */
public class Test2 {
    public static void main(String[] args){
        String[] a = new String[]{"/Users/Linhh/AndroidStudioProjects/meetyou/component/AndroidComponentProject/AndroidComponentMain/app/build/intermediates/multi-dex/product/debug/componentClasses.jar","/Users/Linhh/AndroidStudioProjects/meetyou/component/AndroidComponentProject/AndroidComponentMain/app/build/intermediates/transforms/jarMerging/product/debug/jars/1/1f/combined.jar"};
        MainDexListBuilder.main(a);
    }
}
