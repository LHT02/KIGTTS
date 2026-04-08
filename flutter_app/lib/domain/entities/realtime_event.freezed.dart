// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'realtime_event.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
  'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models',
);

/// @nodoc
mixin _$RealtimeEvent {
  @optionalTypeArgs
  TResult when<TResult extends Object?>({
    required TResult Function(int id, String text) result,
    required TResult Function(String text) streaming,
    required TResult Function(int id, double value) progress,
    required TResult Function(double value) level,
    required TResult Function(String label) inputDevice,
    required TResult Function(String label) outputDevice,
    required TResult Function(String status) aec3Status,
    required TResult Function(double similarity, bool accepted) speakerVerify,
    required TResult Function(String message) error,
  }) => throw _privateConstructorUsedError;
  @optionalTypeArgs
  TResult? whenOrNull<TResult extends Object?>({
    TResult? Function(int id, String text)? result,
    TResult? Function(String text)? streaming,
    TResult? Function(int id, double value)? progress,
    TResult? Function(double value)? level,
    TResult? Function(String label)? inputDevice,
    TResult? Function(String label)? outputDevice,
    TResult? Function(String status)? aec3Status,
    TResult? Function(double similarity, bool accepted)? speakerVerify,
    TResult? Function(String message)? error,
  }) => throw _privateConstructorUsedError;
  @optionalTypeArgs
  TResult maybeWhen<TResult extends Object?>({
    TResult Function(int id, String text)? result,
    TResult Function(String text)? streaming,
    TResult Function(int id, double value)? progress,
    TResult Function(double value)? level,
    TResult Function(String label)? inputDevice,
    TResult Function(String label)? outputDevice,
    TResult Function(String status)? aec3Status,
    TResult Function(double similarity, bool accepted)? speakerVerify,
    TResult Function(String message)? error,
    required TResult orElse(),
  }) => throw _privateConstructorUsedError;
  @optionalTypeArgs
  TResult map<TResult extends Object?>({
    required TResult Function(RealtimeResult value) result,
    required TResult Function(RealtimeStreaming value) streaming,
    required TResult Function(RealtimeProgress value) progress,
    required TResult Function(RealtimeLevel value) level,
    required TResult Function(RealtimeInputDevice value) inputDevice,
    required TResult Function(RealtimeOutputDevice value) outputDevice,
    required TResult Function(RealtimeAec3Status value) aec3Status,
    required TResult Function(RealtimeSpeakerVerify value) speakerVerify,
    required TResult Function(RealtimeError value) error,
  }) => throw _privateConstructorUsedError;
  @optionalTypeArgs
  TResult? mapOrNull<TResult extends Object?>({
    TResult? Function(RealtimeResult value)? result,
    TResult? Function(RealtimeStreaming value)? streaming,
    TResult? Function(RealtimeProgress value)? progress,
    TResult? Function(RealtimeLevel value)? level,
    TResult? Function(RealtimeInputDevice value)? inputDevice,
    TResult? Function(RealtimeOutputDevice value)? outputDevice,
    TResult? Function(RealtimeAec3Status value)? aec3Status,
    TResult? Function(RealtimeSpeakerVerify value)? speakerVerify,
    TResult? Function(RealtimeError value)? error,
  }) => throw _privateConstructorUsedError;
  @optionalTypeArgs
  TResult maybeMap<TResult extends Object?>({
    TResult Function(RealtimeResult value)? result,
    TResult Function(RealtimeStreaming value)? streaming,
    TResult Function(RealtimeProgress value)? progress,
    TResult Function(RealtimeLevel value)? level,
    TResult Function(RealtimeInputDevice value)? inputDevice,
    TResult Function(RealtimeOutputDevice value)? outputDevice,
    TResult Function(RealtimeAec3Status value)? aec3Status,
    TResult Function(RealtimeSpeakerVerify value)? speakerVerify,
    TResult Function(RealtimeError value)? error,
    required TResult orElse(),
  }) => throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $RealtimeEventCopyWith<$Res> {
  factory $RealtimeEventCopyWith(
    RealtimeEvent value,
    $Res Function(RealtimeEvent) then,
  ) = _$RealtimeEventCopyWithImpl<$Res, RealtimeEvent>;
}

/// @nodoc
class _$RealtimeEventCopyWithImpl<$Res, $Val extends RealtimeEvent>
    implements $RealtimeEventCopyWith<$Res> {
  _$RealtimeEventCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
}

/// @nodoc
abstract class _$$RealtimeResultImplCopyWith<$Res> {
  factory _$$RealtimeResultImplCopyWith(
    _$RealtimeResultImpl value,
    $Res Function(_$RealtimeResultImpl) then,
  ) = __$$RealtimeResultImplCopyWithImpl<$Res>;
  @useResult
  $Res call({int id, String text});
}

/// @nodoc
class __$$RealtimeResultImplCopyWithImpl<$Res>
    extends _$RealtimeEventCopyWithImpl<$Res, _$RealtimeResultImpl>
    implements _$$RealtimeResultImplCopyWith<$Res> {
  __$$RealtimeResultImplCopyWithImpl(
    _$RealtimeResultImpl _value,
    $Res Function(_$RealtimeResultImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({Object? id = null, Object? text = null}) {
    return _then(
      _$RealtimeResultImpl(
        id: null == id
            ? _value.id
            : id // ignore: cast_nullable_to_non_nullable
                  as int,
        text: null == text
            ? _value.text
            : text // ignore: cast_nullable_to_non_nullable
                  as String,
      ),
    );
  }
}

/// @nodoc

class _$RealtimeResultImpl implements RealtimeResult {
  const _$RealtimeResultImpl({required this.id, required this.text});

  @override
  final int id;
  @override
  final String text;

  @override
  String toString() {
    return 'RealtimeEvent.result(id: $id, text: $text)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$RealtimeResultImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.text, text) || other.text == text));
  }

  @override
  int get hashCode => Object.hash(runtimeType, id, text);

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$RealtimeResultImplCopyWith<_$RealtimeResultImpl> get copyWith =>
      __$$RealtimeResultImplCopyWithImpl<_$RealtimeResultImpl>(
        this,
        _$identity,
      );

  @override
  @optionalTypeArgs
  TResult when<TResult extends Object?>({
    required TResult Function(int id, String text) result,
    required TResult Function(String text) streaming,
    required TResult Function(int id, double value) progress,
    required TResult Function(double value) level,
    required TResult Function(String label) inputDevice,
    required TResult Function(String label) outputDevice,
    required TResult Function(String status) aec3Status,
    required TResult Function(double similarity, bool accepted) speakerVerify,
    required TResult Function(String message) error,
  }) {
    return result(id, text);
  }

  @override
  @optionalTypeArgs
  TResult? whenOrNull<TResult extends Object?>({
    TResult? Function(int id, String text)? result,
    TResult? Function(String text)? streaming,
    TResult? Function(int id, double value)? progress,
    TResult? Function(double value)? level,
    TResult? Function(String label)? inputDevice,
    TResult? Function(String label)? outputDevice,
    TResult? Function(String status)? aec3Status,
    TResult? Function(double similarity, bool accepted)? speakerVerify,
    TResult? Function(String message)? error,
  }) {
    return result?.call(id, text);
  }

  @override
  @optionalTypeArgs
  TResult maybeWhen<TResult extends Object?>({
    TResult Function(int id, String text)? result,
    TResult Function(String text)? streaming,
    TResult Function(int id, double value)? progress,
    TResult Function(double value)? level,
    TResult Function(String label)? inputDevice,
    TResult Function(String label)? outputDevice,
    TResult Function(String status)? aec3Status,
    TResult Function(double similarity, bool accepted)? speakerVerify,
    TResult Function(String message)? error,
    required TResult orElse(),
  }) {
    if (result != null) {
      return result(id, text);
    }
    return orElse();
  }

  @override
  @optionalTypeArgs
  TResult map<TResult extends Object?>({
    required TResult Function(RealtimeResult value) result,
    required TResult Function(RealtimeStreaming value) streaming,
    required TResult Function(RealtimeProgress value) progress,
    required TResult Function(RealtimeLevel value) level,
    required TResult Function(RealtimeInputDevice value) inputDevice,
    required TResult Function(RealtimeOutputDevice value) outputDevice,
    required TResult Function(RealtimeAec3Status value) aec3Status,
    required TResult Function(RealtimeSpeakerVerify value) speakerVerify,
    required TResult Function(RealtimeError value) error,
  }) {
    return result(this);
  }

  @override
  @optionalTypeArgs
  TResult? mapOrNull<TResult extends Object?>({
    TResult? Function(RealtimeResult value)? result,
    TResult? Function(RealtimeStreaming value)? streaming,
    TResult? Function(RealtimeProgress value)? progress,
    TResult? Function(RealtimeLevel value)? level,
    TResult? Function(RealtimeInputDevice value)? inputDevice,
    TResult? Function(RealtimeOutputDevice value)? outputDevice,
    TResult? Function(RealtimeAec3Status value)? aec3Status,
    TResult? Function(RealtimeSpeakerVerify value)? speakerVerify,
    TResult? Function(RealtimeError value)? error,
  }) {
    return result?.call(this);
  }

  @override
  @optionalTypeArgs
  TResult maybeMap<TResult extends Object?>({
    TResult Function(RealtimeResult value)? result,
    TResult Function(RealtimeStreaming value)? streaming,
    TResult Function(RealtimeProgress value)? progress,
    TResult Function(RealtimeLevel value)? level,
    TResult Function(RealtimeInputDevice value)? inputDevice,
    TResult Function(RealtimeOutputDevice value)? outputDevice,
    TResult Function(RealtimeAec3Status value)? aec3Status,
    TResult Function(RealtimeSpeakerVerify value)? speakerVerify,
    TResult Function(RealtimeError value)? error,
    required TResult orElse(),
  }) {
    if (result != null) {
      return result(this);
    }
    return orElse();
  }
}

abstract class RealtimeResult implements RealtimeEvent {
  const factory RealtimeResult({
    required final int id,
    required final String text,
  }) = _$RealtimeResultImpl;

  int get id;
  String get text;

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$RealtimeResultImplCopyWith<_$RealtimeResultImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class _$$RealtimeStreamingImplCopyWith<$Res> {
  factory _$$RealtimeStreamingImplCopyWith(
    _$RealtimeStreamingImpl value,
    $Res Function(_$RealtimeStreamingImpl) then,
  ) = __$$RealtimeStreamingImplCopyWithImpl<$Res>;
  @useResult
  $Res call({String text});
}

/// @nodoc
class __$$RealtimeStreamingImplCopyWithImpl<$Res>
    extends _$RealtimeEventCopyWithImpl<$Res, _$RealtimeStreamingImpl>
    implements _$$RealtimeStreamingImplCopyWith<$Res> {
  __$$RealtimeStreamingImplCopyWithImpl(
    _$RealtimeStreamingImpl _value,
    $Res Function(_$RealtimeStreamingImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({Object? text = null}) {
    return _then(
      _$RealtimeStreamingImpl(
        text: null == text
            ? _value.text
            : text // ignore: cast_nullable_to_non_nullable
                  as String,
      ),
    );
  }
}

/// @nodoc

class _$RealtimeStreamingImpl implements RealtimeStreaming {
  const _$RealtimeStreamingImpl({required this.text});

  @override
  final String text;

  @override
  String toString() {
    return 'RealtimeEvent.streaming(text: $text)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$RealtimeStreamingImpl &&
            (identical(other.text, text) || other.text == text));
  }

  @override
  int get hashCode => Object.hash(runtimeType, text);

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$RealtimeStreamingImplCopyWith<_$RealtimeStreamingImpl> get copyWith =>
      __$$RealtimeStreamingImplCopyWithImpl<_$RealtimeStreamingImpl>(
        this,
        _$identity,
      );

  @override
  @optionalTypeArgs
  TResult when<TResult extends Object?>({
    required TResult Function(int id, String text) result,
    required TResult Function(String text) streaming,
    required TResult Function(int id, double value) progress,
    required TResult Function(double value) level,
    required TResult Function(String label) inputDevice,
    required TResult Function(String label) outputDevice,
    required TResult Function(String status) aec3Status,
    required TResult Function(double similarity, bool accepted) speakerVerify,
    required TResult Function(String message) error,
  }) {
    return streaming(text);
  }

  @override
  @optionalTypeArgs
  TResult? whenOrNull<TResult extends Object?>({
    TResult? Function(int id, String text)? result,
    TResult? Function(String text)? streaming,
    TResult? Function(int id, double value)? progress,
    TResult? Function(double value)? level,
    TResult? Function(String label)? inputDevice,
    TResult? Function(String label)? outputDevice,
    TResult? Function(String status)? aec3Status,
    TResult? Function(double similarity, bool accepted)? speakerVerify,
    TResult? Function(String message)? error,
  }) {
    return streaming?.call(text);
  }

  @override
  @optionalTypeArgs
  TResult maybeWhen<TResult extends Object?>({
    TResult Function(int id, String text)? result,
    TResult Function(String text)? streaming,
    TResult Function(int id, double value)? progress,
    TResult Function(double value)? level,
    TResult Function(String label)? inputDevice,
    TResult Function(String label)? outputDevice,
    TResult Function(String status)? aec3Status,
    TResult Function(double similarity, bool accepted)? speakerVerify,
    TResult Function(String message)? error,
    required TResult orElse(),
  }) {
    if (streaming != null) {
      return streaming(text);
    }
    return orElse();
  }

  @override
  @optionalTypeArgs
  TResult map<TResult extends Object?>({
    required TResult Function(RealtimeResult value) result,
    required TResult Function(RealtimeStreaming value) streaming,
    required TResult Function(RealtimeProgress value) progress,
    required TResult Function(RealtimeLevel value) level,
    required TResult Function(RealtimeInputDevice value) inputDevice,
    required TResult Function(RealtimeOutputDevice value) outputDevice,
    required TResult Function(RealtimeAec3Status value) aec3Status,
    required TResult Function(RealtimeSpeakerVerify value) speakerVerify,
    required TResult Function(RealtimeError value) error,
  }) {
    return streaming(this);
  }

  @override
  @optionalTypeArgs
  TResult? mapOrNull<TResult extends Object?>({
    TResult? Function(RealtimeResult value)? result,
    TResult? Function(RealtimeStreaming value)? streaming,
    TResult? Function(RealtimeProgress value)? progress,
    TResult? Function(RealtimeLevel value)? level,
    TResult? Function(RealtimeInputDevice value)? inputDevice,
    TResult? Function(RealtimeOutputDevice value)? outputDevice,
    TResult? Function(RealtimeAec3Status value)? aec3Status,
    TResult? Function(RealtimeSpeakerVerify value)? speakerVerify,
    TResult? Function(RealtimeError value)? error,
  }) {
    return streaming?.call(this);
  }

  @override
  @optionalTypeArgs
  TResult maybeMap<TResult extends Object?>({
    TResult Function(RealtimeResult value)? result,
    TResult Function(RealtimeStreaming value)? streaming,
    TResult Function(RealtimeProgress value)? progress,
    TResult Function(RealtimeLevel value)? level,
    TResult Function(RealtimeInputDevice value)? inputDevice,
    TResult Function(RealtimeOutputDevice value)? outputDevice,
    TResult Function(RealtimeAec3Status value)? aec3Status,
    TResult Function(RealtimeSpeakerVerify value)? speakerVerify,
    TResult Function(RealtimeError value)? error,
    required TResult orElse(),
  }) {
    if (streaming != null) {
      return streaming(this);
    }
    return orElse();
  }
}

abstract class RealtimeStreaming implements RealtimeEvent {
  const factory RealtimeStreaming({required final String text}) =
      _$RealtimeStreamingImpl;

  String get text;

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$RealtimeStreamingImplCopyWith<_$RealtimeStreamingImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class _$$RealtimeProgressImplCopyWith<$Res> {
  factory _$$RealtimeProgressImplCopyWith(
    _$RealtimeProgressImpl value,
    $Res Function(_$RealtimeProgressImpl) then,
  ) = __$$RealtimeProgressImplCopyWithImpl<$Res>;
  @useResult
  $Res call({int id, double value});
}

/// @nodoc
class __$$RealtimeProgressImplCopyWithImpl<$Res>
    extends _$RealtimeEventCopyWithImpl<$Res, _$RealtimeProgressImpl>
    implements _$$RealtimeProgressImplCopyWith<$Res> {
  __$$RealtimeProgressImplCopyWithImpl(
    _$RealtimeProgressImpl _value,
    $Res Function(_$RealtimeProgressImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({Object? id = null, Object? value = null}) {
    return _then(
      _$RealtimeProgressImpl(
        id: null == id
            ? _value.id
            : id // ignore: cast_nullable_to_non_nullable
                  as int,
        value: null == value
            ? _value.value
            : value // ignore: cast_nullable_to_non_nullable
                  as double,
      ),
    );
  }
}

/// @nodoc

class _$RealtimeProgressImpl implements RealtimeProgress {
  const _$RealtimeProgressImpl({required this.id, required this.value});

  @override
  final int id;
  @override
  final double value;

  @override
  String toString() {
    return 'RealtimeEvent.progress(id: $id, value: $value)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$RealtimeProgressImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.value, value) || other.value == value));
  }

  @override
  int get hashCode => Object.hash(runtimeType, id, value);

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$RealtimeProgressImplCopyWith<_$RealtimeProgressImpl> get copyWith =>
      __$$RealtimeProgressImplCopyWithImpl<_$RealtimeProgressImpl>(
        this,
        _$identity,
      );

  @override
  @optionalTypeArgs
  TResult when<TResult extends Object?>({
    required TResult Function(int id, String text) result,
    required TResult Function(String text) streaming,
    required TResult Function(int id, double value) progress,
    required TResult Function(double value) level,
    required TResult Function(String label) inputDevice,
    required TResult Function(String label) outputDevice,
    required TResult Function(String status) aec3Status,
    required TResult Function(double similarity, bool accepted) speakerVerify,
    required TResult Function(String message) error,
  }) {
    return progress(id, value);
  }

  @override
  @optionalTypeArgs
  TResult? whenOrNull<TResult extends Object?>({
    TResult? Function(int id, String text)? result,
    TResult? Function(String text)? streaming,
    TResult? Function(int id, double value)? progress,
    TResult? Function(double value)? level,
    TResult? Function(String label)? inputDevice,
    TResult? Function(String label)? outputDevice,
    TResult? Function(String status)? aec3Status,
    TResult? Function(double similarity, bool accepted)? speakerVerify,
    TResult? Function(String message)? error,
  }) {
    return progress?.call(id, value);
  }

  @override
  @optionalTypeArgs
  TResult maybeWhen<TResult extends Object?>({
    TResult Function(int id, String text)? result,
    TResult Function(String text)? streaming,
    TResult Function(int id, double value)? progress,
    TResult Function(double value)? level,
    TResult Function(String label)? inputDevice,
    TResult Function(String label)? outputDevice,
    TResult Function(String status)? aec3Status,
    TResult Function(double similarity, bool accepted)? speakerVerify,
    TResult Function(String message)? error,
    required TResult orElse(),
  }) {
    if (progress != null) {
      return progress(id, value);
    }
    return orElse();
  }

  @override
  @optionalTypeArgs
  TResult map<TResult extends Object?>({
    required TResult Function(RealtimeResult value) result,
    required TResult Function(RealtimeStreaming value) streaming,
    required TResult Function(RealtimeProgress value) progress,
    required TResult Function(RealtimeLevel value) level,
    required TResult Function(RealtimeInputDevice value) inputDevice,
    required TResult Function(RealtimeOutputDevice value) outputDevice,
    required TResult Function(RealtimeAec3Status value) aec3Status,
    required TResult Function(RealtimeSpeakerVerify value) speakerVerify,
    required TResult Function(RealtimeError value) error,
  }) {
    return progress(this);
  }

  @override
  @optionalTypeArgs
  TResult? mapOrNull<TResult extends Object?>({
    TResult? Function(RealtimeResult value)? result,
    TResult? Function(RealtimeStreaming value)? streaming,
    TResult? Function(RealtimeProgress value)? progress,
    TResult? Function(RealtimeLevel value)? level,
    TResult? Function(RealtimeInputDevice value)? inputDevice,
    TResult? Function(RealtimeOutputDevice value)? outputDevice,
    TResult? Function(RealtimeAec3Status value)? aec3Status,
    TResult? Function(RealtimeSpeakerVerify value)? speakerVerify,
    TResult? Function(RealtimeError value)? error,
  }) {
    return progress?.call(this);
  }

  @override
  @optionalTypeArgs
  TResult maybeMap<TResult extends Object?>({
    TResult Function(RealtimeResult value)? result,
    TResult Function(RealtimeStreaming value)? streaming,
    TResult Function(RealtimeProgress value)? progress,
    TResult Function(RealtimeLevel value)? level,
    TResult Function(RealtimeInputDevice value)? inputDevice,
    TResult Function(RealtimeOutputDevice value)? outputDevice,
    TResult Function(RealtimeAec3Status value)? aec3Status,
    TResult Function(RealtimeSpeakerVerify value)? speakerVerify,
    TResult Function(RealtimeError value)? error,
    required TResult orElse(),
  }) {
    if (progress != null) {
      return progress(this);
    }
    return orElse();
  }
}

abstract class RealtimeProgress implements RealtimeEvent {
  const factory RealtimeProgress({
    required final int id,
    required final double value,
  }) = _$RealtimeProgressImpl;

  int get id;
  double get value;

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$RealtimeProgressImplCopyWith<_$RealtimeProgressImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class _$$RealtimeLevelImplCopyWith<$Res> {
  factory _$$RealtimeLevelImplCopyWith(
    _$RealtimeLevelImpl value,
    $Res Function(_$RealtimeLevelImpl) then,
  ) = __$$RealtimeLevelImplCopyWithImpl<$Res>;
  @useResult
  $Res call({double value});
}

/// @nodoc
class __$$RealtimeLevelImplCopyWithImpl<$Res>
    extends _$RealtimeEventCopyWithImpl<$Res, _$RealtimeLevelImpl>
    implements _$$RealtimeLevelImplCopyWith<$Res> {
  __$$RealtimeLevelImplCopyWithImpl(
    _$RealtimeLevelImpl _value,
    $Res Function(_$RealtimeLevelImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({Object? value = null}) {
    return _then(
      _$RealtimeLevelImpl(
        value: null == value
            ? _value.value
            : value // ignore: cast_nullable_to_non_nullable
                  as double,
      ),
    );
  }
}

/// @nodoc

class _$RealtimeLevelImpl implements RealtimeLevel {
  const _$RealtimeLevelImpl({required this.value});

  @override
  final double value;

  @override
  String toString() {
    return 'RealtimeEvent.level(value: $value)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$RealtimeLevelImpl &&
            (identical(other.value, value) || other.value == value));
  }

  @override
  int get hashCode => Object.hash(runtimeType, value);

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$RealtimeLevelImplCopyWith<_$RealtimeLevelImpl> get copyWith =>
      __$$RealtimeLevelImplCopyWithImpl<_$RealtimeLevelImpl>(this, _$identity);

  @override
  @optionalTypeArgs
  TResult when<TResult extends Object?>({
    required TResult Function(int id, String text) result,
    required TResult Function(String text) streaming,
    required TResult Function(int id, double value) progress,
    required TResult Function(double value) level,
    required TResult Function(String label) inputDevice,
    required TResult Function(String label) outputDevice,
    required TResult Function(String status) aec3Status,
    required TResult Function(double similarity, bool accepted) speakerVerify,
    required TResult Function(String message) error,
  }) {
    return level(value);
  }

  @override
  @optionalTypeArgs
  TResult? whenOrNull<TResult extends Object?>({
    TResult? Function(int id, String text)? result,
    TResult? Function(String text)? streaming,
    TResult? Function(int id, double value)? progress,
    TResult? Function(double value)? level,
    TResult? Function(String label)? inputDevice,
    TResult? Function(String label)? outputDevice,
    TResult? Function(String status)? aec3Status,
    TResult? Function(double similarity, bool accepted)? speakerVerify,
    TResult? Function(String message)? error,
  }) {
    return level?.call(value);
  }

  @override
  @optionalTypeArgs
  TResult maybeWhen<TResult extends Object?>({
    TResult Function(int id, String text)? result,
    TResult Function(String text)? streaming,
    TResult Function(int id, double value)? progress,
    TResult Function(double value)? level,
    TResult Function(String label)? inputDevice,
    TResult Function(String label)? outputDevice,
    TResult Function(String status)? aec3Status,
    TResult Function(double similarity, bool accepted)? speakerVerify,
    TResult Function(String message)? error,
    required TResult orElse(),
  }) {
    if (level != null) {
      return level(value);
    }
    return orElse();
  }

  @override
  @optionalTypeArgs
  TResult map<TResult extends Object?>({
    required TResult Function(RealtimeResult value) result,
    required TResult Function(RealtimeStreaming value) streaming,
    required TResult Function(RealtimeProgress value) progress,
    required TResult Function(RealtimeLevel value) level,
    required TResult Function(RealtimeInputDevice value) inputDevice,
    required TResult Function(RealtimeOutputDevice value) outputDevice,
    required TResult Function(RealtimeAec3Status value) aec3Status,
    required TResult Function(RealtimeSpeakerVerify value) speakerVerify,
    required TResult Function(RealtimeError value) error,
  }) {
    return level(this);
  }

  @override
  @optionalTypeArgs
  TResult? mapOrNull<TResult extends Object?>({
    TResult? Function(RealtimeResult value)? result,
    TResult? Function(RealtimeStreaming value)? streaming,
    TResult? Function(RealtimeProgress value)? progress,
    TResult? Function(RealtimeLevel value)? level,
    TResult? Function(RealtimeInputDevice value)? inputDevice,
    TResult? Function(RealtimeOutputDevice value)? outputDevice,
    TResult? Function(RealtimeAec3Status value)? aec3Status,
    TResult? Function(RealtimeSpeakerVerify value)? speakerVerify,
    TResult? Function(RealtimeError value)? error,
  }) {
    return level?.call(this);
  }

  @override
  @optionalTypeArgs
  TResult maybeMap<TResult extends Object?>({
    TResult Function(RealtimeResult value)? result,
    TResult Function(RealtimeStreaming value)? streaming,
    TResult Function(RealtimeProgress value)? progress,
    TResult Function(RealtimeLevel value)? level,
    TResult Function(RealtimeInputDevice value)? inputDevice,
    TResult Function(RealtimeOutputDevice value)? outputDevice,
    TResult Function(RealtimeAec3Status value)? aec3Status,
    TResult Function(RealtimeSpeakerVerify value)? speakerVerify,
    TResult Function(RealtimeError value)? error,
    required TResult orElse(),
  }) {
    if (level != null) {
      return level(this);
    }
    return orElse();
  }
}

abstract class RealtimeLevel implements RealtimeEvent {
  const factory RealtimeLevel({required final double value}) =
      _$RealtimeLevelImpl;

  double get value;

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$RealtimeLevelImplCopyWith<_$RealtimeLevelImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class _$$RealtimeInputDeviceImplCopyWith<$Res> {
  factory _$$RealtimeInputDeviceImplCopyWith(
    _$RealtimeInputDeviceImpl value,
    $Res Function(_$RealtimeInputDeviceImpl) then,
  ) = __$$RealtimeInputDeviceImplCopyWithImpl<$Res>;
  @useResult
  $Res call({String label});
}

/// @nodoc
class __$$RealtimeInputDeviceImplCopyWithImpl<$Res>
    extends _$RealtimeEventCopyWithImpl<$Res, _$RealtimeInputDeviceImpl>
    implements _$$RealtimeInputDeviceImplCopyWith<$Res> {
  __$$RealtimeInputDeviceImplCopyWithImpl(
    _$RealtimeInputDeviceImpl _value,
    $Res Function(_$RealtimeInputDeviceImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({Object? label = null}) {
    return _then(
      _$RealtimeInputDeviceImpl(
        label: null == label
            ? _value.label
            : label // ignore: cast_nullable_to_non_nullable
                  as String,
      ),
    );
  }
}

/// @nodoc

class _$RealtimeInputDeviceImpl implements RealtimeInputDevice {
  const _$RealtimeInputDeviceImpl({required this.label});

  @override
  final String label;

  @override
  String toString() {
    return 'RealtimeEvent.inputDevice(label: $label)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$RealtimeInputDeviceImpl &&
            (identical(other.label, label) || other.label == label));
  }

  @override
  int get hashCode => Object.hash(runtimeType, label);

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$RealtimeInputDeviceImplCopyWith<_$RealtimeInputDeviceImpl> get copyWith =>
      __$$RealtimeInputDeviceImplCopyWithImpl<_$RealtimeInputDeviceImpl>(
        this,
        _$identity,
      );

  @override
  @optionalTypeArgs
  TResult when<TResult extends Object?>({
    required TResult Function(int id, String text) result,
    required TResult Function(String text) streaming,
    required TResult Function(int id, double value) progress,
    required TResult Function(double value) level,
    required TResult Function(String label) inputDevice,
    required TResult Function(String label) outputDevice,
    required TResult Function(String status) aec3Status,
    required TResult Function(double similarity, bool accepted) speakerVerify,
    required TResult Function(String message) error,
  }) {
    return inputDevice(label);
  }

  @override
  @optionalTypeArgs
  TResult? whenOrNull<TResult extends Object?>({
    TResult? Function(int id, String text)? result,
    TResult? Function(String text)? streaming,
    TResult? Function(int id, double value)? progress,
    TResult? Function(double value)? level,
    TResult? Function(String label)? inputDevice,
    TResult? Function(String label)? outputDevice,
    TResult? Function(String status)? aec3Status,
    TResult? Function(double similarity, bool accepted)? speakerVerify,
    TResult? Function(String message)? error,
  }) {
    return inputDevice?.call(label);
  }

  @override
  @optionalTypeArgs
  TResult maybeWhen<TResult extends Object?>({
    TResult Function(int id, String text)? result,
    TResult Function(String text)? streaming,
    TResult Function(int id, double value)? progress,
    TResult Function(double value)? level,
    TResult Function(String label)? inputDevice,
    TResult Function(String label)? outputDevice,
    TResult Function(String status)? aec3Status,
    TResult Function(double similarity, bool accepted)? speakerVerify,
    TResult Function(String message)? error,
    required TResult orElse(),
  }) {
    if (inputDevice != null) {
      return inputDevice(label);
    }
    return orElse();
  }

  @override
  @optionalTypeArgs
  TResult map<TResult extends Object?>({
    required TResult Function(RealtimeResult value) result,
    required TResult Function(RealtimeStreaming value) streaming,
    required TResult Function(RealtimeProgress value) progress,
    required TResult Function(RealtimeLevel value) level,
    required TResult Function(RealtimeInputDevice value) inputDevice,
    required TResult Function(RealtimeOutputDevice value) outputDevice,
    required TResult Function(RealtimeAec3Status value) aec3Status,
    required TResult Function(RealtimeSpeakerVerify value) speakerVerify,
    required TResult Function(RealtimeError value) error,
  }) {
    return inputDevice(this);
  }

  @override
  @optionalTypeArgs
  TResult? mapOrNull<TResult extends Object?>({
    TResult? Function(RealtimeResult value)? result,
    TResult? Function(RealtimeStreaming value)? streaming,
    TResult? Function(RealtimeProgress value)? progress,
    TResult? Function(RealtimeLevel value)? level,
    TResult? Function(RealtimeInputDevice value)? inputDevice,
    TResult? Function(RealtimeOutputDevice value)? outputDevice,
    TResult? Function(RealtimeAec3Status value)? aec3Status,
    TResult? Function(RealtimeSpeakerVerify value)? speakerVerify,
    TResult? Function(RealtimeError value)? error,
  }) {
    return inputDevice?.call(this);
  }

  @override
  @optionalTypeArgs
  TResult maybeMap<TResult extends Object?>({
    TResult Function(RealtimeResult value)? result,
    TResult Function(RealtimeStreaming value)? streaming,
    TResult Function(RealtimeProgress value)? progress,
    TResult Function(RealtimeLevel value)? level,
    TResult Function(RealtimeInputDevice value)? inputDevice,
    TResult Function(RealtimeOutputDevice value)? outputDevice,
    TResult Function(RealtimeAec3Status value)? aec3Status,
    TResult Function(RealtimeSpeakerVerify value)? speakerVerify,
    TResult Function(RealtimeError value)? error,
    required TResult orElse(),
  }) {
    if (inputDevice != null) {
      return inputDevice(this);
    }
    return orElse();
  }
}

abstract class RealtimeInputDevice implements RealtimeEvent {
  const factory RealtimeInputDevice({required final String label}) =
      _$RealtimeInputDeviceImpl;

  String get label;

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$RealtimeInputDeviceImplCopyWith<_$RealtimeInputDeviceImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class _$$RealtimeOutputDeviceImplCopyWith<$Res> {
  factory _$$RealtimeOutputDeviceImplCopyWith(
    _$RealtimeOutputDeviceImpl value,
    $Res Function(_$RealtimeOutputDeviceImpl) then,
  ) = __$$RealtimeOutputDeviceImplCopyWithImpl<$Res>;
  @useResult
  $Res call({String label});
}

/// @nodoc
class __$$RealtimeOutputDeviceImplCopyWithImpl<$Res>
    extends _$RealtimeEventCopyWithImpl<$Res, _$RealtimeOutputDeviceImpl>
    implements _$$RealtimeOutputDeviceImplCopyWith<$Res> {
  __$$RealtimeOutputDeviceImplCopyWithImpl(
    _$RealtimeOutputDeviceImpl _value,
    $Res Function(_$RealtimeOutputDeviceImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({Object? label = null}) {
    return _then(
      _$RealtimeOutputDeviceImpl(
        label: null == label
            ? _value.label
            : label // ignore: cast_nullable_to_non_nullable
                  as String,
      ),
    );
  }
}

/// @nodoc

class _$RealtimeOutputDeviceImpl implements RealtimeOutputDevice {
  const _$RealtimeOutputDeviceImpl({required this.label});

  @override
  final String label;

  @override
  String toString() {
    return 'RealtimeEvent.outputDevice(label: $label)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$RealtimeOutputDeviceImpl &&
            (identical(other.label, label) || other.label == label));
  }

  @override
  int get hashCode => Object.hash(runtimeType, label);

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$RealtimeOutputDeviceImplCopyWith<_$RealtimeOutputDeviceImpl>
  get copyWith =>
      __$$RealtimeOutputDeviceImplCopyWithImpl<_$RealtimeOutputDeviceImpl>(
        this,
        _$identity,
      );

  @override
  @optionalTypeArgs
  TResult when<TResult extends Object?>({
    required TResult Function(int id, String text) result,
    required TResult Function(String text) streaming,
    required TResult Function(int id, double value) progress,
    required TResult Function(double value) level,
    required TResult Function(String label) inputDevice,
    required TResult Function(String label) outputDevice,
    required TResult Function(String status) aec3Status,
    required TResult Function(double similarity, bool accepted) speakerVerify,
    required TResult Function(String message) error,
  }) {
    return outputDevice(label);
  }

  @override
  @optionalTypeArgs
  TResult? whenOrNull<TResult extends Object?>({
    TResult? Function(int id, String text)? result,
    TResult? Function(String text)? streaming,
    TResult? Function(int id, double value)? progress,
    TResult? Function(double value)? level,
    TResult? Function(String label)? inputDevice,
    TResult? Function(String label)? outputDevice,
    TResult? Function(String status)? aec3Status,
    TResult? Function(double similarity, bool accepted)? speakerVerify,
    TResult? Function(String message)? error,
  }) {
    return outputDevice?.call(label);
  }

  @override
  @optionalTypeArgs
  TResult maybeWhen<TResult extends Object?>({
    TResult Function(int id, String text)? result,
    TResult Function(String text)? streaming,
    TResult Function(int id, double value)? progress,
    TResult Function(double value)? level,
    TResult Function(String label)? inputDevice,
    TResult Function(String label)? outputDevice,
    TResult Function(String status)? aec3Status,
    TResult Function(double similarity, bool accepted)? speakerVerify,
    TResult Function(String message)? error,
    required TResult orElse(),
  }) {
    if (outputDevice != null) {
      return outputDevice(label);
    }
    return orElse();
  }

  @override
  @optionalTypeArgs
  TResult map<TResult extends Object?>({
    required TResult Function(RealtimeResult value) result,
    required TResult Function(RealtimeStreaming value) streaming,
    required TResult Function(RealtimeProgress value) progress,
    required TResult Function(RealtimeLevel value) level,
    required TResult Function(RealtimeInputDevice value) inputDevice,
    required TResult Function(RealtimeOutputDevice value) outputDevice,
    required TResult Function(RealtimeAec3Status value) aec3Status,
    required TResult Function(RealtimeSpeakerVerify value) speakerVerify,
    required TResult Function(RealtimeError value) error,
  }) {
    return outputDevice(this);
  }

  @override
  @optionalTypeArgs
  TResult? mapOrNull<TResult extends Object?>({
    TResult? Function(RealtimeResult value)? result,
    TResult? Function(RealtimeStreaming value)? streaming,
    TResult? Function(RealtimeProgress value)? progress,
    TResult? Function(RealtimeLevel value)? level,
    TResult? Function(RealtimeInputDevice value)? inputDevice,
    TResult? Function(RealtimeOutputDevice value)? outputDevice,
    TResult? Function(RealtimeAec3Status value)? aec3Status,
    TResult? Function(RealtimeSpeakerVerify value)? speakerVerify,
    TResult? Function(RealtimeError value)? error,
  }) {
    return outputDevice?.call(this);
  }

  @override
  @optionalTypeArgs
  TResult maybeMap<TResult extends Object?>({
    TResult Function(RealtimeResult value)? result,
    TResult Function(RealtimeStreaming value)? streaming,
    TResult Function(RealtimeProgress value)? progress,
    TResult Function(RealtimeLevel value)? level,
    TResult Function(RealtimeInputDevice value)? inputDevice,
    TResult Function(RealtimeOutputDevice value)? outputDevice,
    TResult Function(RealtimeAec3Status value)? aec3Status,
    TResult Function(RealtimeSpeakerVerify value)? speakerVerify,
    TResult Function(RealtimeError value)? error,
    required TResult orElse(),
  }) {
    if (outputDevice != null) {
      return outputDevice(this);
    }
    return orElse();
  }
}

abstract class RealtimeOutputDevice implements RealtimeEvent {
  const factory RealtimeOutputDevice({required final String label}) =
      _$RealtimeOutputDeviceImpl;

  String get label;

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$RealtimeOutputDeviceImplCopyWith<_$RealtimeOutputDeviceImpl>
  get copyWith => throw _privateConstructorUsedError;
}

/// @nodoc
abstract class _$$RealtimeAec3StatusImplCopyWith<$Res> {
  factory _$$RealtimeAec3StatusImplCopyWith(
    _$RealtimeAec3StatusImpl value,
    $Res Function(_$RealtimeAec3StatusImpl) then,
  ) = __$$RealtimeAec3StatusImplCopyWithImpl<$Res>;
  @useResult
  $Res call({String status});
}

/// @nodoc
class __$$RealtimeAec3StatusImplCopyWithImpl<$Res>
    extends _$RealtimeEventCopyWithImpl<$Res, _$RealtimeAec3StatusImpl>
    implements _$$RealtimeAec3StatusImplCopyWith<$Res> {
  __$$RealtimeAec3StatusImplCopyWithImpl(
    _$RealtimeAec3StatusImpl _value,
    $Res Function(_$RealtimeAec3StatusImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({Object? status = null}) {
    return _then(
      _$RealtimeAec3StatusImpl(
        status: null == status
            ? _value.status
            : status // ignore: cast_nullable_to_non_nullable
                  as String,
      ),
    );
  }
}

/// @nodoc

class _$RealtimeAec3StatusImpl implements RealtimeAec3Status {
  const _$RealtimeAec3StatusImpl({required this.status});

  @override
  final String status;

  @override
  String toString() {
    return 'RealtimeEvent.aec3Status(status: $status)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$RealtimeAec3StatusImpl &&
            (identical(other.status, status) || other.status == status));
  }

  @override
  int get hashCode => Object.hash(runtimeType, status);

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$RealtimeAec3StatusImplCopyWith<_$RealtimeAec3StatusImpl> get copyWith =>
      __$$RealtimeAec3StatusImplCopyWithImpl<_$RealtimeAec3StatusImpl>(
        this,
        _$identity,
      );

  @override
  @optionalTypeArgs
  TResult when<TResult extends Object?>({
    required TResult Function(int id, String text) result,
    required TResult Function(String text) streaming,
    required TResult Function(int id, double value) progress,
    required TResult Function(double value) level,
    required TResult Function(String label) inputDevice,
    required TResult Function(String label) outputDevice,
    required TResult Function(String status) aec3Status,
    required TResult Function(double similarity, bool accepted) speakerVerify,
    required TResult Function(String message) error,
  }) {
    return aec3Status(status);
  }

  @override
  @optionalTypeArgs
  TResult? whenOrNull<TResult extends Object?>({
    TResult? Function(int id, String text)? result,
    TResult? Function(String text)? streaming,
    TResult? Function(int id, double value)? progress,
    TResult? Function(double value)? level,
    TResult? Function(String label)? inputDevice,
    TResult? Function(String label)? outputDevice,
    TResult? Function(String status)? aec3Status,
    TResult? Function(double similarity, bool accepted)? speakerVerify,
    TResult? Function(String message)? error,
  }) {
    return aec3Status?.call(status);
  }

  @override
  @optionalTypeArgs
  TResult maybeWhen<TResult extends Object?>({
    TResult Function(int id, String text)? result,
    TResult Function(String text)? streaming,
    TResult Function(int id, double value)? progress,
    TResult Function(double value)? level,
    TResult Function(String label)? inputDevice,
    TResult Function(String label)? outputDevice,
    TResult Function(String status)? aec3Status,
    TResult Function(double similarity, bool accepted)? speakerVerify,
    TResult Function(String message)? error,
    required TResult orElse(),
  }) {
    if (aec3Status != null) {
      return aec3Status(status);
    }
    return orElse();
  }

  @override
  @optionalTypeArgs
  TResult map<TResult extends Object?>({
    required TResult Function(RealtimeResult value) result,
    required TResult Function(RealtimeStreaming value) streaming,
    required TResult Function(RealtimeProgress value) progress,
    required TResult Function(RealtimeLevel value) level,
    required TResult Function(RealtimeInputDevice value) inputDevice,
    required TResult Function(RealtimeOutputDevice value) outputDevice,
    required TResult Function(RealtimeAec3Status value) aec3Status,
    required TResult Function(RealtimeSpeakerVerify value) speakerVerify,
    required TResult Function(RealtimeError value) error,
  }) {
    return aec3Status(this);
  }

  @override
  @optionalTypeArgs
  TResult? mapOrNull<TResult extends Object?>({
    TResult? Function(RealtimeResult value)? result,
    TResult? Function(RealtimeStreaming value)? streaming,
    TResult? Function(RealtimeProgress value)? progress,
    TResult? Function(RealtimeLevel value)? level,
    TResult? Function(RealtimeInputDevice value)? inputDevice,
    TResult? Function(RealtimeOutputDevice value)? outputDevice,
    TResult? Function(RealtimeAec3Status value)? aec3Status,
    TResult? Function(RealtimeSpeakerVerify value)? speakerVerify,
    TResult? Function(RealtimeError value)? error,
  }) {
    return aec3Status?.call(this);
  }

  @override
  @optionalTypeArgs
  TResult maybeMap<TResult extends Object?>({
    TResult Function(RealtimeResult value)? result,
    TResult Function(RealtimeStreaming value)? streaming,
    TResult Function(RealtimeProgress value)? progress,
    TResult Function(RealtimeLevel value)? level,
    TResult Function(RealtimeInputDevice value)? inputDevice,
    TResult Function(RealtimeOutputDevice value)? outputDevice,
    TResult Function(RealtimeAec3Status value)? aec3Status,
    TResult Function(RealtimeSpeakerVerify value)? speakerVerify,
    TResult Function(RealtimeError value)? error,
    required TResult orElse(),
  }) {
    if (aec3Status != null) {
      return aec3Status(this);
    }
    return orElse();
  }
}

abstract class RealtimeAec3Status implements RealtimeEvent {
  const factory RealtimeAec3Status({required final String status}) =
      _$RealtimeAec3StatusImpl;

  String get status;

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$RealtimeAec3StatusImplCopyWith<_$RealtimeAec3StatusImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class _$$RealtimeSpeakerVerifyImplCopyWith<$Res> {
  factory _$$RealtimeSpeakerVerifyImplCopyWith(
    _$RealtimeSpeakerVerifyImpl value,
    $Res Function(_$RealtimeSpeakerVerifyImpl) then,
  ) = __$$RealtimeSpeakerVerifyImplCopyWithImpl<$Res>;
  @useResult
  $Res call({double similarity, bool accepted});
}

/// @nodoc
class __$$RealtimeSpeakerVerifyImplCopyWithImpl<$Res>
    extends _$RealtimeEventCopyWithImpl<$Res, _$RealtimeSpeakerVerifyImpl>
    implements _$$RealtimeSpeakerVerifyImplCopyWith<$Res> {
  __$$RealtimeSpeakerVerifyImplCopyWithImpl(
    _$RealtimeSpeakerVerifyImpl _value,
    $Res Function(_$RealtimeSpeakerVerifyImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({Object? similarity = null, Object? accepted = null}) {
    return _then(
      _$RealtimeSpeakerVerifyImpl(
        similarity: null == similarity
            ? _value.similarity
            : similarity // ignore: cast_nullable_to_non_nullable
                  as double,
        accepted: null == accepted
            ? _value.accepted
            : accepted // ignore: cast_nullable_to_non_nullable
                  as bool,
      ),
    );
  }
}

/// @nodoc

class _$RealtimeSpeakerVerifyImpl implements RealtimeSpeakerVerify {
  const _$RealtimeSpeakerVerifyImpl({
    required this.similarity,
    required this.accepted,
  });

  @override
  final double similarity;
  @override
  final bool accepted;

  @override
  String toString() {
    return 'RealtimeEvent.speakerVerify(similarity: $similarity, accepted: $accepted)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$RealtimeSpeakerVerifyImpl &&
            (identical(other.similarity, similarity) ||
                other.similarity == similarity) &&
            (identical(other.accepted, accepted) ||
                other.accepted == accepted));
  }

  @override
  int get hashCode => Object.hash(runtimeType, similarity, accepted);

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$RealtimeSpeakerVerifyImplCopyWith<_$RealtimeSpeakerVerifyImpl>
  get copyWith =>
      __$$RealtimeSpeakerVerifyImplCopyWithImpl<_$RealtimeSpeakerVerifyImpl>(
        this,
        _$identity,
      );

  @override
  @optionalTypeArgs
  TResult when<TResult extends Object?>({
    required TResult Function(int id, String text) result,
    required TResult Function(String text) streaming,
    required TResult Function(int id, double value) progress,
    required TResult Function(double value) level,
    required TResult Function(String label) inputDevice,
    required TResult Function(String label) outputDevice,
    required TResult Function(String status) aec3Status,
    required TResult Function(double similarity, bool accepted) speakerVerify,
    required TResult Function(String message) error,
  }) {
    return speakerVerify(similarity, accepted);
  }

  @override
  @optionalTypeArgs
  TResult? whenOrNull<TResult extends Object?>({
    TResult? Function(int id, String text)? result,
    TResult? Function(String text)? streaming,
    TResult? Function(int id, double value)? progress,
    TResult? Function(double value)? level,
    TResult? Function(String label)? inputDevice,
    TResult? Function(String label)? outputDevice,
    TResult? Function(String status)? aec3Status,
    TResult? Function(double similarity, bool accepted)? speakerVerify,
    TResult? Function(String message)? error,
  }) {
    return speakerVerify?.call(similarity, accepted);
  }

  @override
  @optionalTypeArgs
  TResult maybeWhen<TResult extends Object?>({
    TResult Function(int id, String text)? result,
    TResult Function(String text)? streaming,
    TResult Function(int id, double value)? progress,
    TResult Function(double value)? level,
    TResult Function(String label)? inputDevice,
    TResult Function(String label)? outputDevice,
    TResult Function(String status)? aec3Status,
    TResult Function(double similarity, bool accepted)? speakerVerify,
    TResult Function(String message)? error,
    required TResult orElse(),
  }) {
    if (speakerVerify != null) {
      return speakerVerify(similarity, accepted);
    }
    return orElse();
  }

  @override
  @optionalTypeArgs
  TResult map<TResult extends Object?>({
    required TResult Function(RealtimeResult value) result,
    required TResult Function(RealtimeStreaming value) streaming,
    required TResult Function(RealtimeProgress value) progress,
    required TResult Function(RealtimeLevel value) level,
    required TResult Function(RealtimeInputDevice value) inputDevice,
    required TResult Function(RealtimeOutputDevice value) outputDevice,
    required TResult Function(RealtimeAec3Status value) aec3Status,
    required TResult Function(RealtimeSpeakerVerify value) speakerVerify,
    required TResult Function(RealtimeError value) error,
  }) {
    return speakerVerify(this);
  }

  @override
  @optionalTypeArgs
  TResult? mapOrNull<TResult extends Object?>({
    TResult? Function(RealtimeResult value)? result,
    TResult? Function(RealtimeStreaming value)? streaming,
    TResult? Function(RealtimeProgress value)? progress,
    TResult? Function(RealtimeLevel value)? level,
    TResult? Function(RealtimeInputDevice value)? inputDevice,
    TResult? Function(RealtimeOutputDevice value)? outputDevice,
    TResult? Function(RealtimeAec3Status value)? aec3Status,
    TResult? Function(RealtimeSpeakerVerify value)? speakerVerify,
    TResult? Function(RealtimeError value)? error,
  }) {
    return speakerVerify?.call(this);
  }

  @override
  @optionalTypeArgs
  TResult maybeMap<TResult extends Object?>({
    TResult Function(RealtimeResult value)? result,
    TResult Function(RealtimeStreaming value)? streaming,
    TResult Function(RealtimeProgress value)? progress,
    TResult Function(RealtimeLevel value)? level,
    TResult Function(RealtimeInputDevice value)? inputDevice,
    TResult Function(RealtimeOutputDevice value)? outputDevice,
    TResult Function(RealtimeAec3Status value)? aec3Status,
    TResult Function(RealtimeSpeakerVerify value)? speakerVerify,
    TResult Function(RealtimeError value)? error,
    required TResult orElse(),
  }) {
    if (speakerVerify != null) {
      return speakerVerify(this);
    }
    return orElse();
  }
}

abstract class RealtimeSpeakerVerify implements RealtimeEvent {
  const factory RealtimeSpeakerVerify({
    required final double similarity,
    required final bool accepted,
  }) = _$RealtimeSpeakerVerifyImpl;

  double get similarity;
  bool get accepted;

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$RealtimeSpeakerVerifyImplCopyWith<_$RealtimeSpeakerVerifyImpl>
  get copyWith => throw _privateConstructorUsedError;
}

/// @nodoc
abstract class _$$RealtimeErrorImplCopyWith<$Res> {
  factory _$$RealtimeErrorImplCopyWith(
    _$RealtimeErrorImpl value,
    $Res Function(_$RealtimeErrorImpl) then,
  ) = __$$RealtimeErrorImplCopyWithImpl<$Res>;
  @useResult
  $Res call({String message});
}

/// @nodoc
class __$$RealtimeErrorImplCopyWithImpl<$Res>
    extends _$RealtimeEventCopyWithImpl<$Res, _$RealtimeErrorImpl>
    implements _$$RealtimeErrorImplCopyWith<$Res> {
  __$$RealtimeErrorImplCopyWithImpl(
    _$RealtimeErrorImpl _value,
    $Res Function(_$RealtimeErrorImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({Object? message = null}) {
    return _then(
      _$RealtimeErrorImpl(
        message: null == message
            ? _value.message
            : message // ignore: cast_nullable_to_non_nullable
                  as String,
      ),
    );
  }
}

/// @nodoc

class _$RealtimeErrorImpl implements RealtimeError {
  const _$RealtimeErrorImpl({required this.message});

  @override
  final String message;

  @override
  String toString() {
    return 'RealtimeEvent.error(message: $message)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$RealtimeErrorImpl &&
            (identical(other.message, message) || other.message == message));
  }

  @override
  int get hashCode => Object.hash(runtimeType, message);

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$RealtimeErrorImplCopyWith<_$RealtimeErrorImpl> get copyWith =>
      __$$RealtimeErrorImplCopyWithImpl<_$RealtimeErrorImpl>(this, _$identity);

  @override
  @optionalTypeArgs
  TResult when<TResult extends Object?>({
    required TResult Function(int id, String text) result,
    required TResult Function(String text) streaming,
    required TResult Function(int id, double value) progress,
    required TResult Function(double value) level,
    required TResult Function(String label) inputDevice,
    required TResult Function(String label) outputDevice,
    required TResult Function(String status) aec3Status,
    required TResult Function(double similarity, bool accepted) speakerVerify,
    required TResult Function(String message) error,
  }) {
    return error(message);
  }

  @override
  @optionalTypeArgs
  TResult? whenOrNull<TResult extends Object?>({
    TResult? Function(int id, String text)? result,
    TResult? Function(String text)? streaming,
    TResult? Function(int id, double value)? progress,
    TResult? Function(double value)? level,
    TResult? Function(String label)? inputDevice,
    TResult? Function(String label)? outputDevice,
    TResult? Function(String status)? aec3Status,
    TResult? Function(double similarity, bool accepted)? speakerVerify,
    TResult? Function(String message)? error,
  }) {
    return error?.call(message);
  }

  @override
  @optionalTypeArgs
  TResult maybeWhen<TResult extends Object?>({
    TResult Function(int id, String text)? result,
    TResult Function(String text)? streaming,
    TResult Function(int id, double value)? progress,
    TResult Function(double value)? level,
    TResult Function(String label)? inputDevice,
    TResult Function(String label)? outputDevice,
    TResult Function(String status)? aec3Status,
    TResult Function(double similarity, bool accepted)? speakerVerify,
    TResult Function(String message)? error,
    required TResult orElse(),
  }) {
    if (error != null) {
      return error(message);
    }
    return orElse();
  }

  @override
  @optionalTypeArgs
  TResult map<TResult extends Object?>({
    required TResult Function(RealtimeResult value) result,
    required TResult Function(RealtimeStreaming value) streaming,
    required TResult Function(RealtimeProgress value) progress,
    required TResult Function(RealtimeLevel value) level,
    required TResult Function(RealtimeInputDevice value) inputDevice,
    required TResult Function(RealtimeOutputDevice value) outputDevice,
    required TResult Function(RealtimeAec3Status value) aec3Status,
    required TResult Function(RealtimeSpeakerVerify value) speakerVerify,
    required TResult Function(RealtimeError value) error,
  }) {
    return error(this);
  }

  @override
  @optionalTypeArgs
  TResult? mapOrNull<TResult extends Object?>({
    TResult? Function(RealtimeResult value)? result,
    TResult? Function(RealtimeStreaming value)? streaming,
    TResult? Function(RealtimeProgress value)? progress,
    TResult? Function(RealtimeLevel value)? level,
    TResult? Function(RealtimeInputDevice value)? inputDevice,
    TResult? Function(RealtimeOutputDevice value)? outputDevice,
    TResult? Function(RealtimeAec3Status value)? aec3Status,
    TResult? Function(RealtimeSpeakerVerify value)? speakerVerify,
    TResult? Function(RealtimeError value)? error,
  }) {
    return error?.call(this);
  }

  @override
  @optionalTypeArgs
  TResult maybeMap<TResult extends Object?>({
    TResult Function(RealtimeResult value)? result,
    TResult Function(RealtimeStreaming value)? streaming,
    TResult Function(RealtimeProgress value)? progress,
    TResult Function(RealtimeLevel value)? level,
    TResult Function(RealtimeInputDevice value)? inputDevice,
    TResult Function(RealtimeOutputDevice value)? outputDevice,
    TResult Function(RealtimeAec3Status value)? aec3Status,
    TResult Function(RealtimeSpeakerVerify value)? speakerVerify,
    TResult Function(RealtimeError value)? error,
    required TResult orElse(),
  }) {
    if (error != null) {
      return error(this);
    }
    return orElse();
  }
}

abstract class RealtimeError implements RealtimeEvent {
  const factory RealtimeError({required final String message}) =
      _$RealtimeErrorImpl;

  String get message;

  /// Create a copy of RealtimeEvent
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$RealtimeErrorImplCopyWith<_$RealtimeErrorImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
