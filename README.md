# BrushScribbleSdk

apk下载地址:(体验:拇指写毛笔字)

https://phone-love-piano-public-ro.oss-cn-shenzhen.aliyuncs.com/demo-apk/BrushScribbleSdk_debug_0403.apk

或手机扫码下载

![Image scan qrcode download apk](https://phone-love-piano-public-ro.oss-cn-shenzhen.aliyuncs.com/demo-apk/brush_scribble_apk_download_qrcode.png)


##  1.对!是你想要的毛笔笔锋效果

##  2.BrushScribbleView是一个背景透明、实时显示毛笔笔迹的surfaceView，不管是大笔迹量还是超长笔迹，都不闪

##  3.欢迎体验，欢迎参与优化！

##  使用姿势

### 1.项目根目录的gradle文件
buildscript.repositories{ maven { url "https://jitpack.io" } }

allprojects.repositories{ maven { url "https://jitpack.io" } }

###  2.您要使用BrushScribbleView的module(比如app)的gradle文件添加
implementation 'com.github.jj532655203:BrushScribbleSdk:1.0.3'

