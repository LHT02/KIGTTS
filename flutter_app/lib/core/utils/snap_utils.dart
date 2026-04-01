/// Utility for slider snap behavior.
/// Playback gain snaps to 100% when within ±20% range.
double snapToTarget({
  required double value,
  required double target,
  required double snapRange,
}) {
  if ((value - target).abs() <= snapRange) {
    return target;
  }
  return value;
}

/// Snap playback gain to 100% when within 80-120 range.
int snapPlaybackGain(int percent) {
  if (percent >= 80 && percent <= 120) {
    return 100;
  }
  return percent;
}
