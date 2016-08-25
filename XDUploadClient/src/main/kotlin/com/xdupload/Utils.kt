package com.xdupload

import com.google.gson.*
import net.dongliu.apk.parser.*
import java.io.*
import java.math.*
import java.nio.channels.*
import java.security.*

/**
 * Created by xingda on 16-8-24.
 */
internal object Utils {

    val sGson: Gson

    init {
        sGson = Gson()
    }

    fun extractApkInfo(): XdUpdateBean {
        val apk = File(Environment.sPackageName + ".apk")
        val parser = ApkParser(apk)
        val apkMeta = parser.apkMeta
        val xdUpdateBean = XdUpdateBean()
        xdUpdateBean.versionCode = Math.toIntExact(apkMeta.versionCode!!)
        xdUpdateBean.size = fileSize
        xdUpdateBean.versionName = apkMeta.versionName
        xdUpdateBean.url = Environment.sCdnDomain + Environment.sPathPrefix + Environment.sPackageName + ".apk"
        xdUpdateBean.note = Environment.sReleaseNote
        xdUpdateBean.md5 = md5ByFile
        return xdUpdateBean
    }

    private val fileSize: Int
        get() {
            val file = File(Environment.sPackageName + ".apk")
            return file.length().toInt()
        }

    private val md5ByFile: String
        get() {
            val file = File(Environment.sPackageName + ".apk")
            val fis = FileInputStream(file)
            val byteBuffer = fis.channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
            val md5 = MessageDigest.getInstance("MD5")
            md5.update(byteBuffer)
            val bi = BigInteger(1, md5.digest())
            return bi.toString(16)
        }

    fun saveJsonToFile(json: String) {
        val jsonFile = File(Environment.sPackageName + ".json")
        val fos = FileOutputStream(jsonFile)
        fos.write(json.toByteArray(charset("UTF-8")))
        fos.close()
    }

    fun readPropertiesAsString(): String {
        val fis = FileInputStream(File("config.properties"))
        var len: Int
        val buffer = ByteArray(8192)
        val baos = ByteArrayOutputStream()
        while (true) {
            len = fis.read(buffer)
            if (len < 0) break
            baos.write(buffer, 0, len)
        }
        val content = baos.toString("UTF-8")
        baos.close()
        fis.close()
        return content
    }
}
