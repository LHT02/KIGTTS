// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'quick_card_state.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
  'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models',
);

/// @nodoc
mixin _$QuickCardState {
  List<QuickCard> get cards => throw _privateConstructorUsedError;
  int get selectedIndex => throw _privateConstructorUsedError;
  bool get loading => throw _privateConstructorUsedError;
  String? get error => throw _privateConstructorUsedError;

  /// Create a copy of QuickCardState
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $QuickCardStateCopyWith<QuickCardState> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $QuickCardStateCopyWith<$Res> {
  factory $QuickCardStateCopyWith(
    QuickCardState value,
    $Res Function(QuickCardState) then,
  ) = _$QuickCardStateCopyWithImpl<$Res, QuickCardState>;
  @useResult
  $Res call({
    List<QuickCard> cards,
    int selectedIndex,
    bool loading,
    String? error,
  });
}

/// @nodoc
class _$QuickCardStateCopyWithImpl<$Res, $Val extends QuickCardState>
    implements $QuickCardStateCopyWith<$Res> {
  _$QuickCardStateCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of QuickCardState
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? cards = null,
    Object? selectedIndex = null,
    Object? loading = null,
    Object? error = freezed,
  }) {
    return _then(
      _value.copyWith(
            cards: null == cards
                ? _value.cards
                : cards // ignore: cast_nullable_to_non_nullable
                      as List<QuickCard>,
            selectedIndex: null == selectedIndex
                ? _value.selectedIndex
                : selectedIndex // ignore: cast_nullable_to_non_nullable
                      as int,
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
}

/// @nodoc
abstract class _$$QuickCardStateImplCopyWith<$Res>
    implements $QuickCardStateCopyWith<$Res> {
  factory _$$QuickCardStateImplCopyWith(
    _$QuickCardStateImpl value,
    $Res Function(_$QuickCardStateImpl) then,
  ) = __$$QuickCardStateImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({
    List<QuickCard> cards,
    int selectedIndex,
    bool loading,
    String? error,
  });
}

/// @nodoc
class __$$QuickCardStateImplCopyWithImpl<$Res>
    extends _$QuickCardStateCopyWithImpl<$Res, _$QuickCardStateImpl>
    implements _$$QuickCardStateImplCopyWith<$Res> {
  __$$QuickCardStateImplCopyWithImpl(
    _$QuickCardStateImpl _value,
    $Res Function(_$QuickCardStateImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of QuickCardState
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? cards = null,
    Object? selectedIndex = null,
    Object? loading = null,
    Object? error = freezed,
  }) {
    return _then(
      _$QuickCardStateImpl(
        cards: null == cards
            ? _value._cards
            : cards // ignore: cast_nullable_to_non_nullable
                  as List<QuickCard>,
        selectedIndex: null == selectedIndex
            ? _value.selectedIndex
            : selectedIndex // ignore: cast_nullable_to_non_nullable
                  as int,
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

class _$QuickCardStateImpl implements _QuickCardState {
  const _$QuickCardStateImpl({
    final List<QuickCard> cards = const [],
    this.selectedIndex = 0,
    this.loading = false,
    this.error,
  }) : _cards = cards;

  final List<QuickCard> _cards;
  @override
  @JsonKey()
  List<QuickCard> get cards {
    if (_cards is EqualUnmodifiableListView) return _cards;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_cards);
  }

  @override
  @JsonKey()
  final int selectedIndex;
  @override
  @JsonKey()
  final bool loading;
  @override
  final String? error;

  @override
  String toString() {
    return 'QuickCardState(cards: $cards, selectedIndex: $selectedIndex, loading: $loading, error: $error)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$QuickCardStateImpl &&
            const DeepCollectionEquality().equals(other._cards, _cards) &&
            (identical(other.selectedIndex, selectedIndex) ||
                other.selectedIndex == selectedIndex) &&
            (identical(other.loading, loading) || other.loading == loading) &&
            (identical(other.error, error) || other.error == error));
  }

  @override
  int get hashCode => Object.hash(
    runtimeType,
    const DeepCollectionEquality().hash(_cards),
    selectedIndex,
    loading,
    error,
  );

  /// Create a copy of QuickCardState
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$QuickCardStateImplCopyWith<_$QuickCardStateImpl> get copyWith =>
      __$$QuickCardStateImplCopyWithImpl<_$QuickCardStateImpl>(
        this,
        _$identity,
      );
}

abstract class _QuickCardState implements QuickCardState {
  const factory _QuickCardState({
    final List<QuickCard> cards,
    final int selectedIndex,
    final bool loading,
    final String? error,
  }) = _$QuickCardStateImpl;

  @override
  List<QuickCard> get cards;
  @override
  int get selectedIndex;
  @override
  bool get loading;
  @override
  String? get error;

  /// Create a copy of QuickCardState
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$QuickCardStateImplCopyWith<_$QuickCardStateImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
