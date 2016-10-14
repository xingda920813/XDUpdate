package com.xdupload

import com.aliyun.oss.*
import com.aliyun.oss.common.auth.*
import com.aliyun.oss.model.*
import rx.*
import rx.schedulers.*
import java.io.*

object OSSMain {

    @JvmStatic fun main(args: Array<String>) {
        val xdUpdateBean = Utils.extractApkInfo(args)
        val json = Utils.sGson.toJson(xdUpdateBean)
        Utils.saveJsonToFile(json)
        doUpload()
        Thread.sleep(15 * 60 * 1000.toLong())
    }

    private fun doUpload() {
        val credentialsProvider = DefaultCredentialProvider(Environment.sAccessKeyId, Environment.sAccessKeySecret)
        val oss = OSSClient(Environment.sEndpoint, credentialsProvider)
        val jsonObsrv = Observable.create(Observable.OnSubscribe<Boolean> {putObject("json", oss, it)}).subscribeOn(Schedulers.io())
        val apkObsrv = Observable.create(Observable.OnSubscribe<Boolean> {putObject("apk", oss, it)}).subscribeOn(Schedulers.io())
        Observable.combineLatest(listOf(jsonObsrv, apkObsrv), { true })
                .observeOn(Schedulers.immediate())
                .subscribe({
                               oss.shutdown()
                               System.exit(0)
                           }, { it.printStackTrace() })
    }

    private fun putObject(type: String, oss: OSS, subscriber: Subscriber<in Boolean>) {
        val request: PutObjectRequest
        when (type) {
            "json" -> request = PutObjectRequest(Environment.sBucketName,
                                                 Environment.sPathPrefix + Environment.sPackageName + ".json",
                                                 File(Environment.sPackageName + ".json"))
            else   -> {
                val apk = File(Environment.sPackageName + ".apk")
                request = PutObjectRequest(Environment.sBucketName,
                                           Environment.sPathPrefix + Environment.sPackageName + ".apk",
                                           apk)
            }
        }

        try {
            oss.putObject(request.withProgressListener(PutObjectProgressListener()))
            subscriber.onNext(true)
            subscriber.onCompleted()
        } catch (e: ClientException) {
            subscriber.onError(e)
        } catch (e: ServiceException) {
            subscriber.onError(e)
        } catch (e: NullPointerException) {
            subscriber.onError(e)
        }
    }
}
