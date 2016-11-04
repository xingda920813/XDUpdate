package com.xdandroid.pushhotfix

import scala.beans.BeanProperty

/**
  * Created by xingda on 16-11-4.
  */
class PushCustomParams extends Serializable {
  @BeanProperty var deliveryItemId: String = _
  @BeanProperty var `type`: String = _
  @BeanProperty var subType: String = _
}
