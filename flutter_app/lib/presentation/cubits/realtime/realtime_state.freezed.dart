// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'realtime_state.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
  'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models',
);

/// @nodoc
mixin _$RealtimeState {
  bool get running => throw _privateConstructorUsedError;
  String get status => throw _privateConstructorUsedError;
  List<RecognizedItem> get recognized => throw _privateConstructorUsedError;
  double get inputLevel => throw _privateConstructorUsedError;
  double get playbackProgress => throw _privateConstructorUsedError;
  int get playingId => throw _privateConstructorUsedError;
  String get inputDeviceLabel => throw _privateConstructorUsedError;
  String get outputDeviceLabel => throw _privateConstructorUsedError;
  String get aec3Status => throw _privateConstructorUsedError;
  bool get pttPressed => throw _privateConstructorUsedError;
  String get pttStreamingText => throw _privateConstructorUsedError;
  double get speakerLastSimilarity => throw _privateConstructorUsedError;
  bool get loading => throw _privateConstructorUsedError;
  String? get error => throw _privateConstructorUsedError;
  String? get currentAsrDir => throw _privateConstructorUsedError;
  String? get currentVoiceDir => throw _privateConstructorUsedError;

  /// Create a copy of RealtimeState
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $RealtimeStateCopyWith<RealtimeState> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $RealtimeStateCopyWith<$Res> {
  factory $RealtimeStateCopyWith(
    RealtimeState value,
    $Res Function(RealtimeState) then,
  ) = _$RealtimeStateCopyWithImpl<$Res, RealtimeState>;
  @useResult
  $Res call({
    bool running,
    String status,
    List<RecognizedItem> recognized,
    double inputLevel,
    double playbackProgress,
    int playingId,
    String inputDeviceLabel,
    String outputDeviceLabel,
    String aec3Status,
    bool pttPressed,
    String pttStreamingText,
    double speakerLastSimilarity,
    bool loading,
    String? error,
    String? currentAsrDir,
    String? currentVoiceDir,
  });
}

/// @nodoc
class _$RealtimeStateCopyWithImpl<$Res, $Val extends RealtimeState>
    implements $RealtimeStateCopyWith<$Res> {
  _$RealtimeStateCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of RealtimeState
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? running = null,
    Object? status = null,
    Object? recognized = null,
    Object? inputLevel = null,
    Object? playbackProgress = null,
    Object? playingId = null,
    Object? inputDeviceLabel = null,
    Object? outputDeviceLabel = null,
    Object? aec3Status = null,
    Object? pttPressed = null,
    Object? pttStreamingText = null,
    Object? speakerLastSimilarity = null,
    Object? loading = null,
    Object? error = freezed,
    Object? currentAsrDir = freezed,
    Object? currentVoiceDir = freezed,
  }) {
    return _then(
      _value.copyWith(
            running: null == running
                ? _value.running
                : running // ignore: cast_nullable_to_non_nullable
                      as bool,
            status: null == status
                ? _value.status
                : status // ignore: cast_nullable_to_non_nullable
                      as String,
            recognized: null == recognized
                ? _value.recognized
                : recognized // ignore: cast_nullable_to_non_nullable
                      as List<RecognizedItem>,
            inputLevel: null == inputLevel
                ? _value.inputLevel
                : inputLevel // ignore: cast_nullable_to_non_nullable
                      as double,
            playbackProgress: null == playbackProgress
                ? _value.playbackProgress
                : playbackProgress // ignore: cast_nullable_to_non_nullable
                      as double,
            playingId: null == playingId
                ? _value.playingId
                : playingId // ignore: cast_nullable_to_non_nullable
                      as int,
            inputDeviceLabel: null == inputDeviceLabel
                ? _value.inputDeviceLabel
                : inputDeviceLabel // ignore: cast_nullable_to_non_nullable
                      as String,
            outputDeviceLabel: null == outputDeviceLabel
                ? _value.outputDeviceLabel
                : outputDeviceLabel // ignore: cast_nullable_to_non_nullable
                      as String,
            aec3Status: null == aec3Status
                ? _value.aec3Status
                : aec3Status // ignore: cast_nullable_to_non_nullable
                      as String,
            pttPressed: null == pttPressed
                ? _value.pttPressed
                : pttPressed // ignore: cast_nullable_to_non_nullable
                      as bool,
            pttStreamingText: null == pttStreamingText
                ? _value.pttStreamingText
                : pttStreamingText // ignore: cast_nullable_to_non_nullable
                      as String,
            speakerLastSimilarity: null == speakerLastSimilarity
                ? _value.speakerLastSimilarity
                : speakerLastSimilarity // ignore: cast_nullable_to_non_nullable
                      as double,
            loading: null == loading
                ? _value.loading
                : loading // ignore: cast_nullable_to_non_nullable
                      as bool,
            error: freezed == error
                ? _value.error
                : error // ignore: cast_nullable_to_non_nullable
                      as String?,
            currentAsrDir: freezed == currentAsrDir
                ? _value.currentAsrDir
                : currentAsrDir // ignore: cast_nullable_to_non_nullable
                      as String?,
            currentVoiceDir: freezed == currentVoiceDir
                ? _value.currentVoiceDir
                : currentVoiceDir // ignore: cast_nullable_to_non_nullable
                      as String?,
          )
          as $Val,
    );
  }
}

