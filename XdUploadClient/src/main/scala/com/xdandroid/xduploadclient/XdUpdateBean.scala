package com.xdandroid.xduploadclient

import scala.beans.BeanProperty

/**
  * Created by xingda on 16-11-4.
  */
class XdUpdateBean extends Serializable {

  @BeanProperty var versionCode: Int = _
  @BeanProperty var size: Int = _
  @BeanProperty var versionName: String = _
  @BeanProperty var url: String = _
  @BeanProperty var note: String = _
  @BeanProperty var md5: String = _
}
