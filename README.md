# Android-Image-Compress-Utils
Android轻量级图片压缩工具库。

[下载地址](https://github.com/zrheasy/Android-Image-Compress-Utils/releases/download/v1.0.0/image-compress-v1.0.aar)

![压缩截图](assets/Image-Compress.png)

### 使用方式
#### 1.下载AAR并添加到项目依赖中。
```groovy
implementation fileTree(dir: 'libs', include: ['*.jar','*.aar'])
```
#### 2.调用压缩图片函数（建议在IO线程中执行）。
```kotlin
val file = ImageCompressUtils.compress(imageFile, dir, fileName, mConfig)
```