/// @nodoc
abstract class _$$RealtimeStateImplCopyWith<$Res>
    implements $RealtimeStateCopyWith<$Res> {
  factory _$$RealtimeStateImplCopyWith(
    _$RealtimeStateImpl value,
    $Res Function(_$RealtimeStateImpl) then,
  ) = __$$RealtimeStateImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({
    bool running,
    String status,
    List<RecognizedItem> recognized,
    double inputLevel,
    double playbackProgress,
    int playingId,
    String inputDeviceLabel,
    String outputDeviceLabel,
    String aec3Status,
    bool pttPressed,
    String pttStreamingText,
    double speakerLastSimilarity,
    bool loading,
    String? error,
    String? currentAsrDir,
    String? currentVoiceDir,
  });
}

/// @nodoc
class __$$RealtimeStateImplCopyWithImpl<$Res>
    extends _$RealtimeStateCopyWithImpl<$Res, _$RealtimeStateImpl>
    implements _$$RealtimeStateImplCopyWith<$Res> {
  __$$RealtimeStateImplCopyWithImpl(
    _$RealtimeStateImpl _value,
    $Res Function(_$RealtimeStateImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of RealtimeState
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? running = null,
    Object? status = null,
    Object? recognized = null,
    Object? inputLevel = null,
    Object? playbackProgress = null,
    Object? playingId = null,
    Object? inputDeviceLabel = null,
    Object? outputDeviceLabel = null,
    Object? aec3Status = null,
    Object? pttPressed = null,
    Object? pttStreamingText = null,
    Object? speakerLastSimilarity = null,
    Object? loading = null,
    Object? error = freezed,
    Object? currentAsrDir = freezed,
    Object? currentVoiceDir = freezed,
  }) {
    return _then(
      _$RealtimeStateImpl(
        running: null == running
            ? _value.running
            : running // ignore: cast_nullable_to_non_nullable
                  as bool,
        status: null == status
            ? _value.status
            : status // ignore: cast_nullable_to_non_nullable
                  as String,
        recognized: null == recognized
            ? _value._recognized
            : recognized // ignore: cast_nullable_to_non_nullable
                  as List<RecognizedItem>,
        inputLevel: null == inputLevel
            ? _value.inputLevel
            : inputLevel // ignore: cast_nullable_to_non_nullable
                  as double,
        playbackProgress: null == playbackProgress
            ? _value.playbackProgress
            : playbackProgress // ignore: cast_nullable_to_non_nullable
                  as double,
        playingId: null == playingId
            ? _value.playingId
            : playingId // ignore: cast_nullable_to_non_nullable
                  as int,
        inputDeviceLabel: null == inputDeviceLabel
            ? _value.inputDeviceLabel
            : inputDeviceLabel // ignore: cast_nullable_to_non_nullable
                  as String,
        outputDeviceLabel: null == outputDeviceLabel
            ? _value.outputDeviceLabel
            : outputDeviceLabel // ignore: cast_nullable_to_non_nullable
                  as String,
        aec3Status: null == aec3Status
            ? _value.aec3Status
            : aec3Status // ignore: cast_nullable_to_non_nullable
                  as String,
        pttPressed: null == pttPressed
            ? _value.pttPressed
            : pttPressed // ignore: cast_nullable_to_non_nullable
                  as bool,
        pttStreamingText: null == pttStreamingText
            ? _value.pttStreamingText
            : pttStreamingText // ignore: cast_nullable_to_non_nullable
                  as String,
        speakerLastSimilarity: null == speakerLastSimilarity
            ? _value.speakerLastSimilarity
            : speakerLastSimilarity // ignore: cast_nullable_to_non_nullable
                  as double,
        loading: null == loading
            ? _value.loading
            : loading // ignore: cast_nullable_to_non_nullable
                  as bool,
        error: freezed == error
            ? _value.error
            : error // ignore: cast_nullable_to_non_nullable
                  as String?,
        currentAsrDir: freezed == currentAsrDir
            ? _value.currentAsrDir
            : currentAsrDir // ignore: cast_nullable_to_non_nullable
                  as String?,
        currentVoiceDir: freezed == currentVoiceDir
            ? _value.currentVoiceDir
            : currentVoiceDir // ignore: cast_nullable_to_non_nullable
                  as String?,
      ),
    );
  }
}

