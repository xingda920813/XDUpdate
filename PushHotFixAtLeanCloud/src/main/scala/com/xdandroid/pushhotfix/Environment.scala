package com.xdandroid.pushhotfix

import java.io.{File, StringReader}
import java.util.Properties

import scala.io.Source

/**
  * Created by xingda on 16-11-4.
  */
object Environment {

  val properties = new Properties()
  properties.load(new StringReader(Source.fromFile(new File("config.properties")).mkString))

  val sAction = properties.getProperty("action")
  val sVersionName = properties.getProperty("versionName")
  val sAppId = properties.getProperty("appId")
  val sAppKey = properties.getProperty("appKey")
}
