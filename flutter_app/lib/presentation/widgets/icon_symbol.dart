import 'package:flutter/material.dart';

/// Widget for displaying Material Symbols Sharp icons.
/// Uses the custom MaterialSymbolsSharp font bundled in assets.
class IconSymbol extends StatelessWidget {
  const IconSymbol(
    this.codePoint, {
    super.key,
    this.size = 24.0,
    this.color,
  });

  /// The Unicode code point of the Material Symbol.
  final int codePoint;

  /// Icon size. Defaults to 24.0.
  final double size;

  /// Icon color. Defaults to theme's onSurface.
  final Color? color;

  // Common icon code points (Material Symbols Sharp)
  static const int mic = 0xe029;
  static const int micOff = 0xe02b;
  static const int stop = 0xe047;
  static const int playArrow = 0xe037;
  static const int settings = 0xe8b8;
  static const int subtitles = 0xe048;
  static const int contactPage = 0xef45;
  static const int brush = 0xe3ae;
  static const int openInNew = 0xe89e;
  static const int recordVoiceOver = 0xe91c;
  static const int graphicEq = 0xe1b8;
  static const int menu = 0xe5d2;
  static const int menuOpen = 0xe9bd;
  static const int add = 0xe145;
  static const int close = 0xe5cd;
  static const int delete = 0xe872;
  static const int edit = 0xe3c9;
  static const int save = 0xe161;
  static const int share = 0xe80d;
  static const int fileDownload = 0xe2c4;
  static const int fileUpload = 0xe2c6;
  static const int pushPin = 0xf10d;
  static const int moreVert = 0xe5d4;
  static const int chevronLeft = 0xe5cb;
  static const int chevronRight = 0xe5cc;
  static const int expandMore = 0xe5cf;
  static const int expandLess = 0xe5ce;
  static const int info = 0xe88e;
  static const int warning = 0xe002;
  static const int error = 0xe000;
  static const int check = 0xe5ca;
  static const int qrCodeScanner = 0xf206;
  static const int photo = 0xe410;
  static const int palette = 0xe40a;
  static const int undo = 0xe166;
  static const int redo = 0xe15a;
  static const int article = 0xef42;
  static const int volumeUp = 0xe050;
  static const int volumeOff = 0xe04f;
  static const int speed = 0xe9e4;

  @override
  Widget build(BuildContext context) {
    return Text(
      String.fromCharCode(codePoint),
      style: TextStyle(
        fontFamily: 'MaterialSymbolsSharp',
        fontSize: size,
        color: color ?? Theme.of(context).colorScheme.onSurface,
        decoration: TextDecoration.none,
      ),
    );
  }
}
