/**
 * Changelog
 *
 * 2019-10-28 nproth
 *
 * * Implement PinTileService which provides a Quick Settings Tile to the user which opens the app.
 */

package de.nproth.pin;

import android.annotation.TargetApi;
import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;


/**
 * Provides a Settings Tile to the user which opens p!n.
 * Tiles are not supported before Api 24.
 */
@TargetApi(24)
public class PinTileService extends TileService {

    /**
     * Launches {@link NoteActivity} when the Quick Settings Tile is clicked
     */
    @Override
    public void onClick() {
        super.onClick();

        Intent i = new Intent(this, NoteActivity.class);
        // Required to start Activity from outside of an Activity context
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityAndCollapse(i);
    }
}