/// @nodoc

class _$RealtimeStateImpl implements _RealtimeState {
  const _$RealtimeStateImpl({
    this.running = false,
    this.status = '待命',
    final List<RecognizedItem> recognized = const [],
    this.inputLevel = 0.0,
    this.playbackProgress = 0.0,
    this.playingId = -1,
    this.inputDeviceLabel = '未知',
    this.outputDeviceLabel = '未知',
    this.aec3Status = '未启用',
    this.pttPressed = false,
    this.pttStreamingText = '',
    this.speakerLastSimilarity = -1.0,
    this.loading = false,
    this.error,
    this.currentAsrDir,
    this.currentVoiceDir,
  }) : _recognized = recognized;

  @override
  @JsonKey()
  final bool running;
  @override
  @JsonKey()
  final String status;
  final List<RecognizedItem> _recognized;
  @override
  @JsonKey()
  List<RecognizedItem> get recognized {
    if (_recognized is EqualUnmodifiableListView) return _recognized;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_recognized);
  }

  @override
  @JsonKey()
  final double inputLevel;
  @override
  @JsonKey()
  final double playbackProgress;
  @override
  @JsonKey()
  final int playingId;
  @override
  @JsonKey()
  final String inputDeviceLabel;
  @override
  @JsonKey()
  final String outputDeviceLabel;
  @override
  @JsonKey()
  final String aec3Status;
  @override
  @JsonKey()
  final bool pttPressed;
  @override
  @JsonKey()
  final String pttStreamingText;
  @override
  @JsonKey()
  final double speakerLastSimilarity;
  @override
  @JsonKey()
  final bool loading;
  @override
  final String? error;
  @override
  final String? currentAsrDir;
  @override
  final String? currentVoiceDir;

  @override
  String toString() {
    return 'RealtimeState(running: $running, status: $status, recognized: $recognized, inputLevel: $inputLevel, playbackProgress: $playbackProgress, playingId: $playingId, inputDeviceLabel: $inputDeviceLabel, outputDeviceLabel: $outputDeviceLabel, aec3Status: $aec3Status, pttPressed: $pttPressed, pttStreamingText: $pttStreamingText, speakerLastSimilarity: $speakerLastSimilarity, loading: $loading, error: $error, currentAsrDir: $currentAsrDir, currentVoiceDir: $currentVoiceDir)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$RealtimeStateImpl &&
            (identical(other.running, running) || other.running == running) &&
            (identical(other.status, status) || other.status == status) &&
            const DeepCollectionEquality().equals(
              other._recognized,
              _recognized,
            ) &&
            (identical(other.inputLevel, inputLevel) ||
                other.inputLevel == inputLevel) &&
            (identical(other.playbackProgress, playbackProgress) ||
                other.playbackProgress == playbackProgress) &&
            (identical(other.playingId, playingId) ||
                other.playingId == playingId) &&
            (identical(other.inputDeviceLabel, inputDeviceLabel) ||
                other.inputDeviceLabel == inputDeviceLabel) &&
            (identical(other.outputDeviceLabel, outputDeviceLabel) ||
                other.outputDeviceLabel == outputDeviceLabel) &&
            (identical(other.aec3Status, aec3Status) ||
                other.aec3Status == aec3Status) &&
            (identical(other.pttPressed, pttPressed) ||
                other.pttPressed == pttPressed) &&
            (identical(other.pttStreamingText, pttStreamingText) ||
                other.pttStreamingText == pttStreamingText) &&
            (identical(other.speakerLastSimilarity, speakerLastSimilarity) ||
                other.speakerLastSimilarity == speakerLastSimilarity) &&
            (identical(other.loading, loading) || other.loading == loading) &&
            (identical(other.error, error) || other.error == error) &&
            (identical(other.currentAsrDir, currentAsrDir) ||
                other.currentAsrDir == currentAsrDir) &&
            (identical(other.currentVoiceDir, currentVoiceDir) ||
                other.currentVoiceDir == currentVoiceDir));
  }

  @override
  int get hashCode => Object.hash(
    runtimeType,
    running,
    status,
    const DeepCollectionEquality().hash(_recognized),
    inputLevel,
    playbackProgress,
    playingId,
    inputDeviceLabel,
    outputDeviceLabel,
    aec3Status,
    pttPressed,
    pttStreamingText,
    speakerLastSimilarity,
    loading,
    error,
    currentAsrDir,
    currentVoiceDir,
  );

  /// Create a copy of RealtimeState
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$RealtimeStateImplCopyWith<_$RealtimeStateImpl> get copyWith =>
      __$$RealtimeStateImplCopyWithImpl<_$RealtimeStateImpl>(this, _$identity);
}

