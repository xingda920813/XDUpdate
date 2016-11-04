package com.xdandroid.pushhotfix

import scala.beans.BeanProperty

/**
  * Created by xingda on 16-11-4.
  */
class Push extends Serializable {

  @BeanProperty var alert: String = _
  @BeanProperty var title: String = _
  @BeanProperty var action: String = _
  @BeanProperty var silent: Boolean = _
  @BeanProperty var pushCustomParams: PushCustomParams = _
}
