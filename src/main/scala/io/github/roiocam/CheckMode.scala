package io.github.roiocam

sealed trait CheckMode

case object CheckConfig extends CheckMode
case object CheckTask extends CheckMode
case object CheckBoth extends CheckMode
