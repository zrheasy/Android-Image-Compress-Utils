package com.zrh.image

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.EditText
import com.zrh.image.databinding.DialogConfigBinding

/**
 *
 * @author zrh
 * @date 2023/7/4
 *
 */
class ConfigDialog(context: Context, val config: ImageCompressUtils.Config) : Dialog(context, R.style.AlertDialog) {
    private val mBinding: DialogConfigBinding

    init {
        mBinding = DialogConfigBinding.inflate(LayoutInflater.from(context))
        setContentView(mBinding.root)

        mBinding.editMaxWidth.setText(config.maxWidth.toString())
        mBinding.editMaxHeight.setText(config.maxHeight.toString())
        mBinding.editBytesLength.setText("${config.maxBytesLength / (1024 * 1024)}")
        mBinding.editMinQuality.setText(config.minQuality.toString())
        when (config.compressFormat) {
            Bitmap.CompressFormat.JPEG -> {
                mBinding.radioGroup.check(R.id.radioJpeg)
            }
            Bitmap.CompressFormat.PNG -> {
                mBinding.radioGroup.check(R.id.radioPng)
            }
            else -> {
                mBinding.radioGroup.check(R.id.radioWebp)
            }
        }

        mBinding.btnCancel.setOnClickListener { dismiss() }
        mBinding.btnOk.setOnClickListener {
            dismiss()

            config.maxWidth = getValue(mBinding.editMaxWidth)
            config.maxHeight = getValue(mBinding.editMaxHeight)
            config.maxBytesLength = getValue(mBinding.editBytesLength) * 1024 * 1024
            config.minQuality = getValue(mBinding.editMinQuality)

            when (mBinding.radioGroup.checkedRadioButtonId) {
                R.id.radioJpeg -> config.compressFormat = Bitmap.CompressFormat.JPEG
                R.id.radioPng -> config.compressFormat = Bitmap.CompressFormat.PNG
                R.id.radioWebp -> config.compressFormat = Bitmap.CompressFormat.WEBP
            }
        }
    }

    private fun getValue(edit: EditText): Int {
        val value = edit.text.toString()
        return if (value.isEmpty()) 0 else value.toInt()
    }

    override fun show() {
        super.show()

        val window = window
        val p = window!!.attributes
        p.width = dp2px(300)
        p.gravity = Gravity.CENTER
        window.attributes = p

    }

    private fun dp2px(dpValue: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpValue.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}