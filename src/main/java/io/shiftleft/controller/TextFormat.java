package io.shiftleft.controller;

/** Small text helpers for page titles. */
public final class TextFormat {

  private TextFormat() {
  }

  public static String titleCase(String text) {
    if (text == null || text.isEmpty()) {
      return text;
    }
    return Character.toUpperCase(text.charAt(0)) + text.substring(1).toLowerCase();
  }
}
