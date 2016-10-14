package com.xdupload

import com.aliyun.oss.event.*

/**
 * Created by XingDa on 2016/10/13.
 */
class PutObjectProgressListener: ProgressListener {

    var bytesWritten: Long = 0
    var totalBytes: Long = -1
    var lastPercent: Long = 0

    override fun progressChanged(event: ProgressEvent) {
        val bytes = event.bytes
        val eventType = event.eventType
        when (eventType) {
            ProgressEventType.TRANSFER_STARTED_EVENT -> println("开始上传.")
            ProgressEventType.REQUEST_CONTENT_LENGTH_EVENT -> {
                totalBytes = bytes
                println("将上传 " + Utils.formatToMegaBytes(totalBytes) + ".")
            }
            ProgressEventType.REQUEST_BYTE_TRANSFER_EVENT -> {
                bytesWritten += bytes
                val percent = (bytesWritten * 100 / totalBytes)
                if (percent == lastPercent) return
                lastPercent = percent
                println(percent.toString() + " % (" + Utils.formatToMegaBytes(bytesWritten) + " / " + Utils.formatToMegaBytes(totalBytes) + ")")
            }
            ProgressEventType.TRANSFER_COMPLETED_EVENT -> println("上传成功.")
            ProgressEventType.TRANSFER_FAILED_EVENT -> println("上传失败, 已上传 " + Utils.formatToMegaBytes(bytesWritten) + ".")
            else -> {}
        }
    }
}
