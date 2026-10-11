#!/usr/bin/env bash
# Create publication copies; never modify the source recordings/screenshots.
set -euo pipefail

if (( $# < 5 || $# > 6 )); then
  echo 'Usage: bash scripts/prepare-release-media.sh ON_DEVICE_MP4 QGC_MP4 PHONE_MP4 SCREENSHOT_DIRECTORY OUTPUT_DIRECTORY [QGC_TRIM_SECONDS]' >&2
  exit 2
fi
on_device=$1
qgc=$2
phone=$3
shots=$4
output=$5
offset=${6:-1.3}
[[ $offset =~ ^[0-9]+([.][0-9]+)?$ ]] || { echo 'Invalid QGC trim offset' >&2; exit 2; }
command -v ffmpeg >/dev/null
command -v ffprobe >/dev/null

for input in "$on_device" "$qgc" "$phone" \
  "$shots/Screenshot_20261012-002847_MAVLab.jpg" \
  "$shots/Screenshot_20261012-002855_MAVLab.jpg" \
  "$shots/Screenshot_20261012-002906_MAVLab.jpg" \
  "$shots/Screenshot_20261012-002916_MAVLab.jpg"; do
  [[ -r $input ]] || { echo "Cannot read: $input" >&2; exit 1; }
done
mkdir -p "$output/screenshots"
for name in mavlab-on-device.mp4 mavlab-qgc-demo.mp4 mavlab-on-device-poster.jpg mavlab-qgc-poster.jpg \
  screenshots/cockpit.jpg screenshots/control.jpg screenshots/sim.jpg screenshots/ops.jpg; do
  [[ ! -e "$output/$name" ]] || { echo "Refusing to replace: $output/$name" >&2; exit 1; }
done

# An interrupted render stays here for inspection; existing deliverables are untouched.
stage=$(mktemp -d "$output/.render-XXXXXX")
echo "Rendering into $stage"

# Original portrait aspect ratio, smooth 60 fps, original recorded phone audio.
ffmpeg -hide_banner -loglevel warning -n -threads 2 -i "$on_device" \
  -filter_threads 2 -vf 'fps=60,scale=720:1520:flags=lanczos,setsar=1' \
  -map 0:v:0 -map 0:a:0 -c:a copy -shortest -map_metadata -1 -map_chapters -1 \
  -c:v libx264 -preset fast -crf 22 -threads 2 -pix_fmt yuv420p \
  -movflags +faststart "$stage/mavlab-on-device.mp4"

# Approximate event alignment: source QGC t=1.3 and source phone t=0.
# Keep QGC at full 1080p, with the whole portrait phone frame overlaid on the right.
# End when either recording ends; never loop or freeze a view to extend the clip.
ffmpeg -hide_banner -loglevel warning -n -threads 2 -ss "$offset" -i "$qgc" \
  -threads 2 -i "$phone" -filter_complex_threads 2 \
  -filter_complex '[0:v]setpts=PTS-STARTPTS,fps=30,scale=1920:1080,setsar=1[bg];[1:v]setpts=PTS-STARTPTS,fps=30,scale=432:912:flags=lanczos,setsar=1,pad=436:916:2:2:color=0x60A5FA[phone];[bg][phone]overlay=x=1456:y=112:shortest=1[v]' \
  -map '[v]' -map 1:a:0 -c:a copy -shortest -map_metadata -1 -map_chapters -1 \
  -c:v libx264 -preset fast -crf 22 -threads 2 -pix_fmt yuv420p \
  -movflags +faststart "$stage/mavlab-qgc-demo.mp4"

ffmpeg -hide_banner -loglevel warning -n -threads 2 -ss 41 \
  -i "$stage/mavlab-on-device.mp4" -frames:v 1 -q:v 2 "$stage/mavlab-on-device-poster.jpg"
ffmpeg -hide_banner -loglevel warning -n -threads 2 -ss 78 \
  -i "$stage/mavlab-qgc-demo.mp4" -frames:v 1 -q:v 2 "$stage/mavlab-qgc-poster.jpg"

# These screenshots are exact, uncropped copies of the supplied JPEGs.
cp "$shots/Screenshot_20261012-002847_MAVLab.jpg" "$stage/cockpit.jpg"
cp "$shots/Screenshot_20261012-002855_MAVLab.jpg" "$stage/control.jpg"
cp "$shots/Screenshot_20261012-002906_MAVLab.jpg" "$stage/sim.jpg"
cp "$shots/Screenshot_20261012-002916_MAVLab.jpg" "$stage/ops.jpg"
for name in mavlab-on-device.mp4 mavlab-qgc-demo.mp4 mavlab-on-device-poster.jpg mavlab-qgc-poster.jpg; do
  mv -n "$stage/$name" "$output/$name"
done
for name in cockpit control sim ops; do
  mv -n "$stage/$name.jpg" "$output/screenshots/$name.jpg"
done
rmdir "$stage"
echo "Ready: $output"
