package com.meetyou.dex.walila;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Created by Linhh on 2017/7/21.
 */

public class DexWalila {

    public static String pro = "[===============]";

    public static void print(){
        try {
            File pro_file = new File(pro);
            if(!pro_file.exists()){
                pro_file.createNewFile();
            }
            PrintStream printStream = new PrintStream(new FileOutputStream(new File(

                    pro), true));


//            System.setOut(printStream);

            System.setErr(printStream);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void printArgs(String[] args){
        for(String s : args) {
            System.err.println(s);
        }
    }

    public static String[] maindex(String[] args){
        String[] new_args = new String[args.length + 1];
        new_args[0] = "--disable-annotation-resolution-workaround";
        for (int i = 0; i < args.length; i++) {
            new_args[i + 1] = args[i];
        }

        args = new_args;
        return args;
    }

    public static String[] main(String[] args){
        if(args[0].equals("--dex")) {
            String[] new_args = new String[args.length + 1];
            new_args[0] = args[0];
            new_args[1] = "--minimal-main-dex";
            for (int i = 1; i < args.length; i++) {
                new_args[i + 1] = args[i];
            }
            args = new_args;
        }
        return args;
    }
}
