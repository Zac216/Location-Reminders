package gocrew.locationreminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class autoStart extends BroadcastReceiver {

    public void onReceive(Context context, Intent arg1)
    {
        Intent intent = new Intent(context, RSSPullService.class);
        context.startService(intent);

    }
}
