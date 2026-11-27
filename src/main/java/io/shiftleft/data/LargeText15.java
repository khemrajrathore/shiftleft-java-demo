package io.shiftleft.data;

/** Small text helpers for report headers. */
public final class LargeText15 {

  private LargeText15() {
  }

  public static String title(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }
    return Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }

  public static String padRight(String text, int width) {
    StringBuilder out = new StringBuilder(text == null ? "" : text);
    while (out.length() < width) {
      out.append(' ');
    }
    return out.toString();
  }
}
