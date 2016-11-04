package com.xdandroid.pushhotfix

import com.google.gson.Gson
import okhttp3.{MediaType, OkHttpClient, Request, RequestBody}

/**
  * Created by xingda on 16-11-4.
  */
object PushMain {

  def main(args: Array[String]): Unit = {
    val leanCloudBean = new LeanCloudBean
    leanCloudBean.data = new Push
    leanCloudBean.data.action = Environment.sAction
    leanCloudBean.data.silent = true
    leanCloudBean.data.pushCustomParams = new PushCustomParams
    leanCloudBean.data.pushCustomParams.`type` = "hotfix"
    leanCloudBean.data.pushCustomParams.subType = Environment.sVersionName
    val json = new Gson().toJson(leanCloudBean)
    val req = new Request
      .Builder()
      .url("https://leancloud.cn/1.1/push")
      .method("POST", RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json))
      .header("X-LC-Id", Environment.sAppId)
      .header("X-LC-Key", Environment.sAppKey)
      .build()
    val res = new OkHttpClient().newCall(req).execute()
    println(res.body().string())
  }
}
