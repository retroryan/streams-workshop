package video

import java.awt.Color


object Ansi {
  // Control sequence introducer for ANSI commands.
  private val CSI = s"${27.toChar}["
  // Select graphic Rendition
  private def SGR(n: Int) = s"${CSI}${n}m"

  /** Moves the current cursor to the upper left of the terminal. */
  val MOVE_CURSOR_TO_UPPER_LEFT = s"${CSI}H"
  /** Clears the current screen. */
  val CLEAR_SCREEN = s"${CSI}2J"
  /** Resets the current color */
  val RESET_COLOR = SGR(0)
  Console.RESET
  /** Sets the text to be bold */
  val BOLD = SGR(1)
  /** Sets the text to be underlined. */
  val UNDERLINE = SGR(2)
  /** Sets to the default font. */
  val DEFAULT_FONT = SGR(10)
  /** Changes the font */
  def SELECT_FONT(n: Int) = SGR(10 + n) // TODO - force n > 0 and n < 10

  // TODO - default color table
  private val ANSI_FOREGROUND = "38"
  private val ANSI_BASIC_BASE = 16
  /** Convert a color into the closest possible ANSI equivalent. */
  def FOREGROUND_COLOR(c: Color): String = {
    val r = toAnsiiSpace(c.getRed)
    val g = toAnsiiSpace(c.getGreen)
    val b = toAnsiiSpace(c.getBlue)
    val code = (ANSI_BASIC_BASE + (r * 36) + (g * 6) + (b))
    //if(code == ANSI_BASIC_BASE) RESET_COLOR
    //else
    s"${CSI}${ANSI_FOREGROUND};5;${code}m"
  }
  // Convert a magnitude in RGB 32bit space into RGB ANSI space
  private def toAnsiiSpace(mag: Int): Int = {
    // ANSII uses 6 bits of percisiion
    // RGB starts w/ 16 bits of precision
    (6 * (mag.toFloat / 255)).toInt
  }
}