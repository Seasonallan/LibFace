# 第一步
在根目录的 build.gradle中 allprojects的repositories里添加jitpack依赖
maven { url 'https://jitpack.io' }
# 第二步
在app项目的build.gradle下的dependencies中添加IMO库依赖
implementation 'com.github.Seasonallan:LibFace:2.0'
# 第三步
在app项目的build.gradle下添加配置
1、在defaultConfig中添加
        ndk {
            abiFilters "x86", 'armeabi-v7a', 'armeabi'
        }
2、在android中添加
 repositories {
        flatDir {
            dirs 'libs'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
        
# 第四步
在AndroidManifest.xml中添加摄像头权限和网络权限（IMO校验key是否正确需要）
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
# 第五步
使用IMoBridge调用IMO库API
示例1 交互式活体检测：
 demo中的DemoFaceActionActivity
示例2 人脸登录：
 demo中的DemoFaceLoginActivity
示例3 静态活体检测：
 demo中的DemoFaceStaticActivity
 