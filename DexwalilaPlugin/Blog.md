# 美柚Android DEX分包演进之路（二）

## 前言

通过[美柚Android DEX分包演进之路（一）](http://git.meiyou.im/Android/AndroidDoc/blob/master/DexWalila.md)，我们完成了对主DEX的内容控制，极大的缩减了主DEX的大小，使美柚的主DEX只有53KB，提升了APP启动的响应速度。但是实际使用中，我们发现APP虽然启动很快，但是第一次安装（或版本升级）后，APP进入欢迎界面的时间却很长，而后续启动却没有这个问题，经过观察发现实际上Dex优化前的APP以前版本第一次启动欢迎界面也会有较长时间，这是为什么呢？

要想知道为什么，首先我们要知道为什么这种情况会出现在第一次。

## 万恶的MultiDex

在演进之路（一）中，我们知道一旦业务多了，App就会变大，产生65535。用了MultiDex后，65535的问题得到解决，但应用的Dex数量也会变多，主要是MultiDex将原本的1个Dex切割成了几个Dex，编译打包时，这些dex会随主DEX被打入Apk中。

当用户安装了应用，点击icon启动应用时，系统只会加载主DEX，其他副Dex只有当主Dex启动后调用MultiDex.install才会被载入到PathClassLoader中，而MultiDex.install则是我们手动调用的，系统并不会主动调用，而副Dex被加载后，应用内相关的业务才算正式可以运行。

实际上Android系统默认并不是直接运行Apk的Dex文件的，而是有一个DEX优化过程，这个是dex提前解释的阶段，在Dalvik虚拟机下这个过程是dex2opt，输出odex文件，在ART虚拟机下这个过程是dex2oat，输出oat文件，这些文件会被缓存到Android系统目录中，Android系统实际上加载的是这些被优化过的dex。

一个新应用安装后（版本升级）初次启动，系统会先解压apk，提取主DEX，加载主DEX，然后根据信息到缓存目录寻找是否存在优化的dex文件，如果有则加载优化的dex，没有就将主DEX进行对应的优化，保存到缓存目录，再加载这个优化过的dex，启动App，app启动后当执行到MultiDex.install的时候，系统会查看apk中是否存在其他dex，然后将他们提取优化，也就是主DEX的那一系列步骤。

而MultiDex.install是无法被放到异步线程的，因为我们无法确保这个dex内的业务是否需要马上被使用，所以MultiDex.install只能放置在主线程，而这一步骤相对耗时，万一这一步骤执行较长，达到了5秒，那么App就会ANR，所以MultiDex.install是耗时的万恶之源。

## 美柚现在的思路

美柚现在的做法是当Application启动时，启动一个私有进程：mini，同时主进程阻塞轮询，mini进程进行MultiDex.install，完成后创建一个文件，主进程轮询到有文件后进行Application后续的操作打开。

所以我们可以模拟美柚启动的情况，用户启动APP，系统需要花费时间加载主DEX，由于初次安装（或升级）又花费了时间去做dex优化，（这里的加载和优化dex的时间根据主DEX大小而异，因为现在优化到53KB，所以这步变的很快，之前是7.3MB），主DEX启动后，Application启动，创建私有进程mini进行MultiDex.install开始加载其他副Dex，加载副Dex后又进行了Dex优化，导致整个时间较长，优化完毕后Application才进行后续的操作。

但是这个耗时仍然是无法避免的，统计发现这个时间在10s左右。

另外由于插件化是以apk打包的，所以如果我们使用插件化，那么插件中的dex也必然会耗费很长的加载时间，这显然是个必须解决的问题。

## Dex瓦莉拉2.0

瓦莉拉1.0虽然减小了主DEX的大小，使App能达到毫秒级响应，但是没有对Dex加载速度进行优化。

那有没有什么办法来优化这个时间呢？

我们首先想到的是跳过系统优化DEX的时间。

主DEX的时间我们是没办法避免的，因为这是系统必须做的，我们唯一能优化主DEX加载时间的办法只有尽可能的让主DEX变小，好在主DEX目前已经能被瓦莉拉缩小到了53K，他的响应速度已经达到了毫秒级别，唯一需要处理的就是副DEX。

我们首先想到如果我们内置优化过的dex到apk中，主dex加载后释放这些优化过的dex到系统缓存目录是否就可行了呢？答案是不行的，因为这个DEX优化过程因设备而定，不同的设备优化文件有所不同。

但是我们找到了另一个优化办法，那就是不让系统进行dex优化，优化工作放到后台。

实际上在ART虚拟机下，系统是支持直接原始dex解释的，我们完全可以不需要让系统进行dex优化，但是我们却想使用这个系统优化的dex文件，所以我们可以这样做：应用启动，判断dex是否已经优化过，如果没有则跳过dex优化，直接将DEX载入执行Application后续操作，同时将优化放置到后台，下次启动时直接使用优化的dex文件，这样优化dex的时间就不会被安排到用户启动app的时间上，一切无感知。

要做到这点，我们需要明白dex优化的步骤在哪里。

### ART虚拟机

通过对代码的跟踪，我们发现，dex优化虽然是MultiDex.install触发的，但是代码却不在MultiDex中，在ART虚拟机模式下，DEX优化新建一个进程通过/system/bin/dex2oat命令进入libc.so的execv方法中，主进程保持阻塞，待新建进程execv执行完毕后继续，所以我们只需要hook这个execv方法，判断如果是/system/bin/dex2oat发起就直接退出当前进程，让主进程不阻塞就可以了。

但是libc.so不在Java层，而在系统内，通过美柚安娜无法达到hook，所以只能通过Cydia进行hook，JNI hook代码如下。

```groovy

# Walila.cpp
# lhh 2017/7/30

int meetyou_execv(const char *name, char **argv) {
	if(isDex2oat(name)) {
		//如果是dex2oat就直接退出
		exit(0);
	}

  	return origin_execv(name, argv);
}
```

hook完毕后，我们就可以跳过系统优化dex的时间，应用到美柚中，第一次启动MultiDex.install的返回耗时达到了毫秒级。

但是这只是跳过优化时间，我们还是需要系统优化过的dex，因此我们对hook方法进行了改造，当我们的Application启动时判断是否有优化的dex，没有则进行hook，同时创建一个私有进程进行优化，优化完毕后保存结果提供给Application下次启动时使用。

### Dalvik虚拟机

Dalvik虚拟机天然无法支持Dex文件，但是在JNI层提供了一个openDexFile(byte *file)的方法，openDexFile没有对应的Java层，只能通过JNI HOOK。

Hook后也能达到毫秒级加载，但是当项目中使用了java.lang.Class.getTypeParameters()等函数时，就会在Android 4.4上crash掉，原因是4.4 JNI层在使用这些方法时new了新的Dex对象，hook后创建dex使用的参数都是空的，所以会出现问题。

所以我们暂时不在4.4 的Dalvik虚拟机下使用Walila。

## 结语

通过这一系列的处理，美柚的dex第一次安装加载速度从10s降低到了10ms,加载速度达到了真正意义的毫秒级，从主Dex内容的控制到Dex加载的控制，我们都能随心所欲，唯一的缺点是不支持4.4。

目前这一系列技术已经在瓦莉拉2.0中实现。


author

2017.7.30 linhonghong