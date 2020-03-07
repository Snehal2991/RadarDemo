Radar-

Geofence

How it works
Geofencing works in the foreground and in the background. All geofencing and event generation happens server-side. This allows Radar geofencing to be more powerful than native iOS or Android geofencing, with cross-platform support for unlimited geofences, polygon geofences, and stop detection.




In the background, the SDK will wake up while the user is moving (usually every 3-5 minutes), then shut down when the user stops (usually within 5-10 minutes). To save battery, the SDK will not wake up when stopped, and the user must move at least 100 meters from a stop to wake up the SDK.


For maximum reliability, create geofences with a radius of 100 meters or bigger for circles or an area of 10,000 square meters or bigger for polygons, and add padding to compensate for location accuracy. While you can create geofences with a smaller size, events may be missed or delayed.


All geofence events have confidence levels. Confidence levels range from 1 (low) to 3 (high).
When geofences do not overlap, confidence will always be high. When geofences do overlap, confidence will be medium or low for geofences with a radius of 200 meters or less, depending on the user's location, the user's location accuracy, and the geometry of the geofence.
You may decide to ignore some events based on confidence levels.



Radar generates a geofence entry event if a user enters a geofence (if stop detection is off) or stops in a geofence (if stop detection is on) with sufficient confidence, then a geofence exit event when the user leaves the geofence with sufficient confidence


Stop detection
When Geofence Stop Detection is on, Radar will understand the difference between a user walking or driving through a geofence and stopping in a geofence, and will only generate a geofence entry event when a user stops in a geofence. Turn on Geofence Stop Detection globally on the Settings page, under Geofences.


