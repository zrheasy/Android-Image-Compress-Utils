package com.zrh.image

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.zrh.image.databinding.ActivityMainBinding
import com.zrh.permission.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val selectFileLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent = result.data!!
                if (data.data != null) {
                    loadImage(data.data!!)
                }
            }
        }

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
            val file = ImageCompressUtils.compressImage(imageFile!!, dir, fileName, mConfig)
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
        val i = Intent()
        i.type = "image/*"
        i.putExtra(Intent.EXTRA_MIME_TYPES, arrayListOf("image/*"))
        i.action = Intent.ACTION_GET_CONTENT
        i.addCategory(Intent.CATEGORY_OPENABLE)

        try {
            selectFileLauncher.launch(i)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadImage(data: Uri) {
        val loading = LoadingDialog(this)
        flow<Pair<Bitmap, File>> {
            val fileName = (System.currentTimeMillis() / 1000).toString()
            val file =
                FileUtils.getFileFromUri(applicationContext, data, File(cacheDir, "image"), fileName)
                    ?: throw IOException("load image error: $data")
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