abstract class _RealtimeState implements RealtimeState {
  const factory _RealtimeState({
    final bool running,
    final String status,
    final List<RecognizedItem> recognized,
    final double inputLevel,
    final double playbackProgress,
    final int playingId,
    final String inputDeviceLabel,
    final String outputDeviceLabel,
    final String aec3Status,
    final bool pttPressed,
    final String pttStreamingText,
    final double speakerLastSimilarity,
    final bool loading,
    final String? error,
    final String? currentAsrDir,
    final String? currentVoiceDir,
  }) = _$RealtimeStateImpl;

  @override
  bool get running;
  @override
  String get status;
  @override
  List<RecognizedItem> get recognized;
  @override
  double get inputLevel;
  @override
  double get playbackProgress;
  @override
  int get playingId;
  @override
  String get inputDeviceLabel;
  @override
  String get outputDeviceLabel;
  @override
  String get aec3Status;
  @override
  bool get pttPressed;
  @override
  String get pttStreamingText;
  @override
  double get speakerLastSimilarity;
  @override
  bool get loading;
  @override
  String? get error;
  @override
  String? get currentAsrDir;
  @override
  String? get currentVoiceDir;

  /// Create a copy of RealtimeState
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$RealtimeStateImplCopyWith<_$RealtimeStateImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
