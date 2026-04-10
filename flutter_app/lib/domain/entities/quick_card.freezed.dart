// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'quick_card.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
  'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models',
);

QuickCard _$QuickCardFromJson(Map<String, dynamic> json) {
  return _QuickCard.fromJson(json);
}

/// @nodoc
mixin _$QuickCard {
  String get id => throw _privateConstructorUsedError;
  QuickCardType get type => throw _privateConstructorUsedError;
  String get title => throw _privateConstructorUsedError;
  String get content => throw _privateConstructorUsedError;
  String? get imagePath => throw _privateConstructorUsedError;
  int get order => throw _privateConstructorUsedError;

  /// Serializes this QuickCard to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of QuickCard
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $QuickCardCopyWith<QuickCard> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $QuickCardCopyWith<$Res> {
  factory $QuickCardCopyWith(QuickCard value, $Res Function(QuickCard) then) =
      _$QuickCardCopyWithImpl<$Res, QuickCard>;
  @useResult
  $Res call({
    String id,
    QuickCardType type,
    String title,
    String content,
    String? imagePath,
    int order,
  });
}

/// @nodoc
class _$QuickCardCopyWithImpl<$Res, $Val extends QuickCard>
    implements $QuickCardCopyWith<$Res> {
  _$QuickCardCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of QuickCard
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? type = null,
    Object? title = null,
    Object? content = null,
    Object? imagePath = freezed,
    Object? order = null,
  }) {
    return _then(
      _value.copyWith(
            id: null == id
                ? _value.id
                : id // ignore: cast_nullable_to_non_nullable
                      as String,
            type: null == type
                ? _value.type
                : type // ignore: cast_nullable_to_non_nullable
                      as QuickCardType,
            title: null == title
                ? _value.title
                : title // ignore: cast_nullable_to_non_nullable
                      as String,
            content: null == content
                ? _value.content
                : content // ignore: cast_nullable_to_non_nullable
                      as String,
            imagePath: freezed == imagePath
                ? _value.imagePath
                : imagePath // ignore: cast_nullable_to_non_nullable
                      as String?,
            order: null == order
                ? _value.order
                : order // ignore: cast_nullable_to_non_nullable
                      as int,
          )
          as $Val,
    );
  }
}

/// @nodoc
abstract class _$$QuickCardImplCopyWith<$Res>
    implements $QuickCardCopyWith<$Res> {
  factory _$$QuickCardImplCopyWith(
    _$QuickCardImpl value,
    $Res Function(_$QuickCardImpl) then,
  ) = __$$QuickCardImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({
    String id,
    QuickCardType type,
    String title,
    String content,
    String? imagePath,
    int order,
  });
}

/// @nodoc
class __$$QuickCardImplCopyWithImpl<$Res>
    extends _$QuickCardCopyWithImpl<$Res, _$QuickCardImpl>
    implements _$$QuickCardImplCopyWith<$Res> {
  __$$QuickCardImplCopyWithImpl(
    _$QuickCardImpl _value,
    $Res Function(_$QuickCardImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of QuickCard
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? type = null,
    Object? title = null,
    Object? content = null,
    Object? imagePath = freezed,
    Object? order = null,
  }) {
    return _then(
      _$QuickCardImpl(
        id: null == id
            ? _value.id
            : id // ignore: cast_nullable_to_non_nullable
                  as String,
        type: null == type
            ? _value.type
            : type // ignore: cast_nullable_to_non_nullable
                  as QuickCardType,
        title: null == title
            ? _value.title
            : title // ignore: cast_nullable_to_non_nullable
                  as String,
        content: null == content
            ? _value.content
            : content // ignore: cast_nullable_to_non_nullable
                  as String,
        imagePath: freezed == imagePath
            ? _value.imagePath
            : imagePath // ignore: cast_nullable_to_non_nullable
                  as String?,
        order: null == order
            ? _value.order
            : order // ignore: cast_nullable_to_non_nullable
                  as int,
      ),
    );
  }
}

/// @nodoc
@JsonSerializable()
class _$QuickCardImpl implements _QuickCard {
  const _$QuickCardImpl({
    required this.id,
    required this.type,
    this.title = '',
    this.content = '',
    this.imagePath,
    this.order = 0,
  });

  factory _$QuickCardImpl.fromJson(Map<String, dynamic> json) =>
      _$$QuickCardImplFromJson(json);

  @override
  final String id;
  @override
  final QuickCardType type;
  @override
  @JsonKey()
  final String title;
  @override
  @JsonKey()
  final String content;
  @override
  final String? imagePath;
  @override
  @JsonKey()
  final int order;

  @override
  String toString() {
    return 'QuickCard(id: $id, type: $type, title: $title, content: $content, imagePath: $imagePath, order: $order)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$QuickCardImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.type, type) || other.type == type) &&
            (identical(other.title, title) || other.title == title) &&
            (identical(other.content, content) || other.content == content) &&
            (identical(other.imagePath, imagePath) ||
                other.imagePath == imagePath) &&
            (identical(other.order, order) || other.order == order));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode =>
      Object.hash(runtimeType, id, type, title, content, imagePath, order);

  /// Create a copy of QuickCard
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$QuickCardImplCopyWith<_$QuickCardImpl> get copyWith =>
      __$$QuickCardImplCopyWithImpl<_$QuickCardImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$QuickCardImplToJson(this);
  }
}

abstract class _QuickCard implements QuickCard {
  const factory _QuickCard({
    required final String id,
    required final QuickCardType type,
    final String title,
    final String content,
    final String? imagePath,
    final int order,
  }) = _$QuickCardImpl;

  factory _QuickCard.fromJson(Map<String, dynamic> json) =
      _$QuickCardImpl.fromJson;

  @override
  String get id;
  @override
  QuickCardType get type;
  @override
  String get title;
  @override
  String get content;
  @override
  String? get imagePath;
  @override
  int get order;

  /// Create a copy of QuickCard
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$QuickCardImplCopyWith<_$QuickCardImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
