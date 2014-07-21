package video

import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit

/** Fundamental unit of video in the client. */
case class Frame(image: BufferedImage, timeStamp: Long, timeUnit: TimeUnit)
