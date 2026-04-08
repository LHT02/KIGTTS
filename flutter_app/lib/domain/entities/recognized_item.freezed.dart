// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'recognized_item.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
  'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models',
);

/// @nodoc
mixin _$RecognizedItem {
  int get id => throw _privateConstructorUsedError;
  String get text => throw _privateConstructorUsedError;
  double get progress => throw _privateConstructorUsedError;
  bool get playing => throw _privateConstructorUsedError;
  bool get completed => throw _privateConstructorUsedError;

  /// Create a copy of RecognizedItem
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $RecognizedItemCopyWith<RecognizedItem> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $RecognizedItemCopyWith<$Res> {
  factory $RecognizedItemCopyWith(
    RecognizedItem value,
    $Res Function(RecognizedItem) then,
  ) = _$RecognizedItemCopyWithImpl<$Res, RecognizedItem>;
  @useResult
  $Res call({
    int id,
    String text,
    double progress,
    bool playing,
    bool completed,
  });
}

/// @nodoc
class _$RecognizedItemCopyWithImpl<$Res, $Val extends RecognizedItem>
    implements $RecognizedItemCopyWith<$Res> {
  _$RecognizedItemCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of RecognizedItem
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? text = null,
    Object? progress = null,
    Object? playing = null,
    Object? completed = null,
  }) {
    return _then(
      _value.copyWith(
            id: null == id
                ? _value.id
                : id // ignore: cast_nullable_to_non_nullable
                      as int,
            text: null == text
                ? _value.text
                : text // ignore: cast_nullable_to_non_nullable
                      as String,
            progress: null == progress
                ? _value.progress
                : progress // ignore: cast_nullable_to_non_nullable
                      as double,
            playing: null == playing
                ? _value.playing
                : playing // ignore: cast_nullable_to_non_nullable
                      as bool,
            completed: null == completed
                ? _value.completed
                : completed // ignore: cast_nullable_to_non_nullable
                      as bool,
          )
          as $Val,
    );
  }
}

/// @nodoc
abstract class _$$RecognizedItemImplCopyWith<$Res>
    implements $RecognizedItemCopyWith<$Res> {
  factory _$$RecognizedItemImplCopyWith(
    _$RecognizedItemImpl value,
    $Res Function(_$RecognizedItemImpl) then,
  ) = __$$RecognizedItemImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({
    int id,
    String text,
    double progress,
    bool playing,
    bool completed,
  });
}

/// @nodoc
class __$$RecognizedItemImplCopyWithImpl<$Res>
    extends _$RecognizedItemCopyWithImpl<$Res, _$RecognizedItemImpl>
    implements _$$RecognizedItemImplCopyWith<$Res> {
  __$$RecognizedItemImplCopyWithImpl(
    _$RecognizedItemImpl _value,
    $Res Function(_$RecognizedItemImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of RecognizedItem
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? text = null,
    Object? progress = null,
    Object? playing = null,
    Object? completed = null,
  }) {
    return _then(
      _$RecognizedItemImpl(
        id: null == id
            ? _value.id
            : id // ignore: cast_nullable_to_non_nullable
                  as int,
        text: null == text
            ? _value.text
            : text // ignore: cast_nullable_to_non_nullable
                  as String,
        progress: null == progress
            ? _value.progress
            : progress // ignore: cast_nullable_to_non_nullable
                  as double,
        playing: null == playing
            ? _value.playing
            : playing // ignore: cast_nullable_to_non_nullable
                  as bool,
        completed: null == completed
            ? _value.completed
            : completed // ignore: cast_nullable_to_non_nullable
                  as bool,
      ),
    );
  }
}

/// @nodoc

class _$RecognizedItemImpl implements _RecognizedItem {
  const _$RecognizedItemImpl({
    required this.id,
    required this.text,
    this.progress = 0.0,
    this.playing = false,
    this.completed = false,
  });

  @override
  final int id;
  @override
  final String text;
  @override
  @JsonKey()
  final double progress;
  @override
  @JsonKey()
  final bool playing;
  @override
  @JsonKey()
  final bool completed;

  @override
  String toString() {
    return 'RecognizedItem(id: $id, text: $text, progress: $progress, playing: $playing, completed: $completed)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$RecognizedItemImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.text, text) || other.text == text) &&
            (identical(other.progress, progress) ||
                other.progress == progress) &&
            (identical(other.playing, playing) || other.playing == playing) &&
            (identical(other.completed, completed) ||
                other.completed == completed));
  }

  @override
  int get hashCode =>
      Object.hash(runtimeType, id, text, progress, playing, completed);

  /// Create a copy of RecognizedItem
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$RecognizedItemImplCopyWith<_$RecognizedItemImpl> get copyWith =>
      __$$RecognizedItemImplCopyWithImpl<_$RecognizedItemImpl>(
        this,
        _$identity,
      );
}

abstract class _RecognizedItem implements RecognizedItem {
  const factory _RecognizedItem({
    required final int id,
    required final String text,
    final double progress,
    final bool playing,
    final bool completed,
  }) = _$RecognizedItemImpl;

  @override
  int get id;
  @override
  String get text;
  @override
  double get progress;
  @override
  bool get playing;
  @override
  bool get completed;

  /// Create a copy of RecognizedItem
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$RecognizedItemImplCopyWith<_$RecognizedItemImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
