package com.xdupload

import java.io.*

/**
 * Created by xingda on 16-8-24.
 */
internal class XdUpdateBean : Serializable {

    var versionCode: Int = 0
    var size: Int = 0
    var versionName: String? = null
    var url: String? = null
    var note: String? = null
    var md5: String? = null
}
