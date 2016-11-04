package com.xdandroid.xduploadclient

import java.io.StringReader
import java.util.Properties


/**
  * Created by xingda on 16-11-4.
  */
object Environment {

  val properties = new Properties()
  properties.load(new StringReader(Utils.readPropertiesAsString))

  val sPackageName = properties.getProperty("packageName")
  val sReleaseNote = properties.getProperty("releaseNote")
  val sCdnDomain = properties.getProperty("cdnDomain")
  val sEndpoint = properties.getProperty("endpoint")
  val sAccessKeyId = properties.getProperty("accessKeyId")
  val sAccessKeySecret = properties.getProperty("accessKeySecret")
  val sBucketName = properties.getProperty("bucketName")
  val sPathPrefix = properties.getProperty("pathPrefix")
}
