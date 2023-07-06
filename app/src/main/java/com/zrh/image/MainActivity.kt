package com.zrh.image

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.zrh.file.picker.FilePickCallback
import com.zrh.file.picker.FilePickOptions
import com.zrh.file.picker.FilePicker
import com.zrh.file.picker.UriUtils
import com.zrh.image.databinding.ActivityMainBinding
import com.zrh.permission.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding
    private val mConfig = ImageCompressUtils.Config().apply {
        maxWidth = 1080
        maxHeight = 1960
    }
    private var imageFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.btnSelectImage.setOnClickListener {
            PermissionUtils.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)) { _, granted ->
                if (granted) {
                    selectFile()
                }
            }
        }

        mBinding.btnCompressImage.setOnClickListener {
            compressImage()
        }

        mBinding.btnCompressConfig.setOnClickListener {
            ConfigDialog(this, mConfig).show()
        }
    }

    private fun compressImage() {
        if (imageFile == null) {
            toast("请选择图片文件")
            return
        }
        val loading = LoadingDialog(this)
        flow<Pair<Bitmap, File>> {
            val fileName = (System.currentTimeMillis() / 1000).toString()
            val dir = File(cacheDir.absolutePath + "/image_compress")
            val file = ImageCompressUtils.compress(imageFile!!, dir, fileName, mConfig)
            val bitmap = ImageCompressUtils.getBitmap(file, 0, 0)
                ?: throw IOException("load image error: ${file.absoluteFile}")
            emit(Pair(bitmap, file))
        }.flowOn(Dispatchers.IO)
            .onStart { loading.show() }
            .onEach {
                mBinding.tvCompress.text =
                    "压缩大小: ${it.second.length() / 1024}kb | 分辨率：${it.first.width}x${it.first.height}"
                mBinding.ivCompress.setImageBitmap(it.first)
                mBinding.tvCompress.isVisible = true
                mBinding.ivCompress.isVisible = true
            }
            .catch { toast("压缩失败") }
            .onCompletion { loading.dismiss() }
            .launchIn(lifecycleScope)
    }

    private fun selectFile() {
        val options = FilePickOptions().apply {
            mimeType = "image/*"
            isAllowMultiple = false
        }
        FilePicker.pick(this, options, object: FilePickCallback{
            override fun onResult(data: MutableList<Uri>) {
                loadImage(data[0])
            }

            override fun onError(p0: Int, p1: String) {

            }
        })
    }

    private fun loadImage(data: Uri) {
        val loading = LoadingDialog(this)
        flow<Pair<Bitmap, File>> {
            val cache = if (externalCacheDir != null) externalCacheDir else cacheDir
            val file = UriUtils.getFileFromUri(applicationContext, data, File(cache, "image"))
            val bitmap = ImageCompressUtils.getBitmap(file, 0, 0)
                ?: throw IOException("load image error: $data")
            emit(Pair(bitmap, file))
        }.flowOn(Dispatchers.IO)
            .onStart { loading.show() }
            .onEach {
                imageFile = it.second
                mBinding.tvOrigin.text =
                    "原图大小: ${it.second.length() / 1024}kb | 分辨率：${it.first.width}x${it.first.height}"
                mBinding.ivOrigin.setImageBitmap(it.first)
                mBinding.tvCompress.isVisible = false
                mBinding.ivCompress.isVisible = false
            }
            .catch { toast("加载出错") }
            .onCompletion { loading.dismiss() }
            .launchIn(lifecycleScope)
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}