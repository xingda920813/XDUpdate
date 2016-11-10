package com.xdandroid.xduploadclient

import java.io.{File, FileInputStream, FileOutputStream}
import java.math.BigInteger
import java.nio.channels.FileChannel
import java.security.MessageDigest
import java.text.DecimalFormat

import com.google.gson.Gson
import net.dongliu.apk.parser.ApkParser

import scala.io.Source

/**
  * Created by xingda on 16-11-4.
  */
object Utils {

  var sArgs: Array[String] = _

  val sGson = new Gson

  def extractApkInfo(args: Array[String]): XdUpdateBean = {
    sArgs = args
    val apk = new File(Environment.sPackageName + ".apk")
    val parser = new ApkParser(apk)
    val apkMeta = parser.getApkMeta
    val xdUpdateBean = new XdUpdateBean()
    xdUpdateBean.versionCode = Math.toIntExact(apkMeta.getVersionCode)
    xdUpdateBean.size = getFileSize
    xdUpdateBean.versionName = apkMeta.getVersionName
    xdUpdateBean.url = Environment.sCdnDomain + Environment.sPathPrefix + Environment.sPackageName + ".apk"
    xdUpdateBean.note = if (sArgs != null && sArgs.length >= 2) sArgs(1) else Environment.sReleaseNote
    xdUpdateBean.md5 = getMd5ByFile
    xdUpdateBean
  }

  def getFileSize: Int = {
    val file = new File(Environment.sPackageName + ".apk")
    file.length().toInt
  }

  def getMd5ByFile: String = {
    val file = new File(Environment.sPackageName + ".apk")
    val fis = new FileInputStream(file)
    val byteBuffer = fis.getChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
    val md5 = MessageDigest.getInstance("MD5")
    md5.update(byteBuffer)
    val bi = new BigInteger(1, md5.digest())
    String.format("%032x", bi)
  }

  def formatToMegaBytes(bytes: Long): String = {
    val megaBytes = bytes / 1048576.0
    if (megaBytes < 1) {
      return new DecimalFormat("0.0").format(megaBytes) + " M"
    }
    new DecimalFormat("#.0").format(megaBytes) + " M"
  }

  def saveJsonToFile(json: String) {
    val jsonFile = new File(Environment.sPackageName + ".json")
    val fos = new FileOutputStream(jsonFile)
    fos.write(json.getBytes("UTF-8"))
    fos.close()
  }

  def readPropertiesAsString: String = {
    val file = if (sArgs != null && sArgs.length >= 1) new File(sArgs(0)) else new File("config.properties")
    Source.fromFile(file, "UTF-8").mkString
  }
}
