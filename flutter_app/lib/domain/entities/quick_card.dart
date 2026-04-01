import 'package:freezed_annotation/freezed_annotation.dart';

part 'quick_card.freezed.dart';
part 'quick_card.g.dart';

/// Type of quick card content.
enum QuickCardType {
  image,
  qr,
  text,
}

/// A quick card that can display image, QR code, or text.
@freezed
abstract class QuickCard with _$QuickCard {
  const factory QuickCard({
    required String id,
    required QuickCardType type,
    @Default('') String title,
    @Default('') String content,
    String? imagePath,
    @Default(0) int order,
  }) = _QuickCard;

  factory QuickCard.fromJson(Map<String, dynamic> json) =>
      _$QuickCardFromJson(json);
}
