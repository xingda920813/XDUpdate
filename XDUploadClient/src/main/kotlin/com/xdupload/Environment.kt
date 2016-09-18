package com.xdupload

import java.io.*
import java.util.*

/**
 * Created by xingda on 16-8-24.
 */
internal object Environment {

    val sPackageName: String
    val sReleaseNote: String
    val sCdnDomain: String
    val sEndpoint: String
    val sAccessKeyId: String
    val sAccessKeySecret: String
    val sBucketName: String
    val sPathPrefix: String

    init {
        val properties = Properties()
        properties.load(StringReader(Utils.readPropertiesAsString()))
        sPackageName = properties.getProperty("packageName")
        sReleaseNote = properties.getProperty("releaseNote")
        sCdnDomain = properties.getProperty("cdnDomain")
        sEndpoint = properties.getProperty("endpoint")
        sAccessKeyId = properties.getProperty("accessKeyId")
        sAccessKeySecret = properties.getProperty("accessKeySecret")
        sBucketName = properties.getProperty("bucketName")
        sPathPrefix = properties.getProperty("pathPrefix")
    }
}
