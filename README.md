# ![App Icon](app/src/main/res/mipmap-mdpi/ic_launcher.png) p!n

A minimalistic note-taking app utilizing your phone's notification area:

* Take notes and save them as notifications
* Edit pinned notices
* Delete pins
* Hide notes for a specific period of time

The app was built with __Material Design__ in mind.

All notes are stored in the app's database and survive reboots.

<a href="https://f-droid.org/packages/de.nproth.pin/"><img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="75"></a>

## How to create a new pin

Start the app to create a new pin.

- Enter the text you want to display in the notification.

- There's a number in a semicircle  - that's the number of hours:minutes that
  the pin will wait for if snoozed. Tap it to change the time.
  
- The semicircle acts as a slider and adjusts the snooze time.

- Click the circular pin button to create your pin.

- Nb. the small clock icon on the right will create the pin and immediately
  snooze it.

## Pin notifications

When a pin is shown as a notification you can:

- tap **Done** - this will delete the pin.

- tap **Snooze** - this will make it go away for the configured time period.

- tap **Edit** - use this to change the snooze time or title of the pin.

## Show all pins

You can cause all pins - including the snoozed ones - to show as notifications
by *pressing and holding* the semicircular snooze time button on the main
interface.

## Locking pins

Pins are unlocked by default, that means that you can swipe the group
notification away which snoozes all pins at once.

This also happens if all notifications are cleared. To prevent this you can
*lock* your pins so that the group notification and the individual notes it
contains can't be swiped away.

## Troubleshooting

If your pins do not reappear directly after start-up, open the app because on some devices __p!n__ may not be able to start in background.
In this case try to enable 'Autostart' or disable some battery optimizations for this application.

A good resource for more information on this topic and solutions for various devices I have found [here](https://dontkillmyapp.com/).
