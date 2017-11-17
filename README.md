# 美柚 Android DEX 处理插件

## 什么是瓦莉拉

瓦莉拉是一个能处理Dex的插件,他能控制主DEX的内容,帮助降低主DEX 65535的风险。

## 使用

使用瓦莉拉,先需要关闭jumbo mode,由于目前官方默认开启jumboMode,所以需要手动关闭。

```groovy
    dexOptions {
        //incremental true
        //preDexLibraries = false
        jumboMode = false
        javaMaxHeapSize "4g"

    }
```

关闭后在工程classpath中引入插件。

```groovy
classpath 'com.meiyou.sdk.plugin:dexwalila-compiler:0.0.43-SNAPSHOT'
```


然后在主工程中apply

```groovy
apply plugin: 'dexwalila'
```

最后在主工程的gradle中配置瓦莉拉

```groovy
dexwalila {
    enable true
}

```

enable 表示是否开启瓦莉拉,一切安排妥当后,sync gradle并run。

瓦莉拉会把执行日志输出到工程目录下的dexwalila_log.txt中。

## 瓦莉拉做了什么?

瓦莉拉对SDK内的dex打包工具做了修改,使得原本的dex打包流程发现了变化。

`1、对dx增加minidex,阻止dx塞入其他额外的类`

`2、对dx修改了keepannotations,不允许在componentClassese.jar之后插入依赖可见注解类`

`3、日志输出`

`4、线程数量控制`

`5、自定义mainifest_keep替换`

`6、替换了官方的MultiDexTransform,排除了rules keep`

第1、6和第2项使得主dex的内容严格按照keep文件的依赖结果,maindexlist不额外增加类。第5项能让dex根据我们想要的keep内容保证主DEX存放我们想要的类。

linhh 2017.07.24