// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'quick_subtitle_state.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
  'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models',
);

/// @nodoc
mixin _$QuickSubtitleState {
  QuickSubtitleConfig get config => throw _privateConstructorUsedError;
  int get selectedGroupIndex => throw _privateConstructorUsedError;
  String get inputText => throw _privateConstructorUsedError;
  bool get loading => throw _privateConstructorUsedError;
  String? get error => throw _privateConstructorUsedError;

  /// Create a copy of QuickSubtitleState
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $QuickSubtitleStateCopyWith<QuickSubtitleState> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $QuickSubtitleStateCopyWith<$Res> {
  factory $QuickSubtitleStateCopyWith(
    QuickSubtitleState value,
    $Res Function(QuickSubtitleState) then,
  ) = _$QuickSubtitleStateCopyWithImpl<$Res, QuickSubtitleState>;
  @useResult
  $Res call({
    QuickSubtitleConfig config,
    int selectedGroupIndex,
    String inputText,
    bool loading,
    String? error,
  });

  $QuickSubtitleConfigCopyWith<$Res> get config;
}

/// @nodoc
class _$QuickSubtitleStateCopyWithImpl<$Res, $Val extends QuickSubtitleState>
    implements $QuickSubtitleStateCopyWith<$Res> {
  _$QuickSubtitleStateCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of QuickSubtitleState
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? config = null,
    Object? selectedGroupIndex = null,
    Object? inputText = null,
    Object? loading = null,
    Object? error = freezed,
  }) {
    return _then(
      _value.copyWith(
            config: null == config
                ? _value.config
                : config // ignore: cast_nullable_to_non_nullable
                      as QuickSubtitleConfig,
            selectedGroupIndex: null == selectedGroupIndex
                ? _value.selectedGroupIndex
                : selectedGroupIndex // ignore: cast_nullable_to_non_nullable
                      as int,
            inputText: null == inputText
                ? _value.inputText
                : inputText // ignore: cast_nullable_to_non_nullable
                      as String,
            loading: null == loading
                ? _value.loading
                : loading // ignore: cast_nullable_to_non_nullable
                      as bool,
            error: freezed == error
                ? _value.error
                : error // ignore: cast_nullable_to_non_nullable
                      as String?,
          )
          as $Val,
    );
  }

  /// Create a copy of QuickSubtitleState
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $QuickSubtitleConfigCopyWith<$Res> get config {
    return $QuickSubtitleConfigCopyWith<$Res>(_value.config, (value) {
      return _then(_value.copyWith(config: value) as $Val);
    });
  }
}

/// @nodoc
abstract class _$$QuickSubtitleStateImplCopyWith<$Res>
    implements $QuickSubtitleStateCopyWith<$Res> {
  factory _$$QuickSubtitleStateImplCopyWith(
    _$QuickSubtitleStateImpl value,
    $Res Function(_$QuickSubtitleStateImpl) then,
  ) = __$$QuickSubtitleStateImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({
    QuickSubtitleConfig config,
    int selectedGroupIndex,
    String inputText,
    bool loading,
    String? error,
  });

  @override
  $QuickSubtitleConfigCopyWith<$Res> get config;
}

/// @nodoc
class __$$QuickSubtitleStateImplCopyWithImpl<$Res>
    extends _$QuickSubtitleStateCopyWithImpl<$Res, _$QuickSubtitleStateImpl>
    implements _$$QuickSubtitleStateImplCopyWith<$Res> {
  __$$QuickSubtitleStateImplCopyWithImpl(
    _$QuickSubtitleStateImpl _value,
    $Res Function(_$QuickSubtitleStateImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of QuickSubtitleState
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? config = null,
    Object? selectedGroupIndex = null,
    Object? inputText = null,
    Object? loading = null,
    Object? error = freezed,
  }) {
    return _then(
      _$QuickSubtitleStateImpl(
        config: null == config
            ? _value.config
            : config // ignore: cast_nullable_to_non_nullable
                  as QuickSubtitleConfig,
        selectedGroupIndex: null == selectedGroupIndex
            ? _value.selectedGroupIndex
            : selectedGroupIndex // ignore: cast_nullable_to_non_nullable
                  as int,
        inputText: null == inputText
            ? _value.inputText
            : inputText // ignore: cast_nullable_to_non_nullable
                  as String,
        loading: null == loading
            ? _value.loading
            : loading // ignore: cast_nullable_to_non_nullable
                  as bool,
        error: freezed == error
            ? _value.error
            : error // ignore: cast_nullable_to_non_nullable
                  as String?,
      ),
    );
  }
}

/// @nodoc

class _$QuickSubtitleStateImpl implements _QuickSubtitleState {
  const _$QuickSubtitleStateImpl({
    this.config = const QuickSubtitleConfig(),
    this.selectedGroupIndex = 0,
    this.inputText = '',
    this.loading = false,
    this.error,
  });

  @override
  @JsonKey()
  final QuickSubtitleConfig config;
  @override
  @JsonKey()
  final int selectedGroupIndex;
  @override
  @JsonKey()
  final String inputText;
  @override
  @JsonKey()
  final bool loading;
  @override
  final String? error;

  @override
  String toString() {
    return 'QuickSubtitleState(config: $config, selectedGroupIndex: $selectedGroupIndex, inputText: $inputText, loading: $loading, error: $error)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$QuickSubtitleStateImpl &&
            (identical(other.config, config) || other.config == config) &&
            (identical(other.selectedGroupIndex, selectedGroupIndex) ||
                other.selectedGroupIndex == selectedGroupIndex) &&
            (identical(other.inputText, inputText) ||
                other.inputText == inputText) &&
            (identical(other.loading, loading) || other.loading == loading) &&
            (identical(other.error, error) || other.error == error));
  }

  @override
  int get hashCode => Object.hash(
    runtimeType,
    config,
    selectedGroupIndex,
    inputText,
    loading,
    error,
  );

  /// Create a copy of QuickSubtitleState
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$QuickSubtitleStateImplCopyWith<_$QuickSubtitleStateImpl> get copyWith =>
      __$$QuickSubtitleStateImplCopyWithImpl<_$QuickSubtitleStateImpl>(
        this,
        _$identity,
      );
}

abstract class _QuickSubtitleState implements QuickSubtitleState {
  const factory _QuickSubtitleState({
    final QuickSubtitleConfig config,
    final int selectedGroupIndex,
    final String inputText,
    final bool loading,
    final String? error,
  }) = _$QuickSubtitleStateImpl;

  @override
  QuickSubtitleConfig get config;
  @override
  int get selectedGroupIndex;
  @override
  String get inputText;
  @override
  bool get loading;
  @override
  String? get error;

  /// Create a copy of QuickSubtitleState
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$QuickSubtitleStateImplCopyWith<_$QuickSubtitleStateImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
