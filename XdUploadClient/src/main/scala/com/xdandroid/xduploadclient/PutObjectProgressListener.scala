package com.xdandroid.xduploadclient

import com.aliyun.oss.event.{ProgressEvent, ProgressEventType, ProgressListener}

/**
  * Created by xingda on 16-11-4.
  */
class PutObjectProgressListener extends ProgressListener {

  var bytesWritten: Long = 0
  var totalBytes: Long = -1
  var lastPercent: Long = 0

  override def progressChanged(event: ProgressEvent) {
  val bytes = event.getBytes
  val eventType = event.getEventType
    eventType match {
      case ProgressEventType.TRANSFER_STARTED_EVENT => println("开始上传.")
      case ProgressEventType.REQUEST_CONTENT_LENGTH_EVENT =>
        totalBytes = bytes
        println("将上传 " + Utils.formatToMegaBytes(totalBytes) + ".")
      case ProgressEventType.REQUEST_BYTE_TRANSFER_EVENT =>
        bytesWritten += bytes
        val percent = bytesWritten * 100 / totalBytes
        if (percent == lastPercent) return
        lastPercent = percent
        println(percent.toString + " % (" + Utils.formatToMegaBytes(bytesWritten) + " / " + Utils.formatToMegaBytes(totalBytes) + ")")
      case ProgressEventType.TRANSFER_COMPLETED_EVENT => println("上传成功.")
      case ProgressEventType.TRANSFER_FAILED_EVENT => println("上传失败, 已上传 " + Utils.formatToMegaBytes(bytesWritten) + ".")
      case _ =>
    }
}
}
