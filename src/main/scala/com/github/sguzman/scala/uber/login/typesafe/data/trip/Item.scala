package com.github.sguzman.scala.uber.login.typesafe.data.trip

case class Item(
               description: String,
               icon: Option[String],
               amount: String,
               itemType: String,
               shouldShowPlusSign: Boolean,
               disclaimer: Option[String],
               recognizedAt: Option[Long]
               )
