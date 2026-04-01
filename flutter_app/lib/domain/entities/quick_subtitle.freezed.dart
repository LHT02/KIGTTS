// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'quick_subtitle.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
  'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models',
);

QuickSubtitleItem _$QuickSubtitleItemFromJson(Map<String, dynamic> json) {
  return _QuickSubtitleItem.fromJson(json);
}

/// @nodoc
mixin _$QuickSubtitleItem {
  String get id => throw _privateConstructorUsedError;
  String get text => throw _privateConstructorUsedError;
  int get order => throw _privateConstructorUsedError;

  /// Serializes this QuickSubtitleItem to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of QuickSubtitleItem
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $QuickSubtitleItemCopyWith<QuickSubtitleItem> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $QuickSubtitleItemCopyWith<$Res> {
  factory $QuickSubtitleItemCopyWith(
    QuickSubtitleItem value,
    $Res Function(QuickSubtitleItem) then,
  ) = _$QuickSubtitleItemCopyWithImpl<$Res, QuickSubtitleItem>;
  @useResult
  $Res call({String id, String text, int order});
}

/// @nodoc
class _$QuickSubtitleItemCopyWithImpl<$Res, $Val extends QuickSubtitleItem>
    implements $QuickSubtitleItemCopyWith<$Res> {
  _$QuickSubtitleItemCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of QuickSubtitleItem
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({Object? id = null, Object? text = null, Object? order = null}) {
    return _then(
      _value.copyWith(
            id: null == id
                ? _value.id
                : id // ignore: cast_nullable_to_non_nullable
                      as String,
            text: null == text
                ? _value.text
                : text // ignore: cast_nullable_to_non_nullable
                      as String,
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
abstract class _$$QuickSubtitleItemImplCopyWith<$Res>
    implements $QuickSubtitleItemCopyWith<$Res> {
  factory _$$QuickSubtitleItemImplCopyWith(
    _$QuickSubtitleItemImpl value,
    $Res Function(_$QuickSubtitleItemImpl) then,
  ) = __$$QuickSubtitleItemImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({String id, String text, int order});
}

/// @nodoc
class __$$QuickSubtitleItemImplCopyWithImpl<$Res>
    extends _$QuickSubtitleItemCopyWithImpl<$Res, _$QuickSubtitleItemImpl>
    implements _$$QuickSubtitleItemImplCopyWith<$Res> {
  __$$QuickSubtitleItemImplCopyWithImpl(
    _$QuickSubtitleItemImpl _value,
    $Res Function(_$QuickSubtitleItemImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of QuickSubtitleItem
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({Object? id = null, Object? text = null, Object? order = null}) {
    return _then(
      _$QuickSubtitleItemImpl(
        id: null == id
            ? _value.id
            : id // ignore: cast_nullable_to_non_nullable
                  as String,
        text: null == text
            ? _value.text
            : text // ignore: cast_nullable_to_non_nullable
                  as String,
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
class _$QuickSubtitleItemImpl implements _QuickSubtitleItem {
  const _$QuickSubtitleItemImpl({
    required this.id,
    required this.text,
    this.order = 0,
  });

  factory _$QuickSubtitleItemImpl.fromJson(Map<String, dynamic> json) =>
      _$$QuickSubtitleItemImplFromJson(json);

  @override
  final String id;
  @override
  final String text;
  @override
  @JsonKey()
  final int order;

  @override
  String toString() {
    return 'QuickSubtitleItem(id: $id, text: $text, order: $order)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$QuickSubtitleItemImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.text, text) || other.text == text) &&
            (identical(other.order, order) || other.order == order));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, text, order);

  /// Create a copy of QuickSubtitleItem
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$QuickSubtitleItemImplCopyWith<_$QuickSubtitleItemImpl> get copyWith =>
      __$$QuickSubtitleItemImplCopyWithImpl<_$QuickSubtitleItemImpl>(
        this,
        _$identity,
      );

  @override
  Map<String, dynamic> toJson() {
    return _$$QuickSubtitleItemImplToJson(this);
  }
}

abstract class _QuickSubtitleItem implements QuickSubtitleItem {
  const factory _QuickSubtitleItem({
    required final String id,
    required final String text,
    final int order,
  }) = _$QuickSubtitleItemImpl;

  factory _QuickSubtitleItem.fromJson(Map<String, dynamic> json) =
      _$QuickSubtitleItemImpl.fromJson;

  @override
  String get id;
  @override
  String get text;
  @override
  int get order;

  /// Create a copy of QuickSubtitleItem
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$QuickSubtitleItemImplCopyWith<_$QuickSubtitleItemImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

QuickSubtitleGroup _$QuickSubtitleGroupFromJson(Map<String, dynamic> json) {
  return _QuickSubtitleGroup.fromJson(json);
}

/// @nodoc
mixin _$QuickSubtitleGroup {
  String get id => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  List<QuickSubtitleItem> get items => throw _privateConstructorUsedError;

  /// Serializes this QuickSubtitleGroup to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of QuickSubtitleGroup
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $QuickSubtitleGroupCopyWith<QuickSubtitleGroup> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $QuickSubtitleGroupCopyWith<$Res> {
  factory $QuickSubtitleGroupCopyWith(
    QuickSubtitleGroup value,
    $Res Function(QuickSubtitleGroup) then,
  ) = _$QuickSubtitleGroupCopyWithImpl<$Res, QuickSubtitleGroup>;
  @useResult
  $Res call({String id, String name, List<QuickSubtitleItem> items});
}

/// @nodoc
class _$QuickSubtitleGroupCopyWithImpl<$Res, $Val extends QuickSubtitleGroup>
    implements $QuickSubtitleGroupCopyWith<$Res> {
  _$QuickSubtitleGroupCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of QuickSubtitleGroup
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({Object? id = null, Object? name = null, Object? items = null}) {
    return _then(
      _value.copyWith(
            id: null == id
                ? _value.id
                : id // ignore: cast_nullable_to_non_nullable
                      as String,
            name: null == name
                ? _value.name
                : name // ignore: cast_nullable_to_non_nullable
                      as String,
            items: null == items
                ? _value.items
                : items // ignore: cast_nullable_to_non_nullable
                      as List<QuickSubtitleItem>,
          )
          as $Val,
    );
  }
}

/// @nodoc
abstract class _$$QuickSubtitleGroupImplCopyWith<$Res>
    implements $QuickSubtitleGroupCopyWith<$Res> {
  factory _$$QuickSubtitleGroupImplCopyWith(
    _$QuickSubtitleGroupImpl value,
    $Res Function(_$QuickSubtitleGroupImpl) then,
  ) = __$$QuickSubtitleGroupImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({String id, String name, List<QuickSubtitleItem> items});
}

/// @nodoc
class __$$QuickSubtitleGroupImplCopyWithImpl<$Res>
    extends _$QuickSubtitleGroupCopyWithImpl<$Res, _$QuickSubtitleGroupImpl>
    implements _$$QuickSubtitleGroupImplCopyWith<$Res> {
  __$$QuickSubtitleGroupImplCopyWithImpl(
    _$QuickSubtitleGroupImpl _value,
    $Res Function(_$QuickSubtitleGroupImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of QuickSubtitleGroup
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({Object? id = null, Object? name = null, Object? items = null}) {
    return _then(
      _$QuickSubtitleGroupImpl(
        id: null == id
            ? _value.id
            : id // ignore: cast_nullable_to_non_nullable
                  as String,
        name: null == name
            ? _value.name
            : name // ignore: cast_nullable_to_non_nullable
                  as String,
        items: null == items
            ? _value._items
            : items // ignore: cast_nullable_to_non_nullable
                  as List<QuickSubtitleItem>,
      ),
    );
  }
}

/// @nodoc
@JsonSerializable()
class _$QuickSubtitleGroupImpl implements _QuickSubtitleGroup {
  const _$QuickSubtitleGroupImpl({
    required this.id,
    required this.name,
    final List<QuickSubtitleItem> items = const [],
  }) : _items = items;

  factory _$QuickSubtitleGroupImpl.fromJson(Map<String, dynamic> json) =>
      _$$QuickSubtitleGroupImplFromJson(json);

  @override
  final String id;
  @override
  final String name;
  final List<QuickSubtitleItem> _items;
  @override
  @JsonKey()
  List<QuickSubtitleItem> get items {
    if (_items is EqualUnmodifiableListView) return _items;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_items);
  }

  @override
  String toString() {
    return 'QuickSubtitleGroup(id: $id, name: $name, items: $items)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$QuickSubtitleGroupImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            const DeepCollectionEquality().equals(other._items, _items));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
    runtimeType,
    id,
    name,
    const DeepCollectionEquality().hash(_items),
  );

  /// Create a copy of QuickSubtitleGroup
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$QuickSubtitleGroupImplCopyWith<_$QuickSubtitleGroupImpl> get copyWith =>
      __$$QuickSubtitleGroupImplCopyWithImpl<_$QuickSubtitleGroupImpl>(
        this,
        _$identity,
      );

  @override
  Map<String, dynamic> toJson() {
    return _$$QuickSubtitleGroupImplToJson(this);
  }
}

abstract class _QuickSubtitleGroup implements QuickSubtitleGroup {
  const factory _QuickSubtitleGroup({
    required final String id,
    required final String name,
    final List<QuickSubtitleItem> items,
  }) = _$QuickSubtitleGroupImpl;

  factory _QuickSubtitleGroup.fromJson(Map<String, dynamic> json) =
      _$QuickSubtitleGroupImpl.fromJson;

  @override
  String get id;
  @override
  String get name;
  @override
  List<QuickSubtitleItem> get items;

  /// Create a copy of QuickSubtitleGroup
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$QuickSubtitleGroupImplCopyWith<_$QuickSubtitleGroupImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

QuickSubtitleConfig _$QuickSubtitleConfigFromJson(Map<String, dynamic> json) {
  return _QuickSubtitleConfig.fromJson(json);
}

/// @nodoc
mixin _$QuickSubtitleConfig {
  List<QuickSubtitleGroup> get groups => throw _privateConstructorUsedError;
  double get fontSize => throw _privateConstructorUsedError;
  bool get bold => throw _privateConstructorUsedError;
  bool get centered => throw _privateConstructorUsedError;
  bool get playOnSend => throw _privateConstructorUsedError;

  /// Serializes this QuickSubtitleConfig to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of QuickSubtitleConfig
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $QuickSubtitleConfigCopyWith<QuickSubtitleConfig> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $QuickSubtitleConfigCopyWith<$Res> {
  factory $QuickSubtitleConfigCopyWith(
    QuickSubtitleConfig value,
    $Res Function(QuickSubtitleConfig) then,
  ) = _$QuickSubtitleConfigCopyWithImpl<$Res, QuickSubtitleConfig>;
  @useResult
  $Res call({
    List<QuickSubtitleGroup> groups,
    double fontSize,
    bool bold,
    bool centered,
    bool playOnSend,
  });
}

/// @nodoc
class _$QuickSubtitleConfigCopyWithImpl<$Res, $Val extends QuickSubtitleConfig>
    implements $QuickSubtitleConfigCopyWith<$Res> {
  _$QuickSubtitleConfigCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of QuickSubtitleConfig
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? groups = null,
    Object? fontSize = null,
    Object? bold = null,
    Object? centered = null,
    Object? playOnSend = null,
  }) {
    return _then(
      _value.copyWith(
            groups: null == groups
                ? _value.groups
                : groups // ignore: cast_nullable_to_non_nullable
                      as List<QuickSubtitleGroup>,
            fontSize: null == fontSize
                ? _value.fontSize
                : fontSize // ignore: cast_nullable_to_non_nullable
                      as double,
            bold: null == bold
                ? _value.bold
                : bold // ignore: cast_nullable_to_non_nullable
                      as bool,
            centered: null == centered
                ? _value.centered
                : centered // ignore: cast_nullable_to_non_nullable
                      as bool,
            playOnSend: null == playOnSend
                ? _value.playOnSend
                : playOnSend // ignore: cast_nullable_to_non_nullable
                      as bool,
          )
          as $Val,
    );
  }
}

/// @nodoc
abstract class _$$QuickSubtitleConfigImplCopyWith<$Res>
    implements $QuickSubtitleConfigCopyWith<$Res> {
  factory _$$QuickSubtitleConfigImplCopyWith(
    _$QuickSubtitleConfigImpl value,
    $Res Function(_$QuickSubtitleConfigImpl) then,
  ) = __$$QuickSubtitleConfigImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({
    List<QuickSubtitleGroup> groups,
    double fontSize,
    bool bold,
    bool centered,
    bool playOnSend,
  });
}

/// @nodoc
class __$$QuickSubtitleConfigImplCopyWithImpl<$Res>
    extends _$QuickSubtitleConfigCopyWithImpl<$Res, _$QuickSubtitleConfigImpl>
    implements _$$QuickSubtitleConfigImplCopyWith<$Res> {
  __$$QuickSubtitleConfigImplCopyWithImpl(
    _$QuickSubtitleConfigImpl _value,
    $Res Function(_$QuickSubtitleConfigImpl) _then,
  ) : super(_value, _then);

  /// Create a copy of QuickSubtitleConfig
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? groups = null,
    Object? fontSize = null,
    Object? bold = null,
    Object? centered = null,
    Object? playOnSend = null,
  }) {
    return _then(
      _$QuickSubtitleConfigImpl(
        groups: null == groups
            ? _value._groups
            : groups // ignore: cast_nullable_to_non_nullable
                  as List<QuickSubtitleGroup>,
        fontSize: null == fontSize
            ? _value.fontSize
            : fontSize // ignore: cast_nullable_to_non_nullable
                  as double,
        bold: null == bold
            ? _value.bold
            : bold // ignore: cast_nullable_to_non_nullable
                  as bool,
        centered: null == centered
            ? _value.centered
            : centered // ignore: cast_nullable_to_non_nullable
                  as bool,
        playOnSend: null == playOnSend
            ? _value.playOnSend
            : playOnSend // ignore: cast_nullable_to_non_nullable
                  as bool,
      ),
    );
  }
}

/// @nodoc
@JsonSerializable()
class _$QuickSubtitleConfigImpl implements _QuickSubtitleConfig {
  const _$QuickSubtitleConfigImpl({
    final List<QuickSubtitleGroup> groups = const [],
    this.fontSize = 28.0,
    this.bold = false,
    this.centered = true,
    this.playOnSend = true,
  }) : _groups = groups;

  factory _$QuickSubtitleConfigImpl.fromJson(Map<String, dynamic> json) =>
      _$$QuickSubtitleConfigImplFromJson(json);

  final List<QuickSubtitleGroup> _groups;
  @override
  @JsonKey()
  List<QuickSubtitleGroup> get groups {
    if (_groups is EqualUnmodifiableListView) return _groups;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_groups);
  }

  @override
  @JsonKey()
  final double fontSize;
  @override
  @JsonKey()
  final bool bold;
  @override
  @JsonKey()
  final bool centered;
  @override
  @JsonKey()
  final bool playOnSend;

  @override
  String toString() {
    return 'QuickSubtitleConfig(groups: $groups, fontSize: $fontSize, bold: $bold, centered: $centered, playOnSend: $playOnSend)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$QuickSubtitleConfigImpl &&
            const DeepCollectionEquality().equals(other._groups, _groups) &&
            (identical(other.fontSize, fontSize) ||
                other.fontSize == fontSize) &&
            (identical(other.bold, bold) || other.bold == bold) &&
            (identical(other.centered, centered) ||
                other.centered == centered) &&
            (identical(other.playOnSend, playOnSend) ||
                other.playOnSend == playOnSend));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
    runtimeType,
    const DeepCollectionEquality().hash(_groups),
    fontSize,
    bold,
    centered,
    playOnSend,
  );

  /// Create a copy of QuickSubtitleConfig
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$QuickSubtitleConfigImplCopyWith<_$QuickSubtitleConfigImpl> get copyWith =>
      __$$QuickSubtitleConfigImplCopyWithImpl<_$QuickSubtitleConfigImpl>(
        this,
        _$identity,
      );

  @override
  Map<String, dynamic> toJson() {
    return _$$QuickSubtitleConfigImplToJson(this);
  }
}

abstract class _QuickSubtitleConfig implements QuickSubtitleConfig {
  const factory _QuickSubtitleConfig({
    final List<QuickSubtitleGroup> groups,
    final double fontSize,
    final bool bold,
    final bool centered,
    final bool playOnSend,
  }) = _$QuickSubtitleConfigImpl;

  factory _QuickSubtitleConfig.fromJson(Map<String, dynamic> json) =
      _$QuickSubtitleConfigImpl.fromJson;

  @override
  List<QuickSubtitleGroup> get groups;
  @override
  double get fontSize;
  @override
  bool get bold;
  @override
  bool get centered;
  @override
  bool get playOnSend;

  /// Create a copy of QuickSubtitleConfig
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$QuickSubtitleConfigImplCopyWith<_$QuickSubtitleConfigImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
